package com.zxw.modules.provider.dto;

import java.time.LocalDateTime;

public record ProviderListItemResponse(
        Long id,
        String providerCode,
        String providerName,
        String baseUrl,
        String providerType,
        String status,
        Integer priorityNo,
        Integer timeoutMs,
        Integer tokenCount,
        LocalDateTime createdAt
) {
}
