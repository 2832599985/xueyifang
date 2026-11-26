package com.xueyifang.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息视图
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "聊天消息", description = "单条聊天消息信息")
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "消息ID")
    private Long messageId;

    @ApiModelProperty(value = "发送者信息")
    private UserSimpleVO sender;

    @ApiModelProperty(value = "接收者信息")
    private UserSimpleVO receiver;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "消息类型（1=文本, 2=图片, 3=文件）")
    private Integer messageType;

    @ApiModelProperty(value = "是否已读（0=未读, 1=已读）")
    private Integer isRead;

    @ApiModelProperty(value = "关联服务ID")
    private Long relatedServiceId;

    @ApiModelProperty(value = "关联订单ID")
    private Long relatedOrderId;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
