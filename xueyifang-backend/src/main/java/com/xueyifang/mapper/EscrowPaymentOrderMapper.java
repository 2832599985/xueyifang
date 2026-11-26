package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.EscrowPaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EscrowPaymentOrderMapper extends BaseMapper<EscrowPaymentOrder> {

    @Select("SELECT * FROM escrow_payment_order WHERE payment_no = #{paymentNo} FOR UPDATE")
    EscrowPaymentOrder selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
}
