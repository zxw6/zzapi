package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 请求日志列表视图对象。
 * 用于后台请求日志列表页展示主要统计字段。
 */
/**
 * 请求日志列表视图对象。
 * 用于展示调用日志中的模型、金额和耗时信息。
 */
public class RequestLogListView {

    private String requestId;
    private String username;
    private String modelCode;
    private String upstreamModel;
    private String packageName;
    private BigDecimal multiplier;
    private Integer statusCode;
    private Integer latencyMs;
    private Integer firstTokenLatencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer cachedPromptTokens;
    private BigDecimal userAmount;
    private BigDecimal costAmount;
    private Integer success;
    private LocalDateTime createdAt;
}
