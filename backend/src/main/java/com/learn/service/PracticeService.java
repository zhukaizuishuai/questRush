package com.learn.service;

import com.learn.dto.PracticeSessionDTO;
import com.learn.dto.SubmitDTO;
import com.learn.vo.QuestionPracticeVO;
import com.learn.vo.QuestionSubmitVO;
import com.learn.vo.StatsVO;
import com.learn.vo.WrongBookPageVO;

import java.util.Map;

/**
 * 刷题服务（文档 6.2）：会话、逐题拉取、提交判分、错题本、复习队列、统计
 */
public interface PracticeService {

    /**
     * 创建刷题会话：按权限筛题、顺序/随机、存缓存 TTL 2 小时（文档 4.3）。
     * source=all 常规刷题（按分类/难度选题）；source=review 复习模式（从遗忘曲线队列选题，
     * 支撑复习闭环）；source=wrong 错题重做模式。三种来源的判分与状态回写逻辑完全一致。
     */
    Map<String, Object> createSession(PracticeSessionDTO dto, Long userId);

    /** 按 sessionId + index 取题（答题前 VO，不含答案） */
    QuestionPracticeVO next(String sessionId, int index, Long userId);

    /** 提交答题：判分 + 状态表 upsert + 流水表追加（文档 4.3） */
    QuestionSubmitVO submit(SubmitDTO dto, Long userId);

    /**
     * 错题本（越权条目脱敏，文档 4.1）。
     * 同时回传全队列可作答条数 answerableCount，供前端判断「开始复习」是否可用。
     */
    WrongBookPageVO wrongBook(long pageNum, long pageSize, Long userId);

    /**
     * 复习队列（mastered=0 且 next_review_time <= NOW()，文档 4.3）。
     * 同样回传全队列可作答条数。
     */
    WrongBookPageVO reviewList(long pageNum, long pageSize, Long userId);

    /** 统计：总答题数、正确率、按分类正确率、连续打卡天数 */
    StatsVO stats(Long userId);
}
