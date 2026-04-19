package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.stream.Collectors;

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
        allowedOriginPatterns = defaultIfEmpty(sanitize(allowedOriginPatterns), List.of("*"));
        allowedMethods = defaultIfEmpty(sanitize(allowedMethods), List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        allowedHeaders = defaultIfEmpty(sanitize(allowedHeaders), List.of("*"));
        exposedHeaders = sanitize(exposedHeaders);
        allowCredentials = allowCredentials == null || allowCredentials;
        maxAge = maxAge == null || maxAge <= 0 ? 3600L : maxAge;
    }

    private static List<String> sanitize(List<String> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<String> defaultIfEmpty(List<String> value, List<String> defaults) {
        return value == null || value.isEmpty() ? defaults : value;
    }
}
