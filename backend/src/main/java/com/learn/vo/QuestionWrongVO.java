package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本 / 复习队列条目 VO（文档 4.3）。
 * title 字段：正常时下发题干；VIP 过期等越权场景由 service 脱敏置空，只保留 id + 分类名（文档 4.1）。
 */
@Data
public class QuestionWrongVO {

    private Long questionId;
    private Integer type;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String lastAnswer;
    private Integer totalCount;
    private Integer wrongCount;
    private LocalDateTime submitTime;
    private LocalDateTime nextReviewTime;
}
