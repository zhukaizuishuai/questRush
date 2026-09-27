package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.dto.QuestionSaveDTO;
import com.learn.entity.Question;
import com.learn.entity.QuestionCategory;
import com.learn.entity.QuestionOption;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.QuestionOptionMapper;
import com.learn.mapper.UserAnswerLogMapper;
import com.learn.mapper.UserMapper;
import com.learn.service.AdminQuestionService;
import com.learn.util.Md5Util;
import com.learn.vo.QuestionAdminVO;
import com.learn.vo.QuestionOptionVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理端题库服务实现（文档 3.3 / 6.4）。
 * 保存 / 更新时：校验分类必须是叶子；客观题答案必须落在选项范围内；
 * 多选答案入库前做「按字符排序拼接」归一化；同步维护 title_md5 与选项表。
 */
@Service
public class AdminQuestionServiceImpl implements AdminQuestionService {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionOptionMapper optionMapper;

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserAnswerLogMapper answerLogMapper;

    @Override
    public IPage<QuestionAdminVO> page(long pageNum, long pageSize, Long categoryId, String keyword,
                                       Integer isVip, Integer difficulty, Integer status) {
        List<Long> categoryIds = categoryId == null ? null : List.of(categoryId);
        Page<QuestionAdminVO> page = new Page<>(pageNum, Math.min(pageSize, 100));
        IPage<QuestionAdminVO> result = questionMapper.selectAdminPage(page, categoryIds, keyword, isVip, difficulty, status);
        fillOptions(result.getRecords());
        return result;
    }

    @Override
    public QuestionAdminVO detail(Long id) {
        QuestionAdminVO vo = questionMapper.selectAdminById(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "题目不存在");
        }
        fillOptions(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(QuestionSaveDTO dto) {
        validate(dto);
        Question question = new Question();
        applyDto(question, dto);
        questionMapper.insert(question);
        saveOptions(question.getId(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(QuestionSaveDTO dto) {
        if (dto.getId() == null || questionMapper.selectById(dto.getId()) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "题目不存在");
        }
        validate(dto);
        Question question = new Question();
        question.setId(dto.getId());
        applyDto(question, dto);
        questionMapper.updateById(question);
        // OVERWRITE 语义：旧选项全部逻辑删除再插入新选项（文档 4.4）
        optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, dto.getId()));
        saveOptions(dto.getId(), dto);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (questionMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "题目不存在");
        }
        Question update = new Question();
        update.setId(id);
        update.setStatus(status);
        questionMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        if (questionMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "题目不存在");
        }
        questionMapper.deleteById(id);
    }

    @Override
    public int recalcLike() {
        return questionMapper.recalcLikeCount();
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("questionCount", questionMapper.selectCount(null));
        result.put("vipUserCount", userMapper.countVipUsers());
        Double dailyAvg = answerLogMapper.selectDailyAvg();
        result.put("dailyAvgAnswers", dailyAvg == null ? 0.0 : Math.round(dailyAvg * 10.0) / 10.0);
        return result;
    }

    // ---------- 私有方法 ----------

    /** 题目落库前校验（文档 4.4 校验项的子集，单条录入同样执行） */
    private void validate(QuestionSaveDTO dto) {
        QuestionCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "分类不存在");
        }
        // 题目只允许挂叶子分类（文档 3.2）
        if (category.getParentId() == null || category.getParentId() == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "题目只能挂载在二级分类下");
        }
        Integer type = dto.getType();
        if (type != 4) {
            // 客观题：选项与答案非空，答案字符落在选项范围内
            if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "客观题必须提供选项");
            }
            String answer = dto.getAnswer() == null ? "" : dto.getAnswer().trim().toUpperCase();
            if (answer.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "客观题必须提供正确答案");
            }
            List<String> codes = dto.getOptions().stream()
                    .map(o -> o.getOptionCode().trim().toUpperCase()).toList();
            for (char c : answer.toCharArray()) {
                if (!codes.contains(String.valueOf(c))) {
                    throw new BizException(ResultCode.PARAM_ERROR, "答案 " + c + " 不在选项范围内");
                }
            }
        } else if (dto.getAnswerText() == null || dto.getAnswerText().isBlank()) {
            // 简答题建议提供参考答案（不强制，留宽松）
            dto.setAnswerText(dto.getAnswerText());
        }
    }

    private void applyDto(Question question, QuestionSaveDTO dto) {
        question.setCategoryId(dto.getCategoryId());
        question.setType(dto.getType());
        question.setDifficulty(dto.getDifficulty() == null ? 2 : dto.getDifficulty());
        question.setTitle(dto.getTitle());
        question.setAnswerText(dto.getAnswerText());
        question.setAnalysis(dto.getAnalysis());
        question.setIsVip(dto.getIsVip() == null ? 0 : dto.getIsVip());
        question.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getType() != 4) {
            // 多选答案入库前归一化：按字符排序拼接（文档 4.3）
            question.setAnswer(normalizeAnswer(dto.getAnswer(), dto.getType()));
        } else {
            question.setAnswer("");
        }
        question.setTitleMd5(Md5Util.titleMd5(dto.getTitle()));
    }

    private String normalizeAnswer(String answer, Integer type) {
        String s = answer == null ? "" : answer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (type != 2) {
            return s;
        }
        return s.chars().sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void saveOptions(Long questionId, QuestionSaveDTO dto) {
        if (dto.getType() == 4 || dto.getOptions() == null) {
            return;
        }
        int sort = 0;
        for (QuestionSaveDTO.OptionItem item : dto.getOptions()) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionCode(item.getOptionCode().trim().toUpperCase());
            option.setOptionContent(item.getOptionContent());
            option.setSort(sort++);
            optionMapper.insert(option);
        }
    }

    private void fillOptions(List<QuestionAdminVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(QuestionAdminVO::getId).toList();
        Map<Long, List<QuestionOption>> map = optionMapper.selectByQuestionIds(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(QuestionOption::getQuestionId));
        for (QuestionAdminVO vo : records) {
            List<QuestionOption> opts = map.getOrDefault(vo.getId(), new ArrayList<>());
            vo.setOptions(opts.stream()
                    .map(o -> new QuestionOptionVO(o.getOptionCode(), o.getOptionContent()))
                    .toList());
        }
    }
}
