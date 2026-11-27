package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.controller.SellerDepositController;
import com.xueyifang.domain.po.OrderMilestone;
import com.xueyifang.domain.po.SellerDepositLedger;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.OrderMilestoneMapper;
import com.xueyifang.mapper.SellerDepositLedgerMapper;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICreditScoreService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.UserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * v4.1 分阶段里程碑部分结算与创作者保证金风控单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderMilestoneAndDepositTest {

    @Mock
    private OrderMilestoneMapper orderMilestoneMapper;
    @Mock
    private ServiceOrderMapper serviceOrderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private IWalletTransactionService walletTransactionService;
    @Mock
    private ISysConfigService sysConfigService;
    @Mock
    private IServiceOrderLogService orderLogService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private ICreditScoreService creditScoreService;
    @Mock
    private SellerDepositLedgerMapper depositLedgerMapper;
    @Mock
    private ServiceDisputeMapper serviceDisputeMapper;

    @InjectMocks
    private OrderMilestoneServiceImpl milestoneService;

    @InjectMocks
    private SellerDepositController depositController;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OrderMilestone.class);
        TableInfoHelper.initTableInfo(assistant, SellerDepositLedger.class);
        TableInfoHelper.initTableInfo(assistant, ServiceOrder.class);
        TableInfoHelper.initTableInfo(assistant, ServiceDispute.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(milestoneService, "baseMapper", orderMilestoneMapper);
        lenient().when(walletTransactionService.save(any(WalletTransaction.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void releaseMilestone_partialThenFinalStage_settlesProportionallyAndCompletesOrder() {
        User buyer = new User().setId(10L).setWalletBalance(new BigDecimal("50.00")).setFrozenAmount(new BigDecimal("200.00"));
        User seller = new User().setId(20L).setWalletBalance(new BigDecimal("0.00")).setFrozenAmount(BigDecimal.ZERO);
        UserContext.setCurrentUser(buyer);

        ServiceOrder order = new ServiceOrder();
        order.setId(801L);
        order.setOrderNumber("ORDER202611270801");
        order.setBuyerId(10L);
        order.setSellerId(20L);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_SHIP);
        order.setRefundStatus(OrderConstant.REFUND_NONE);
        order.setFrozenAmount(new BigDecimal("200.00"));

        OrderMilestone stage1 = new OrderMilestone()
                .setId(1L).setOrderId(801L).setStageIndex(1).setTitle("初稿框架").setAmount(new BigDecimal("80.00"))
                .setMilestoneStatus(OrderMilestoneServiceImpl.STATUS_SUBMITTED);
        OrderMilestone stage2 = new OrderMilestone()
                .setId(2L).setOrderId(801L).setStageIndex(2).setTitle("终稿移交").setAmount(new BigDecimal("120.00"))
                .setMilestoneStatus(OrderMilestoneServiceImpl.STATUS_SUBMITTED);

        when(sysConfigService.getIntValue(SysConfigConstant.PLATFORM_COMMISSION_BPS, 500)).thenReturn(500); // 5%
        when(serviceOrderMapper.selectForUpdate(801L)).thenReturn(order);
        when(userMapper.selectForUpdate(10L)).thenReturn(buyer);
        when(userMapper.selectForUpdate(20L)).thenReturn(seller);

        // 1. 释放第一阶段 80 元（5% 服务费 = 4.00，卖家净入账 76.00，订单仍为待收货且冻结剩余 120.00）
        when(orderMilestoneMapper.selectForUpdate(1L)).thenReturn(stage1);
        milestoneService.releaseMilestone(1L);

        assertEquals(0, new BigDecimal("120.00").compareTo(order.getFrozenAmount()));
        assertEquals(OrderConstant.ORDER_PENDING_RECEIPT, order.getOrderStatus());
        assertEquals(0, new BigDecimal("76.00").compareTo(seller.getWalletBalance()));
        assertEquals(OrderMilestoneServiceImpl.STATUS_RELEASED, stage1.getMilestoneStatus());

        // 2. 释放第二阶段 120 元（5% 服务费 = 6.00，卖家累计净入账 76 + 114 = 190.00，订单冻结归零自动完结）
        when(orderMilestoneMapper.selectForUpdate(2L)).thenReturn(stage2);
        milestoneService.releaseMilestone(2L);

        assertEquals(0, BigDecimal.ZERO.compareTo(order.getFrozenAmount()));
        assertEquals(OrderConstant.ORDER_COMPLETED, order.getOrderStatus());
        assertEquals(0, new BigDecimal("190.00").compareTo(seller.getWalletBalance()));
        assertEquals(0, new BigDecimal("10.00").compareTo(order.getPlatformFee()));
    }

    @Test
    void sellerDeposit_paySucceedsAndWithdrawBlockedWhenActiveOrderExists() {
        User seller = new User()
                .setId(20L)
                .setWalletBalance(new BigDecimal("300.00"))
                .setSellerDeposit(BigDecimal.ZERO)
                .setFrozenAmount(BigDecimal.ZERO);
        UserContext.setCurrentUser(seller);
        when(userMapper.selectForUpdate(20L)).thenReturn(seller);

        Map<String, Object> payBody = new HashMap<>();
        payBody.put("amount", "100.00");
        depositController.payDeposit(payBody);

        assertEquals(0, new BigDecimal("200.00").compareTo(seller.getWalletBalance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(seller.getSellerDeposit()));

        // 模拟存在 1 笔在途销售单，退还保证金必须被拦截
        when(serviceOrderMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> depositController.withdrawDeposit(payBody));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), ex.getCode());
        assertEquals(0, new BigDecimal("100.00").compareTo(seller.getSellerDeposit()));
    }
}
