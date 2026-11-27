package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupSessionCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campusId;
    private Long tagId;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private Integer tierTargetCount;
    private BigDecimal tierDiscountPrice;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer tradeType;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private BigDecimal meetupLat;
    private BigDecimal meetupLng;
    private String meetupPoiName;
    private String meetupAddress;
    private Integer meetupRadiusMeters;
}
