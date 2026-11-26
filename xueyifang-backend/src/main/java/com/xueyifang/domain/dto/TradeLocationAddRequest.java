package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 交易地点添加请求
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "交易地点添加请求", description = "添加交易地点请求参数")
public class TradeLocationAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "地点名称", required = true, example = "校园咖啡厅")
    @NotBlank(message = "地点名称不能为空")
    private String locationName;

    @ApiModelProperty(value = "地点描述", example = "学生常聚集场所，环境舒适")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址", example = "校园中心广场旁")
    private String locationAddress;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）", example = "1")
    private Integer isAvailable;
}

