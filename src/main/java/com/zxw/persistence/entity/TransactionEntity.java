package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("transactions")
@ApiModel("交易流水实体")
/**
 * 交易流水实体类。
 * 对应钱包充值、消费等资金变动记录。
 */
/**
 * 钱包交易流水实体。
 * 记录充值、消费等资金变动明细。
 */
public class TransactionEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("流水ID")
    private Long id;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("钱包ID")
    private Long walletId;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("交易类型")
    private String transactionType;
    @ApiModelProperty("收支方向")
    private String direction;
    @ApiModelProperty("交易金额")
    private BigDecimal amount;
    @ApiModelProperty("变更前余额")
    private BigDecimal balanceBefore;
    @ApiModelProperty("变更后余额")
    private BigDecimal balanceAfter;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("描述")
    private String descriptionText;
    @ApiModelProperty("交易日期")
    private LocalDate transactionDate;
}
