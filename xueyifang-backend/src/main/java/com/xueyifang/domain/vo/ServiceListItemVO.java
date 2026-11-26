package com.xueyifang.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceListItemVO {
    private Long id;
    private String serviceTitle;
    private BigDecimal price;
    private Long tagId;
    private String tagName;
    private Integer tradeType;
    private String coverImage;
    private Integer viewCount;
    private Integer collectionCount;
    private Integer salesCount;
    private Double averageRating;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private LocalDateTime createTime;

    // 以下字段用于"我的服务"列表（卖家视角）
    private Integer status;
    private Integer reviewStatus;
    private String reviewReason;
    private Integer maxPurchases;
    private Integer currentPurchaseCount;
    private LocalDateTime updateTime;
}
