package com.zxw.modules.access.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ModelGroupCreateRequest(
        @NotBlank(message = "套餐编码不能为空")
        String groupCode,
        @NotBlank(message = "套餐名称不能为空")
        String groupName,
        @NotNull(message = "套餐售价不能为空")
        @DecimalMin(value = "0.0000", message = "套餐售价不能小于 0")
        BigDecimal salePrice,
        @NotNull(message = "套餐天数不能为空")
        Integer packageDays,
        @NotNull(message = "日额度不能为空")
        @DecimalMin(value = "0.0000", message = "日额度不能小于 0")
        BigDecimal dailyQuota,
        @NotNull(message = "周额度不能为空")
        @DecimalMin(value = "0.0000", message = "周额度不能小于 0")
        BigDecimal weeklyQuota,
        @NotNull(message = "月额度不能为空")
        @DecimalMin(value = "0.0000", message = "月额度不能小于 0")
        BigDecimal monthlyQuota,
        String remark
) {
}
