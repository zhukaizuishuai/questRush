package com.learn.vo;

import lombok.Data;

/**
 * 管理端数据统计 VO（文档 6.4）
 */
@Data
public class AdminStatsVO {

    private Long userCount;
    private Long questionCount;
    private Long vipUserCount;
    /** 日均答题量 = 总作答数 / 覆盖天数 */
    private Double dailyAvgAnswers;
}
