package com.zxw.modules.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletRechargeRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @NotNull(message = "充值金额不能为空")
        @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
        BigDecimal amount,
        String remark
) {
}
