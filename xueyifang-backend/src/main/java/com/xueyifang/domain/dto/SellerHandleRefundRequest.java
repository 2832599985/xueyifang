package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 卖家处理退款申请请求
 *
 * @author xueyifang
 * @date 2025-11-28
 */
@Data
@ApiModel(value = "卖家处理退款请求", description = "卖家同意或拒绝退款申请")
public class SellerHandleRefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否同意退款
     */
    @NotNull(message = "请选择同意或拒绝")
    @ApiModelProperty(value = "是否同意退款", required = true)
    private Boolean approve;

    /**
     * 拒绝原因（拒绝时必填）
     */
    @ApiModelProperty(value = "拒绝原因（拒绝时必填）")
    private String rejectReason;
}
