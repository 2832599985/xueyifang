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
@TableName("order_milestone")
public class OrderMilestone implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Integer stageIndex;
    private String title;
    private BigDecimal amount;
    private Integer milestoneStatus; // 0=待提交, 1=已提交待验收, 2=已验收释放资金
    private String proofNote;
    private LocalDateTime submittedTime;
    private LocalDateTime releasedTime;
    private BigDecimal platformFee;
    private BigDecimal sellerNetAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
