package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新模型状态请求。
 */
@ApiModel("更新模型状态请求")
/**
 * 模型状态更新请求对象。
 * 用于单独切换模型启用状态。
 */
public record ModelStatusUpdateRequest(
        @ApiModelProperty(value = "状态", required = true)
        @NotBlank(message = "状态不能为空")
        String status
) {
}
