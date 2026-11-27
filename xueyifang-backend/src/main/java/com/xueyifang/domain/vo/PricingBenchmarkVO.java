package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 基于历史成交与在售样本分位数的技能智能定价参考区间 VO（v4.3）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "PricingBenchmarkVO", description = "校内技能历史成交分位数智能定价参考区间")
public class PricingBenchmarkVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("分类标签ID")
    private Long tagId;

    @ApiModelProperty("分类标签名称")
    private String tagName;

    @ApiModelProperty("所属校区ID")
    private Long campusId;

    @ApiModelProperty("交付方式（1=线上担保, 2=校园线下）")
    private Integer tradeType;

    @ApiModelProperty("历史真实成交订单样本数")
    private Integer completedOrderSampleCount;

    @ApiModelProperty("当前校内在售活跃服务样本数")
    private Integer activeListingSampleCount;

    @ApiModelProperty("参与分位数统计的总有效样本数")
    private Integer totalSampleCount;

    @ApiModelProperty("样本最低价")
    private BigDecimal minPrice;

    @ApiModelProperty("25%分位数（P25 高性价比引流线）")
    private BigDecimal p25Price;

    @ApiModelProperty("50%分位数（P50 校内主流成交中位价）")
    private BigDecimal p50Price;

    @ApiModelProperty("75%分位数（P75 资深创作者品质溢价线）")
    private BigDecimal p75Price;

    @ApiModelProperty("90%分位数（P90 深度定制专家顶配线）")
    private BigDecimal p90Price;

    @ApiModelProperty("样本最高价")
    private BigDecimal maxPrice;

    @ApiModelProperty("样本算术均价")
    private BigDecimal avgPrice;

    @ApiModelProperty("推荐黄金成交区间下限（元）")
    private BigDecimal recommendedMinPrice;

    @ApiModelProperty("推荐黄金成交区间上限（元）")
    private BigDecimal recommendedMaxPrice;

    @ApiModelProperty("当前测算报价（元）")
    private BigDecimal proposedPrice;

    @ApiModelProperty("当前报价在全校同类样本中的预估分位百分比（0~100）")
    private Integer percentileRank;

    @ApiModelProperty("价格带诊断编码（BELOW_P25_VALUE / SWEET_SPOT_P25_P75 / PREMIUM_P75_P90 / ABOVE_P90_OUTLIER）")
    private String pricePositionCode;

    @ApiModelProperty("价格带诊断标签")
    private String pricePositionLabel;

    @ApiModelProperty("转化率与定价策略建议")
    private String conversionAdvice;
}
