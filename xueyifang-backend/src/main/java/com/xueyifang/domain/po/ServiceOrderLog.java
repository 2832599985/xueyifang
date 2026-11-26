package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单状态/资金变更日志
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service_order_log")
@ApiModel(value="ServiceOrderLog对象", description="订单状态/资金变更日志")
public class ServiceOrderLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单ID")
    private Long orderId;

    @ApiModelProperty(value = "变更前订单状态")
    private Integer oldStatus;

    @ApiModelProperty(value = "变更后订单状态")
    private Integer newStatus;

    @ApiModelProperty(value = "操作人ID（为空表示系统自动）")
    private Long operatorId;

    @ApiModelProperty(value = "操作人角色（1=买家,2=卖家,3=管理员,4=系统）")
    private Integer operatorRole;

    @ApiModelProperty(value = "动作类型（PAY/BUYER_CONFIRM/SELLER_CONFIRM/AUTO_RELEASE/REFUND/CANCEL/DISPUTE_CREATE等）")
    private String actionType;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


}
