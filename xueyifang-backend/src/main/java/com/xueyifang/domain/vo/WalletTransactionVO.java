package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易记录视图对象
 * 用于返回给前端的交易流水信息
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "钱包交易记录", description = "钱包交易记录视图对象")
public class WalletTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "流水ID")
    private Long transactionId;

    @ApiModelProperty(value = "交易类型（1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻）")
    private Integer transactionType;

    @ApiModelProperty(value = "交易类型名称")
    private String typeName;

    @ApiModelProperty(value = "交易金额")
    private BigDecimal amount;

    @ApiModelProperty(value = "交易前余额")
    private BigDecimal balanceBefore;

    @ApiModelProperty(value = "交易后余额")
    private BigDecimal balanceAfter;

    @ApiModelProperty(value = "交易前冻结金额")
    private BigDecimal frozenBefore;

    @ApiModelProperty(value = "交易后冻结金额")
    private BigDecimal frozenAfter;

    @ApiModelProperty(value = "关联订单ID")
    private Long relatedOrderId;

    @ApiModelProperty(value = "关联订单编号")
    private String relatedOrderNumber;

    @ApiModelProperty(value = "流水号")
    private String transactionNo;

    @ApiModelProperty(value = "备注说明")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
