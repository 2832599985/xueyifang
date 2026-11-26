package com.xueyifang.domain.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PendingServiceVO {
    private Long serviceId;
    private String serviceTitle;
    private String coverImage;
    private String description;
    private String sellerName;
    private String tagName;
    private BigDecimal price;
    private Integer status;
    private Integer reviewStatus;
    private String reviewReason;
    private LocalDateTime createTime;
}
