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
@TableName("ble_verify_record")
public class BleVerifyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String sessionId;
    private Long buyerId;
    private Long sellerId;
    private String buyerNonce;
    private String sellerNonce;
    private Integer rssiDbm;
    private BigDecimal buyerLat;
    private BigDecimal buyerLng;
    private BigDecimal sellerLat;
    private BigDecimal sellerLng;
    private BigDecimal distanceToMeetupM;
    private String buyerSig;
    private String sellerSig;
    private LocalDateTime createTime;
}
