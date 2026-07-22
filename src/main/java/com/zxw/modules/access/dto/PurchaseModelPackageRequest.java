package com.zxw.modules.access.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;

/**
 * 购买套餐请求。
 */
@ApiModel("购买套餐请求")
/**
 * 购买套餐请求对象。
 * 用于提交要购买的套餐分组主键。
 */
public record PurchaseModelPackageRequest(
        @ApiModelProperty(value = "套餐分组ID", required = true)
        @NotNull(message = "模型分组不能为空")
        Long groupId
) {
}
