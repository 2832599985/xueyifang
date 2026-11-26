package com.xueyifang.domain.dto.wallet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包充值请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "WalletRechargeRequest", description = "钱包充值请求")
public class WalletRechargeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "充值金额", required = true, example = "100.00")
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;
}
