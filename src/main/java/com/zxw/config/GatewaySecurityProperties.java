package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关安全配置。
 * 用于承接 JWT 和 AES 加解密相关配置项。
 */
@ConfigurationProperties(prefix = "gateway.security")
/**
 * 网关安全配置对象。
 * 聚合 JWT 密钥、过期时间和 AES 加密密钥。
 */
public record GatewaySecurityProperties(
        String jwtSecret,
        long jwtExpireMinutes,
        String cryptoSecret
) {
}
