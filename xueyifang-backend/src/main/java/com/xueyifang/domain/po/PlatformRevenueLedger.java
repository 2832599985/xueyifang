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
@TableName("platform_revenue_ledger")
public class PlatformRevenueLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String ledgerNo;
    private Long orderId;
    private Long sellerId;
    private BigDecimal orderTotalAmount;
    private Integer commissionBps;
    private BigDecimal commissionAmount;
    private BigDecimal sellerNetAmount;
    private LocalDateTime createTime;
}
