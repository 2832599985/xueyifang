package com.xueyifang.domain.dto.chat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 发送聊天消息请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "ChatSendRequest", description = "发送聊天消息请求")
public class ChatSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "接收者ID", required = true, example = "2")
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    @ApiModelProperty(value = "消息内容", required = true, example = "你好，这个服务的具体内容是什么？")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @ApiModelProperty(value = "消息类型（1=文本, 2=图片, 3=文件）", example = "1")
    private Integer messageType = 1;

    @ApiModelProperty(value = "关联服务ID（可选）", example = "1")
    private Long relatedServiceId;

    @ApiModelProperty(value = "关联订单ID（可选）", example = "1")
    private Long relatedOrderId;
}
