package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.admin.AdminStatisticsVO;
import com.xueyifang.domain.vo.admin.AdminTrendVO;
import com.xueyifang.domain.vo.admin.PendingUserVO;
import com.xueyifang.domain.vo.statistics.UserSalesStatisticsVO;
import com.xueyifang.mapper.ProfessionalMapper;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceTagMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员统计与卖家销售统计单元测试：验证 SQL 聚合调用与空值安全
 */
@ExtendWith(MockitoExtension.class)
class AdminAndStatisticsServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private ServiceOrderMapper serviceOrderMapper;
    @Mock
    private ServiceDisputeMapper serviceDisputeMapper;
    @Mock
    private ProfessionalMapper professionalMapper;
    @Mock
    private ServiceTagMapper serviceTagMapper;
    @Mock
    private IServiceImageService serviceImageService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private RedisUtils redisUtils;

    @InjectMocks
    private AdminServiceImpl adminService;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getSystemStatistics_usesSqlAggregationInsteadOfFullSelectList() {
        when(redisUtils.getJson(CacheConstant.STATISTICS_ADMIN_SYSTEM, AdminStatisticsVO.class)).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(20L, 2L);
        when(serviceOrderMapper.countActiveUsersSince(any())).thenReturn(8L);
        when(serviceMapper.selectCount(any())).thenReturn(15L, 1L);
        when(serviceOrderMapper.selectCount(any())).thenReturn(30L, 3L);
        when(serviceDisputeMapper.selectCount(any())).thenReturn(1L);

        Map<String, Object> completed = new HashMap<>();
        completed.put("order_count", 18L);
        completed.put("total_amount", new BigDecimal("1234.50"));
        when(serviceOrderMapper.countAndSumByStatus(OrderConstant.ORDER_COMPLETED, null)).thenReturn(completed);

        AdminStatisticsVO vo = adminService.getSystemStatistics();

        assertEquals(20, vo.getTotalUsers());
        assertEquals(8, vo.getActiveUsers());
        assertEquals(18, vo.getCompletedOrders());
        assertEquals(new BigDecimal("1234.50"), vo.getTotalTransactionAmount());
        // 关键：不再把所有订单拉回内存累加
        verify(serviceOrderMapper, never()).selectList(any());
    }

    @Test
    void getTrendStatistics_queriesOncePerTableAndPadsMissingDaysWithZero() {
        when(redisUtils.getJson(CacheConstant.STATISTICS_ADMIN_TREND, AdminTrendVO.class)).thenReturn(null);

        String todayKey = LocalDate.now().toString();
        Map<String, Object> todayOrder = new HashMap<>();
        todayOrder.put("stat_date", todayKey);
        todayOrder.put("order_count", 4L);
        todayOrder.put("completed_amount", new BigDecimal("200.00"));
        when(serviceOrderMapper.dailyOrderStats(any(), eq(OrderConstant.ORDER_COMPLETED)))
                .thenReturn(Collections.singletonList(todayOrder));

        Map<String, Object> todayService = new HashMap<>();
        todayService.put("stat_date", todayKey);
        todayService.put("service_count", 2L);
        when(serviceMapper.countDailyCreated(any())).thenReturn(Collections.singletonList(todayService));

        AdminTrendVO trend = adminService.getTrendStatistics();

        assertEquals(7, trend.getDates().size());
        assertEquals(0, trend.getOrderCounts().get(0));
        assertEquals(4, trend.getOrderCounts().get(6));
        assertEquals(2, trend.getServiceCounts().get(6));
        assertEquals(new BigDecimal("200.00"), trend.getTransactionAmounts().get(6));
    }

    @Test
    void getPendingUsers_nullProfessionalName_doesNotThrowNpe() {
        User u = new User();
        u.setId(5L);
        u.setStudentId("20249999");
        u.setProfessionalId(99L);

        Page<User> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(u));
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        com.xueyifang.domain.po.Professional p = new com.xueyifang.domain.po.Professional();
        p.setId(99L);
        p.setProfessionalName(null);
        when(professionalMapper.selectList(any())).thenReturn(Collections.singletonList(p));

        Page<PendingUserVO> result = adminService.getPendingUsers(1, 10);
        assertEquals(1, result.getRecords().size());
        assertNull(result.getRecords().get(0).getProfessionalName());
    }

    @Test
    void getUserSalesStatistics_usesSqlAggregation() {
        User seller = new User();
        seller.setId(2L);
        seller.setIsDelete(0);
        when(userMapper.selectById(2L)).thenReturn(seller);
        when(redisUtils.getJson(CacheConstant.STATISTICS_USER_SALES_PREFIX + 2L, UserSalesStatisticsVO.class))
                .thenReturn(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("order_count", 3L);
        stats.put("total_amount", new BigDecimal("100.00"));
        when(serviceOrderMapper.countAndSumByStatus(OrderConstant.ORDER_COMPLETED, 2L)).thenReturn(stats);
        when(serviceOrderMapper.selectMaps(any())).thenReturn(Collections.emptyList());
        List<ServiceOrder> recent = Collections.emptyList();
        when(serviceOrderMapper.selectList(any())).thenReturn(recent);

        UserSalesStatisticsVO vo = statisticsService.getUserSalesStatistics(2L);
        assertEquals(3, vo.getTotalSales());
        assertEquals(new BigDecimal("100.00"), vo.getTotalRevenue());
        assertEquals(new BigDecimal("33.33"), vo.getAveragePrice());
    }
}
