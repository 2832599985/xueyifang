package com.xueyifang.service.impl;

import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.RiskAlertResolveRequest;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.po.RiskAuditAlert;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.RiskAuditAlertVO;
import com.xueyifang.mapper.CreditScoreLogMapper;
import com.xueyifang.mapper.NotificationMapper;
import com.xueyifang.mapper.RiskAuditAlertMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiskRadarServiceImplTest {

    @Mock
    private RiskAuditAlertMapper riskAuditAlertMapper;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private ServiceMapper serviceMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditScoreLogMapper creditScoreLogMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private RiskRadarServiceImpl riskRadarService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void scanRecentOrdersForRisk_detectsBidirectionalWashAndUltraFastCompletion() {
        LocalDateTime now = LocalDateTime.now();

        // 订单 1：用户 11 买 用户 22 的服务，且支付后 45 秒即秒拍秒确
        ServiceOrder forwardOrder = new ServiceOrder()
                .setId(5001L)
                .setOrderNumber("ORD202611270001")
                .setServiceId(301L)
                .setBuyerId(11L)
                .setSellerId(22L)
                .setCampusId(1L)
                .setTotalAmount(new BigDecimal("15.00"))
                .setOrderStatus(OrderConstant.ORDER_COMPLETED)
                .setPaymentTime(now.minusSeconds(60))
                .setBuyerConfirmTime(now.minusSeconds(15))
                .setCreateTime(now.minusHours(2));

        // 订单 2：用户 22 反过来买 用户 11 的服务（构成双向角色互换对倒互刷）
        ServiceOrder reverseOrder = new ServiceOrder()
                .setId(5002L)
                .setOrderNumber("ORD202611270002")
                .setServiceId(302L)
                .setBuyerId(22L)
                .setSellerId(11L)
                .setCampusId(1L)
                .setTotalAmount(new BigDecimal("15.00"))
                .setOrderStatus(OrderConstant.ORDER_COMPLETED)
                .setPaymentTime(now.minusHours(1))
                .setBuyerConfirmTime(now.minusMinutes(10))
                .setCreateTime(now.minusHours(1));

        when(serviceOrderMapper.selectList(any())).thenReturn(Arrays.asList(forwardOrder, reverseOrder));
        when(riskAuditAlertMapper.selectOne(any())).thenReturn(null);

        int createdCount = riskRadarService.scanRecentOrdersForRisk(7);

        // forwardOrder 命中 BIDIRECTIONAL_WASH_TRADE + ULTRA_FAST_COMPLETION (2条)，reverseOrder 命中 BIDIRECTIONAL_WASH_TRADE (1条)，共 3 条
        assertEquals(3, createdCount);
        verify(riskAuditAlertMapper, times(3)).insert(any(RiskAuditAlert.class));
    }

    @Test
    void resolveAlert_whenConfirmedViolation_deductsBothUsersCreditAndOfflinesService() {
        RiskAuditAlert pendingAlert = new RiskAuditAlert()
                .setId(900L)
                .setAlertNo("RSK900")
                .setRuleCode(RiskRadarServiceImpl.RULE_BIDIRECTIONAL_WASH)
                .setRuleName("买卖角色互换双向对倒互刷")
                .setSeverity(3)
                .setRiskScore(92)
                .setOrderId(5001L)
                .setOrderNumber("ORD202611270001")
                .setServiceId(301L)
                .setBuyerId(11L)
                .setSellerId(22L)
                .setHandleStatus(0);

        when(riskAuditAlertMapper.selectById(900L)).thenReturn(pendingAlert);

        User buyer = new User().setId(11L).setRealName("张同学").setStudentId("20230011").setCreditScore(100);
        User seller = new User().setId(22L).setRealName("李同学").setStudentId("20230022").setCreditScore(105);
        when(userMapper.selectById(11L)).thenReturn(buyer);
        when(userMapper.selectById(22L)).thenReturn(seller);

        Service service = new Service().setId(301L).setServiceTitle("测试刷分服务").setStatus(1);
        when(serviceMapper.selectById(301L)).thenReturn(service);

        RiskAlertResolveRequest resolveReq = new RiskAlertResolveRequest();
        resolveReq.setHandleStatus(1);
        resolveReq.setCreditDeductPoints(15);
        resolveReq.setOfflineService(true);
        resolveReq.setHandleRemark("核实属于同寝室对倒互刷信誉分");

        RiskAuditAlertVO resolvedVO = riskRadarService.resolveAlert(1L, 900L, resolveReq);

        assertNotNull(resolvedVO);
        assertEquals(1, resolvedVO.getHandleStatus());
        assertEquals(15, resolvedVO.getCreditDeducted());
        assertEquals("DEDUCT_CREDIT_AND_OFFLINE", resolvedVO.getPenaltyAction());

        // 验证买卖双方信用分均被扣减 15 分
        assertEquals(85, buyer.getCreditScore());
        assertEquals(90, seller.getCreditScore());
        // 验证涉事服务已被强制下架 (status = 0)
        assertEquals(0, service.getStatus());

        verify(creditScoreLogMapper, times(2)).insert(any(CreditScoreLog.class));
        verify(notificationMapper, times(2)).insert(any(Notification.class));
        verify(riskAuditAlertMapper, times(1)).updateById(any(RiskAuditAlert.class));
    }
}
