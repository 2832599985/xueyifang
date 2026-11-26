package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 响应对方会面地点提案请求（同意锁定 / 婉拒说明）
 */
@Data
public class MeetupProposalRespondRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean accept;
    private String rejectReason;
}
