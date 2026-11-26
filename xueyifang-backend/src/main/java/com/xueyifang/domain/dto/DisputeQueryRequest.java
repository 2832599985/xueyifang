package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员查询纠纷列表请求
 *
 * @author xueyifang
 */
@Data
public class DisputeQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10）
     */
    private Integer pageSize = 10;

    /**
     * 纠纷状态筛选：1=待审核 2=处理中 3=已解决 4=已拒绝
     */
    private Integer disputeStatus;

    /**
     * 订单编号模糊搜索
     */
    private String orderNumber;

    /**
     * 发起人ID筛选
     */
    private Long initiatorId;

    /**
     * 纠纷类型筛选：1=卖家未履约 2=货不对板 3=质量问题 4=其他
     */
    private Integer disputeType;
}
