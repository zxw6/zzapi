package com.zxw.modules.provider.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

@ApiModel("更新渠道状态请求")
/**
 * 渠道状态更新请求对象。
 */
/**
 * 渠道状态更新请求对象。
 * 用于切换渠道的启用或停用状态。
 */
public record ProviderStatusUpdateRequest(
        @ApiModelProperty(value = "状态", required = true)
        @NotBlank(message = "状态不能为空")
        String status
) {
}
