package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@ApiModel("钱包流水响应")
/**
 * 钱包流水列表项响应对象。
 */
/**
 * 钱包流水列表项响应对象。
 * 用于展示每笔充值或消费后的余额变化。
 */
public record WalletTransactionItemResponse(
        @ApiModelProperty("流水ID")
        Long id,
        @ApiModelProperty("用户ID")
        Long userId,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("钱包ID")
        Long walletId,
        @ApiModelProperty("订单号")
        String orderNo,
        @ApiModelProperty("交易类型")
        String transactionType,
        @ApiModelProperty("收支方向")
        String direction,
        @ApiModelProperty("金额")
        BigDecimal amount,
        @ApiModelProperty("变更前余额")
        BigDecimal balanceBefore,
        @ApiModelProperty("变更后余额")
        BigDecimal balanceAfter,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("描述")
        String descriptionText,
        @ApiModelProperty("交易日期")
        LocalDate transactionDate,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
