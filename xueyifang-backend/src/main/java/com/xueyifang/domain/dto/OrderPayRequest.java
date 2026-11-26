package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付订单请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "OrderPayRequest", description = "支付订单请求")
public class OrderPayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "支付方式（1=钱包余额）", required = true)
    private Integer paymentMethod;
}
