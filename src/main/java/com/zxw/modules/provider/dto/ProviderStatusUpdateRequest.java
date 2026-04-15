package com.zxw.modules.provider.dto;

import jakarta.validation.constraints.NotBlank;

public record ProviderStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
