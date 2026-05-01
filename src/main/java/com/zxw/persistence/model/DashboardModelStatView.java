package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 仪表盘模型统计视图对象。
 * 用于展示每个模型的调用量、延迟、成功率和金额统计。
 */
/**
 * 仪表盘模型统计视图对象。
 * 承接模型维度的请求量、金额和成功率统计结果。
 */
public class DashboardModelStatView {

    private String modelCode;
    private String upstreamModels;
    private Long requestCount;
    private Long totalTokens;
    private Double avgLatencyMs;
    private Long totalLatencyMs;
    private Double successRate;
    private BigDecimal userAmount;
}
