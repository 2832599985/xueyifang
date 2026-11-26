package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包余额视图对象
 * 用于返回给前端的钱包余额信息
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "钱包余额信息", description = "钱包余额视图对象")
public class WalletBalanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "可用余额")
    private BigDecimal walletBalance;

    @ApiModelProperty(value = "冻结金额（担保交易中）")
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "总资产（可用余额 + 冻结金额）")
    private BigDecimal totalAmount;
}
