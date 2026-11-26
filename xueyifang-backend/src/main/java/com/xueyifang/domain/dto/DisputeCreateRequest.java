package com.xueyifang.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 创建纠纷请求
 *
 * @author xueyifang
 * @date 2025-11-21
 */
@Data
public class DisputeCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 纠纷类型：1=卖家未履约 2=货不对板 3=质量问题 4=其他
     */
    @NotNull(message = "纠纷类型不能为空")
    private Integer disputeType;

    /**
     * 纠纷描述
     */
    @NotBlank(message = "纠纷描述不能为空")
    private String description;

    /**
     * 纠纷举证图片URL列表（选填，最多6张）
     */
    private List<String> evidenceImages;

    public Long getOrderId() {
        return orderId;
    }

    public Integer getDisputeType() {
        return disputeType;
    }

    public String getDescription() {
        return description;
    }
}
