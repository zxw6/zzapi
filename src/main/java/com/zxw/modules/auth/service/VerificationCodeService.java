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
    private final long expireSeconds;
    private final long resendIntervalSeconds;
    private final boolean returnCodeInResponse;
    private final Map<String, ExpiringValue> localRegisterCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> localCooldowns = new ConcurrentHashMap<>();

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
        String code = generateCode();
        try {
            Boolean coolingDown = stringRedisTemplate.hasKey(cooldownKey);
            if (Boolean.TRUE.equals(coolingDown)) {
                throw new BusinessException(429, "Verification code requests are too frequent");
            }
            stringRedisTemplate.opsForValue().set(buildRegisterCodeKey(normalizedEmail), code, Duration.ofSeconds(expireSeconds));
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        } catch (RedisConnectionFailureException ex) {
            saveCodeLocally(normalizedEmail, code);
        }
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
        try {
            String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedCode == null || cachedCode.isBlank()) {
                throw new BusinessException(400, "Verification code expired");
            }
            if (!cachedCode.equals(code.trim())) {
                throw new BusinessException(400, "Verification code is invalid");
            }
            stringRedisTemplate.delete(cacheKey);
            return;
        } catch (RedisConnectionFailureException ex) {
            verifyCodeLocally(normalizedEmail, code.trim());
        }
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

    private void saveCodeLocally(String email, String code) {
        cleanupLocalState();
        long now = System.currentTimeMillis();
        Long cooldownUntil = localCooldowns.get(email);
        if (cooldownUntil != null && cooldownUntil > now) {
            throw new BusinessException(429, "Verification code requests are too frequent");
        }
        localRegisterCodes.put(email, new ExpiringValue(code, now + Duration.ofSeconds(expireSeconds).toMillis()));
        localCooldowns.put(email, now + Duration.ofSeconds(resendIntervalSeconds).toMillis());
        log.warn("Redis unavailable, falling back to in-memory verification code storage for {}", email);
    }

    private void verifyCodeLocally(String email, String code) {
        cleanupLocalState();
        ExpiringValue cached = localRegisterCodes.get(email);
        if (cached == null || cached.expiresAtMillis() <= System.currentTimeMillis()) {
            localRegisterCodes.remove(email);
            throw new BusinessException(400, "Verification code expired");
        }
        if (!cached.value().equals(code)) {
            throw new BusinessException(400, "Verification code is invalid");
        }
        localRegisterCodes.remove(email);
    }

    private void cleanupLocalState() {
        long now = System.currentTimeMillis();
        localRegisterCodes.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        localCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record ExpiringValue(String value, long expiresAtMillis) {
    }
}
