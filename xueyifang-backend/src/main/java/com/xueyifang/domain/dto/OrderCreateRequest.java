package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单请求（支持预设安全交接点或地图自定义选点 + 档期预约）
 */
@Data
@ApiModel(value = "OrderCreateRequest", description = "创建订单请求")
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "服务ID", required = true)
    private Long serviceId;

    @ApiModelProperty(value = "购买数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "交易类型（1=线下, 2=线上）")
    private Integer tradeType;

    @ApiModelProperty(value = "预设交易地点ID（可选，若用地图自由选点可不传）")
    private Long tradeLocationId;

    @ApiModelProperty(value = "预约档期ID（服务开启档期预约时必选）")
    private Long slotId;

    @ApiModelProperty(value = "期望履约开始时间（未开启固定档期时可自由提议）")
    private LocalDateTime scheduledStart;

    @ApiModelProperty(value = "地图选点纬度（GCJ-02）")
    private BigDecimal meetupLat;

    @ApiModelProperty(value = "地图选点经度（GCJ-02）")
    private BigDecimal meetupLng;

    @ApiModelProperty(value = "地图选点POI名称")
    private String meetupPoiName;

    @ApiModelProperty(value = "会面详细楼层/地标说明")
    private String meetupAddress;

    @ApiModelProperty(value = "买家下单备注")
    private String remark;
}
