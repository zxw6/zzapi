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
/**
 * 验证码服务。
 * 优先使用 Redis 保存注册验证码，Redis 不可用时降级到进程内缓存。
 */
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

    /**
     * 发送注册验证码。
     */
    public VerificationCodeSendResponse sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = buildCooldownKey(normalizedEmail);
        String code = generateCode();
        try {
            Boolean coolingDown = stringRedisTemplate.hasKey(cooldownKey);
            if (Boolean.TRUE.equals(coolingDown)) {
                throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
            }
            stringRedisTemplate.opsForValue().set(buildRegisterCodeKey(normalizedEmail), code, Duration.ofSeconds(expireSeconds));
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        } catch (RedisConnectionFailureException ex) {
            // Redis 异常时降级到本地内存，保证注册流程不中断。
            saveCodeLocally(normalizedEmail, code);
        }
        return new VerificationCodeSendResponse(
                normalizedEmail,
                expireSeconds,
                returnCodeInResponse ? code : null
        );
    }

    /**
     * 校验注册验证码。
     */
    public void verifyRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "验证码不能为空");
        }

        String cacheKey = buildRegisterCodeKey(normalizedEmail);
        try {
            String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedCode == null || cachedCode.isBlank()) {
                throw new BusinessException(400, "验证码已过期");
            }
            if (!cachedCode.equals(code.trim())) {
                throw new BusinessException(400, "验证码不正确");
            }
            stringRedisTemplate.delete(cacheKey);
            return;
        } catch (RedisConnectionFailureException ex) {
            verifyCodeLocally(normalizedEmail, code.trim());
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "QQ 邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (!normalized.endsWith("@qq.com")) {
            throw new BusinessException(400, "请使用 QQ 邮箱");
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

    /**
     * 使用本地缓存兜底保存验证码与冷却时间。
     */
    private void saveCodeLocally(String email, String code) {
        cleanupLocalState();
        long now = System.currentTimeMillis();
        Long cooldownUntil = localCooldowns.get(email);
        if (cooldownUntil != null && cooldownUntil > now) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }
        localRegisterCodes.put(email, new ExpiringValue(code, now + Duration.ofSeconds(expireSeconds).toMillis()));
        localCooldowns.put(email, now + Duration.ofSeconds(resendIntervalSeconds).toMillis());
        log.warn("Redis unavailable, falling back to in-memory verification code storage for {}", email);
    }

    /**
     * 使用本地缓存校验验证码。
     */
    private void verifyCodeLocally(String email, String code) {
        cleanupLocalState();
        ExpiringValue cached = localRegisterCodes.get(email);
        if (cached == null || cached.expiresAtMillis() <= System.currentTimeMillis()) {
            localRegisterCodes.remove(email);
            throw new BusinessException(400, "验证码已过期");
        }
        if (!cached.value().equals(code)) {
            throw new BusinessException(400, "验证码不正确");
        }
        localRegisterCodes.remove(email);
    }

    /**
     * 清理本地已过期的验证码和发送冷却记录。
     */
    private void cleanupLocalState() {
        long now = System.currentTimeMillis();
        localRegisterCodes.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        localCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record ExpiringValue(String value, long expiresAtMillis) {
    }
}
