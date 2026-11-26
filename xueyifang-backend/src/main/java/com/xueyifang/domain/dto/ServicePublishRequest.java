package com.xueyifang.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicePublishRequest {
    private String serviceTitle;
    private String serviceDescription;
    private Long tagId;
    private Integer tradeType;
    private Long tradeLocationId;
    private BigDecimal price;
    private Integer maxPurchases;
    private Integer isPhysical;
    private List<String> images;
}
