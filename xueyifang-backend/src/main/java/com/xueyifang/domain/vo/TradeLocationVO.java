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

    private Long id;
    private Long campusId;
    private String locationName;
    private String locationDescription;
    private String locationAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @ApiModelProperty(value = "允许当面核销的有效半径（米）")
    private Integer validRadiusMeters;

    private Integer hasCctv;
    private Integer isAvailable;
}
