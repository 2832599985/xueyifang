package com.xueyifang.domain.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员趋势统计数据
 */
@Data
public class AdminTrendVO {
    /**
     * 日期列表（近7天，格式：MM-dd）
     */
    private List<String> dates;

    /**
     * 每日新增订单数
     */
    private List<Integer> orderCounts;

    /**
     * 每日新增服务数
     */
    private List<Integer> serviceCounts;

    /**
     * 每日交易金额
     */
    private List<BigDecimal> transactionAmounts;
}
