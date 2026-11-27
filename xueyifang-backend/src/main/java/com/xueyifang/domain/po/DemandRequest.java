package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("demand_request")
public class DemandRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String demandNo;
    private Long publisherId;
    private Long campusId;
    private Long tagId;
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private Integer tradeType;
    private Integer urgencyLevel;
    private LocalDateTime deadlineTime;
    private BigDecimal meetupLat;
    private BigDecimal meetupLng;
    private String meetupPoiName;
    private String meetupAddress;
    private Integer meetupRadiusMeters;
    private Integer bidCount;
    private Integer demandStatus;
    private Long acceptedBidId;
    private Long matchedServiceId;
    private Long matchedOrderId;
    private String closeReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
