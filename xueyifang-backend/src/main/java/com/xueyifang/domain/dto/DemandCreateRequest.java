package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemandCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campusId;
    private Long tagId;
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private Integer tradeType;
    private Integer urgencyLevel;
    private LocalDateTime deadlineTime;
    private BigDecimal meetupLat;
    private BigDecimal meetupLng;
    private String meetupPoiName;
    private String meetupAddress;
    private Integer meetupRadiusMeters;
}
