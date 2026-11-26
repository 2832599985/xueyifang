package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "OrderVerifyCodeRequest", description = "线下当面核销码结单请求（支持实时地理围栏半径校验）")
public class OrderVerifyCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入6位当面核销码")
    @ApiModelProperty(value = "买家出示的6位当面核销码", required = true, example = "582941")
    private String verifyCode;

    @ApiModelProperty(value = "核销现场实时纬度（GCJ-02）")
    private BigDecimal verifierLat;

    @ApiModelProperty(value = "核销现场实时经度（GCJ-02）")
    private BigDecimal verifierLng;
}
