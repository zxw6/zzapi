package com.zxw.modules.dashboard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("控制台模型统计响应")
/**
 * 仪表盘模型统计响应对象。
 */
/**
 * 仪表盘模型统计响应对象。
 * 返回各模型的请求量、成功率和金额统计。
 */
public record DashboardModelStatResponse(
        @ApiModelProperty("模型编码")
        String modelCode,
        @ApiModelProperty("上游模型列表")
        String upstreamModels,
        @ApiModelProperty("请求次数")
        long requestCount,
        @ApiModelProperty("总 token 数")
        long totalTokens,
        @ApiModelProperty("平均延迟毫秒")
        double avgLatencyMs,
        @ApiModelProperty("总延迟毫秒")
        long totalLatencyMs,
        @ApiModelProperty("成功率")
        double successRate,
        @ApiModelProperty("用户侧金额")
        BigDecimal userAmount
) {
}
