package com.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * VIP 下单请求（文档 4.5）：month / quarter / year
 */
@Data
public class VipOrderDTO {

    @NotBlank(message = "套餐类型不能为空")
    private String planType;
}
