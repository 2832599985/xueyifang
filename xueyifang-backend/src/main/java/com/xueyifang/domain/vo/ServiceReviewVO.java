package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务评价视图对象
 *
 * @author xueyifang
 * @since 2025-11-30
 */
@Data
@ApiModel(value = "ServiceReviewVO", description = "服务评价视图")
public class ServiceReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评价ID")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("评分(1-5)")
    private Integer rating;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价时间")
    private LocalDateTime createTime;

    @ApiModelProperty("评价人ID（匿名时为null）")
    private Long reviewerId;

    @ApiModelProperty("评价人名称（匿名时为'匿名用户'）")
    private String reviewerName;

    @ApiModelProperty("评价人头像（匿名时为null，前端使用默认头像）")
    private String reviewerAvatar;

    @ApiModelProperty("是否匿名")
    private Boolean anonymous;
}
