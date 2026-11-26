package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务评价表
 *
 * @author xueyifang
 * @since 2025-11-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service_review")
@ApiModel(value = "ServiceReview对象", description = "服务评价表")
public class ServiceReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "服务ID")
    private Long serviceId;

    @ApiModelProperty(value = "订单ID")
    private Long orderId;

    @ApiModelProperty(value = "评价人ID（买家）")
    private Long buyerId;

    @ApiModelProperty(value = "卖家ID（冗余）")
    private Long sellerId;

    @ApiModelProperty(value = "评分（1-5）")
    private Integer rating;

    @ApiModelProperty(value = "评价内容")
    private String content;

    @ApiModelProperty(value = "是否匿名（0=否, 1=是）")
    private Integer isAnonymous;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除（0=未删, 1=已删）")
    @TableLogic
    private Integer isDelete;
}
