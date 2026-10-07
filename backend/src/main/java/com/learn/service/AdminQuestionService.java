package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.dto.QuestionSaveDTO;
import com.learn.vo.AdminStatsVO;
import com.learn.vo.QuestionAdminVO;

/**
 * 管理端题库服务（文档 6.4）：单条 CRUD、上下架、重算点赞、统计
 */
public interface AdminQuestionService {

    IPage<QuestionAdminVO> page(long pageNum, long pageSize, Long categoryId, String keyword,
                                Integer type, Integer isVip, Integer difficulty, Integer status);

    QuestionAdminVO detail(Long id);

    void save(QuestionSaveDTO dto);

    void update(QuestionSaveDTO dto);

    /** 上下架 */
    void updateStatus(Long id, Integer status);

    void delete(Long id);

    /** 按点赞表重算 like_count 兜底（文档 4.6） */
    int recalcLike();

    /** 数据统计（文档 6.4）：用户数、题目数、VIP 用户数、日均答题量 */
    AdminStatsVO stats();
}
