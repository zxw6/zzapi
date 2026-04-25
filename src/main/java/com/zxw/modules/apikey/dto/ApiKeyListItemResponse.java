package com.zxw.modules.apikey.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApiKeyListItemResponse(
        Long id,
        Long userId,
        String username,
        String name,
        String accessKey,
        String status,
        Long modelPackageId,
        String modelPackageName,
        Long modelGroupId,
        String modelGroupName,
        BigDecimal totalQuota,
        BigDecimal usedQuota,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
}
