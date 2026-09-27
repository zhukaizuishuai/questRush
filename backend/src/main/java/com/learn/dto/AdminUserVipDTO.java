package com.learn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端手动设置 VIP 过期时间（文档 4.5：直接覆盖）
 */
@Data
public class AdminUserVipDTO {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;

    /** 传 null 表示取消 VIP */
    private LocalDateTime vipExpireTime;
}
