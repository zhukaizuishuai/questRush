package com.learn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目分类表（文档 3.2）。题目只允许挂叶子分类，一级分类仅分组展示。
 */
@Data
@TableName("question_category")
public class QuestionCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 父分类 ID，0 = 一级分类 */
    private Long parentId;

    /** 排序权重，越大越靠前 */
    private Integer sort;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
