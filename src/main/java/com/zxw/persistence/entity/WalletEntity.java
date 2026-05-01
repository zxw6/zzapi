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
@TableName("wallets")
@ApiModel("钱包实体")
/**
 * 钱包实体类。
 * 用于保存用户余额、冻结金额和累计充值消费信息。
 */
/**
 * 用户钱包实体。
 * 保存当前余额、冻结金额和累计收支统计。
 */
public class WalletEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("钱包ID")
    private Long id;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("可用余额")
    private BigDecimal balance;
    @ApiModelProperty("冻结余额")
    private BigDecimal frozenBalance;
    @ApiModelProperty("累计充值金额")
    private BigDecimal totalRecharge;
    @ApiModelProperty("累计消费金额")
    private BigDecimal totalConsume;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
