package com.xueyifang.domain.dto.wallet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 钱包交易记录查询请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "WalletTransactionQueryRequest", description = "钱包交易记录查询请求")
public class WalletTransactionQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页数量（默认10）", example = "10")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "交易类型筛选（1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻）")
    private Integer transactionType;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;
}
