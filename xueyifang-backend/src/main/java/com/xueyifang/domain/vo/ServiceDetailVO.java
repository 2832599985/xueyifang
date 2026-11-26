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
    private Long tagId;
    private String tagName;
    private Integer tradeType;
    private Long tradeLocationId;
    private String tradeLocationName;
    private BigDecimal price;
    private Integer maxPurchases;
    private Integer currentPurchaseCount;
    private Integer isPhysical;
    private Integer status;           // 服务状态: 1=上架, 2=下架, 3=待审核, 4=驳回
    private Integer reviewStatus;     // 审核状态: 1=通过, 2=待审核, 3=驳回
    private String reviewReason;      // 审核驳回原因
    private Integer viewCount;
    private Integer collectionCount;
    private Integer salesCount;
    private Integer reviewCount;
    private BigDecimal averageRating;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> images;
    private Boolean isCollected;
}
