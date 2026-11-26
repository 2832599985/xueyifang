package com.xueyifang.service;

import com.xueyifang.domain.po.ServiceOrderLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单状态/资金变更日志 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
public interface IServiceOrderLogService extends IService<ServiceOrderLog> {

    /**
     * 记录订单操作日志
     *
     * @param orderId 订单ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param actionType 操作类型
     * @param remark 备注
     */
    void recordLog(Long orderId, Integer oldStatus, Integer newStatus, String actionType, String remark);
}
