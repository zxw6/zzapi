package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("model_group_models")
@ApiModel("模型分组绑定实体")
/**
 * 模型分组绑定实体类。
 * 用于描述模型与套餐分组之间的归属和定价关系。
 */
/**
 * 模型与分组绑定实体。
 * 保存分组下模型的价格覆盖和绑定关系。
 */
public class ModelGroupModelEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("绑定ID")
    private Long id;
    @ApiModelProperty("分组ID")
    private Long groupId;
    @ApiModelProperty("模型ID")
    private Long modelId;
    @ApiModelProperty("计费类型")
    private String billingType;
    @ApiModelProperty("输入价格")
    private BigDecimal promptPrice;
    @ApiModelProperty("缓存输入价格")
    private BigDecimal cachedPromptPrice;
    @ApiModelProperty("输出价格")
    private BigDecimal completionPrice;
    @ApiModelProperty("按次价格")
    private BigDecimal requestPrice;
    @ApiModelProperty("倍率")
    private BigDecimal multiplier;
}
