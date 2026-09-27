package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记 VO（仅本人可见，文档 3.8）
 */
@Data
public class NoteVO {

    private Long id;
    private Long questionId;
    /** 题干（越权脱敏时为 null，前端显示「该题目需会员权限」） */
    private String questionTitle;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
