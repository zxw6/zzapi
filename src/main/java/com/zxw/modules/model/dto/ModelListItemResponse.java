package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApiModel("模型列表项响应")
/**
 * 模型列表项响应对象。
 */
/**
 * 模型列表项响应对象。
 * 用于展示模型价格、分组、渠道和状态等信息。
 */
public record ModelListItemResponse(
        @ApiModelProperty("模型ID")
        Long id,
        @ApiModelProperty("绑定ID")
        Long bindingId,
        @ApiModelProperty("模型编码")
        String modelCode,
        @ApiModelProperty("模型名称")
        String modelName,
        @ApiModelProperty("模型类型")
        String modelType,
        @ApiModelProperty("计费类型")
        String billingType,
        @ApiModelProperty("输入价格")
        BigDecimal promptPrice,
        @ApiModelProperty("缓存输入价格")
        BigDecimal cachedPromptPrice,
        @ApiModelProperty("输出价格")
        BigDecimal completionPrice,
        @ApiModelProperty("请求价格")
        BigDecimal requestPrice,
        @ApiModelProperty("倍率")
        BigDecimal multiplier,
        @ApiModelProperty("是否公开")
        Integer isPublic,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("分组ID")
        Long groupId,
        @ApiModelProperty("分组编码")
        String groupCode,
        @ApiModelProperty("分组名称")
        String groupName,
        @ApiModelProperty("模型关联的全部套餐组")
        List<ModelGroupItemResponse> groups,
        @ApiModelProperty("渠道ID")
        Long providerId,
        @ApiModelProperty("渠道名称")
        String providerName,
        @ApiModelProperty("渠道类型")
        String providerType,
        @ApiModelProperty("上游模型")
        String upstreamModel,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
