package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 钱包流水表
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wallet_transaction")
@ApiModel(value="WalletTransaction对象", description="钱包流水表")
public class WalletTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "交易类型（1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻）")
    private Integer transactionType;

    @ApiModelProperty(value = "交易金额（正数）")
    private BigDecimal amount;

    @ApiModelProperty(value = "交易前可用余额")
    private BigDecimal balanceBefore;

    @ApiModelProperty(value = "交易后可用余额")
    private BigDecimal balanceAfter;

    @ApiModelProperty(value = "交易前冻结金额")
    private BigDecimal frozenBefore;

    @ApiModelProperty(value = "交易后冻结金额")
    private BigDecimal frozenAfter;

    @ApiModelProperty(value = "关联订单ID（可选）")
    private Long relatedOrderId;

    @ApiModelProperty(value = "流水号")
    private String transactionNo;

    @ApiModelProperty(value = "备注说明")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
