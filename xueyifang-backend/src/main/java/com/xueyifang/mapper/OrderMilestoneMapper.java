package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.OrderMilestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMilestoneMapper extends BaseMapper<OrderMilestone> {

    @Select("SELECT * FROM order_milestone WHERE id = #{id} FOR UPDATE")
    OrderMilestone selectForUpdate(@Param("id") Long id);
}
