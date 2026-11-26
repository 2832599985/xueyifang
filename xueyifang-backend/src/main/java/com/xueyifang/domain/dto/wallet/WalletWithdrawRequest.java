package com.xueyifang.domain.dto.wallet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包提现请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "WalletWithdrawRequest", description = "钱包提现请求")
public class WalletWithdrawRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "提现金额", required = true, example = "50.00")
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;
}
