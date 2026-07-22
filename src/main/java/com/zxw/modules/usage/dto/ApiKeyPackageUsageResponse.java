package com.zxw.modules.usage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@ApiModel("API Key package usage response")
public record ApiKeyPackageUsageResponse(
        @ApiModelProperty("Package name")
        String groupName,
        @ApiModelProperty("Package type: QUOTA or BALANCE")
        String packageType,
        @ApiModelProperty("Package status")
        String status,
        @ApiModelProperty("Statistics date")
        LocalDate statDate,
        @ApiModelProperty("Package expiration time")
        LocalDateTime expiresAt,
        @ApiModelProperty("Remaining days")
        Long remainingDays,
        @ApiModelProperty("Package days")
        Integer packageDays,
        @ApiModelProperty("Daily quota")
        BigDecimal dailyQuota,
        @ApiModelProperty("Daily used")
        BigDecimal dailyUsed,
        @ApiModelProperty("Daily remaining")
        BigDecimal dailyRemaining,
        @ApiModelProperty("Weekly quota")
        BigDecimal weeklyQuota,
        @ApiModelProperty("Weekly used")
        BigDecimal weeklyUsed,
        @ApiModelProperty("Weekly remaining")
        BigDecimal weeklyRemaining,
        @ApiModelProperty("Monthly quota")
        BigDecimal monthlyQuota,
        @ApiModelProperty("Monthly used")
        BigDecimal monthlyUsed,
        @ApiModelProperty("Monthly remaining")
        BigDecimal monthlyRemaining,
        @ApiModelProperty("Total quota")
        BigDecimal totalQuota,
        @ApiModelProperty("Total used")
        BigDecimal totalUsed,
        @ApiModelProperty("Total remaining")
        BigDecimal totalRemaining
) {
}
