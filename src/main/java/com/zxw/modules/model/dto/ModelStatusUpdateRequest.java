package com.zxw.modules.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ModelStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
