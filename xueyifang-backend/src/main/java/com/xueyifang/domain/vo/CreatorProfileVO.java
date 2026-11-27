package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatorProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String displayName;
    private String avatar;
    private String grade;
    private String professionalName;
    private String campusName;
    private String bio;
    private Integer eduVerified;
    private Integer creditScore;
    private String creditTier;
    private Integer publishPermission;
    private BigDecimal sellerDeposit;

    private Long completedOrderCount;
    private Long activeServiceCount;
    private Long reviewCount;
    private BigDecimal averageRating;

    private List<CreatorServiceItem> services;
    private List<CreatorReviewItem> recentReviews;

    @Data
    public static class CreatorServiceItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String serviceTitle;
        private String serviceDescription;
        private String coverImage;
        private String tagName;
        private Integer tradeType;
        private BigDecimal price;
        private Integer currentPurchaseCount;
        private BigDecimal averageRating;
    }

    @Data
    public static class CreatorReviewItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long serviceId;
        private String serviceTitle;
        private String buyerDisplayName;
        private Integer rating;
        private String content;
        private LocalDateTime createTime;
    }
}
