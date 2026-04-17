package com.zxw.modules.access.dto;

import jakarta.validation.constraints.NotNull;

public record PurchaseModelPackageRequest(
        @NotNull(message = "模型分组不能为空")
        Long groupId
) {
}
