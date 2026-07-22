package com.zxw.modules.access.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@ApiModel("修改套餐分组请求")
public record ModelGroupUpdateRequest(
        @ApiModelProperty(value = "套餐分组编码", required = true)
        @NotBlank(message = "套餐编码不能为空")
        String groupCode,
        @ApiModelProperty(value = "套餐分组名称", required = true)
        @NotBlank(message = "套餐名称不能为空")
        String groupName,
        @ApiModelProperty(value = "套餐类型: QUOTA=额度套餐, BALANCE=余额套餐", required = true)
        @NotBlank(message = "套餐类型不能为空")
        @Pattern(regexp = "QUOTA|BALANCE", flags = Pattern.Flag.CASE_INSENSITIVE, message = "套餐类型仅支持 QUOTA 或 BALANCE")
        String packageType,
        @ApiModelProperty(value = "套餐售价", required = true)
        @NotNull(message = "套餐售价不能为空")
        @DecimalMin(value = "0.0000", message = "套餐售价不能小于 0")
        BigDecimal salePrice,
        @ApiModelProperty(value = "套餐有效天数", required = true)
        @NotNull(message = "套餐天数不能为空")
        Integer packageDays,
        @ApiModelProperty(value = "日额度", required = true)
        @NotNull(message = "日额度不能为空")
        @DecimalMin(value = "0.0000", message = "日额度不能小于 0")
        BigDecimal dailyQuota,
        @ApiModelProperty(value = "周额度", required = true)
        @NotNull(message = "周额度不能为空")
        @DecimalMin(value = "0.0000", message = "周额度不能小于 0")
        BigDecimal weeklyQuota,
        @ApiModelProperty(value = "月额度", required = true)
        @NotNull(message = "月额度不能为空")
        @DecimalMin(value = "0.0000", message = "月额度不能小于 0")
        BigDecimal monthlyQuota,
        @ApiModelProperty("备注")
        String remark
) {
}
