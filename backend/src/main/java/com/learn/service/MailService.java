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
 *
 * 注意：不能把「JavaMailSender Bean 是否为 null」当配置开关。application.yml 里
 * `host: ${MAIL_HOST:}` 会让 @ConditionalOnProperty 判定为「已配置」（空串也算 property 存在），
 * Bean 会被创建。必须显式判断 host 是否为空，否则在「配了 username 没配 host」的组合下
 * 会向空 host 真实发送、异常被吞掉，验证码既发不出去也不落日志。
 */
@Slf4j
@Service
public class MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String host;

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
        if (mailSender == null || host == null || host.isBlank()) {
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
            // 邮件发送失败不阻断业务流程，但必须打日志（否则验证码会静默丢失）
            log.error("mail send failed to {}: {}", to, e.getMessage());
        }
    }
}
