package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务表（v3.0 商业版：含多校区、时间段预约规则与近场蓝牙核销开关）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service")
@ApiModel(value = "Service对象", description = "服务表")
public class Service implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String serviceTitle;
    private String serviceDescription;
    private Long sellerId;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    private Long tagId;
    private Integer tradeType;
    private Long tradeLocationId;
    private BigDecimal price;
    private Integer maxPurchases;
    private Integer currentPurchaseCount;
    private Integer isPhysical;

    @ApiModelProperty(value = "是否需选时间段预约（0=否, 1=是）")
    private Integer requireSlot;

    @ApiModelProperty(value = "预约开始前免费退改小时数")
    private Integer cancelFreeHours;

    @ApiModelProperty(value = "是否启用蓝牙近场加密核销（0=否, 1=是）")
    private Integer requireBleVerify;

    private Integer status;
    private Integer reviewStatus;
    private String reviewReason;
    private Long reviewedBy;
    private LocalDateTime reviewTime;
    private Integer viewCount;
    private Integer collectionCount;
    private BigDecimal averageRating;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDelete;
}
