package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "RiskAlertQueryRequest", description = "风控审计预警分页查询请求")
public class RiskAlertQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("页码")
    private Integer pageNum = 1;

    @ApiModelProperty("每页大小")
    private Integer pageSize = 15;

    @ApiModelProperty("命中规则编码筛选")
    private String ruleCode;

    @ApiModelProperty("风险等级筛选（1=低风险, 2=中危, 3=高危）")
    private Integer severity;

    @ApiModelProperty("处置状态筛选（0=待核查, 1=确认违规并处罚, 2=排除误报）")
    private Integer handleStatus;

    @ApiModelProperty("订单号或风控单号关键字")
    private String keyword;
}
