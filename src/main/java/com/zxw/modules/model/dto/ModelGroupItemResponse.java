package com.zxw.modules.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("模型关联套餐组")
public record ModelGroupItemResponse(
        @ApiModelProperty("套餐组ID")
        Long groupId,
        @ApiModelProperty("套餐组编码")
        String groupCode,
        @ApiModelProperty("套餐组名称")
        String groupName
) {
}
