package com.learn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户作答流水表（文档 3.6）。
 * 文档 3.0 唯一例外：事实流水只追加，不设 deleted、不设 update_time。
 */
@Data
@TableName("user_answer_log")
public class UserAnswerLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 本次作答内容 */
    private String userAnswer;

    /** 1 正确 0 错误 NULL 简答未判分 */
    private Integer isCorrect;

    /** 提交时间（数据库 DEFAULT CURRENT_TIMESTAMP，插入时不传） */
    private LocalDateTime submitTime;
}
