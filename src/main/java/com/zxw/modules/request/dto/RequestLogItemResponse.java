package com.zxw.modules.request.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel("请求日志列表项响应")
/**
 * 请求日志列表项响应对象。
 */
/**
 * 请求日志列表项响应对象。
 * 用于后台查看单次调用的模型、额度和耗时信息。
 */
public record RequestLogItemResponse(
        @ApiModelProperty("请求ID")
        String requestId,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("模型编码")
        String modelCode,
        @ApiModelProperty("上游模型")
        String upstreamModel,
        @ApiModelProperty("套餐名称")
        String packageName,
        @ApiModelProperty("倍率")
        BigDecimal multiplier,
        @ApiModelProperty("状态码")
        Integer statusCode,
        @ApiModelProperty("耗时毫秒")
        Integer latencyMs,
        @ApiModelProperty("输入 token")
        Integer promptTokens,
        @ApiModelProperty("输出 token")
        Integer completionTokens,
        @ApiModelProperty("总 token")
        Integer totalTokens,
        @ApiModelProperty("缓存输入 token")
        Integer cachedPromptTokens,
        @ApiModelProperty("用户侧金额")
        BigDecimal userAmount,
        @ApiModelProperty("成本金额")
        BigDecimal costAmount,
        @ApiModelProperty("是否成功")
        Integer success,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
