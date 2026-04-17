package com.zxw.modules.access.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ModelAccessSummaryResponse(
        boolean packageRestrictionEnabled,
        String packageStatus,
        String packageStatusText,
        Long activeGroupId,
        String activeGroupCode,
        String activeGroupName,
        BigDecimal packagePrice,
        BigDecimal dailyQuota,
        BigDecimal weeklyQuota,
        BigDecimal monthlyQuota,
        BigDecimal dailyUsed,
        BigDecimal weeklyUsed,
        BigDecimal monthlyUsed,
        LocalDateTime expiresAt,
        Long remainingDays,
        List<ModelGroupOptionResponse> groups
) {
}
