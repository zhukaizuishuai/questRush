package com.learn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息（文档 6.1：昵称、邮箱）
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称过长")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}
