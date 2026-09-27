package com.learn.vo;

import lombok.Data;

import java.util.List;

/**
 * 答题前 / 刷题页 VO（文档 4.2）。
 * 硬性约束：不含 answer / answerText / analysis 字段，SQL 层也不 select 这三列。
 */
@Data
public class QuestionPracticeVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private Integer difficulty;
    private Integer isVip;
    private String title;

    /** 选项（仅 code 与 content） */
    private List<QuestionOptionVO> options;

    /** 当前用户是否已收藏 */
    private Boolean favorited;

    /** 当前用户是否已点赞（详情页 LikeButton 初始状态） */
    private Boolean liked;

    /** 点赞数（冗余字段） */
    private Integer likeCount;
}
