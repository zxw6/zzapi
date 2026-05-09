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
    private final String registerSubject;
    private final String passwordResetSubject;

    public RegisterEmailSender(JavaMailSender javaMailSender,
                               @Value("${app.mail.from:}") String fromAddress,
                               @Value("${app.mail.register-code-subject:AI LayCode Register Verification Code}") String registerSubject,
                               @Value("${app.mail.password-reset-code-subject:AI LayCode Password Reset Verification Code}") String passwordResetSubject) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.registerSubject = registerSubject;
        this.passwordResetSubject = passwordResetSubject;
    }

    public void sendRegisterCode(String email, String code, long expireSeconds) {
        sendCodeEmail(email, registerSubject, buildRegisterContent(code, expireSeconds), "register");
    }

    public void sendPasswordResetCode(String email, String code, long expireSeconds) {
        sendCodeEmail(email, passwordResetSubject, buildPasswordResetContent(code, expireSeconds), "password reset");
    }

    private void sendCodeEmail(String email, String subject, String content, String scene) {
        if (fromAddress.isBlank()) {
            throw new BusinessException(500, "Mail sender is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);

        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send {} verification code email to {}", scene, email, ex);
            throw new BusinessException(500, "Verification code email sending failed");
        }
    }

    private String buildRegisterContent(String code, long expireSeconds) {
        long minutes = Math.max(1, expireSeconds / 60);
        return """
                Hello,
                Your registration verification code is: %s

                The code is valid for %d minutes. Please complete registration as soon as possible.
                If this was not your action, please ignore this email.
                AI LayCode
                """.formatted(code, minutes);
    }

    private String buildPasswordResetContent(String code, long expireSeconds) {
        long minutes = Math.max(1, expireSeconds / 60);
        return """
                Hello,
                Your password reset verification code is: %s

                The code is valid for %d minutes. Please use it to complete your password reset.
                If this was not your action, please ignore this email.
                AI LayCode
                """.formatted(code, minutes);
    }
}
