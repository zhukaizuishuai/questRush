package com.learn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务。
 *
 * 简化说明：本实现为 mock——未配置 spring.mail.host 时只把内容输出到日志；
 * 配置了 SMTP（见 application.yml）后自动走真实发送（生产必须 465 SSL，文档 4.7）。
 */
@Slf4j
@Service
public class MailService {

    /** required = false：未配置 spring.mail.host 时 Spring Boot 不会创建 JavaMailSender Bean */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送邮箱验证码（找回密码 / 绑定邮箱，文档 4.7）
     */
    public void sendCaptcha(String to, String code) {
        String subject = "QuestRush 找回密码验证码";
        String content = "您的验证码是：" + code + "，10 分钟内有效。请勿泄露给他人。";
        send(to, subject, content);
    }

    public void send(String to, String subject, String content) {
        if (mailSender == null || from == null || from.isBlank()) {
            // mock 模式：找回密码流程完整，验证码打印到日志
            log.info("[MOCK MAIL] to={} | subject={} | content={}", to, subject, content);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("mail sent to {}", to);
        } catch (Exception e) {
            // 邮件发送失败不阻断业务流程，验证码已在缓存中可查
            log.error("mail send failed to {}: {}", to, e.getMessage());
        }
    }
}
