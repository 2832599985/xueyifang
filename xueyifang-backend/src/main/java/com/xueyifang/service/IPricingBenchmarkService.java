package com.xueyifang.service;

import com.xueyifang.domain.vo.PricingBenchmarkVO;

import java.math.BigDecimal;

public interface IPricingBenchmarkService {

    /**
     * 计算指定分类、校区与交易方式下的历史成交与在售样本分位数定价参考区间
     */
    PricingBenchmarkVO getPricingBenchmark(Long tagId, Long campusId, Integer tradeType, BigDecimal proposedPrice);
}
