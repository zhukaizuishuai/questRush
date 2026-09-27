package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目列表 VO（文档 4.2）：仅含题干纯文本摘要，绝不含 answer/answerText/analysis。
 */
@Data
public class QuestionListVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private Integer difficulty;
    private Integer isVip;
    private Integer likeCount;
    private Integer status;
    /** 题干纯文本摘要（Markdown 已剥除） */
    private String title;
    private LocalDateTime createTime;
}
