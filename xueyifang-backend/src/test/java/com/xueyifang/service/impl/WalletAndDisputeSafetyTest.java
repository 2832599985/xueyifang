package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.DisputeConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.domain.dto.DisputeCreateRequest;
import com.xueyifang.domain.dto.DisputeHandleRequest;
import com.xueyifang.domain.dto.wallet.WalletRechargeRequest;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserChatMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IUserService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.UserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 钱包风控上限与纠纷行锁顺序安全单元测试
 */
@ExtendWith(MockitoExtension.class)
class WalletAndDisputeSafetyTest {

    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private IWalletTransactionService walletTransactionService;
    @Mock
    private IServiceOrderService serviceOrderService;
    @Mock
    private ISysConfigService sysConfigService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Mock
    private ServiceDisputeMapper serviceDisputeMapper;
    @Mock
    private ServiceOrderMapper serviceOrderMapper;
    @Mock
    private IServiceOrderLogService orderLogService;
    @Mock
    private IServiceService serviceService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private UserChatMapper userChatMapper;
    @Mock
    private FileUrlResolver fileUrlResolver;

    @InjectMocks
    private ServiceDisputeServiceImpl disputeService;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, ServiceOrder.class);
        TableInfoHelper.initTableInfo(assistant, ServiceDispute.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void recharge_exceedsSingleLimit_rejectedBeforeLockingUser() {
        User u = new User();
        u.setId(3L);
        UserContext.setCurrentUser(u);
        when(sysConfigService.getIntValue(SysConfigConstant.WALLET_SINGLE_RECHARGE_LIMIT, 5000)).thenReturn(5000);

        WalletRechargeRequest req = new WalletRechargeRequest();
        req.setAmount(new BigDecimal("99999.00"));

        BusinessException ex = assertThrows(BusinessException.class, () -> walletService.recharge(req));
        assertEquals(ErrorCode.WALLET_AMOUNT_INVALID.getCode(), ex.getCode());
        verify(userMapper, never()).selectForUpdate(any());
    }

    @Test
    void recharge_exceedsDailyCumulativeLimit_rejected() {
        User u = new User();
        u.setId(3L);
        u.setAccountStatus(1);
        u.setWalletBalance(new BigDecimal("100.00"));
        UserContext.setCurrentUser(u);

        when(sysConfigService.getIntValue(SysConfigConstant.WALLET_SINGLE_RECHARGE_LIMIT, 5000)).thenReturn(5000);
        when(sysConfigService.getIntValue(SysConfigConstant.WALLET_DAILY_RECHARGE_LIMIT, 20000)).thenReturn(20000);
        when(userMapper.selectForUpdate(3L)).thenReturn(u);

        WalletTransaction earlierToday = new WalletTransaction();
        earlierToday.setAmount(new BigDecimal("18500.00"));
        when(walletTransactionService.list(any())).thenReturn(Collections.singletonList(earlierToday));

        WalletRechargeRequest req = new WalletRechargeRequest();
        req.setAmount(new BigDecimal("2000.00"));

        BusinessException ex = assertThrows(BusinessException.class, () -> walletService.recharge(req));
        assertEquals(ErrorCode.WALLET_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void handleDisputeByAdmin_locksOrderBeforeDisputeAndRefunds() {
        ReflectionTestUtils.setField(disputeService, "baseMapper", serviceDisputeMapper);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(2);
        UserContext.setCurrentUser(admin);

        ServiceDispute dispute = new ServiceDispute();
        dispute.setId(88L);
        dispute.setOrderId(500L);
        dispute.setDisputeStatus(DisputeConstant.STATUS_PENDING);
        dispute.setIsDelete(0);

        ServiceOrder order = new ServiceOrder();
        order.setId(500L);
        order.setOrderNumber("ORDER202611250001");
        order.setBuyerId(10L);
        order.setSellerId(20L);
        order.setPaymentStatus(OrderConstant.PAYMENT_PAID);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_RECEIPT);
        order.setFrozenAmount(new BigDecimal("88.00"));
        order.setIsDelete(0);

        when(serviceDisputeMapper.selectById(88L)).thenReturn(dispute);
        when(serviceOrderMapper.selectForUpdate(500L)).thenReturn(order);
        when(serviceDisputeMapper.selectForUpdate(88L)).thenReturn(dispute);
        when(serviceDisputeMapper.updateById(any(ServiceDispute.class))).thenReturn(1);

        DisputeHandleRequest handleReq = new DisputeHandleRequest();
        handleReq.setActionType(DisputeHandleRequest.ACTION_RESOLVE);
        handleReq.setNeedRefund(true);
        handleReq.setResolution("卖方未按期履约，全额退款");
        handleReq.setAdminReply("经核查沟通记录与交付凭证，支持买方全额退款");

        disputeService.handleDisputeByAdmin(88L, handleReq);

        InOrder orderVerifier = inOrder(serviceOrderMapper, serviceDisputeMapper, serviceOrderService);
        orderVerifier.verify(serviceOrderMapper).selectForUpdate(500L);
        orderVerifier.verify(serviceDisputeMapper).selectForUpdate(88L);
        orderVerifier.verify(serviceOrderService).adminRefundOrder(eq(500L), anyString());
        assertEquals(DisputeConstant.STATUS_RESOLVED, dispute.getDisputeStatus());
    }

    @Test
    void createDispute_locksOrderAndSavesEvidenceImages() {
        ReflectionTestUtils.setField(disputeService, "baseMapper", serviceDisputeMapper);
        User buyer = new User();
        buyer.setId(10L);
        buyer.setNickname("买家同学");
        UserContext.setCurrentUser(buyer);

        ServiceOrder order = new ServiceOrder();
        order.setId(501L);
        order.setOrderNumber("ORDER202611250002");
        order.setBuyerId(10L);
        order.setSellerId(20L);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_RECEIPT);

        when(serviceOrderMapper.selectForUpdate(501L)).thenReturn(order);
        when(serviceDisputeMapper.selectCount(any())).thenReturn(0L);
        when(fileUrlResolver.toRelativeKey(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceDisputeMapper.insert(any(ServiceDispute.class))).thenAnswer(inv -> {
            ServiceDispute d = inv.getArgument(0);
            d.setId(99L);
            return 1;
        });
        when(userService.getById(10L)).thenReturn(buyer);

        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setOrderId(501L);
        req.setDisputeType(1);
        req.setDescription("约定时间未出现");
        req.setEvidenceImages(Collections.singletonList("dispute_evidence/10/202611/chat1.png"));

        Long id = disputeService.createDispute(req);
        assertEquals(99L, id);
        verify(serviceOrderMapper).selectForUpdate(501L);
        verify(orderLogService).recordLog(eq(501L), eq(OrderConstant.ORDER_PENDING_RECEIPT), eq(OrderConstant.ORDER_PENDING_RECEIPT), anyString(), anyString());
    }
}
