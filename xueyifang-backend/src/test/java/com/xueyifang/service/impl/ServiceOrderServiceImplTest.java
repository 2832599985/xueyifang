package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.dto.OrderPayRequest;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.service.IUserService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.UserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单核心流程单元测试：重点验证并发超卖防护、行锁状态校验、退款日志/库存归还、流水号唯一性、米级半径围栏核销强校验
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceImplTest {

    @Mock
    private ServiceOrderMapper serviceOrderMapper;
    @Mock
    private IServiceService serviceService;
    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private com.xueyifang.mapper.ServiceDisputeMapper serviceDisputeMapper;
    @Mock
    private ITradeLocationService tradeLocationService;
    @Mock
    private IServiceOrderLogService orderLogService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private ISysConfigService sysConfigService;
    @Mock
    private IServiceImageService serviceImageService;
    @Mock
    private IServiceReviewService serviceReviewService;
    @Mock
    private IProfessionalService professionalService;
    @Mock
    private IWalletTransactionService walletTransactionService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private com.xueyifang.utils.RedisUtils redisUtils;
    @Mock
    private com.xueyifang.mapper.EscrowPaymentOrderMapper escrowPaymentOrderMapper;

    @InjectMocks
    private ServiceOrderServiceImpl orderService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, ServiceOrder.class);
        TableInfoHelper.initTableInfo(assistant, com.xueyifang.domain.po.ServiceDispute.class);
        TableInfoHelper.initTableInfo(assistant, com.xueyifang.domain.po.EscrowPaymentOrder.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", serviceOrderMapper);
        User buyer = new User();
        buyer.setId(10L);
        buyer.setNickname("买家A");
        UserContext.setCurrentUser(buyer);
        lenient().when(walletTransactionService.save(any(WalletTransaction.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void payOrder_whenStockExhausted_throwsSoldOutAndSkipsWalletDeduction() {
        ServiceOrder order = buildOrder(100L, 10L, 20L, OrderConstant.ORDER_UNPAID, OrderConstant.REFUND_NONE);
        when(serviceOrderMapper.selectForUpdate(100L)).thenReturn(order);
        when(serviceMapper.incrementPurchaseCountIfAvailable(order.getServiceId(), order.getQuantity())).thenReturn(0);

        OrderPayRequest req = new OrderPayRequest();
        req.setPaymentMethod(OrderConstant.PAYMENT_METHOD_WALLET);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.payOrder(100L, req));
        assertEquals(ErrorCode.SERVICE_SOLD_OUT.getCode(), ex.getCode());
        verify(userMapper, never()).selectForUpdate(anyLong());
        verify(walletTransactionService, never()).save(any());
    }

    @Test
    void payOrder_success_recordsDistinctTransactionNos() {
        ServiceOrder order = buildOrder(101L, 10L, 20L, OrderConstant.ORDER_UNPAID, OrderConstant.REFUND_NONE);
        User buyer = buildUser(10L, "500.00", "0.00");
        Service svc = new Service();
        svc.setId(5L);
        svc.setServiceTitle("Java一对一辅导");

        when(serviceOrderMapper.selectForUpdate(101L)).thenReturn(order);
        when(serviceMapper.incrementPurchaseCountIfAvailable(5L, 2)).thenReturn(1);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(serviceService.getById(5L)).thenReturn(svc);

        OrderPayRequest req = new OrderPayRequest();
        req.setPaymentMethod(OrderConstant.PAYMENT_METHOD_WALLET);
        orderService.payOrder(101L, req);

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionService, times(2)).save(txCaptor.capture());
        List<WalletTransaction> txs = txCaptor.getAllValues();
        assertEquals(WalletConstant.TRANSACTION_TYPE_PAYMENT, txs.get(0).getTransactionType());
        assertEquals(WalletConstant.TRANSACTION_TYPE_FREEZE, txs.get(1).getTransactionType());
        assertNotEquals(txs.get(0).getTransactionNo(), txs.get(1).getTransactionNo(),
                "同一事务内生成的支付与冻结流水号必须不同");
        assertEquals(OrderConstant.ORDER_PENDING_SHIP, order.getOrderStatus());
    }

    @Test
    void sellerHandleRefund_approve_logsPreviousStatusOnceAndRestoresStock() {
        User seller = new User();
        seller.setId(20L);
        seller.setNickname("卖家B");
        UserContext.setCurrentUser(seller);

        ServiceOrder order = buildOrder(102L, 10L, 20L, OrderConstant.ORDER_PENDING_RECEIPT, OrderConstant.REFUND_PENDING);
        order.setFrozenAmount(new BigDecimal("100.00"));
        order.setRefundReason("不想要了");
        User buyer = buildUser(10L, "100.00", "100.00");

        when(serviceOrderMapper.selectForUpdate(102L)).thenReturn(order);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);

        orderService.sellerHandleRefund(102L, true, null);

        verify(serviceMapper).decrementPurchaseCount(5L, 2);
        assertEquals(OrderConstant.ORDER_FAILED, order.getOrderStatus());
        assertEquals(OrderConstant.REFUND_COMPLETED, order.getRefundStatus());
        assertEquals(BigDecimal.ZERO, order.getFrozenAmount());
        verify(orderLogService, times(1)).recordLog(
                eq(102L),
                eq(OrderConstant.ORDER_PENDING_RECEIPT),
                eq(OrderConstant.ORDER_FAILED),
                eq(OrderLogConstant.ACTION_SELLER_APPROVE_REFUND),
                anyString()
        );
    }

    @Test
    void requestRefund_pendingShip_logsPreviousStatus2To6() {
        ServiceOrder order = buildOrder(103L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        order.setFrozenAmount(new BigDecimal("100.00"));
        order.setRemark("买家原始备注：请周六下午送达");
        User buyer = buildUser(10L, "0.00", "100.00");

        when(serviceOrderMapper.selectForUpdate(103L)).thenReturn(order);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);

        orderService.requestRefund(103L, "买错了");

        verify(serviceMapper).decrementPurchaseCount(5L, 2);
        assertEquals("买家原始备注：请周六下午送达", order.getRemark(), "退款不应覆盖买家原始下单备注");
        assertEquals("买错了", order.getRefundReason(), "退款原因应写入 refundReason 字段");
        verify(orderLogService, times(1)).recordLog(
                eq(103L),
                eq(OrderConstant.ORDER_PENDING_SHIP),
                eq(OrderConstant.ORDER_FAILED),
                eq(OrderLogConstant.ACTION_REFUND),
                anyString()
        );
    }

    @Test
    void createOrder_whenQuantityZero_throwsParamsError() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setServiceId(5L);
        req.setQuantity(0);
        req.setTradeType(OrderConstant.TRADE_TYPE_ONLINE);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(req));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createOrder_success_usesNewOrderNumberFormat() {
        Service svc = new Service();
        svc.setId(5L);
        svc.setSellerId(20L);
        svc.setStatus(ServiceConstant.STATUS_ONLINE);
        svc.setPrice(new BigDecimal("50.00"));
        svc.setMaxPurchases(10);
        svc.setCurrentPurchaseCount(0);
        when(serviceService.getById(5L)).thenReturn(svc);
        when(serviceOrderMapper.insert(any(ServiceOrder.class))).thenAnswer(inv -> {
            ServiceOrder o = inv.getArgument(0);
            o.setId(999L);
            return 1;
        });

        OrderCreateRequest req = new OrderCreateRequest();
        req.setServiceId(5L);
        req.setQuantity(2);
        req.setTradeType(OrderConstant.TRADE_TYPE_ONLINE);

        Long id = orderService.createOrder(req);
        assertEquals(999L, id);

        ArgumentCaptor<ServiceOrder> orderCaptor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(serviceOrderMapper).insert(orderCaptor.capture());
        assertNotNull(orderCaptor.getValue().getOrderNumber());
        assertTrue(orderCaptor.getValue().getOrderNumber().startsWith("ORDER"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoCancelUnpaidOrders_oneFailureDoesNotStopOthers() {
        when(sysConfigService.getIntValue(SysConfigConstant.ORDER_UNPAID_TIMEOUT_HOURS, 24)).thenReturn(24);
        ServiceOrder bad = buildOrder(201L, 10L, 20L, OrderConstant.ORDER_UNPAID, OrderConstant.REFUND_NONE);
        ServiceOrder good = buildOrder(202L, 10L, 20L, OrderConstant.ORDER_UNPAID, OrderConstant.REFUND_NONE);
        when(serviceOrderMapper.selectList(any())).thenReturn(Arrays.asList(bad, good));

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Boolean> cb = inv.getArgument(0);
            TransactionStatus status = new SimpleTransactionStatus();
            return cb.doInTransaction(status);
        });
        when(serviceOrderMapper.selectForUpdate(201L)).thenThrow(new RuntimeException("模拟单笔DB异常"));
        when(serviceOrderMapper.selectForUpdate(202L)).thenReturn(good);

        orderService.autoCancelUnpaidOrders();

        assertEquals(OrderConstant.ORDER_CANCELLED, good.getOrderStatus());
        verify(orderLogService, times(1)).recordLog(
                eq(202L),
                eq(OrderConstant.ORDER_UNPAID),
                eq(OrderConstant.ORDER_CANCELLED),
                eq(OrderLogConstant.ACTION_AUTO_CANCEL),
                anyString()
        );
        verify(orderLogService, never()).recordLog(eq(201L), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoConfirmReceipt_whenOrderHasActiveDispute_skipsAutoConfirm() {
        when(sysConfigService.getIntValue(SysConfigConstant.AUTO_CONFIRM_RECEIPT_DAYS, 7)).thenReturn(7);
        ServiceOrder disputedOrder = buildOrder(301L, 10L, 20L, OrderConstant.ORDER_PENDING_RECEIPT, OrderConstant.REFUND_NONE);
        disputedOrder.setFrozenAmount(new BigDecimal("100.00"));
        when(serviceOrderMapper.selectList(any())).thenReturn(Arrays.asList(disputedOrder));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Boolean> cb = inv.getArgument(0);
            TransactionStatus status = new SimpleTransactionStatus();
            return cb.doInTransaction(status);
        });
        when(serviceOrderMapper.selectForUpdate(301L)).thenReturn(disputedOrder);
        when(serviceDisputeMapper.selectCount(any())).thenReturn(1L);

        orderService.autoConfirmReceipt();

        assertEquals(OrderConstant.ORDER_PENDING_RECEIPT, disputedOrder.getOrderStatus(),
                "存在未解决纠纷的订单不得被定时任务自动确认收货");
        verify(userMapper, never()).selectForUpdate(anyLong());
        verify(orderLogService, never()).recordLog(eq(301L), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void payOrder_offlineTrade_generatesSeedAndDerivesDynamic6DigitCodeMaskedForSeller() {
        ServiceOrder offlineOrder = buildOrder(401L, 10L, 20L, OrderConstant.ORDER_UNPAID, OrderConstant.REFUND_NONE);
        offlineOrder.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        User buyer = buildUser(10L, "300.00", "0.00");
        User seller = buildUser(20L, "50.00", "0.00");
        Service svc = new Service();
        svc.setId(5L);
        svc.setServiceTitle("游戏本深度清灰");

        when(serviceOrderMapper.selectForUpdate(401L)).thenReturn(offlineOrder);
        when(serviceMapper.incrementPurchaseCountIfAvailable(5L, 2)).thenReturn(1);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(serviceService.getById(5L)).thenReturn(svc);

        OrderPayRequest req = new OrderPayRequest();
        req.setPaymentMethod(OrderConstant.PAYMENT_METHOD_WALLET);
        orderService.payOrder(401L, req);

        assertNotNull(offlineOrder.getVerifyCode());
        assertEquals(16, offlineOrder.getVerifyCode().length(), "库内应存储16位高熵核销密钥种子");

        when(serviceOrderMapper.selectById(401L)).thenReturn(offlineOrder);
        when(userService.listByIds(any())).thenReturn(Arrays.asList(buyer, seller));
        com.xueyifang.domain.vo.OrderDetailVO buyerView = orderService.getOrderDetail(401L);
        assertNotNull(buyerView.getVerifyCode());
        assertTrue(buyerView.getVerifyCode().matches("^\\d{6}$"), "前端下发的动态核销码必须为6位数字");
        assertNotEquals(offlineOrder.getVerifyCode(), buyerView.getVerifyCode());

        com.xueyifang.domain.vo.OrderVerifyCredentialVO cred = orderService.getOfflineVerifyCredential(401L);
        assertEquals(30, cred.getCodePeriodSeconds());
        assertEquals(10, cred.getQrPeriodSeconds());
        assertTrue(cred.getVerifyCode().matches("^\\d{6}$"));
        assertTrue(cred.getQrToken().startsWith("XYF1:401:"));

        UserContext.setCurrentUser(seller);
        com.xueyifang.domain.vo.OrderDetailVO sellerView = orderService.getOrderDetail(401L);
        org.junit.jupiter.api.Assertions.assertNull(sellerView.getVerifyCode(), "卖家端严禁直接读取买家的当面核销码");
        assertThrows(BusinessException.class, () -> orderService.getOfflineVerifyCredential(401L));
    }

    @Test
    void verifyOfflineCode_dynamicTotpCode_completesAndReleasesEscrowImmediately() {
        User seller = buildUser(20L, "50.00", "0.00");
        User buyer = buildUser(10L, "200.00", "100.00");

        ServiceOrder offlineOrder = buildOrder(402L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        offlineOrder.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        offlineOrder.setFrozenAmount(new BigDecimal("100.00"));
        offlineOrder.setVerifyCode("K8M2P5Q9W3X7Z4N6");

        when(serviceOrderMapper.selectById(402L)).thenReturn(offlineOrder);
        String liveCode = orderService.getOfflineVerifyCredential(402L).getVerifyCode();

        UserContext.setCurrentUser(seller);
        when(serviceOrderMapper.selectForUpdate(402L)).thenReturn(offlineOrder);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(20L)).thenReturn(seller);

        orderService.verifyOfflineCode(402L, liveCode);

        assertEquals(OrderConstant.ORDER_COMPLETED, offlineOrder.getOrderStatus());
        assertEquals(BigDecimal.ZERO, offlineOrder.getFrozenAmount());
        assertNotNull(offlineOrder.getSellerShipTime());
        assertNotNull(offlineOrder.getBuyerConfirmTime());
        verify(orderLogService, times(1)).recordLog(
                eq(402L),
                eq(OrderConstant.ORDER_PENDING_SHIP),
                eq(OrderConstant.ORDER_COMPLETED),
                eq(OrderLogConstant.ACTION_OFFLINE_VERIFY),
                anyString()
        );
    }

    @Test
    void verifyOfflineCode_enforcesConfiguredLocationRadiusInMeters() {
        User seller = buildUser(20L, "50.00", "0.00");
        User buyer = buildUser(10L, "200.00", "100.00");

        ServiceOrder offlineOrder = buildOrder(406L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        offlineOrder.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        offlineOrder.setFrozenAmount(new BigDecimal("100.00"));
        offlineOrder.setVerifyCode("M7N4P8Q2R6T9W1X5");
        // 设定图书馆监控区坐标与 60 米严格有效核销半径
        offlineOrder.setMeetupPoiName("主图书馆正门监控区");
        offlineOrder.setMeetupLat(new BigDecimal("30.514000"));
        offlineOrder.setMeetupLng(new BigDecimal("114.354400"));
        offlineOrder.setMeetupRadiusMeters(60);

        when(serviceOrderMapper.selectById(406L)).thenReturn(offlineOrder);
        String liveCode = orderService.getOfflineVerifyCredential(406L).getVerifyCode();

        UserContext.setCurrentUser(seller);
        when(serviceOrderMapper.selectForUpdate(406L)).thenReturn(offlineOrder);

        // 1. 核销人距离约 220 米（超出 60 米半径）：必须被 GEOFENCE_OUT_OF_BOUNDS 拦截
        BusinessException outEx = assertThrows(BusinessException.class, () ->
                orderService.verifyOfflineCode(
                        406L,
                        liveCode,
                        new BigDecimal("30.516000"),
                        new BigDecimal("114.354400")
                )
        );
        assertEquals(ErrorCode.GEOFENCE_OUT_OF_BOUNDS.getCode(), outEx.getCode());
        assertEquals(OrderConstant.ORDER_PENDING_SHIP, offlineOrder.getOrderStatus());

        // 2. 核销人走进约 30 米范围内（<= 60 米半径）：验证通过并结算资金
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(20L)).thenReturn(seller);
        orderService.verifyOfflineCode(
                406L,
                liveCode,
                new BigDecimal("30.514250"),
                new BigDecimal("114.354400")
        );
        assertEquals(OrderConstant.ORDER_COMPLETED, offlineOrder.getOrderStatus());
        assertEquals(BigDecimal.ZERO, offlineOrder.getFrozenAmount());
    }

    @Test
    void verifyOfflineQr_live10SecondToken_completesAndRejectsExpiredScreenshot() {
        User seller = buildUser(20L, "50.00", "0.00");
        User buyer = buildUser(10L, "200.00", "100.00");

        ServiceOrder offlineOrder = buildOrder(404L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        offlineOrder.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        offlineOrder.setFrozenAmount(new BigDecimal("100.00"));
        offlineOrder.setVerifyCode("R9T4V2Y8B6C3D5F7");

        when(serviceOrderMapper.selectById(404L)).thenReturn(offlineOrder);
        String liveQrToken = orderService.getOfflineVerifyCredential(404L).getQrToken();

        UserContext.setCurrentUser(seller);
        when(serviceOrderMapper.selectForUpdate(404L)).thenReturn(offlineOrder);
        String expiredQr = com.xueyifang.utils.OfflineVerifyCodeUtils.generateQrToken(
                404L, offlineOrder.getVerifyCode(), "xueyifang-dev-jwt-secret-change-in-production",
                java.time.Instant.now().getEpochSecond() - 30);
        BusinessException expiredEx = assertThrows(BusinessException.class,
                () -> orderService.verifyOfflineQr(expiredQr, 404L));
        assertTrue(expiredEx.getMessage().contains("已过期"));

        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(20L)).thenReturn(seller);
        Long verifiedId = orderService.verifyOfflineQr(liveQrToken, 404L);

        assertEquals(404L, verifiedId);
        assertEquals(OrderConstant.ORDER_COMPLETED, offlineOrder.getOrderStatus());
        assertEquals(BigDecimal.ZERO, offlineOrder.getFrozenAmount());
    }

    @Test
    void sellerShip_withDeliveryProof_savesNoteAndNormalizedImages() {
        User seller = buildUser(20L, "50.00", "0.00");
        UserContext.setCurrentUser(seller);

        ServiceOrder onlineOrder = buildOrder(403L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        onlineOrder.setTradeType(OrderConstant.TRADE_TYPE_ONLINE);
        when(serviceOrderMapper.selectForUpdate(403L)).thenReturn(onlineOrder);

        com.xueyifang.domain.dto.SellerShipRequest shipReq = new com.xueyifang.domain.dto.SellerShipRequest();
        shipReq.setDeliveryNote("已完成全栈架构辅导，架构图与排障记录见附件");
        shipReq.setDeliveryImages(Arrays.asList("delivery_image/20/202611/proof1.png", "delivery_image/20/202611/proof2.png"));

        orderService.sellerShip(403L, shipReq);

        assertEquals(OrderConstant.ORDER_PENDING_RECEIPT, onlineOrder.getOrderStatus());
        assertEquals("已完成全栈架构辅导，架构图与排障记录见附件", onlineOrder.getDeliveryNote());
        assertEquals("delivery_image/20/202611/proof1.png,delivery_image/20/202611/proof2.png", onlineOrder.getDeliveryImages());
    }

    @Test
    void requestRefund_whenPaidByEscrowGateway_doesNotInflateWalletBalance() {
        ServiceOrder gatewayOrder = buildOrder(405L, 10L, 20L, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.REFUND_NONE);
        gatewayOrder.setPaymentMethod(2);
        gatewayOrder.setFrozenAmount(new BigDecimal("100.00"));

        User buyer = buildUser(10L, "0.00", "100.00");
        when(serviceOrderMapper.selectForUpdate(405L)).thenReturn(gatewayOrder);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);

        orderService.requestRefund(405L, "行程冲突协商取消");

        assertEquals(OrderConstant.ORDER_FAILED, gatewayOrder.getOrderStatus());
        assertEquals(BigDecimal.ZERO, gatewayOrder.getFrozenAmount());
        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionService, times(2)).save(txCaptor.capture());
        for (WalletTransaction tx : txCaptor.getAllValues()) {
            assertEquals(0, BigDecimal.ZERO.compareTo(tx.getBalanceAfter()));
        }
        verify(escrowPaymentOrderMapper, times(1)).update(eq(null), any());
    }

    private static ServiceOrder buildOrder(Long id, Long buyerId, Long sellerId, int status, int refundStatus) {
        ServiceOrder order = new ServiceOrder();
        order.setId(id);
        order.setOrderNumber("ORDER202611230001");
        order.setServiceId(5L);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setQuantity(2);
        order.setUnitPrice(new BigDecimal("50.00"));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setOrderStatus(status);
        order.setPaymentStatus(status == OrderConstant.ORDER_UNPAID
                ? OrderConstant.PAYMENT_UNPAID : OrderConstant.PAYMENT_PAID);
        order.setRefundStatus(refundStatus);
        return order;
    }

    private static User buildUser(Long id, String balance, String frozen) {
        User u = new User();
        u.setId(id);
        u.setNickname("用户" + id);
        u.setWalletBalance(new BigDecimal(balance));
        u.setFrozenAmount(new BigDecimal(frozen));
        return u;
    }
}
