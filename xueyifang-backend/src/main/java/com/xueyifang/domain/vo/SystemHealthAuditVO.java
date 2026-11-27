package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@ApiModel(value = "SystemHealthAuditVO", description = "平台全链路安全、资金勾稽与高并发索引健康度巡检报告")
public class SystemHealthAuditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("巡检批次号")
    private String auditBatchNo;

    @ApiModelProperty("巡检执行时间")
    private LocalDateTime inspectTime;

    @ApiModelProperty("全链路综合安全健康评分（0~100）")
    private Integer overallHealthScore;

    @ApiModelProperty("健康等级（EXCELLENT / WARNING / CRITICAL）")
    private String healthLevel;

    @ApiModelProperty("钱包可用或冻结余额为负数的异常账户数")
    private Integer negativeWalletCount;

    @ApiModelProperty("在途已支付订单与托管冻结金额不一致笔数")
    private Integer escrowMismatchCount;

    @ApiModelProperty("未通过教育邮箱核验却上架在售服务的越权创作者数")
    private Integer unverifiedActiveSellerCount;

    @ApiModelProperty("待处置高危互刷风控预警数")
    private Integer pendingHighRiskAlertCount;

    @ApiModelProperty("逾期未自动退赔的预约订单数")
    private Integer overdueUncompensatedOrderCount;

    @ApiModelProperty("全平台注册学生总数")
    private Integer totalUsers;

    @ApiModelProperty("全平台在架活跃技能服务数")
    private Integer totalActiveServices;

    @ApiModelProperty("全平台累计完结担保订单数")
    private Integer totalCompletedOrders;

    @ApiModelProperty("全平台拼班团课档期总数")
    private Integer totalGroupSessions;

    @ApiModelProperty("全平台反向悬赏需求总数")
    private Integer totalDemands;

    @ApiModelProperty("高并发复合覆盖索引是否已就绪")
    private Boolean compositeIndexOptimized;

    @ApiModelProperty("分项安全巡检明细")
    private List<SecurityCheckItem> securityChecks;

    @Data
    @Accessors(chain = true)
    public static class SecurityCheckItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String checkCode;
        private String checkTitle;
        private Boolean passed;
        private String severity;
        private String detail;
        private String recommendation;
    }
}
