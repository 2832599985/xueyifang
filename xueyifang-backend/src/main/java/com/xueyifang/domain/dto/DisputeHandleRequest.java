package com.xueyifang.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理员处理纠纷请求
 *
 * @author xueyifang
 */
@Data
public class DisputeHandleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处理动作类型：1=解决（支持用户诉求） 2=拒绝（驳回纠纷）
     */
    @NotNull(message = "处理动作类型不能为空")
    private Integer actionType;

    /**
     * 管理员处理意见/回复
     */
    @NotBlank(message = "处理意见不能为空")
    private String adminReply;

    /**
     * 解决方案说明（如"全额退款给买家"、"维持原交易结果"）
     */
    @NotBlank(message = "解决方案不能为空")
    private String resolution;

    /**
     * 是否需要系统发起退款（仅线上订单且支付成功时有效）
     */
    private Boolean needRefund;

    /**
     * 处理动作类型常量
     */
    public static final int ACTION_RESOLVE = 1;  // 解决（支持用户诉求）
    public static final int ACTION_REJECT = 2;   // 拒绝（驳回纠纷）
}
