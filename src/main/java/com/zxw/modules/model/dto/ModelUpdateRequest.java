package com.zxw.modules.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ModelUpdateRequest(
        @NotBlank(message = "模型名称不能为空")
        String modelName,
        String modelType,
        String billingType,
        @DecimalMin(value = "0.000000", message = "输入价格不能小于 0")
        BigDecimal promptPrice,
        @DecimalMin(value = "0.000000", message = "输出价格不能小于 0")
        BigDecimal completionPrice,
        @DecimalMin(value = "0.000000", message = "单次最低扣费不能小于 0")
        BigDecimal requestPrice,
        @DecimalMin(value = "0.0000", message = "倍率不能小于 0")
        BigDecimal multiplier,
        Boolean isPublic,
        @NotNull(message = "套餐分组不能为空")
        Long groupId,
        @NotNull(message = "渠道 ID 不能为空")
        Long providerId,
        @NotBlank(message = "上游模型不能为空")
        String upstreamModel
) {
}
