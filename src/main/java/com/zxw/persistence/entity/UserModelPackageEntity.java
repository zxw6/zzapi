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
@TableName("user_model_packages")
@ApiModel("用户套餐实体")
/**
 * 用户套餐实体类。
 * 对应用户购买的模型套餐记录。
 */
/**
 * 用户套餐记录实体。
 * 表示用户已购买套餐的有效期、额度和状态。
 */
public class UserModelPackageEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("套餐记录ID")
    private Long id;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("分组ID")
    private Long groupId;
    @ApiModelProperty("套餐名称")
    private String packageName;
    @ApiModelProperty("购买金额")
    private BigDecimal purchasePrice;
    @ApiModelProperty("开始时间")
    private LocalDateTime startAt;
    @ApiModelProperty("过期时间")
    private LocalDateTime expiresAt;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
