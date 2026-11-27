package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class DemandBidCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal bidPrice;
    private Integer estimatedHours;
    private String bidPlan;
}
