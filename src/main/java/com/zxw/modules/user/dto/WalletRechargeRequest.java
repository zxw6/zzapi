package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@ApiModel("钱包充值请求")
/**
 * 钱包充值请求对象。
 */
/**
 * 钱包充值请求对象。
 * 用于后台给指定用户追加钱包余额。
 */
public record WalletRechargeRequest(
        @ApiModelProperty(value = "用户ID", required = true)
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @ApiModelProperty(value = "充值金额", required = true)
        @NotNull(message = "充值金额不能为空")
        @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
        BigDecimal amount,
        @ApiModelProperty("备注")
        String remark
) {
}
