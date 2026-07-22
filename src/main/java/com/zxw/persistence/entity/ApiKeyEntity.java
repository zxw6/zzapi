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
@TableName("api_keys")
@ApiModel("API Key实体")
/**
 * API Key 实体类。
 * 对应用户创建的接口访问密钥配置。
 */

public class ApiKeyEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键ID")
    private Long id;
    @ApiModelProperty("所属用户ID")
    private Long userId;
    @ApiModelProperty("Key名称")
    private String name;
    @ApiModelProperty("访问前缀")
    private String accessKey;
    @ApiModelProperty("密钥哈希")
    private String secretHash;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("总额度")
    private BigDecimal totalQuota;
    @ApiModelProperty("已使用额度")
    private BigDecimal usedQuota;
    @ApiModelProperty("过期时间")
    private LocalDateTime expiresAt;
    @ApiModelProperty("绑定的用户套餐ID")
    private Long userPackageId;
    @ApiModelProperty("绑定的模型分组ID")
    private Long modelGroupId;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("最后使用时间")
    private LocalDateTime lastUsedAt;
    @ApiModelProperty("删除标记")
    private Integer deleted;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
