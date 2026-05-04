package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("model_groups")
@ApiModel("模型套餐分组实体")
public class ModelGroupEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("分组ID")
    private Long id;

    @ApiModelProperty("分组编码")
    private String groupCode;

    @ApiModelProperty("分组名称")
    private String groupName;

    @ApiModelProperty("套餐类型")
    private String packageType;

    @ApiModelProperty("售价")
    private BigDecimal salePrice;

    @ApiModelProperty("套餐天数")
    private Integer packageDays;

    @ApiModelProperty("日额度")
    private BigDecimal dailyQuota;

    @ApiModelProperty("周额度")
    private BigDecimal weeklyQuota;

    @ApiModelProperty("月额度")
    private BigDecimal monthlyQuota;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
