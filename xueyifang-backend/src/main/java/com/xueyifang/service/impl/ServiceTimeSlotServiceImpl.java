package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.dto.ServiceSlotBatchRequest;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceTimeSlot;
import com.xueyifang.domain.vo.ServiceTimeSlotVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceTimeSlotMapper;
import com.xueyifang.service.IServiceTimeSlotService;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class ServiceTimeSlotServiceImpl extends ServiceImpl<ServiceTimeSlotMapper, ServiceTimeSlot> implements IServiceTimeSlotService {

    @Resource
    private ServiceMapper serviceMapper;

    @Override
    public List<ServiceTimeSlotVO> listAvailableSlots(Long serviceId) {
        if (serviceId == null) {
            return new ArrayList<>();
        }
        List<ServiceTimeSlot> slots = this.list(new LambdaQueryWrapper<ServiceTimeSlot>()
                .eq(ServiceTimeSlot::getServiceId, serviceId)
                .eq(ServiceTimeSlot::getStatus, 1)
                .gt(ServiceTimeSlot::getSlotEnd, LocalDateTime.now())
                .orderByAsc(ServiceTimeSlot::getSlotStart));
        List<ServiceTimeSlotVO> result = new ArrayList<>();
        for (ServiceTimeSlot slot : slots) {
            ServiceTimeSlotVO vo = new ServiceTimeSlotVO();
            BeanUtils.copyProperties(slot, vo);
            int max = slot.getMaxCapacity() != null ? slot.getMaxCapacity() : 1;
            int booked = slot.getBookedCount() != null ? slot.getBookedCount() : 0;
            vo.setRemainingCapacity(Math.max(0, max - booked));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceServiceSlots(Long serviceId, Long currentUserId, ServiceSlotBatchRequest request) {
        if (serviceId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Service service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!currentUserId.equals(service.getSellerId())) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }

        // 仅软删尚未产生预约占用的未来空档期，保护已有预约订单
        this.remove(new LambdaQueryWrapper<ServiceTimeSlot>()
                .eq(ServiceTimeSlot::getServiceId, serviceId)
                .eq(ServiceTimeSlot::getBookedCount, 0));

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty()) {
            return;
        }
        List<ServiceTimeSlot> toSave = new ArrayList<>();
        for (ServiceSlotBatchRequest.SlotItem item : request.getSlots()) {
            if (item == null || item.getSlotStart() == null || item.getSlotEnd() == null) {
                continue;
            }
            if (!item.getSlotEnd().isAfter(item.getSlotStart())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "档期结束时间必须晚于开始时间");
            }
            ServiceTimeSlot slot = new ServiceTimeSlot();
            slot.setServiceId(serviceId);
            slot.setSlotStart(item.getSlotStart());
            slot.setSlotEnd(item.getSlotEnd());
            slot.setMaxCapacity(item.getMaxCapacity() != null && item.getMaxCapacity() > 0 ? item.getMaxCapacity() : 1);
            slot.setBookedCount(0);
            slot.setStatus(1);
            toSave.add(slot);
        }
        if (!toSave.isEmpty()) {
            this.saveBatch(toSave);
        }
    }
}
