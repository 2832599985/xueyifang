package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.po.GroupSession;
import com.xueyifang.domain.po.GroupSessionParticipant;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.mapper.GroupSessionMapper;
import com.xueyifang.mapper.GroupSessionParticipantMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * v4.2 校内多人拼班团课与满员阶梯自动退差价单元测试
 */
@ExtendWith(MockitoExtension.class)
class GroupSessionServiceImplTest {

    @Mock
    private GroupSessionMapper groupSessionMapper;
    @Mock
    private GroupSessionParticipantMapper participantMapper;
    @Mock
    private IServiceService serviceService;
    @Mock
    private IServiceOrderService serviceOrderService;
    @Mock
    private ServiceOrderMapper serviceOrderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private IWalletTransactionService walletTransactionService;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private GroupSessionServiceImpl groupSessionService;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, GroupSession.class);
        TableInfoHelper.initTableInfo(assistant, GroupSessionParticipant.class);
        TableInfoHelper.initTableInfo(assistant, ServiceOrder.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(groupSessionService, "baseMapper", groupSessionMapper);
        lenient().when(walletTransactionService.save(any(WalletTransaction.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void joinGroupSession_whenTierTargetReached_rebatesEarlyBirdPaidParticipantAutomatically() {
        // 设定团课：基础单价 60 元，满 2 人触发阶梯优惠价 45 元（每人省 15 元）
        GroupSession session = new GroupSession()
                .setId(900L)
                .setSessionNo("GRP202611270900")
                .setCreatorId(20L)
                .setCampusId(1L)
                .setTitle("线性代数期末4小时核心题型小班")
                .setBasePrice(new BigDecimal("60.00"))
                .setTierTargetCount(2)
                .setTierDiscountPrice(new BigDecimal("45.00"))
                .setMinParticipants(2)
                .setMaxParticipants(4)
                .setJoinedCount(1)
                .setTierRebateApplied(0)
                .setTradeType(OrderConstant.TRADE_TYPE_OFFLINE)
                .setMeetupLat(new BigDecimal("30.514000"))
                .setMeetupLng(new BigDecimal("114.354400"))
                .setMeetupPoiName("主图书馆302研讨室")
                .setMeetupRadiusMeters(50)
                .setSessionStatus(GroupSessionServiceImpl.STATUS_RECRUITING)
                .setTemplateServiceId(77L);

        // 第 1 位早期学员（ID=11）此前以 60.00 元参团并已支付担保冻结 60.00 元
        GroupSessionParticipant earlyParticipant = new GroupSessionParticipant()
                .setId(1L)
                .setSessionId(900L)
                .setBuyerId(11L)
                .setOrderId(5001L)
                .setLockedPrice(new BigDecimal("60.00"))
                .setEffectivePrice(new BigDecimal("60.00"))
                .setRebateAmount(BigDecimal.ZERO)
                .setJoinStatus(1);

        ServiceOrder earlyOrder = new ServiceOrder();
        earlyOrder.setId(5001L);
        earlyOrder.setBuyerId(11L);
        earlyOrder.setSellerId(20L);
        earlyOrder.setUnitPrice(new BigDecimal("60.00"));
        earlyOrder.setTotalAmount(new BigDecimal("60.00"));
        earlyOrder.setFrozenAmount(new BigDecimal("60.00"));
        earlyOrder.setOrderStatus(OrderConstant.ORDER_PENDING_SHIP);

        User earlyBuyer = new User()
                .setId(11L)
                .setWalletBalance(new BigDecimal("100.00"))
                .setFrozenAmount(new BigDecimal("60.00"));

        // 第 2 位学员（ID=12）现在报名参团，触发 2 人阶梯线
        User secondBuyer = new User().setId(12L).setNickname("参团同学B");
        UserContext.setCurrentUser(secondBuyer);

        when(groupSessionMapper.selectForUpdate(900L)).thenReturn(session);
        when(participantMapper.selectCount(any())).thenReturn(0L);
        when(serviceOrderService.createOrder(any(OrderCreateRequest.class))).thenReturn(5002L);
        when(participantMapper.selectList(any())).thenReturn(Collections.singletonList(earlyParticipant));
        when(serviceOrderMapper.selectForUpdate(5001L)).thenReturn(earlyOrder);
        when(userMapper.selectForUpdate(11L)).thenReturn(earlyBuyer);

        Long secondOrderId = groupSessionService.joinGroupSession(900L);

        assertEquals(5002L, secondOrderId);
        assertEquals(2, session.getJoinedCount());
        assertEquals(1, session.getTierRebateApplied());
        assertEquals(GroupSessionServiceImpl.STATUS_FORMED, session.getSessionStatus());

        // 验证第 1 位早期学员自动获得 15.00 元阶梯差价退还（可用余额 100 -> 115，冻结 60 -> 45）
        assertEquals(0, new BigDecimal("45.00").compareTo(earlyParticipant.getEffectivePrice()));
        assertEquals(0, new BigDecimal("15.00").compareTo(earlyParticipant.getRebateAmount()));
        assertEquals(0, new BigDecimal("45.00").compareTo(earlyOrder.getFrozenAmount()));
        assertEquals(0, new BigDecimal("115.00").compareTo(earlyBuyer.getWalletBalance()));
        assertEquals(0, new BigDecimal("45.00").compareTo(earlyBuyer.getFrozenAmount()));
    }
}
