package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交答题后返回 VO（文档 4.2）：判分结果 + 正确答案 + 解析 + 掌握状态。
 */
@Data
public class QuestionSubmitVO {

    private Long questionId;

    /** 1 正确 0 错误 NULL 简答未判分 */
    private Integer isCorrect;

    /** 正确答案（客观题） */
    private String answer;

    /** 简答题参考答案（Markdown） */
    private String answerText;

    /** 解析（Markdown） */
    private String analysis;

    /** 本次提交后是否已掌握（1/0） */
    private Integer mastered;

    /** 下次复习时间（NULL = 不在复习队列） */
    private LocalDateTime nextReviewTime;
}
