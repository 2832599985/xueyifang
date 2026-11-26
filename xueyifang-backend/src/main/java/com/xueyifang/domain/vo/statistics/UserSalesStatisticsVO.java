package com.xueyifang.domain.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserSalesStatisticsVO {
    private Integer totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal averagePrice;
    private SalesServiceVO bestService;
    private List<RecentOrderVO> recentOrders;
}
