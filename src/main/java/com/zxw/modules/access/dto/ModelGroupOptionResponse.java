package com.zxw.modules.access.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelGroupOptionResponse(
        Long id,
        String groupCode,
        String groupName,
        BigDecimal salePrice,
        Integer packageDays,
        BigDecimal dailyQuota,
        BigDecimal weeklyQuota,
        BigDecimal monthlyQuota,
        Integer modelCount,
        boolean purchased,
        boolean active,
        LocalDateTime expiresAt,
        Long remainingDays,
        BigDecimal dailyUsed,
        BigDecimal weeklyUsed,
        BigDecimal monthlyUsed,
        String packageStatus,
        String packageStatusText,
        String remark
) {
}
