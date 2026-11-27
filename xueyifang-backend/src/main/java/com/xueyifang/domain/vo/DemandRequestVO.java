package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemandRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String demandNo;
    private Long publisherId;
    private String publisherName;
    private String publisherAvatar;
    private Integer publisherEduVerified;
    private Integer publisherCreditScore;
    private Long campusId;
    private String campusName;
    private Long tagId;
    private String tagName;
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
    private Integer bidCount;
    private Integer demandStatus;
    private Long acceptedBidId;
    private Long matchedServiceId;
    private Long matchedOrderId;
    private String closeReason;
    private LocalDateTime createTime;
    private List<DemandBidVO> bids;
}
