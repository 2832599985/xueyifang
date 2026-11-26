package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.ServiceSlotBatchRequest;
import com.xueyifang.domain.po.ServiceTimeSlot;
import com.xueyifang.domain.vo.ServiceTimeSlotVO;

import java.util.List;

public interface IServiceTimeSlotService extends IService<ServiceTimeSlot> {

    List<ServiceTimeSlotVO> listAvailableSlots(Long serviceId);

    void replaceServiceSlots(Long serviceId, Long currentUserId, ServiceSlotBatchRequest request);
}
