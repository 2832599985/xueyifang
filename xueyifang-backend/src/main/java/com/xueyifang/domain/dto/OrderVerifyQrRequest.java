package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 校内线下交易动态二维码实时核销请求
 */
@Data
@ApiModel(value = "OrderVerifyQrRequest", description = "线下动态二维码扫码核销请求")
public class OrderVerifyQrRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请扫描有效的动态核销二维码")
    @ApiModelProperty(value = "实时摄像头扫出的10秒动态签名载荷（XYF1:...）", required = true)
    private String qrToken;

    @ApiModelProperty(value = "可选：若从特定订单页发起扫码，则强制校验二维码必须归属该订单")
    private Long expectedOrderId;
}
