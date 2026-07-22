package com.zxw.modules.dashboard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@ApiModel("控制台趋势点响应")
/**
 * 仪表盘趋势点响应对象。
 */
/**
 * 仪表盘趋势点响应对象。
 * 表示某一天的请求量、Token 用量和金额走势。
 */
public record DashboardTrendPointResponse(
        @ApiModelProperty("统计日期")
        LocalDate statDate,
        @ApiModelProperty("请求次数")
        long requestCount,
        @ApiModelProperty("成功次数")
        long successCount,
        @ApiModelProperty("总 token 数")
        long totalTokens,
        @ApiModelProperty("用户侧金额")
        BigDecimal userAmount,
        @ApiModelProperty("成本金额")
        BigDecimal costAmount
) {
}
