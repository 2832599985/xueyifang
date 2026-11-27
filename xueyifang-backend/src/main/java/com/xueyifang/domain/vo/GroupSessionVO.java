package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sessionNo;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String creatorProfessional;
    private Integer creatorEduVerified;
    private Integer creatorCreditScore;
    private BigDecimal creatorDeposit;
    private Long campusId;
    private String campusName;
    private Long tagId;
    private String tagName;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private Integer tierTargetCount;
    private BigDecimal tierDiscountPrice;
    private BigDecimal currentUnitPrice;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer joinedCount;
    private Integer tierRebateApplied;
    private Integer tradeType;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private BigDecimal meetupLat;
    private BigDecimal meetupLng;
    private String meetupPoiName;
    private String meetupAddress;
    private Integer meetupRadiusMeters;
    private Integer sessionStatus;
    private Long templateServiceId;
    private String closeReason;
    private LocalDateTime createTime;
    private List<ParticipantItem> participants;

    @Data
    public static class ParticipantItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long buyerId;
        private String buyerName;
        private String buyerAvatar;
        private Long orderId;
        private BigDecimal lockedPrice;
        private BigDecimal effectivePrice;
        private BigDecimal rebateAmount;
        private LocalDateTime createTime;
    }
}
