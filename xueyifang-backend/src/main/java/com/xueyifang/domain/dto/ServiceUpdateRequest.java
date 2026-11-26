package com.xueyifang.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceUpdateRequest {
    private String serviceTitle;
    private String serviceDescription;
    private Long campusId;
    private Long tagId;
    private Integer tradeType;
    private Long tradeLocationId;
    private BigDecimal price;
    private Integer maxPurchases;
    private Integer isPhysical;
    private Integer requireSlot;
    private Integer cancelFreeHours;
    private Integer requireBleVerify;
    private List<String> images;
}
