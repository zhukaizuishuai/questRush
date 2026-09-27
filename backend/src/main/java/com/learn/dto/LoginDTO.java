package com.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求（文档 4.7：失败 >= 3 次要求图形验证码）
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String password;

    /** 渐进式：正常登录可不传，失败 3 次后必传 */
    private String captchaId;
    private String captchaCode;
}
