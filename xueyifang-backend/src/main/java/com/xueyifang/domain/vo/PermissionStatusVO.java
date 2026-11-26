package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "权限审核状态", description = "当前用户的发布权限审核状态")
public class PermissionStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("学号")
    private String studentId;

    @ApiModelProperty("是否已具备发布权限")
    private Boolean hasPermission;

    @ApiModelProperty("发布权限（0=无, 1=有）")
    private Integer publishPermission;

    @ApiModelProperty("权限审核状态（0=未审核/审核中, 1=已批准, 2=已拒绝）")
    private Integer permissionReviewStatus;

    @ApiModelProperty("提交的权限申请理由")
    private String applyReason;

    @ApiModelProperty("状态英文：pending / approved / rejected / unknown")
    private String status;

    @ApiModelProperty("状态中文：审核中 / 已批准 / 已拒绝 / 未知")
    private String statusText;

    @ApiModelProperty("最近一次审核结果：approved / rejected / null")
    private String lastReviewResult;

    @ApiModelProperty("最近一次审核意见（来自通知内容），可能为null")
    private String lastReviewReason;

    @ApiModelProperty("最近一次审核时间，可能为null")
    private LocalDateTime reviewedAt;
}
