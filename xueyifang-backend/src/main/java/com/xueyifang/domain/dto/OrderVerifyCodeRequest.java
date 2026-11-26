package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 校园线下交易当面核销码验证请求
 */
@Data
@ApiModel(value = "OrderVerifyCodeRequest", description = "线下当面核销码结单请求")
public class OrderVerifyCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入6位当面核销码")
    @ApiModelProperty(value = "买家出示的6位当面核销码", required = true, example = "582941")
    private String verifyCode;
}
