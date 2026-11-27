package com.xueyifang.service;

import com.xueyifang.domain.vo.OrderDeliveryCertificateVO;
import com.xueyifang.domain.vo.SystemHealthAuditVO;

import java.util.Map;

public interface ISystemHealthAuditService {

    /**
     * 执行全平台资金安全、实名权限、风控积压与高并发复合索引健康度巡检
     */
    SystemHealthAuditVO inspectSystemHealth();

    /**
     * 为已完结担保订单生成 SHA-256 防伪签章的数字履约与校园实践存证证书
     */
    OrderDeliveryCertificateVO generateOrderDeliveryCertificate(Long currentUserId, Long orderId);

    /**
     * 校验数字履约存证证书的 SHA-256 签章真伪
     */
    Map<String, Object> verifyCertificateSeal(Long orderId, String sealHash);
}
