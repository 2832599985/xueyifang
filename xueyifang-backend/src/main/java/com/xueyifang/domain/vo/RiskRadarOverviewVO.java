package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@ApiModel(value = "RiskRadarOverviewVO", description = "风控审计雷达全局态势指标")
public class RiskRadarOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("累计识别风控预警总数")
    private Integer totalAlerts;

    @ApiModelProperty("待人工核查预警数")
    private Integer pendingAlerts;

    @ApiModelProperty("高危互刷/对倒预警数（severity=3）")
    private Integer highSeverityAlerts;

    @ApiModelProperty("已确认违规并执行扣分/下架处罚数")
    private Integer confirmedPenalties;

    @ApiModelProperty("已排除误报数")
    private Integer dismissedAlerts;

    @ApiModelProperty("近7日纳入图谱扫描的订单总数")
    private Integer scannedOrders7d;

    @ApiModelProperty("双向角色对倒互刷命中数")
    private Integer bidirectionalWashCount;

    @ApiModelProperty("同买卖对高频重复交易命中数")
    private Integer highFreqPairCount;

    @ApiModelProperty("极速秒拍秒确异常履约命中数")
    private Integer ultraFastCompletionCount;
}
