package com.zxw.modules.access.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelPackagePurchaseRecordResponse(
        Long id,
        Long userId,
        String username,
        Long groupId,
        String groupCode,
        String groupName,
        Integer modelCount,
        BigDecimal purchasePrice,
        LocalDateTime startAt,
        LocalDateTime expiresAt,
        String status,
        LocalDateTime createdAt,
        boolean active,
        BigDecimal dailyQuota,
        BigDecimal weeklyQuota,
        BigDecimal monthlyQuota,
        BigDecimal totalQuota,
        BigDecimal dailyUsed,
        BigDecimal weeklyUsed,
        BigDecimal monthlyUsed,
        BigDecimal totalUsed,
        Long remainingDays
) {
}
