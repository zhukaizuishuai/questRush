package com.learn.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户做题统计 VO（文档 6.2）：总答题数、正确率、按分类正确率、连续打卡天数。
 */
@Data
public class StatsVO {

    /** 总作答条数（流水表口径） */
    private Long totalCount;

    /** 答对条数（is_correct=1，简答 NULL 不计入分母? 文档口径为正确条数/总作答条数） */
    private Long correctCount;

    /** 正确率（百分比 0-100） */
    private Double accuracy;

    private List<CategoryStatVO> categoryStats;

    private Integer continuousCheckInDays;

    /** 错题本条数（首页提醒用） */
    private Long wrongBookCount;

    /** 复习队列当前待复习条数（首页提醒用） */
    private Long reviewCount;
}
