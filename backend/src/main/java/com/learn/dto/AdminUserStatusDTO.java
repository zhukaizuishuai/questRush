package com.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端用户状态设置（文档 6.4：禁用 / 启用）
 */
@Data
public class AdminUserStatusDTO {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(1)
    private Integer status;
}
