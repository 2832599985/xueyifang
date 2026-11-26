package com.xueyifang.domain.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesServiceVO {
    private Long serviceId;
    private String serviceTitle;
    private Integer sales;
    private BigDecimal revenue;
}
