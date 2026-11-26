package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 服务标签视图对象
 * 用于返回给前端的服务标签信息（不包含内部字段）
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "服务标签信息", description = "服务标签视图对象，用于返回服务标签列表和详情")
public class ServiceTagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "标签ID")
    private Long tagId;

    @ApiModelProperty(value = "标签名称")
    private String tagName;

    @ApiModelProperty(value = "标签描述")
    private String description;

    @ApiModelProperty(value = "标签图标URL")
    private String icon;
}

