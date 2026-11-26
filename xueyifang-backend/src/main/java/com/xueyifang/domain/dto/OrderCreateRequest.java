package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "OrderCreateRequest", description = "创建订单请求")
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "服务ID", required = true)
    private Long serviceId;

    @ApiModelProperty(value = "购买数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "交易类型（1=线下, 2=线上）", required = true)
    private Integer tradeType;

    @ApiModelProperty(value = "交易地点ID（线下交易必填）")
    private Long tradeLocationId;
}
