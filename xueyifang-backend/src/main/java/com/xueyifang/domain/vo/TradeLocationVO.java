package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "交易地点信息", description = "预设校园安全交接点视图对象")
public class TradeLocationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "地点ID")
    private Long id;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    @ApiModelProperty(value = "地点名称")
    private String locationName;

    @ApiModelProperty(value = "地点描述")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址")
    private String locationAddress;

    @ApiModelProperty(value = "纬度（GCJ-02）")
    private BigDecimal latitude;

    @ApiModelProperty(value = "经度（GCJ-02）")
    private BigDecimal longitude;

    @ApiModelProperty(value = "是否监控覆盖安全区（1=是, 0=否）")
    private Integer hasCctv;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）")
    private Integer isAvailable;
}
