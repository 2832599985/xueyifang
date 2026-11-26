package com.xueyifang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueyifang.domain.po.ServiceTimeSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ServiceTimeSlotMapper extends BaseMapper<ServiceTimeSlot> {

    /**
     * 原子占用预约档期名额，杜绝同一时间段并发超约
     */
    @Update("UPDATE service_time_slot SET booked_count = booked_count + #{qty} "
            + "WHERE id = #{slotId} AND is_delete = 0 AND status = 1 AND booked_count + #{qty} <= max_capacity")
    int occupySlotIfAvailable(@Param("slotId") Long slotId, @Param("qty") int qty);

    /**
     * 取消或退单时原子归还预约档期名额
     */
    @Update("UPDATE service_time_slot SET booked_count = GREATEST(0, booked_count - #{qty}) WHERE id = #{slotId}")
    int releaseSlot(@Param("slotId") Long slotId, @Param("qty") int qty);
}
