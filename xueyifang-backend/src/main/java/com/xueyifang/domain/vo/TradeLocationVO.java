package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 交易地点视图对象
 * 用于返回给前端的交易地点信息（不包含内部字段）
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "交易地点信息", description = "交易地点视图对象，用于返回交易地点列表和详情")
public class TradeLocationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "地点ID")
    private Long id;

    @ApiModelProperty(value = "地点名称")
    private String locationName;

    @ApiModelProperty(value = "地点描述")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址")
    private String locationAddress;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）")
    private Integer isAvailable;
}

