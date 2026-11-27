package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.DemandBidCreateRequest;
import com.xueyifang.domain.dto.DemandCreateRequest;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.po.DemandBid;
import com.xueyifang.domain.po.DemandRequest;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.DemandBidMapper;
import com.xueyifang.mapper.DemandRequestMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.ISysConfigService;
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

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C2B 反向悬赏与竞标引擎单元测试：验证电子围栏与米级半径强校验、创作者资质拦截、选标原子生成担保单与竞标状态流转
 */
@ExtendWith(MockitoExtension.class)
class DemandServiceImplTest {

    @Mock
    private DemandRequestMapper demandRequestMapper;
    @Mock
    private DemandBidMapper demandBidMapper;
    @Mock
    private ICampusService campusService;
    @Mock
    private IServiceService serviceService;
    @Mock
    private IServiceOrderService serviceOrderService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private ISysConfigService sysConfigService;

    @InjectMocks
    private DemandServiceImpl demandService;

    @BeforeAll
    static void initMybatisTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, DemandRequest.class);
        TableInfoHelper.initTableInfo(assistant, DemandBid.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demandService, "baseMapper", demandRequestMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void publishDemand_offlineOutsideFenceOrInvalidRadius_rejected() {
        User buyer = new User();
        buyer.setId(10L);
        buyer.setCreditScore(100);
        UserContext.setCurrentUser(buyer);
        when(sysConfigService.getIntValue("MIN_CREDIT_SCORE_FOR_ORDER", 60)).thenReturn(60);

        DemandCreateRequest badRadiusReq = new DemandCreateRequest();
        badRadiusReq.setTitle("高数下册周四晚一对一突击辅导");
        badRadiusReq.setDescription("重点梳理三重积分与曲面积分历年真题");
        badRadiusReq.setBudgetMin(new BigDecimal("60"));
        badRadiusReq.setBudgetMax(new BigDecimal("120"));
        badRadiusReq.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        badRadiusReq.setCampusId(1L);
        badRadiusReq.setMeetupLat(new BigDecimal("30.514000"));
        badRadiusReq.setMeetupLng(new BigDecimal("114.354400"));
        badRadiusReq.setMeetupPoiName("主图书馆一楼监控区");
        badRadiusReq.setMeetupRadiusMeters(5); // < 10m 非法半径

        BusinessException ex = assertThrows(BusinessException.class, () -> demandService.publishDemand(badRadiusReq));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());

        // 校外坐标被校区多边形围栏拦截
        badRadiusReq.setMeetupRadiusMeters(60);
        doThrow(new BusinessException(ErrorCode.GEOFENCE_OUT_OF_BOUNDS))
                .when(campusService).validateCoordinateInCampusFence(1L, 30.514000, 114.354400);
        BusinessException fenceEx = assertThrows(BusinessException.class, () -> demandService.publishDemand(badRadiusReq));
        assertEquals(ErrorCode.GEOFENCE_OUT_OF_BOUNDS.getCode(), fenceEx.getCode());
    }

    @Test
    void submitBid_requiresPublishPermissionAndBlocksSelfBid() {
        User unverifiedUser = new User();
        unverifiedUser.setId(22L);
        unverifiedUser.setPublishPermission(0);
        UserContext.setCurrentUser(unverifiedUser);

        DemandBidCreateRequest bidReq = new DemandBidCreateRequest();
        bidReq.setBidPrice(new BigDecimal("88.00"));
        bidReq.setEstimatedHours(3);
        bidReq.setBidPlan("数学建模国一，带全套历年期末卷现场精讲");

        BusinessException permEx = assertThrows(BusinessException.class, () -> demandService.submitBid(500L, bidReq));
        assertEquals(ErrorCode.USER_NO_PUBLISH_PERMISSION.getCode(), permEx.getCode());

        // 发布者本人不可自刷竞标
        User publisherWithPerm = new User();
        publisherWithPerm.setId(10L);
        publisherWithPerm.setPublishPermission(1);
        publisherWithPerm.setCreditScore(100);
        UserContext.setCurrentUser(publisherWithPerm);
        when(sysConfigService.getIntValue("MIN_CREDIT_SCORE_FOR_PUBLISH", 70)).thenReturn(70);

        DemandRequest demand = new DemandRequest();
        demand.setId(500L);
        demand.setPublisherId(10L);
        demand.setDemandStatus(DemandServiceImpl.DEMAND_STATUS_OPEN);
        when(demandRequestMapper.selectForUpdate(500L)).thenReturn(demand);

        BusinessException selfEx = assertThrows(BusinessException.class, () -> demandService.submitBid(500L, bidReq));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), selfEx.getCode());
    }

    @Test
    void acceptBid_atomicallyGeneratesEscrowOrderInheritingMapCoordinatesAndRadius() {
        User buyer = new User();
        buyer.setId(10L);
        buyer.setNickname("计算机大二同学");
        UserContext.setCurrentUser(buyer);

        DemandRequest demand = new DemandRequest();
        demand.setId(600L);
        demand.setDemandNo("DEMAND202611270001");
        demand.setPublisherId(10L);
        demand.setCampusId(1L);
        demand.setTagId(2L);
        demand.setTitle("STM32串口DMA丢包现场联调");
        demand.setDescription("带逻辑分析仪在工学部创客空间当面排查");
        demand.setTradeType(OrderConstant.TRADE_TYPE_OFFLINE);
        demand.setMeetupLat(new BigDecimal("30.514200"));
        demand.setMeetupLng(new BigDecimal("114.355100"));
        demand.setMeetupPoiName("工学部创客空间一楼监控台");
        demand.setMeetupAddress("靠窗2号焊接台");
        demand.setMeetupRadiusMeters(50);
        demand.setDemandStatus(DemandServiceImpl.DEMAND_STATUS_OPEN);

        DemandBid winBid = new DemandBid();
        winBid.setId(701L);
        winBid.setDemandId(600L);
        winBid.setBidderId(20L);
        winBid.setBidPrice(new BigDecimal("95.00"));
        winBid.setEstimatedHours(2);
        winBid.setBidPlan("电赛省一学长，自带Saleae逻辑分析仪当面解决");
        winBid.setBidStatus(DemandServiceImpl.BID_STATUS_PENDING);

        DemandBid otherBid = new DemandBid();
        otherBid.setId(702L);
        otherBid.setDemandId(600L);
        otherBid.setBidderId(21L);
        otherBid.setBidPrice(new BigDecimal("120.00"));
        otherBid.setEstimatedHours(4);
        otherBid.setBidPlan("明晚有空可帮忙看");
        otherBid.setBidStatus(DemandServiceImpl.BID_STATUS_PENDING);

        when(demandRequestMapper.selectForUpdate(600L)).thenReturn(demand);
        when(demandBidMapper.selectById(701L)).thenReturn(winBid);
        when(serviceService.save(any(Service.class))).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            s.setId(888L);
            return true;
        });
        when(serviceOrderService.createOrder(any(OrderCreateRequest.class))).thenReturn(9001L);
        when(demandBidMapper.selectList(any())).thenReturn(Arrays.asList(winBid, otherBid));

        Long orderId = demandService.acceptBid(600L, 701L);

        assertEquals(9001L, orderId);
        assertEquals(DemandServiceImpl.DEMAND_STATUS_MATCHED, demand.getDemandStatus());
        assertEquals(701L, demand.getAcceptedBidId());
        assertEquals(888L, demand.getMatchedServiceId());
        assertEquals(9001L, demand.getMatchedOrderId());

        assertEquals(DemandServiceImpl.BID_STATUS_ACCEPTED, winBid.getBidStatus());
        assertEquals(DemandServiceImpl.BID_STATUS_OUTBID, otherBid.getBidStatus());

        ArgumentCaptor<OrderCreateRequest> orderReqCaptor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(serviceOrderService).createOrder(orderReqCaptor.capture());
        OrderCreateRequest captured = orderReqCaptor.getValue();
        assertEquals(888L, captured.getServiceId());
        assertEquals(50, captured.getMeetupRadiusMeters());
        assertEquals("工学部创客空间一楼监控台", captured.getMeetupPoiName());
        assertTrue(captured.getRemark().contains("DEMAND202611270001"));
    }
}
