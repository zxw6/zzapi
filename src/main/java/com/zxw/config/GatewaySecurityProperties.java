package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(
        String jwtSecret,
        long jwtExpireMinutes,
        String cryptoSecret
) {
}
