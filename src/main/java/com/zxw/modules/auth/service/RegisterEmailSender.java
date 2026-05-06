package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegisterEmailSender {

    private static final Logger log = LoggerFactory.getLogger(RegisterEmailSender.class);

    private final JavaMailSender javaMailSender;
    private final String fromAddress;
    private final String subject;

    public RegisterEmailSender(JavaMailSender javaMailSender,
                               @Value("${app.mail.from:}") String fromAddress,
                               @Value("${app.mail.register-code-subject:AI Gateway 注册验证码}") String subject) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.subject = subject;
    }

    public void sendRegisterCode(String email, String code, long expireSeconds) {
        if (fromAddress.isBlank()) {
            throw new BusinessException(500, "邮件发件箱未配置，请先设置 MAIL_FROM");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(buildContent(code, expireSeconds));

        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send register verification code email to {}", email, ex);
            throw new BusinessException(500, "验证码邮件发送失败，请稍后重试");
        }
    }

    private String buildContent(String code, long expireSeconds) {
        long minutes = Math.max(1, expireSeconds / 60);
        return """
                您好，

                您本次注册的邮箱验证码为：%s

                验证码 %d 分钟内有效，请尽快完成注册。
                如果这不是您的操作，请忽略此邮件。

                AI Gateway
                """.formatted(code, minutes);
    }
}
