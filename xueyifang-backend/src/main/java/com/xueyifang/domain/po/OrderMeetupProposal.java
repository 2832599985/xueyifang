package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("order_meetup_proposal")
public class OrderMeetupProposal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long proposerId;
    private Integer proposerRole;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String poiName;
    private String detailAddress;
    private LocalDateTime proposedTime;
    private String remark;
    private Integer proposalStatus;
    private String rejectReason;
    private LocalDateTime respondTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
