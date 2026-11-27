package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.domain.po.EscrowPaymentOrder;
import com.xueyifang.domain.po.FinancialReconciliationDaily;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.po.PlatformRevenueLedger;
import com.xueyifang.domain.po.SellerDepositLedger;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.mapper.AppointmentNoshowClaimMapper;
import com.xueyifang.mapper.CreditScoreLogMapper;
import com.xueyifang.mapper.EscrowPaymentOrderMapper;
import com.xueyifang.mapper.FinancialReconciliationDailyMapper;
import com.xueyifang.mapper.NotificationMapper;
import com.xueyifang.mapper.PlatformRevenueLedgerMapper;
import com.xueyifang.mapper.SellerDepositLedgerMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.mapper.WalletTransactionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinancialReconciliationServiceImplTest {

    @Mock
    private FinancialReconciliationDailyMapper reconciliationDailyMapper;

    @Mock
    private AppointmentNoshowClaimMapper noshowClaimMapper;

    @Mock
    private PlatformRevenueLedgerMapper platformRevenueLedgerMapper;

    @Mock
    private SellerDepositLedgerMapper sellerDepositLedgerMapper;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private CreditScoreLogMapper creditScoreLogMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private EscrowPaymentOrderMapper escrowPaymentOrderMapper;

    @InjectMocks
    private FinancialReconciliationServiceImpl financialService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, EscrowPaymentOrder.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateOrRefreshDailyReport_verifiesBalancedLedgerAndZeroDiscrepancy() {
        ServiceOrder paid1 = new ServiceOrder()
                .setId(101L)
                .setTotalAmount(new BigDecimal("100.00"))
                .setFrozenAmount(new BigDecimal("100.00"))
                .setPaymentStatus(OrderConstant.PAYMENT_PAID)
                .setOrderStatus(OrderConstant.ORDER_COMPLETED)
                .setPlatformFee(new BigDecimal("5.00"))
                .setSellerSettleAmount(new BigDecimal("95.00"));

        // 依次对应 paidOrders, completedOrders, refundedOrders, activeOrders
        when(serviceOrderMapper.selectList(any()))
                .thenReturn(Collections.singletonList(paid1))
                .thenReturn(Collections.singletonList(paid1))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        when(noshowClaimMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reconciliationDailyMapper.selectOne(any())).thenReturn(null);

        FinancialReconciliationDaily report = financialService.generateOrRefreshDailyReport(LocalDate.of(2026, 11, 27));

        assertNotNull(report);
        assertEquals("2026-11-27", report.getReportDate());
        assertEquals(1, report.getPaidOrderCount());
        assertEquals(1, report.getCompletedOrderCount());
        assertEquals(new BigDecimal("100.00"), report.getGrossEscrowInflow());
        assertEquals(new BigDecimal("95.00"), report.getSellerSettledAmount());
        assertEquals(new BigDecimal("5.00"), report.getPlatformCommissionAmount());
        assertEquals(0, report.getDiscrepancyCount());
        assertEquals(1, report.getReconcileStatus());
        verify(reconciliationDailyMapper, times(1)).insert(any(FinancialReconciliationDaily.class));
    }

    @Test
    void processSellerNoShowCompensation_refundsPrincipalAndSlashesSellerDepositToBuyerWallet() {
        ServiceOrder overdueOrder = new ServiceOrder()
                .setId(808L)
                .setOrderNumber("ORD202611270808")
                .setServiceId(55L)
                .setBuyerId(1001L)
                .setSellerId(2002L)
                .setCampusId(1L)
                .setTotalAmount(new BigDecimal("80.00"))
                .setFrozenAmount(new BigDecimal("80.00"))
                .setPaymentMethod(OrderConstant.PAYMENT_METHOD_WALLET)
                .setPaymentStatus(OrderConstant.PAYMENT_PAID)
                .setOrderStatus(OrderConstant.ORDER_PENDING_SHIP)
                .setScheduledStart(LocalDateTime.now().minusHours(3))
                .setScheduledEnd(LocalDateTime.now().minusHours(1));

        when(serviceOrderMapper.selectForUpdate(808L)).thenReturn(overdueOrder);
        when(noshowClaimMapper.selectOne(any())).thenReturn(null);

        User buyer = new User()
                .setId(1001L)
                .setRealName("王买家")
                .setWalletBalance(new BigDecimal("20.00"))
                .setFrozenAmount(new BigDecimal("80.00"))
                .setCreditScore(100);

        User seller = new User()
                .setId(2002L)
                .setRealName("赵卖家")
                .setWalletBalance(new BigDecimal("50.00"))
                .setSellerDeposit(new BigDecimal("100.00"))
                .setCreditScore(100);

        when(userMapper.selectForUpdate(1001L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(2002L)).thenReturn(seller);

        AppointmentNoshowClaim claim = financialService.processSellerNoShowCompensation(
                1001L, 808L, "卖方逾期1小时未到图书馆研讨室履约");

        assertNotNull(claim);
        assertEquals(new BigDecimal("80.00"), claim.getRefundToBuyerAmount());
        // 80.00 * 10% = 8.00 元违约赔偿金
        assertEquals(new BigDecimal("8.00"), claim.getCompensationAmount());
        assertEquals("SELLER_DEPOSIT", claim.getCompensationSource());
        assertEquals(10, claim.getCreditDeducted());

        // 验证买方钱包余额 = 原 20.00 + 退回本金 80.00 + 获赔违约金 8.00 = 108.00，冻结金额清零
        assertEquals(new BigDecimal("108.00"), buyer.getWalletBalance());
        assertEquals(new BigDecimal("0.00"), buyer.getFrozenAmount());

        // 验证卖方履约保证金 = 原 100.00 - 划扣 8.00 = 92.00，信用分 = 100 - 10 = 90
        assertEquals(new BigDecimal("92.00"), seller.getSellerDeposit());
        assertEquals(90, seller.getCreditScore());

        // 验证订单已更新为退款关闭与已退款状态
        assertEquals(OrderConstant.ORDER_FAILED, overdueOrder.getOrderStatus());
        assertEquals(OrderConstant.PAYMENT_REFUNDED, overdueOrder.getPaymentStatus());
        assertEquals(OrderConstant.REFUND_COMPLETED, overdueOrder.getRefundStatus());
        assertEquals(BigDecimal.ZERO, overdueOrder.getFrozenAmount());

        verify(sellerDepositLedgerMapper, times(1)).insert(any(SellerDepositLedger.class));
        verify(walletTransactionMapper, times(2)).insert(any(WalletTransaction.class));
        verify(creditScoreLogMapper, times(1)).insert(any(CreditScoreLog.class));
        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    @Test
    void processSellerNoShowCompensation_whenPaidByEscrowGateway_doesNotInflateWalletWithPrincipal() {
        ServiceOrder gatewayOrder = new ServiceOrder()
                .setId(809L)
                .setOrderNumber("ORD202611270809")
                .setServiceId(55L)
                .setBuyerId(1001L)
                .setSellerId(2002L)
                .setCampusId(1L)
                .setTotalAmount(new BigDecimal("80.00"))
                .setFrozenAmount(new BigDecimal("80.00"))
                .setPaymentMethod(2)
                .setPaymentStatus(OrderConstant.PAYMENT_PAID)
                .setOrderStatus(OrderConstant.ORDER_PENDING_SHIP)
                .setScheduledStart(LocalDateTime.now().minusHours(3))
                .setScheduledEnd(LocalDateTime.now().minusHours(1));

        when(serviceOrderMapper.selectForUpdate(809L)).thenReturn(gatewayOrder);
        when(noshowClaimMapper.selectOne(any())).thenReturn(null);

        User buyer = new User()
                .setId(1001L)
                .setRealName("王买家")
                .setWalletBalance(new BigDecimal("20.00"))
                .setFrozenAmount(new BigDecimal("80.00"))
                .setCreditScore(100);
        User seller = new User()
                .setId(2002L)
                .setRealName("赵卖家")
                .setWalletBalance(new BigDecimal("50.00"))
                .setSellerDeposit(new BigDecimal("100.00"))
                .setCreditScore(100);

        when(userMapper.selectForUpdate(1001L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(2002L)).thenReturn(seller);

        AppointmentNoshowClaim claim = financialService.processSellerNoShowCompensation(
                1001L, 809L, "第三方托管直付爽约赔付验证");

        assertNotNull(claim);
        // 第三方支付本金原路退回，不得误计入钱包可用余额；仅保证金违约赔付 8.00 元入账：20.00 + 8.00 = 28.00
        assertEquals(new BigDecimal("28.00"), buyer.getWalletBalance());
        assertEquals(new BigDecimal("0.00"), buyer.getFrozenAmount());
        assertEquals(OrderConstant.ORDER_FAILED, gatewayOrder.getOrderStatus());
        assertEquals(OrderConstant.PAYMENT_REFUNDED, gatewayOrder.getPaymentStatus());
        assertEquals(OrderConstant.REFUND_COMPLETED, gatewayOrder.getRefundStatus());
        verify(escrowPaymentOrderMapper, times(1)).update(eq(null), any());
    }

    @Test
    void exportFinancialReconciliationCsv_outputsUtf8BomAndThreeSections() {
        FinancialReconciliationDaily r = new FinancialReconciliationDaily()
                .setReportDate("2026-11-27")
                .setReportNo("FIN20261127AAAA")
                .setPaidOrderCount(2)
                .setCompletedOrderCount(2)
                .setGrossEscrowInflow(new BigDecimal("150.00"))
                .setSellerSettledAmount(new BigDecimal("142.50"))
                .setPlatformCommissionAmount(new BigDecimal("7.50"))
                .setReconcileStatus(1)
                .setAuditNote("总分勾稽零差额");

        PlatformRevenueLedger l = new PlatformRevenueLedger()
                .setLedgerNo("REV001")
                .setOrderId(101L)
                .setSellerId(2002L)
                .setOrderTotalAmount(new BigDecimal("100.00"))
                .setCommissionBps(500)
                .setCommissionAmount(new BigDecimal("5.00"))
                .setSellerNetAmount(new BigDecimal("95.00"));

        when(reconciliationDailyMapper.selectList(any())).thenReturn(Collections.singletonList(r));
        when(platformRevenueLedgerMapper.selectList(any())).thenReturn(Collections.singletonList(l));
        when(noshowClaimMapper.selectList(any())).thenReturn(Collections.emptyList());

        String csv = financialService.exportFinancialReconciliationCsv(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 27));

        assertNotNull(csv);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("【一、学艺坊平台财务日终对账汇总表】"));
        assertTrue(csv.contains("FIN20261127AAAA"));
        assertTrue(csv.contains("【二、平台技术服务费抽佣与创作者分账明细台账】"));
        assertTrue(csv.contains("REV001"));
        assertTrue(csv.contains("【三、预约爽约自动赔付存证与保证金划扣明细】"));
    }
}
