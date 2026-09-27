package com.learn.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.learn.common.Result;
import com.learn.dto.EmailCodeDTO;
import com.learn.dto.LoginDTO;
import com.learn.dto.RegisterDTO;
import com.learn.dto.ResetPasswordDTO;
import com.learn.service.AuthService;
import com.learn.vo.CaptchaVO;
import com.learn.vo.LoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（文档 6.1）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /** 获取图形验证码 */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha(HttpServletRequest request) {
        return Result.ok(authService.captcha(request));
    }

    /** 注册（强制图形验证码） */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        authService.register(dto, request);
        return Result.ok();
    }

    /** 登录（失败 >= 3 次要求验证码，5 次锁定 15 分钟） */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.ok(authService.login(dto, request));
    }

    /** 退出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 发送邮箱验证码（找回密码 / 绑定邮箱） */
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailCodeDTO dto, HttpServletRequest request) {
        authService.sendEmailCode(dto, request);
        return Result.ok();
    }

    /** 找回密码（邮箱验证码校验通过后重置） */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.ok();
    }

    /** 当前登录态探测（前端路由守卫用） */
    @GetMapping("/me")
    public Result<Long> me() {
        return Result.ok(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null);
    }
}
