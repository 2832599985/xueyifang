package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异常高频互刷与反作弊风控审计预警表（v4.3）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("risk_audit_alert")
@ApiModel(value = "RiskAuditAlert对象", description = "异常高频互刷与反作弊风控审计预警表")
public class RiskAuditAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("风控单号（RSK...）")
    private String alertNo;

    @ApiModelProperty("命中规则编码")
    private String ruleCode;

    @ApiModelProperty("命中风控规则名称")
    private String ruleName;

    @ApiModelProperty("风险等级（1=低风险提示, 2=中危可疑预警, 3=高危互刷阻断）")
    private Integer severity;

    @ApiModelProperty("量化风险评分（0~100）")
    private Integer riskScore;

    @ApiModelProperty("触发预警的关联订单ID")
    private Long orderId;

    @ApiModelProperty("关联订单号快照")
    private String orderNumber;

    @ApiModelProperty("关联技能服务ID")
    private Long serviceId;

    @ApiModelProperty("买方用户ID")
    private Long buyerId;

    @ApiModelProperty("卖方用户ID")
    private Long sellerId;

    @ApiModelProperty("所属校区ID")
    private Long campusId;

    @ApiModelProperty("涉事订单金额")
    private BigDecimal orderAmount;

    @ApiModelProperty("近7日同买卖对累计交易笔数")
    private Integer pairOrderCount7d;

    @ApiModelProperty("近7日买卖角色互换反向对倒笔数")
    private Integer reverseOrderCount7d;

    @ApiModelProperty("支付至确认收货耗时（秒）")
    private Long fulfillDurationSeconds;

    @ApiModelProperty("风控取证摘要说明")
    private String evidenceSummary;

    @ApiModelProperty("处置状态（0=待核查, 1=确认违规互刷并处罚, 2=排除误报放行）")
    private Integer handleStatus;

    @ApiModelProperty("执行处罚动作")
    private String penaltyAction;

    @ApiModelProperty("双方各扣减履约信用分值")
    private Integer creditDeducted;

    @ApiModelProperty("处置管理员ID")
    private Long handlerId;

    @ApiModelProperty("管理员核查结论与备注")
    private String handleRemark;

    @ApiModelProperty("处置时间")
    private LocalDateTime handleTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
