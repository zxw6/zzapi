package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@ApiModel("更新模型请求")
/**
 * 模型更新请求对象。
 */
/**
 * 模型更新请求对象。
 * 用于修改模型价格、分组和路由配置。
 */
public record ModelUpdateRequest(
        @ApiModelProperty("绑定ID")
        Long bindingId,
        @ApiModelProperty(value = "模型名称", required = true)
        @NotBlank(message = "模型名称不能为空")
        String modelName,
        @ApiModelProperty("模型类型")
        String modelType,
        @ApiModelProperty("计费类型")
        String billingType,
        @ApiModelProperty("输入价格")
        @DecimalMin(value = "0.000000", message = "输入价格不能小于 0")
        BigDecimal promptPrice,
        @ApiModelProperty("缓存输入价格")
        @DecimalMin(value = "0.000000", message = "cached prompt price must be greater than or equal to 0")
        BigDecimal cachedPromptPrice,
        @ApiModelProperty("输出价格")
        @DecimalMin(value = "0.000000", message = "输出价格不能小于 0")
        BigDecimal completionPrice,
        @ApiModelProperty("单次请求价格")
        @DecimalMin(value = "0.000000", message = "单次最低扣费不能小于 0")
        BigDecimal requestPrice,
        @ApiModelProperty("倍率")
        @DecimalMin(value = "0.0000", message = "倍率不能小于 0")
        BigDecimal multiplier,
        @ApiModelProperty("是否公开")
        Boolean isPublic,
        @ApiModelProperty(value = "套餐分组ID", required = true)
        @NotNull(message = "套餐分组不能为空")
        Long groupId,
        @ApiModelProperty(value = "渠道ID", required = true)
        @NotNull(message = "渠道 ID 不能为空")
        Long providerId,
        @ApiModelProperty(value = "上游模型", required = true)
        @NotBlank(message = "上游模型不能为空")
        String upstreamModel
) {
}
