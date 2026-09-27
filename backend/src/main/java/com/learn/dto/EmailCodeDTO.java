package com.learn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送邮箱验证码请求（文档 4.7：找回密码强制图形验证码 + 60s 冷却 + IP 限流）
 */
@Data
public class EmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaId;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
