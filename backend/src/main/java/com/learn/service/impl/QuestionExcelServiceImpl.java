package com.learn.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.dto.QuestionExcelDTO;
import com.learn.entity.Question;
import com.learn.entity.QuestionCategory;
import com.learn.entity.QuestionOption;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.QuestionOptionMapper;
import com.learn.service.QuestionExcelService;
import com.learn.util.Md5Util;
import com.learn.vo.ImportResultVO;
import com.learn.vo.QuestionAdminVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Excel 导入导出实现（文档 4.4 全部规则落地）。
 *
 * 导入幂等：查重键 = (分类, 题干去空白 MD5)；跨分类不算重复；
 * 批内 Set 挡住同一文件内的重复行；批量查库一次拿到已存在映射，不逐条循环查。
 * 策略：SKIP 跳过 / OVERWRITE 更新（保留 id 与点赞数）/ ERROR 记为失败行。
 * 流式读取每 500 条一批入库（单批同一事务，见 QuestionBatchWriter），失败行不中断整体流程。
 */
@Slf4j
@Service
public class QuestionExcelServiceImpl implements QuestionExcelService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ROWS = 5000;

    private static final Map<String, Integer> TYPE_MAP = Map.of("单选", 1, "多选", 2, "判断", 3, "简答", 4);
    private static final Map<Integer, String> TYPE_NAME = Map.of(1, "单选", 2, "多选", 3, "判断", 4, "简答");
    private static final Map<String, Integer> DIFF_MAP = Map.of("简单", 1, "中等", 2, "困难", 3);
    private static final Map<Integer, String> DIFF_NAME = Map.of(1, "简单", 2, "中等", 3, "困难");

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionOptionMapper optionMapper;

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Resource
    private QuestionBatchWriter batchWriter;

    // ---------- 模板 ----------

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("题目导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            QuestionExcelDTO sample = new QuestionExcelDTO();
            sample.setCategoryName("Java基础");
            sample.setTypeName("单选");
            sample.setDifficultyName("中等");
            sample.setTitle("示例题干（支持 Markdown）");
            sample.setOptions("A.选项一|B.选项二|C.选项三|D.选项四");
            sample.setAnswer("A");
            sample.setAnalysis("示例解析");
            sample.setVipFlag("0");
            EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class)
                    .sheet("模板")
                    .doWrite(List.of(sample));
        } catch (IOException e) {
            throw new BizException(ResultCode.SERVER_ERROR, "模板下载失败");
        }
    }

    // ---------- 导入 ----------

    @Override
    public ImportResultVO importExcel(MultipartFile file, String duplicateStrategy) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "请选择导入文件");
        }
        // 仅允许 .xlsx：校验文件头（PK zip magic）而非仅扩展名（文档 4.4）
        try {
            byte[] head = new byte[2];
            int n = file.getInputStream().readNBytes(head, 0, 2);
            if (n < 2 || head[0] != 0x50 || head[1] != 0x4B) {
                throw new BizException(ResultCode.PARAM_ERROR, "仅支持 .xlsx 文件");
            }
        } catch (IOException e) {
            throw new BizException(ResultCode.SERVER_ERROR, "文件读取失败");
        }
        final String strategy = "OVERWRITE".equals(duplicateStrategy) || "ERROR".equals(duplicateStrategy)
                ? duplicateStrategy : "SKIP";
        ImportResultVO result = new ImportResultVO();
        try {
            BatchListener listener = new BatchListener(strategy, result);
            EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class, listener)
                    .sheet()
                    .doRead();
            listener.flushTail();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("excel import failed", e);
            throw new BizException(ResultCode.PARAM_ERROR, "文件解析失败，请使用下载的模板填写");
        }
        result.setFailCount(result.getFailures().size());
        return result;
    }

    /**
     * EasyExcel 流式读取监听器：每 500 条交由 QuestionBatchWriter 入库（单批同一事务），
     * 行级失败收集 {行号, 原因} 不中断整体流程。
     */
    private class BatchListener extends AnalysisEventListener<QuestionExcelDTO> {

        private final String strategy;
        private final ImportResultVO result;
        private final List<RowWrapper> buffer = new ArrayList<>();
        private boolean overflowReported = false;

        BatchListener(String strategy, ImportResultVO result) {
            this.strategy = strategy;
            this.result = result;
        }

        @Override
        public void invoke(QuestionExcelDTO dto, AnalysisContext context) {
            // Excel 人类行号（含表头）
            int row = context.readRowHolder().getRowIndex() + 1;
            if (row > MAX_ROWS + 1) {
                // 超过 5000 行：不再入库，但明确回传失败明细（文档 4.4 限制），避免静默丢行
                if (!overflowReported) {
                    overflowReported = true;
                    result.getFailures().add(new ImportResultVO.FailureItem(row,
                            "超出单次导入上限 " + MAX_ROWS + " 行，本行及之后的行未处理"));
                }
                return;
            }
            buffer.add(new RowWrapper(row, dto));
            if (buffer.size() >= BATCH_SIZE) {
                flush();
            }
        }

        void flushTail() {
            if (!buffer.isEmpty()) {
                flush();
            }
        }

        private void flush() {
            processBatch(new ArrayList<>(buffer), result, strategy);
            buffer.clear();
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 尾批交给 flushTail，便于判断 stopped 状态
        }
    }

    /** 行包装：原始行号 + DTO */
    private record RowWrapper(int row, QuestionExcelDTO dto) {
    }

    /**
     * 一批（<= 500 条）处理：
     * 第一步：行校验与归一化（只读 DB），失败行记入 failures；
     * 第二步：批内 Set 查重（文档 4.4 要点 2）；
     * 第三步：一次性查库拿已存在映射（要点 1），整批写入同一事务（QuestionBatchWriter）；
     * 批级系统异常 → 整批回滚并把该批行号范围记入失败（文档 4.4）。
     */
    private void processBatch(List<RowWrapper> rows, ImportResultVO result, String strategy) {
        Map<String, QuestionCategory> categoryCache = new HashMap<>();
        List<QuestionBatchWriter.ValidRow> validRows = new ArrayList<>();
        Set<String> batchSeen = new HashSet<>();

        for (RowWrapper wrapper : rows) {
            int row = wrapper.row();
            QuestionExcelDTO dto = wrapper.dto();
            try {
                if (dto == null || isBlank(dto.getTitle())) {
                    throw new IllegalArgumentException("题干为空");
                }
                Integer type = TYPE_MAP.get(trim(dto.getTypeName()));
                if (type == null) {
                    throw new IllegalArgumentException("题型不合法（须为 单选/多选/判断/简答）");
                }
                Integer difficulty = DIFF_MAP.get(trim(dto.getDifficultyName()));
                if (difficulty == null) {
                    difficulty = 2;
                }
                final Integer finalType = type;
                QuestionCategory category = categoryCache.computeIfAbsent(trim(dto.getCategoryName()), name -> {
                    // 分类名无唯一约束，重名时 selectOne 会抛 TooManyResultsException 并伪装成「行数据解析失败」，
                    // 这里改用 selectList 并给出明确的重名提示。
                    List<QuestionCategory> matched = categoryMapper.selectList(new LambdaQueryWrapper<QuestionCategory>()
                            .eq(QuestionCategory::getName, name));
                    if (matched.isEmpty()) {
                        throw new IllegalArgumentException("分类不存在：" + name);
                    }
                    if (matched.size() > 1) {
                        throw new IllegalArgumentException("分类名称重复，无法唯一定位：" + name + "，请先重命名");
                    }
                    QuestionCategory c = matched.get(0);
                    if (c.getParentId() == null || c.getParentId() == 0) {
                        throw new IllegalArgumentException("分类必须为二级（叶子）分类：" + name);
                    }
                    return c;
                });
                List<QuestionExcelDTO.OptionParsed> options = new ArrayList<>();
                String answer = "";
                if (finalType != 4) {
                    if (isBlank(dto.getOptions())) {
                        throw new IllegalArgumentException("客观题选项为空");
                    }
                    if (isBlank(dto.getAnswer())) {
                        throw new IllegalArgumentException("客观题答案为空");
                    }
                    // 选项格式：A.内容|B.内容|C.内容
                    String[] parts = dto.getOptions().trim().split("\\|");
                    Set<String> codes = new HashSet<>();
                    int sort = 0;
                    for (String p : parts) {
                        String t = p.trim();
                        int idx = t.indexOf('.');
                        if (idx != 1) {
                            throw new IllegalArgumentException("选项格式错误，须为 A.内容|B.内容");
                        }
                        String code = t.substring(0, idx).trim().toUpperCase();
                        String content = t.substring(idx + 1).trim();
                        if (code.length() != 1 || content.isEmpty() || !codes.add(code)) {
                            throw new IllegalArgumentException("选项格式错误或重复：" + p);
                        }
                        options.add(new QuestionExcelDTO.OptionParsed(code, content, sort++));
                    }
                    answer = dto.getAnswer().replaceAll("[^A-Za-z]", "").toUpperCase();
                    if (answer.isEmpty()) {
                        throw new IllegalArgumentException("客观题答案为空");
                    }
                    for (char ch : answer.toCharArray()) {
                        if (!codes.contains(String.valueOf(ch))) {
                            throw new IllegalArgumentException("答案 " + ch + " 不在选项范围内");
                        }
                    }
                    if (finalType == 2) {
                        // 多选归一化：按字符排序拼接（文档 4.3）
                        StringBuilder sb = new StringBuilder();
                        answer.chars().sorted().forEach(sb::appendCodePoint);
                        answer = sb.toString();
                    }
                }
                int isVip = "1".equals(trim(dto.getVipFlag())) ? 1 : 0;
                String md5 = Md5Util.titleMd5(dto.getTitle());
                String dedupKey = category.getId() + ":" + md5;
                if (!batchSeen.add(dedupKey)) {
                    // 同一文件内两行完全相同：只入库一条（文档 4.4）
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                validRows.add(new QuestionBatchWriter.ValidRow(row, category.getId(), finalType, difficulty,
                        dto.getTitle().trim(), md5, options, answer,
                        dto.getAnalysis() == null ? "" : dto.getAnalysis().trim(), isVip));
            } catch (IllegalArgumentException e) {
                result.getFailures().add(new ImportResultVO.FailureItem(row, e.getMessage()));
            } catch (Exception e) {
                log.warn("excel row {} parse failed", row, e);
                result.getFailures().add(new ImportResultVO.FailureItem(row, "行数据解析失败"));
            }
        }
        if (validRows.isEmpty()) {
            result.setFailCount(result.getFailures().size());
            return;
        }
        // 一次性查库拿已存在映射（文档 4.4 要点 1）
        List<Long> categoryIds = validRows.stream().map(QuestionBatchWriter.ValidRow::categoryId).distinct().toList();
        List<String> md5s = validRows.stream().map(QuestionBatchWriter.ValidRow::titleMd5).distinct().toList();
        Map<String, Question> existing = new LinkedHashMap<>();
        for (Question q : questionMapper.selectByCategoryAndMd5(categoryIds, md5s)) {
            existing.put(q.getCategoryId() + ":" + q.getTitleMd5(), q);
        }
        int successBefore = result.getSuccessCount();
        int skippedBefore = result.getSkippedCount();
        int failuresBefore = result.getFailures().size();
        List<Integer> batchRows = validRows.stream().map(QuestionBatchWriter.ValidRow::row).toList();
        try {
            // 单批同一事务；SKIP/ERROR 的行级计数在事务内完成
            int written = batchWriter.writeBatch(validRows, existing, strategy, result);
            result.setSuccessCount(successBefore + written);
        } catch (Exception e) {
            log.warn("excel batch persist failed, rollback rows {}", batchRows, e);
            // 整批回滚：先清掉本批事务内已写入的残留计数/明细（例如 ERROR 策略的「题目重复」），
            // 再把该批全部行号记为失败（文档 4.4）。顺序不能颠倒，否则刚补的失败行会被一起清掉。
            result.getFailures().subList(failuresBefore, result.getFailures().size()).clear();
            for (Integer row : batchRows) {
                result.getFailures().add(new ImportResultVO.FailureItem(row, "入库失败，本批次已整体回滚"));
            }
            result.setSuccessCount(successBefore);
            result.setSkippedCount(skippedBefore);
        }
        result.setFailCount(result.getFailures().size());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------- 导出 ----------

    @Override
    public void export(Long categoryId, Integer isVip, Integer difficulty, Integer status,
                       HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("题目导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            com.alibaba.excel.ExcelWriter writer = EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class).build();
            WriteSheet sheet = EasyExcel.writerSheet(0, "题目导出").build();
            try (writer) {
                // 分批写出（每页 1000 条），避免大结果集 OOM（文档 4.4）
                long pageNum = 1;
                while (true) {
                    Page<QuestionAdminVO> page = new Page<>(pageNum, 1000);
                    IPage<QuestionAdminVO> data = questionMapper.selectAdminPage(page,
                            categoryId == null ? null : List.of(categoryId), null, null, isVip, difficulty, status);
                    if (data.getRecords().isEmpty()) {
                        break;
                    }
                    Map<Long, List<QuestionOption>> optMap = optionMapper
                            .selectByQuestionIds(data.getRecords().stream()
                                    .map(QuestionAdminVO::getId).toList()).stream()
                            .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
                    List<QuestionExcelDTO> rows = new ArrayList<>();
                    for (QuestionAdminVO vo : data.getRecords()) {
                        QuestionExcelDTO row = new QuestionExcelDTO();
                        row.setCategoryName(vo.getCategoryName());
                        row.setTypeName(TYPE_NAME.get(vo.getType()));
                        row.setDifficultyName(DIFF_NAME.get(vo.getDifficulty()));
                        row.setTitle(vo.getTitle());
                        StringBuilder sb = new StringBuilder();
                        for (QuestionOption o : optMap.getOrDefault(vo.getId(), List.of())) {
                            if (sb.length() > 0) {
                                sb.append("|");
                            }
                            sb.append(o.getOptionCode()).append(".").append(o.getOptionContent());
                        }
                        row.setOptions(sb.toString());
                        row.setAnswer(vo.getAnswer());
                        row.setAnalysis(vo.getAnalysis());
                        row.setVipFlag(vo.getIsVip() != null && vo.getIsVip() == 1 ? "1" : "0");
                        rows.add(row);
                    }
                    writer.write(rows, sheet);
                    if (data.getRecords().size() < 1000) {
                        break;
                    }
                    pageNum++;
                }
            }
        } catch (IOException e) {
            throw new BizException(ResultCode.SERVER_ERROR, "导出失败");
        }
    }
}
