package com.zxw.modules.provider.dto;

import jakarta.validation.constraints.NotBlank;

public record ProviderCreateRequest(
        @NotBlank(message = "渠道编码不能为空")
        String providerCode,
        @NotBlank(message = "渠道名称不能为空")
        String providerName,
        @NotBlank(message = "基础地址不能为空")
        String baseUrl,
        String providerType,
        Integer priorityNo,
        Integer timeoutMs,
        String remark,
        String tokenName,
        String tokenValue,
        Integer weightNo,
        Integer rpmLimit,
        Integer tpmLimit
) {
}
