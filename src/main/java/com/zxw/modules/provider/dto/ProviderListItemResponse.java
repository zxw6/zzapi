package com.zxw.modules.provider.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@ApiModel("渠道列表项响应")
/**
 * 渠道列表项响应对象。
 */
/**
 * 渠道列表项响应对象。
 * 用于展示渠道基础信息、状态和令牌数量。
 */
public record ProviderListItemResponse(
        @ApiModelProperty("渠道ID")
        Long id,
        @ApiModelProperty("渠道编码")
        String providerCode,
        @ApiModelProperty("渠道名称")
        String providerName,
        @ApiModelProperty("基础地址")
        String baseUrl,
        @ApiModelProperty("渠道类型")
        String providerType,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("优先级")
        Integer priorityNo,
        @ApiModelProperty("超时时间毫秒")
        Integer timeoutMs,
        @ApiModelProperty("令牌数量")
        Integer tokenCount,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
