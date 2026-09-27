package com.learn.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 订单 VO（文档 6.3）
 */
@Data
public class VipOrderVO {

    private Long id;
    private String orderNo;
    private String planType;
    private Integer months;
    private BigDecimal amount;
    private String payChannel;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}
