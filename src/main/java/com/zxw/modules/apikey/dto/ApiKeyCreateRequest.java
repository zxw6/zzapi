package com.zxw.modules.apikey.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApiKeyCreateRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        @DecimalMin(value = "0.00", message = "总额度不能小于 0")
        BigDecimal totalQuota,
        String expiresAt,
        String remark
) {
}
