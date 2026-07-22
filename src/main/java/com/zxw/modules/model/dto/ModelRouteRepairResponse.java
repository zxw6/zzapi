package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel("模型路由修复响应")
public record ModelRouteRepairResponse(
        @ApiModelProperty("修复数量")
        int repairedCount,
        @ApiModelProperty("跳过数量")
        int skippedCount,
        @ApiModelProperty("修复列表")
        List<String> repairedModels,
        @ApiModelProperty("跳过列表")
        List<String> skippedModels
) {
}
