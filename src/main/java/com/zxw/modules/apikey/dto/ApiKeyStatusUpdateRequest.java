package com.zxw.modules.apikey.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiKeyStatusUpdateRequest(
        @NotBlank(message = "Status must not be blank")
        String status
) {
}
