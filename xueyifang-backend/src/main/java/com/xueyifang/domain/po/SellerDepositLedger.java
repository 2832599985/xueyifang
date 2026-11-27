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
@TableName("seller_deposit_ledger")
public class SellerDepositLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String ledgerNo;
    private Long userId;
    private Integer actionType; // 1=缴纳, 2=退还, 3=违约划扣
    private BigDecimal amount;
    private BigDecimal depositBefore;
    private BigDecimal depositAfter;
    private String remark;
    private LocalDateTime createTime;
}
