package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 网关跨域配置。
 * 对应 application 配置中的 gateway.cors 节点。
 */
@ConfigurationProperties(prefix = "gateway.cors")
public record GatewayCorsProperties(
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        Boolean allowCredentials,
        Long maxAge
) {

    public GatewayCorsProperties {
        // 对空配置做兜底，保证跨域组件始终有可用默认值
        allowedOriginPatterns = defaultIfEmpty(sanitize(allowedOriginPatterns), List.of("*"));
        allowedMethods = defaultIfEmpty(sanitize(allowedMethods), List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        allowedHeaders = defaultIfEmpty(sanitize(allowedHeaders), List.of("*"));
        exposedHeaders = sanitize(exposedHeaders);
        allowCredentials = allowCredentials == null || allowCredentials;
        maxAge = maxAge == null || maxAge <= 0 ? 3600L : maxAge;
    }

    /**
     * 清理列表中的空白配置项。
     */
    private static List<String> sanitize(List<String> value) {
        // 去掉空白项，避免配置中出现无效字符串
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 配置为空时回退到默认值。
     */
    private static List<String> defaultIfEmpty(List<String> value, List<String> defaults) {
        // 配置为空时回退到默认列表
        return value == null || value.isEmpty() ? defaults : value;
    }
}
