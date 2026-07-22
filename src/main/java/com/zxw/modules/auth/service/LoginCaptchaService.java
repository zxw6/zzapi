package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.modules.auth.dto.LoginCaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * 登录图形验证码服务。
 * 优先使用 Redis 保存登录验证码，Redis 不可用时降级到进程内缓存。
 */
public class LoginCaptchaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private final StringRedisTemplate stringRedisTemplate;
    private final long expireSeconds;
    private final Map<String, ExpiringValue> localCaptchas = new ConcurrentHashMap<>();

    public LoginCaptchaService(StringRedisTemplate stringRedisTemplate,
                               @Value("${app.auth.login-captcha.expire-seconds:120}") long expireSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成一个新的登录图形验证码。
     */
    public LoginCaptchaResponse createCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = generateCode(4);
        saveCaptcha(captchaId, code);
        return new LoginCaptchaResponse(
                captchaId,
                "data:image/png;base64," + renderCaptchaImageBase64(code),
                expireSeconds
        );
    }

    /**
     * 校验并消费登录图形验证码。
     */
    public void verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new BusinessException(400, "验证码标识不能为空");
        }
        if (captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(400, "图形验证码不能为空");
        }

        String normalizedCaptchaId = captchaId.trim();
        String normalizedCode = captchaCode.trim().toUpperCase();
        String key = buildCaptchaKey(normalizedCaptchaId);
        try {
            String cachedCode = stringRedisTemplate.opsForValue().get(key);
            if (cachedCode == null || cachedCode.isBlank()) {
                throw new BusinessException(400, "图形验证码已过期，请刷新后重试");
            }
            if (!cachedCode.equalsIgnoreCase(normalizedCode)) {
                throw new BusinessException(400, "图形验证码不正确");
            }
            stringRedisTemplate.delete(key);
            return;
        } catch (RedisConnectionFailureException ex) {
            verifyCaptchaLocally(normalizedCaptchaId, normalizedCode);
        }
    }

    private void saveCaptcha(String captchaId, String code) {
        String key = buildCaptchaKey(captchaId);
        try {
            stringRedisTemplate.opsForValue().set(key, code, Duration.ofSeconds(expireSeconds));
        } catch (RedisConnectionFailureException ex) {
            localCaptchas.put(captchaId, new ExpiringValue(code, System.currentTimeMillis() + Duration.ofSeconds(expireSeconds).toMillis()));
            cleanupLocalCaptchas();
        }
    }

    private void verifyCaptchaLocally(String captchaId, String captchaCode) {
        cleanupLocalCaptchas();
        ExpiringValue cached = localCaptchas.get(captchaId);
        if (cached == null || cached.expiresAtMillis() <= System.currentTimeMillis()) {
            localCaptchas.remove(captchaId);
            throw new BusinessException(400, "图形验证码已过期，请刷新后重试");
        }
        if (!cached.value().equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(400, "图形验证码不正确");
        }
        localCaptchas.remove(captchaId);
    }

    private void cleanupLocalCaptchas() {
        long now = System.currentTimeMillis();
        localCaptchas.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private String buildCaptchaKey(String captchaId) {
        return "auth:captcha:login:" + captchaId;
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CAPTCHA_CHARS[SECURE_RANDOM.nextInt(CAPTCHA_CHARS.length)]);
        }
        return builder.toString();
    }

    private String renderCaptchaImageBase64(String code) {
        try {
            BufferedImage image = new BufferedImage(132, 44, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(250, 248, 243));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            // 先画一些干扰线，避免验证码过于规整。
            for (int index = 0; index < 6; index++) {
                graphics.setColor(randomSoftColor());
                graphics.setStroke(new BasicStroke(1.4f));
                graphics.drawLine(
                        SECURE_RANDOM.nextInt(image.getWidth()),
                        SECURE_RANDOM.nextInt(image.getHeight()),
                        SECURE_RANDOM.nextInt(image.getWidth()),
                        SECURE_RANDOM.nextInt(image.getHeight())
                );
            }

            // 再随机撒点，增加肉眼可见的扰动。
            for (int index = 0; index < 36; index++) {
                graphics.setColor(randomSoftColor());
                int x = SECURE_RANDOM.nextInt(image.getWidth());
                int y = SECURE_RANDOM.nextInt(image.getHeight());
                graphics.fillOval(x, y, 2, 2);
            }

            graphics.setFont(new Font("Arial", Font.BOLD, 28));
            for (int index = 0; index < code.length(); index++) {
                graphics.setColor(randomDeepColor());
                int x = 18 + index * 26 + SECURE_RANDOM.nextInt(5);
                int y = 31 + SECURE_RANDOM.nextInt(6);
                double rotation = Math.toRadians(SECURE_RANDOM.nextInt(21) - 10);
                graphics.rotate(rotation, x, y);
                graphics.drawString(String.valueOf(code.charAt(index)), x, y);
                graphics.rotate(-rotation, x, y);
            }

            graphics.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException(500, "生成图形验证码失败");
        }
    }

    private Color randomSoftColor() {
        return new Color(
                150 + SECURE_RANDOM.nextInt(80),
                150 + SECURE_RANDOM.nextInt(80),
                150 + SECURE_RANDOM.nextInt(80)
        );
    }

    private Color randomDeepColor() {
        return new Color(
                40 + SECURE_RANDOM.nextInt(90),
                40 + SECURE_RANDOM.nextInt(90),
                40 + SECURE_RANDOM.nextInt(90)
        );
    }

    private record ExpiringValue(String value, long expiresAtMillis) {
    }
}
