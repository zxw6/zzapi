package com.zxw.modules.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelListItemResponse(
        Long id,
        Long bindingId,
        String modelCode,
        String modelName,
        String modelType,
        String billingType,
        BigDecimal promptPrice,
        BigDecimal cachedPromptPrice,
        BigDecimal completionPrice,
        BigDecimal requestPrice,
        BigDecimal multiplier,
        Integer isPublic,
        String status,
        Long groupId,
        String groupCode,
        String groupName,
        Long providerId,
        String providerName,
        String providerType,
        String upstreamModel,
        LocalDateTime createdAt
) {
}
