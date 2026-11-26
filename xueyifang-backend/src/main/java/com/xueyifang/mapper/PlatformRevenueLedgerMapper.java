package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.PlatformRevenueLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface PlatformRevenueLedgerMapper extends BaseMapper<PlatformRevenueLedger> {

    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM platform_revenue_ledger")
    BigDecimal sumTotalPlatformRevenue();
}
