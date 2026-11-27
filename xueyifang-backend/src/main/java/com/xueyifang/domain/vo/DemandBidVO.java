package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemandBidVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long demandId;
    private Long bidderId;
    private String bidderName;
    private String bidderAvatar;
    private String bidderProfessional;
    private Integer bidderEduVerified;
    private Integer bidderCreditScore;
    private BigDecimal bidPrice;
    private Integer estimatedHours;
    private String bidPlan;
    private Integer bidStatus;
    private String demandTitle;
    private Long matchedOrderId;
    private LocalDateTime createTime;
}
