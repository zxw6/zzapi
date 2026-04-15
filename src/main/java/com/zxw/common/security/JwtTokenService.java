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
public class JwtTokenService {

    private final SecretKey secretKey;
    private final GatewaySecurityProperties properties;

    public JwtTokenService(GatewaySecurityProperties properties) {
        this.properties = properties;
        String secret = properties.jwtSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.length() < 32) {
            throw new IllegalArgumentException("gateway.security.jwt-secret 至少需要 32 位");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(JwtUser jwtUser) {
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

    public JwtUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return new JwtUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("roleCode", String.class)
            );
        } catch (Exception ex) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
    }
}
