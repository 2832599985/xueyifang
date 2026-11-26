package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 线下当面核销实时凭证（买方专属）：同时下发 30 秒动态数字码与 10 秒防截图动态二维码载荷
 */
@Data
@ApiModel(value = "OrderVerifyCredentialVO", description = "线下当面动态核销凭证")
public class OrderVerifyCredentialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单ID")
    private Long orderId;

    @ApiModelProperty(value = "订单编号")
    private String orderNumber;

    @ApiModelProperty(value = "当前订单状态")
    private Integer orderStatus;

    @ApiModelProperty(value = "当前 30 秒周期动态 6 位数字核销码")
    private String verifyCode;

    @ApiModelProperty(value = "数字核销码刷新周期（秒）")
    private Integer codePeriodSeconds;

    @ApiModelProperty(value = "数字核销码当前剩余有效秒数")
    private Integer codeRemainingSeconds;

    @ApiModelProperty(value = "当前 10 秒周期动态防截图二维码签名载荷")
    private String qrToken;

    @ApiModelProperty(value = "动态二维码刷新周期（秒）")
    private Integer qrPeriodSeconds;

    @ApiModelProperty(value = "动态二维码当前剩余有效秒数")
    private Integer qrRemainingSeconds;

    @ApiModelProperty(value = "服务端基准时间戳（Unix 秒）")
    private Long serverTimestamp;
}
