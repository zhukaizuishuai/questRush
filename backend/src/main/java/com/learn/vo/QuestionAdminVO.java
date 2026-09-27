package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理题目 VO（文档 4.2）：全字段含 answer/answerText/analysis，仅 admin 接口返回。
 */
@Data
public class QuestionAdminVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private Integer difficulty;
    private String title;
    private String answer;
    private String answerText;
    private String analysis;
    private Integer isVip;
    private Integer status;
    private String titleMd5;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 选项列表（service 层组装） */
    private List<QuestionOptionVO> options;
}
