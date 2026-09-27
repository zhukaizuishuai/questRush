package com.learn.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端用户状态设置（文档 6.4：禁用 / 启用）。
 * userId 兼容前端传 id（@JsonAlias），避免历史客户端 422。
 */
@Data
public class AdminUserStatusDTO {

    @NotNull(message = "用户 id 不能为空")
    @JsonAlias("id")
    private Long userId;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(1)
    private Integer status;
}
