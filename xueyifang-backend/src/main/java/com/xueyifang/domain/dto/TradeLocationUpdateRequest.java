package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 交易地点更新请求
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "交易地点更新请求", description = "更新交易地点请求参数")
public class TradeLocationUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "地点ID", required = true, example = "1")
    @NotNull(message = "地点ID不能为空")
    private Long id;

    @ApiModelProperty(value = "地点名称", example = "校园咖啡厅")
    private String locationName;

    @ApiModelProperty(value = "地点描述", example = "学生常聚集场所，环境舒适")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址", example = "校园中心广场旁")
    private String locationAddress;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）", example = "1")
    private Integer isAvailable;
}

