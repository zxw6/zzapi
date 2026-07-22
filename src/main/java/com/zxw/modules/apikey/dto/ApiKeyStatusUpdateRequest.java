package com.zxw.modules.apikey.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

@ApiModel("更新API Key状态请求")
/**
 * API Key 状态更新请求对象。
 */
/**
 * API Key 状态更新请求对象。
 * 用于启用或停用指定的 API Key。
 */
public record ApiKeyStatusUpdateRequest(
        @ApiModelProperty(value = "状态", required = true)
        @NotBlank(message = "Status must not be blank")
        String status
) {
}
