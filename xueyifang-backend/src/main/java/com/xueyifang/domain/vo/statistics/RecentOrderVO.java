package com.xueyifang.domain.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecentOrderVO {
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}
