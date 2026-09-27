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
 * 题目表（文档 3.3）。
 * type：1 单选 2 多选 3 判断 4 简答；difficulty：1 简单 2 中等 3 困难。
 * titleMd5：题干去空白 MD5，导入幂等查重用（文档 4.4）。
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属叶子分类 ID */
    private Long categoryId;

    /** 1 单选 2 多选 3 判断 4 简答 */
    private Integer type;

    /** 1 简单 2 中等 3 困难 */
    private Integer difficulty;

    /** 题干（Markdown） */
    private String title;

    /** 客观题标准答案：单选/判断存 A，多选存归一化后的 ABD */
    private String answer;

    /** 简答题参考答案（Markdown） */
    private String answerText;

    /** 题目解析（Markdown） */
    private String analysis;

    /** 0 免费 1 VIP 专属 */
    private Integer isVip;

    /** 1 启用 0 下架 */
    private Integer status;

    /** 题干 MD5（去空白归一化） */
    private String titleMd5;

    /** 点赞数冗余字段，由点赞事务同步增减（文档 4.6） */
    private Integer likeCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
