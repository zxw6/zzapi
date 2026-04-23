package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.modules.auth.dto.VerificationCodeSendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class VerificationCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final long expireSeconds;
    private final long resendIntervalSeconds;
    private final boolean returnCodeInResponse;

    public VerificationCodeService(StringRedisTemplate stringRedisTemplate,
                                   @Value("${app.auth.verification-code.expire-seconds:300}") long expireSeconds,
                                   @Value("${app.auth.verification-code.resend-interval-seconds:60}") long resendIntervalSeconds,
                                   @Value("${app.auth.verification-code.return-code-in-response:true}") boolean returnCodeInResponse) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expireSeconds = expireSeconds;
        this.resendIntervalSeconds = resendIntervalSeconds;
        this.returnCodeInResponse = returnCodeInResponse;
    }

    public VerificationCodeSendResponse sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = buildCooldownKey(normalizedEmail);
        Boolean coolingDown = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(coolingDown)) {
            throw new BusinessException(429, "Verification code requests are too frequent");
        }

        String code = generateCode();
        stringRedisTemplate.opsForValue().set(buildRegisterCodeKey(normalizedEmail), code, Duration.ofSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        return new VerificationCodeSendResponse(
                normalizedEmail,
                expireSeconds,
                returnCodeInResponse ? code : null
        );
    }

    public void verifyRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "Verification code cannot be blank");
        }

        String cacheKey = buildRegisterCodeKey(normalizedEmail);
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || cachedCode.isBlank()) {
            throw new BusinessException(400, "Verification code expired");
        }
        if (!cachedCode.equals(code.trim())) {
            throw new BusinessException(400, "Verification code is invalid");
        }

        stringRedisTemplate.delete(cacheKey);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "QQ email cannot be blank");
        }
        String normalized = email.trim().toLowerCase();
        if (!normalized.endsWith("@qq.com")) {
            throw new BusinessException(400, "Please use a QQ email");
        }
        return normalized;
    }

    private String generateCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return Integer.toString(code);
    }

    private String buildRegisterCodeKey(String email) {
        return "auth:verify:register:" + email;
    }

    private String buildCooldownKey(String email) {
        return "auth:verify:register:cooldown:" + email;
    }
}
