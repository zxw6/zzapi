package com.zxw.modules.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApiKeyCreateRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        Long modelGroupId,
        String expiresAt,
        String remark
) {
}
