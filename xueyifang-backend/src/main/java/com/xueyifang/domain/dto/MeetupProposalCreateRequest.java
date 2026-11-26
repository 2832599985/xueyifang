package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发起会面地点与时间协商提案请求（买家和卖家均可发起）
 */
@Data
public class MeetupProposalCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String poiName;
    private String detailAddress;
    private LocalDateTime proposedTime;
    private String remark;
}
