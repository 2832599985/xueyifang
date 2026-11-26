package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户聊天记录表
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_chat")
@ApiModel(value="UserChat对象", description="用户聊天记录表")
public class UserChat implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "发送者ID")
    private Long senderId;

    @ApiModelProperty(value = "接收者ID")
    private Long receiverId;

    @ApiModelProperty(value = "聊天内容")
    private String content;

    @ApiModelProperty(value = "消息类型（1=文本, 2=图片, 3=文件）")
    private Integer messageType;

    @ApiModelProperty(value = "是否已读（0=未读, 1=已读）")
    private Integer isRead;

    @ApiModelProperty(value = "关联服务ID（可选）")
    private Long relatedServiceId;

    @ApiModelProperty(value = "关联订单ID（可选）")
    private Long relatedOrderId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除（0=未删除, 1=已删除）")
    @TableLogic
    private Integer isDelete;

}
