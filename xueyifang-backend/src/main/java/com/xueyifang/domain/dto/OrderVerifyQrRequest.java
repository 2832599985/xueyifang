package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "OrderVerifyQrRequest", description = "线下动态二维码扫码核销请求（支持实时地理围栏半径校验）")
public class OrderVerifyQrRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请扫描有效的动态核销二维码")
    @ApiModelProperty(value = "实时摄像头扫出的10秒动态签名载荷（XYF1:...）", required = true)
    private String qrToken;

    @ApiModelProperty(value = "可选：限定必须归属该订单")
    private Long expectedOrderId;

    @ApiModelProperty(value = "核销现场实时纬度（GCJ-02）")
    private BigDecimal verifierLat;

    @ApiModelProperty(value = "核销现场实时经度（GCJ-02）")
    private BigDecimal verifierLng;
}
