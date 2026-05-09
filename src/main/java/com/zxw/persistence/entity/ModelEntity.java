package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("models")
@ApiModel("模型实体")
/**
 * 模型实体类。
 * 对应平台内可对外提供的模型配置。
 */

public class ModelEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("模型ID")
    private Long id;
    @ApiModelProperty("模型编码")
    private String modelCode;
    @ApiModelProperty("模型名称")
    private String modelName;
    @ApiModelProperty("模型类型")
    private String modelType;
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
    @ApiModelProperty("图片价格")
    private BigDecimal imagePrice;
    @ApiModelProperty("倍率")
    private BigDecimal multiplier;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("删除标记")
    private Integer deleted;
    @TableField("is_public")
    @ApiModelProperty("是否公开")
    private Integer isPublic;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
