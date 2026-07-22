package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("providers")
@ApiModel("渠道实体")
/**
 * 渠道实体类。
 * 对应上游服务商的基础接入配置。
 */
/**
 * 渠道主数据实体。
 * 保存上游服务商的接入地址、类型和优先级。
 */
public class ProviderEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("渠道ID")
    private Long id;
    @ApiModelProperty("渠道编码")
    private String providerCode;
    @ApiModelProperty("渠道名称")
    private String providerName;
    @ApiModelProperty("基础地址")
    private String baseUrl;
    @ApiModelProperty("渠道类型")
    private String providerType;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("优先级")
    private Integer priorityNo;
    @ApiModelProperty("超时时间毫秒")
    private Integer timeoutMs;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("删除标记")
    private Integer deleted;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
