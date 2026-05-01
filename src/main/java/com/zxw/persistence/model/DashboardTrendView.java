package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * 仪表盘趋势视图对象。
 * 表示按天统计的请求量、成功量、Token 与金额趋势。
 */
/**
 * 仪表盘趋势视图对象。
 * 表示按日期聚合后的请求和金额走势数据。
 */
public class DashboardTrendView {

    private LocalDate statDate;
    private Long requestCount;
    private Long successCount;
    private Long totalTokens;
    private BigDecimal userAmount;
    private BigDecimal costAmount;
}
