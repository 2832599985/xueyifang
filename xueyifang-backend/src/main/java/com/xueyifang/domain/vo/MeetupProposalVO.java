package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MeetupProposalVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long proposerId;
    private String proposerName;
    private Integer proposerRole;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String poiName;
    private String detailAddress;
    private Integer radiusMeters;
    private LocalDateTime proposedTime;
    private String remark;
    private Integer proposalStatus;
    private String rejectReason;
    private LocalDateTime respondTime;
    private LocalDateTime createTime;
}
