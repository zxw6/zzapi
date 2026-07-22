package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量导入模型请求。
 */
@ApiModel("批量导入模型请求")
/**
 * 批量导入模型请求对象。
 * 用于把多个上游模型一次性绑定到指定分组和渠道。
 */
public record ModelBatchImportRequest(
        @ApiModelProperty(value = "套餐分组ID", required = true)
        @NotNull(message = "套餐分组不能为空")
        Long groupId,
        @ApiModelProperty(value = "渠道ID", required = true)
        @NotNull(message = "渠道ID不能为空")
        Long providerId,
        @ApiModelProperty(value = "上游模型列表", required = true)
        @NotEmpty(message = "至少选择一个上游模型")
        List<String> upstreamModels,
        @ApiModelProperty("输入价格")
        @DecimalMin(value = "0.000000", message = "输入价格不能小于 0")
        BigDecimal promptPrice,
        @ApiModelProperty("缓存输入价格")
        @DecimalMin(value = "0.000000", message = "缓存输入价格不能小于 0")
        BigDecimal cachedPromptPrice,
        @ApiModelProperty("输出价格")
        @DecimalMin(value = "0.000000", message = "输出价格不能小于 0")
        BigDecimal completionPrice,
        @ApiModelProperty("倍率")
        @DecimalMin(value = "0.0000", message = "倍率不能小于 0")
        BigDecimal multiplier,
        @ApiModelProperty("是否公开")
        Boolean isPublic
) {
}
