package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@ApiModel(value = "RiskAuditAlertVO", description = "风控审计预警详情视图")
public class RiskAuditAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String alertNo;
    private String ruleCode;
    private String ruleName;
    private Integer severity;
    private Integer riskScore;
    private Long orderId;
    private String orderNumber;
    private Long serviceId;
    private String serviceTitle;
    private Long buyerId;
    private String buyerName;
    private String buyerStudentId;
    private Integer buyerCreditScore;
    private Long sellerId;
    private String sellerName;
    private String sellerStudentId;
    private Integer sellerCreditScore;
    private Long campusId;
    private BigDecimal orderAmount;
    private Integer pairOrderCount7d;
    private Integer reverseOrderCount7d;
    private Long fulfillDurationSeconds;
    private String evidenceSummary;
    private Integer handleStatus;
    private String penaltyAction;
    private Integer creditDeducted;
    private Long handlerId;
    private String handleRemark;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
