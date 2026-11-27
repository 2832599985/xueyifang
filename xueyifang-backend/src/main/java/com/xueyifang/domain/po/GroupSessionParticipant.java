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
@TableName("group_session_participant")
public class GroupSessionParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long buyerId;
    private Long orderId;
    private BigDecimal lockedPrice;
    private BigDecimal effectivePrice;
    private BigDecimal rebateAmount;
    private Integer joinStatus; // 1=有效参团, 2=已退出
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
