package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.QuestionExcelDTO;
import com.learn.entity.Question;
import com.learn.entity.QuestionOption;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.QuestionOptionMapper;
import com.learn.vo.ImportResultVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Excel 导入单批写入器：独立 Bean 使 @Transactional 生效（文档 4.4「单批写入包在同一事务中，
 * 该批失败则整批回滚」）。SKIP / ERROR 行级跳过在事务内计数；系统级异常上抛由调用方整批记失败。
 */
@Service
public class QuestionBatchWriter {

    /** 校验通过的一行（已归一化） */
    public record ValidRow(int row, Long categoryId, int type, int difficulty, String title,
                           String titleMd5, List<QuestionExcelDTO.OptionParsed> options,
                           String answer, String analysis, int isVip) {
    }

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionOptionMapper optionMapper;

    /**
     * @return 本批实际入库（新增或覆盖）的行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int writeBatch(List<ValidRow> rows, Map<String, Question> existing,
                          String strategy, ImportResultVO result) {
        int written = 0;
        for (ValidRow vr : rows) {
            String key = vr.categoryId() + ":" + vr.titleMd5();
            Question old = existing.get(key);
            if (old != null && "SKIP".equals(strategy)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            if (old != null && "ERROR".equals(strategy)) {
                result.getFailures().add(new ImportResultVO.FailureItem(vr.row(), "题目重复"));
                continue;
            }
            upsertQuestion(old, vr);
            written++;
        }
        return written;
    }

    /** OVERWRITE：更新题干/选项/答案/解析，保留题目 id 与点赞数（文档 4.4） */
    private void upsertQuestion(Question old, ValidRow vr) {
        Question q = new Question();
        if (old != null) {
            q.setId(old.getId());
        }
        q.setCategoryId(vr.categoryId());
        q.setType(vr.type());
        q.setDifficulty(vr.difficulty());
        q.setTitle(vr.title());
        q.setAnswer(vr.answer());
        q.setAnalysis(vr.analysis());
        q.setIsVip(vr.isVip());
        q.setTitleMd5(vr.titleMd5());
        if (old != null) {
            // OVERWRITE 不改上下架状态：文档 4.4 只授权更新题干/选项/答案/解析，
            // 若强制 setStatus(1) 会把管理员特意下架的题目意外重新上架。
            questionMapper.updateById(q);
            // 更新选项：旧选项全部逻辑删除再插入（选项 id 变化不影响历史作答记录，文档 4.4 要点 3）
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, old.getId()));
            insertOptions(old.getId(), vr.options());
        } else {
            q.setStatus(1);
            questionMapper.insert(q);
            insertOptions(q.getId(), vr.options());
        }
    }

    private void insertOptions(Long questionId, List<QuestionExcelDTO.OptionParsed> options) {
        for (QuestionExcelDTO.OptionParsed o : options) {
            QuestionOption po = new QuestionOption();
            po.setQuestionId(questionId);
            po.setOptionCode(o.code());
            po.setOptionContent(o.content());
            po.setSort(o.sort());
            optionMapper.insert(po);
        }
    }
}
