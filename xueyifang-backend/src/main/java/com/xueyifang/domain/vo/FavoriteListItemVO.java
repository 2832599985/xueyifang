package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏列表项 VO
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "FavoriteListItemVO", description = "收藏列表项")
public class FavoriteListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "收藏ID")
    private Long favoriteId;

    @ApiModelProperty(value = "服务信息")
    private ServiceListItemVO service;

    @ApiModelProperty(value = "卖家名称")
    private String sellerName;

    @ApiModelProperty(value = "收藏时间")
    private LocalDateTime createTime;
}
