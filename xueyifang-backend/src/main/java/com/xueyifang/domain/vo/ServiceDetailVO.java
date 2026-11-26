package com.xueyifang.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceDetailVO {
    private Long id;
    private String serviceTitle;
    private String serviceDescription;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Double sellerRating;
    private String sellerProfessional;
    private String sellerGrade;
    private String sellerBio;
    private Integer sellerCompletedOrders;
    private Integer sellerEduVerified;
    private Integer sellerCreditScore;
    private Long campusId;
    private String campusName;
    private Long tagId;
    private String tagName;
    private Integer tradeType;
    private Long tradeLocationId;
    private String tradeLocationName;
    private BigDecimal tradeLocationLat;
    private BigDecimal tradeLocationLng;
    private BigDecimal price;
    private Integer maxPurchases;
    private Integer currentPurchaseCount;
    private Integer isPhysical;
    private Integer requireSlot;
    private Integer cancelFreeHours;
    private Integer requireBleVerify;
    private Integer status;
    private Integer reviewStatus;
    private String reviewReason;
    private Integer viewCount;
    private Integer collectionCount;
    private Integer salesCount;
    private Integer reviewCount;
    private BigDecimal averageRating;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> images;
    private Boolean isCollected;
    private List<ServiceTimeSlotVO> availableSlots;
}
