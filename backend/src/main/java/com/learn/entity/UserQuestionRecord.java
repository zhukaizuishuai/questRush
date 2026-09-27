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
 * 用户做题状态表（文档 3.5）：一题一条，服务错题本与复习调度。
 * 写入统一走 INSERT ... ON DUPLICATE KEY UPDATE（UserQuestionRecordMapper）。
 * 注意：本表按文档建表语句无 create_time，只有 update_time / submit_time。
 */
@Data
@TableName("user_question_record")
public class UserQuestionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 最近一次作答内容 */
    private String lastAnswer;

    /** 最近一次结果 1 正确 0 错误 NULL 简答未判分 */
    private Integer isCorrect;

    /** 累计作答次数 */
    private Integer totalCount;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 连续答对次数 */
    private Integer continuousCorrect;

    /** 遗忘曲线层级 0-5 */
    private Integer reviewLevel;

    /** 下次复习时间，NULL = 不在复习队列 */
    private LocalDateTime nextReviewTime;

    /** 1 已掌握，移出错题本 */
    private Integer mastered;

    /** 最近作答时间 */
    private LocalDateTime submitTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
