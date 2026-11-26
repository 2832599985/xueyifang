package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知列表项VO
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "NotificationVO", description = "通知列表项")
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "通知ID")
    private Long id;

    @ApiModelProperty(value = "通知类型（1=权限批准，2=服务下架，3=订单通知，4=纠纷通知）")
    private Integer notificationType;

    @ApiModelProperty(value = "通知标题")
    private String title;

    @ApiModelProperty(value = "通知内容")
    private String content;

    @ApiModelProperty(value = "关联ID（如订单ID、纠纷ID）")
    private Long relatedId;

    @ApiModelProperty(value = "是否已读（0=未读，1=已读）")
    private Integer isRead;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
