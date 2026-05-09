package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.modules.auth.dto.VerificationCodeSendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RegisterEmailSender registerEmailSender;
    private final long expireSeconds;
    private final long resendIntervalSeconds;
    private final Map<String, ExpiringValue> localCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> localCooldowns = new ConcurrentHashMap<>();

    public VerificationCodeService(StringRedisTemplate stringRedisTemplate,
                                   RegisterEmailSender registerEmailSender,
                                   @Value("${app.auth.verification-code.expire-seconds:300}") long expireSeconds,
                                   @Value("${app.auth.verification-code.resend-interval-seconds:60}") long resendIntervalSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.registerEmailSender = registerEmailSender;
        this.expireSeconds = expireSeconds;
        this.resendIntervalSeconds = resendIntervalSeconds;
    }

    public VerificationCodeSendResponse sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = saveCode(normalizedEmail, "register");
        registerEmailSender.sendRegisterCode(normalizedEmail, code, expireSeconds);
        return new VerificationCodeSendResponse(normalizedEmail, expireSeconds);
    }

    public void verifyRegisterCode(String email, String code) {
        verifyCode(email, code, "register");
    }

    public VerificationCodeSendResponse sendPasswordResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = saveCode(normalizedEmail, "password-reset");
        registerEmailSender.sendPasswordResetCode(normalizedEmail, code, expireSeconds);
        return new VerificationCodeSendResponse(normalizedEmail, expireSeconds);
    }

    public void verifyPasswordResetCode(String email, String code) {
        verifyCode(email, code, "password-reset");
    }

    private String saveCode(String email, String scene) {
        String code = generateCode();
        String cooldownKey = buildCooldownKey(scene, email);
        try {
            Boolean coolingDown = stringRedisTemplate.hasKey(cooldownKey);
            if (Boolean.TRUE.equals(coolingDown)) {
                throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
            }
            stringRedisTemplate.opsForValue().set(buildCodeKey(scene, email), code, Duration.ofSeconds(expireSeconds));
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        } catch (RedisConnectionFailureException ex) {
            saveCodeLocally(email, scene, code);
        }
        return code;
    }

    private void verifyCode(String email, String code, String scene) {
        String normalizedEmail = normalizeEmail(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "验证码不能为空");
        }

        String cacheKey = buildCodeKey(scene, normalizedEmail);
        try {
            String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedCode == null || cachedCode.isBlank()) {
                throw new BusinessException(400, "验证码已过期");
            }
            if (!cachedCode.equals(code.trim())) {
                throw new BusinessException(400, "验证码不正确");
            }
            stringRedisTemplate.delete(cacheKey);
        } catch (RedisConnectionFailureException ex) {
            verifyCodeLocally(normalizedEmail, scene, code.trim());
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "QQ邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (!normalized.endsWith("@qq.com")) {
            throw new BusinessException(400, "请使用QQ邮箱");
        }
        return normalized;
    }

    private String generateCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return Integer.toString(code);
    }

    private String buildCodeKey(String scene, String email) {
        return "auth:verify:" + scene + ":" + email;
    }

    private String buildCooldownKey(String scene, String email) {
        return "auth:verify:" + scene + ":cooldown:" + email;
    }

    private String buildLocalKey(String scene, String email) {
        return scene + ":" + email;
    }

    private void saveCodeLocally(String email, String scene, String code) {
        cleanupLocalState();
        long now = System.currentTimeMillis();
        String localKey = buildLocalKey(scene, email);
        Long cooldownUntil = localCooldowns.get(localKey);
        if (cooldownUntil != null && cooldownUntil > now) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }
        localCodes.put(localKey, new ExpiringValue(code, now + Duration.ofSeconds(expireSeconds).toMillis()));
        localCooldowns.put(localKey, now + Duration.ofSeconds(resendIntervalSeconds).toMillis());
        log.warn("Redis unavailable, falling back to in-memory {} verification code storage for {}", scene, email);
    }

    private void verifyCodeLocally(String email, String scene, String code) {
        cleanupLocalState();
        String localKey = buildLocalKey(scene, email);
        ExpiringValue cached = localCodes.get(localKey);
        if (cached == null || cached.expiresAtMillis() <= System.currentTimeMillis()) {
            localCodes.remove(localKey);
            throw new BusinessException(400, "验证码已过期");
        }
        if (!cached.value().equals(code)) {
            throw new BusinessException(400, "验证码不正确");
        }
        localCodes.remove(localKey);
    }

    private void cleanupLocalState() {
        long now = System.currentTimeMillis();
        localCodes.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        localCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record ExpiringValue(String value, long expiresAtMillis) {
    }
}
