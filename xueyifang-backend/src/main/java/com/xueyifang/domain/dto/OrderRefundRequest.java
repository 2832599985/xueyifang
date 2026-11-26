package com.xueyifang.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 订单退款请求
 *
 * @author xueyifang
 * @date 2025-11-21
 */
@Data
public class OrderRefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    /**
     * 备注
     */
    private String remark;

    public String getReason() {
        return reason;
    }
}
