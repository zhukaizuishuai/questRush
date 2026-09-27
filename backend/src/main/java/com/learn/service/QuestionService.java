package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.vo.QuestionAnswerVO;
import com.learn.vo.QuestionListVO;
import com.learn.vo.QuestionPracticeVO;

/**
 * 题目服务（文档 6.2）
 */
public interface QuestionService {

    /** 题目列表 / 搜索（同一套 SQL 与权限过滤，文档 4.1） */
    IPage<QuestionListVO> pageList(long pageNum, long pageSize, Long categoryId,
                                   String keyword, Integer difficulty, Long userId);

    /** 答题前详情：不含答案字段（文档 4.2），逐条鉴权（文档 4.1） */
    QuestionPracticeVO detail(Long questionId, Long userId);

    /** 主动查看答案与解析（详情页「查看答案」按钮；VIP 鉴权同样覆盖） */
    QuestionAnswerVO answer(Long questionId, Long userId);
}
