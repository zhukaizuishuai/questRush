package com.learn.service;

import com.learn.dto.EmailCodeDTO;
import com.learn.dto.LoginDTO;
import com.learn.dto.RegisterDTO;
import com.learn.dto.ResetPasswordDTO;
import com.learn.vo.CaptchaVO;
import com.learn.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务（文档 6.1）
 */
public interface AuthService {

    /** 获取图形验证码（IP 每分钟限 20 次，文档 4.7） */
    CaptchaVO captcha(HttpServletRequest request);

    /** 注册（强制图形验证码，BCrypt 加密） */
    void register(RegisterDTO dto, HttpServletRequest request);

    /** 登录（失败 >= 3 次要求验证码，5 次锁定 15 分钟，失败提示统一防账号枚举） */
    LoginVO login(LoginDTO dto, HttpServletRequest request);

    /** 退出 */
    void logout();

    /** 发送邮箱验证码（60 秒冷却 + IP 每日 10 次） */
    void sendEmailCode(EmailCodeDTO dto, HttpServletRequest request);

    /** 找回密码（邮箱验证码校验通过后重置） */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 图形验证码校验：无论成败立即删除（一次性生效，防重放，文档 4.7）
     */
    void verifyGraphCaptcha(String captchaId, String code);
}
