package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel("批量导入模型响应")
/**
 * 模型批量导入响应对象。
 */
/**
 * 模型批量导入响应对象。
 * 返回本次导入成功和跳过的模型结果。
 */
public record ModelBatchImportResponse(
        @ApiModelProperty("导入成功数量")
        int importedCount,
        @ApiModelProperty("跳过数量")
        int skippedCount,
        @ApiModelProperty("导入成功模型列表")
        List<String> importedModels,
        @ApiModelProperty("跳过模型列表")
        List<String> skippedModels
) {
}
