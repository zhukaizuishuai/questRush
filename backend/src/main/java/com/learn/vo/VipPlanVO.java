package com.learn.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * VIP 套餐 VO（文档 4.5）
 */
@Data
@AllArgsConstructor
public class VipPlanVO {

    /** month / quarter / year */
    private String planType;

    private String name;

    private Integer months;

    private BigDecimal price;

    private String description;
}
