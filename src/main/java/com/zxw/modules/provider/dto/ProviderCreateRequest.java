package com.zxw.modules.provider.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

@ApiModel("创建渠道请求")
/**
 * 渠道创建请求对象。
 */
/**
 * 渠道创建请求对象。
 * 用于新增上游渠道及其默认令牌配置。
 */
public record ProviderCreateRequest(
        @ApiModelProperty(value = "渠道编码", required = true)
        @NotBlank(message = "渠道编码不能为空")
        String providerCode,
        @ApiModelProperty(value = "渠道名称", required = true)
        @NotBlank(message = "渠道名称不能为空")
        String providerName,
        @ApiModelProperty(value = "基础地址", required = true)
        @NotBlank(message = "基础地址不能为空")
        String baseUrl,
        @ApiModelProperty("渠道类型")
        String providerType,
        @ApiModelProperty("优先级")
        Integer priorityNo,
        @ApiModelProperty("超时时间毫秒")
        Integer timeoutMs,
        @ApiModelProperty("备注")
        String remark,
        @ApiModelProperty("默认令牌名称")
        String tokenName,
        @ApiModelProperty("默认令牌值")
        String tokenValue,
        @ApiModelProperty("权重")
        Integer weightNo,
        @ApiModelProperty("RPM限制")
        Integer rpmLimit,
        @ApiModelProperty("TPM限制")
        Integer tpmLimit
) {
}
