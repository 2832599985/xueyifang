package com.xueyifang.service.impl;

import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.OrderDeliveryCertificateVO;
import com.xueyifang.domain.vo.SystemHealthAuditVO;
import com.xueyifang.mapper.CampusMapper;
import com.xueyifang.mapper.DemandRequestMapper;
import com.xueyifang.mapper.GroupSessionMapper;
import com.xueyifang.mapper.RiskAuditAlertMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceTagMapper;
import com.xueyifang.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SystemHealthAuditServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ServiceMapper serviceMapper;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private ServiceTagMapper serviceTagMapper;

    @Mock
    private CampusMapper campusMapper;

    @Mock
    private GroupSessionMapper groupSessionMapper;

    @Mock
    private DemandRequestMapper demandRequestMapper;

    @Mock
    private RiskAuditAlertMapper riskAuditAlertMapper;

    @InjectMocks
    private SystemHealthAuditServiceImpl systemHealthAuditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void inspectSystemHealth_whenAllInvariantsHold_returnsScore100AndExcellentLevel() {
        User normalUser = new User()
                .setId(10L)
                .setPublishPermission(1)
                .setWalletBalance(new BigDecimal("120.00"))
                .setFrozenAmount(new BigDecimal("50.00"));
        when(userMapper.selectList(any())).thenReturn(Collections.singletonList(normalUser));

        Service activeSvc = new Service().setId(1L).setSellerId(10L).setStatus(1);
        when(serviceMapper.selectList(any())).thenReturn(Collections.singletonList(activeSvc));

        ServiceOrder activeOrder = new ServiceOrder()
                .setId(100L)
                .setPaymentStatus(OrderConstant.PAYMENT_PAID)
                .setOrderStatus(OrderConstant.ORDER_PENDING_SHIP)
                .setFrozenAmount(new BigDecimal("50.00"));
        when(serviceOrderMapper.selectList(any())).thenReturn(Collections.singletonList(activeOrder));

        when(riskAuditAlertMapper.selectCount(any())).thenReturn(0L);
        when(groupSessionMapper.selectCount(any())).thenReturn(3L);
        when(demandRequestMapper.selectCount(any())).thenReturn(5L);

        SystemHealthAuditVO report = systemHealthAuditService.inspectSystemHealth();

        assertNotNull(report);
        assertEquals(100, report.getOverallHealthScore());
        assertEquals("EXCELLENT", report.getHealthLevel());
        assertEquals(0, report.getNegativeWalletCount());
        assertEquals(0, report.getEscrowMismatchCount());
        assertEquals(6, report.getSecurityChecks().size());
        assertTrue(report.getCompositeIndexOptimized());
    }

    @Test
    void generateOrderDeliveryCertificate_producesDeterministicSha256AndVerifiesSeal() {
        LocalDateTime completedTime = LocalDateTime.of(2026, 11, 27, 6, 0, 0);
        ServiceOrder completedOrder = new ServiceOrder()
                .setId(999L)
                .setOrderNumber("ORD202611270999")
                .setServiceId(88L)
                .setBuyerId(101L)
                .setSellerId(202L)
                .setCampusId(1L)
                .setTotalAmount(new BigDecimal("120.00"))
                .setTradeType(2)
                .setVerifyChannel("BLE_CRYPTO")
                .setMeetupPoiName("图书馆三楼创客研讨室")
                .setOrderStatus(OrderConstant.ORDER_COMPLETED)
                .setBuyerConfirmTime(completedTime);

        when(serviceOrderMapper.selectById(999L)).thenReturn(completedOrder);
        when(serviceMapper.selectById(88L)).thenReturn(new Service().setId(88L).setServiceTitle("ACM算法竞赛一对一特训").setTagId(5L));
        when(serviceTagMapper.selectById(5L)).thenReturn(new ServiceTag().setId(5L).setTagName("编程与算法"));
        when(campusMapper.selectById(1L)).thenReturn(new Campus().setId(1L).setCampusName("中心校区"));
        when(userMapper.selectById(202L)).thenReturn(new User().setId(202L).setRealName("林学长").setStudentId("20220888").setEduVerified(1));
        when(userMapper.selectById(101L)).thenReturn(new User().setId(101L).setRealName("周同学").setStudentId("20240111"));

        OrderDeliveryCertificateVO cert = systemHealthAuditService.generateOrderDeliveryCertificate(202L, 999L);

        assertNotNull(cert);
        assertTrue(cert.getCertificateNo().startsWith("CERT-XYF-999-"));
        assertEquals(64, cert.getSha256Seal().length());
        assertEquals("VERIFIED_AUTHENTIC", cert.getVerificationStatus());
        assertEquals(new BigDecimal("4.0"), cert.getPracticeCreditHours());

        Map<String, Object> verifyOk = systemHealthAuditService.verifyCertificateSeal(999L, cert.getSha256Seal());
        assertEquals(true, verifyOk.get("authentic"));
        assertEquals(cert.getSha256Seal(), verifyOk.get("expectedSeal"));

        Map<String, Object> verifyTampered = systemHealthAuditService.verifyCertificateSeal(999L, "0000000000000000");
        assertEquals(false, verifyTampered.get("authentic"));
        assertFalse(verifyTampered.containsKey("expectedSeal"));
    }
}
