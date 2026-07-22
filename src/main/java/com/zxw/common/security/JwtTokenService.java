package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;
import com.zxw.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
/**
 * JWT 令牌服务。
 * 负责生成登录令牌以及解析令牌中的用户身份信息。
 */
public class JwtTokenService {

    private final SecretKey secretKey;
    private final GatewaySecurityProperties properties;

    public JwtTokenService(GatewaySecurityProperties properties) {
        this.properties = properties;
        // JWT 密钥长度不足时直接阻止启动，避免签名强度过低
        String secret = properties.jwtSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.length() < 32) {
            throw new IllegalArgumentException("gateway.security.jwt-secret 至少需要 32 位");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 为登录用户生成 JWT 令牌。
     */
    public String createToken(JwtUser jwtUser) {
        // 生成带有用户主键、用户名和角色的登录令牌
        Instant now = Instant.now();
        Instant expireAt = now.plus(properties.jwtExpireMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(jwtUser.userId()))
                .claim("username", jwtUser.username())
                .claim("roleCode", jwtUser.roleCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 JWT 并还原登录用户信息。
     */
    public JwtUser parseToken(String token) {
        try {
            // 解析 JWT 并还原成业务侧使用的用户对象
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return new JwtUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("roleCode", String.class)
            );
        } catch (Exception ex) {
            // 统一把所有解析失败都按登录失效处理
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
    }
}
