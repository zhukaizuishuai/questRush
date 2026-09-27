package com.learn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 订单表（文档 3.9）。MOCK 渠道点击支付即已支付，后置逻辑与真实支付一致（文档 4.5）。
 */
@Data
@TableName("vip_order")
public class VipOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 套餐：month 月 / quarter 季 / year 年 */
    private String planType;

    /** 开通月数 */
    private Integer months;

    private BigDecimal amount;

    /** 支付渠道：MOCK 模拟 / ALIPAY */
    private String payChannel;

    /** 0 待支付 1 已支付 2 已关闭 */
    private Integer status;

    private LocalDateTime payTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
