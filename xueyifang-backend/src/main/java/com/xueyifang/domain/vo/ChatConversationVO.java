package com.xueyifang.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话视图
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "聊天会话", description = "聊天会话列表项")
public class ChatConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "对方用户ID")
    private Long userId;

    @ApiModelProperty(value = "对方真实姓名")
    private String realName;

    @ApiModelProperty(value = "对方头像URL")
    private String avatar;

    @ApiModelProperty(value = "最后一条消息内容")
    private String lastMessage;

    @ApiModelProperty(value = "未读消息数量")
    private Integer unreadCount;

    @ApiModelProperty(value = "最后消息时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastMessageTime;
}
