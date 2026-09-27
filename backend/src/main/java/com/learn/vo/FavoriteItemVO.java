package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏列表条目 VO。title 字段越权时置空（文档 4.1 脱敏）。
 */
@Data
public class FavoriteItemVO {

    private Long questionId;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private Integer difficulty;
    private Integer isVip;
    private String title;
    private LocalDateTime favoritedTime;
}
