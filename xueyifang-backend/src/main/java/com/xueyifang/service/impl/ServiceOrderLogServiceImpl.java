package com.xueyifang.service.impl;

import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceOrderLog;
import com.xueyifang.mapper.ServiceOrderLogMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.service.IServiceOrderLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.UserContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 订单状态/资金变更日志 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
@Service
public class ServiceOrderLogServiceImpl extends ServiceImpl<ServiceOrderLogMapper, ServiceOrderLog> implements IServiceOrderLogService {

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Override
    public void recordLog(Long orderId, Integer oldStatus, Integer newStatus, String actionType, String remark) {
        // 1. 获取当前操作人ID
        Long operatorId = UserContext.getCurrentUserId();

        // 2. 获取订单信息以判断操作人角色
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            return; // 订单不存在时不记录日志
        }

        // 3. 判断操作人角色
        Integer operatorRole;
        if (operatorId == null || operatorId == 0) {
            // 无操作人ID，视为系统操作
            operatorRole = OrderLogConstant.OPERATOR_ROLE_SYSTEM;
        } else if (operatorId.equals(order.getBuyerId())) {
            // 买家
            operatorRole = OrderLogConstant.OPERATOR_ROLE_BUYER;
        } else if (operatorId.equals(order.getSellerId())) {
            // 卖家
            operatorRole = OrderLogConstant.OPERATOR_ROLE_SELLER;
        } else {
            // 其他情况视为管理员
            operatorRole = OrderLogConstant.OPERATOR_ROLE_ADMIN;
        }

        // 4. 构造日志对象
        ServiceOrderLog log = new ServiceOrderLog();
        log.setOrderId(orderId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(operatorId == null ? 0L : operatorId);
        log.setOperatorRole(operatorRole);
        log.setActionType(actionType);
        log.setRemark(remark);

        // 5. 保存日志
        this.save(log);
    }
}
