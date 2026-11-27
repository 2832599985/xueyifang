package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.OrderMilestonePlanRequest;
import com.xueyifang.domain.po.OrderMilestone;

import java.util.List;

public interface IOrderMilestoneService extends IService<OrderMilestone> {

    List<OrderMilestone> listByOrderId(Long orderId);

    List<OrderMilestone> saveMilestonePlan(Long orderId, OrderMilestonePlanRequest request);

    void submitMilestoneProof(Long milestoneId, String proofNote);

    void releaseMilestone(Long milestoneId);
}
