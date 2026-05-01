package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 网关路由查询结果对象。
 * 用于承接模型路由、渠道信息和计费信息的联表结果。
 */
/**
 * 网关路由查询结果对象。
 * 承接模型、渠道和计费信息的联表查询结果。
 */
public class GatewayRouteRow {

    private Long modelId;
    private String modelCode;
    private String modelName;
    private Long providerId;
    private Long providerTokenId;
    private String providerName;
    private String baseUrl;
    private String providerType;
    private Integer timeoutMs;
    private String upstreamModel;
    private String billingType;
    private BigDecimal promptPrice;
    private BigDecimal cachedPromptPrice;
    private BigDecimal completionPrice;
    private BigDecimal requestPrice;
    private BigDecimal multiplier;
    private String tokenValueEncrypted;
}
