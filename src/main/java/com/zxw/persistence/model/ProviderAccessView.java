package com.zxw.persistence.model;

import lombok.Data;

@Data
/**
 * 渠道访问视图对象。
 * 用于拉取上游模型时读取渠道基础信息和生效令牌。
 */
/**
 * 渠道访问视图对象。
 * 用于拉取上游模型时读取渠道和令牌信息。
 */
public class ProviderAccessView {

    private Long id;
    private String providerName;
    private String baseUrl;
    private String providerType;
    private Integer timeoutMs;
    private String tokenValueEncrypted;
}
