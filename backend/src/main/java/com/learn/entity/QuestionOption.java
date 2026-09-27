package com.learn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目选项表（文档 3.4）。
 * 判断题同样落两条选项（A.正确 / B.错误）；简答题无选项数据。
 * 注意：本表没有 create_time / update_time（按文档 3.4 建表语句落地）。
 */
@Data
@TableName("question_option")
public class QuestionOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    /** 选项标识 A/B/C/D */
    private String optionCode;

    /** 选项内容（Markdown） */
    private String optionContent;

    private Integer sort;

    @TableLogic
    private Integer deleted;
}
