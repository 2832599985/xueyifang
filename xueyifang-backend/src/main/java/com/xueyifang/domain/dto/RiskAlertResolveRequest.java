package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "RiskAlertResolveRequest", description = "管理员处置风控预警请求")
public class RiskAlertResolveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("处置决定（1=确认违规互刷并执行处罚, 2=排除误报正常放行）")
    private Integer handleStatus;

    @ApiModelProperty("处罚动作（DEDUCT_CREDIT_AND_OFFLINE / DEDUCT_CREDIT_ONLY / WARN_ONLY / DISMISSED）")
    private String penaltyAction;

    @ApiModelProperty("双方各扣减履约信用分值（0~40，默认15分）")
    private Integer creditDeductPoints;

    @ApiModelProperty("是否同时强制下架涉事技能服务")
    private Boolean offlineService;

    @ApiModelProperty("管理员核查取证结论备注")
    private String handleRemark;
}
