package com.xueyifang.domain.vo.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminStatisticsVO {
    private Integer totalUsers;
    private Integer activeUsers;
    private Integer totalServices;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingDisputes;
    private BigDecimal totalTransactionAmount;
    private Integer todayNewUsers;
    private Integer todayNewServices;
    private Integer todayNewOrders;
}
