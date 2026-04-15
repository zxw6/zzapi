package com.zxw.modules.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ModelBatchImportRequest(
        @NotNull(message = "渠道 ID 不能为空")
        Long providerId,
        @NotEmpty(message = "至少选择一个上游模型")
        List<String> upstreamModels,
        @DecimalMin(value = "0.000000", message = "prompt 单价不能小于 0")
        BigDecimal promptPrice,
        @DecimalMin(value = "0.000000", message = "completion 单价不能小于 0")
        BigDecimal completionPrice,
        @DecimalMin(value = "0.0000", message = "倍率不能小于 0")
        BigDecimal multiplier,
        Boolean isPublic
) {
}
