package com.learn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码请求（文档 6.1：邮箱验证码校验通过后重置）
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String mailCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String newPassword;
}
