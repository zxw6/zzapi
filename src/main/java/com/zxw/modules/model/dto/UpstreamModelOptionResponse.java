package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("上游模型选项响应")
/**
 * 上游模型选项响应对象。
 */
/**
 * 上游模型选项响应对象。
 * 用于前端选择可导入的上游模型列表。
 */
public record UpstreamModelOptionResponse(
        @ApiModelProperty("模型ID")
        String id,
        @ApiModelProperty("展示名称")
        String displayName,
        @ApiModelProperty("所属方")
        String ownedBy,
        @ApiModelProperty("渠道类型")
        String providerType
) {
}
