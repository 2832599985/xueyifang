package com.xueyifang.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.statistics.RecentOrderVO;
import com.xueyifang.domain.vo.statistics.SalesServiceVO;
import com.xueyifang.domain.vo.statistics.UserSalesStatisticsVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IStatisticsService;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 *
 * @author xueyifang
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements IStatisticsService {

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public UserSalesStatisticsVO getUserSalesStatistics(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        // 1. 尝试从缓存获取
        String cacheKey = CacheConstant.STATISTICS_USER_SALES_PREFIX + userId;
        UserSalesStatisticsVO cachedVO = redisUtils.getJson(cacheKey, UserSalesStatisticsVO.class);
        if (cachedVO != null) {
            log.debug("用户销售统计缓存命中: userId={}", userId);
            return cachedVO;
        }

        // 2. 缓存未命中，查询计算
        log.debug("用户销售统计缓存未命中: userId={}", userId);
        UserSalesStatisticsVO vo = new UserSalesStatisticsVO();

        // 1~3. 总销量、总收入：一条 SQL 聚合（不再把全部已完成订单拉回内存）
        Map<String, Object> completedStats = serviceOrderMapper.countAndSumByStatus(OrderConstant.ORDER_COMPLETED, userId);
        int totalSales = Convert.toInt(completedStats.get("order_count"), 0);
        BigDecimal totalRevenue = Convert.toBigDecimal(completedStats.get("total_amount"), BigDecimal.ZERO);
        vo.setTotalSales(totalSales);
        vo.setTotalRevenue(totalRevenue);
        vo.setAveragePrice(totalSales == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP));

        // 4. 最畅销服务（按销量）
        if (totalSales > 0) {
            // 使用MyBatis Plus的group by查询
            QueryWrapper<ServiceOrder> groupWrapper = new QueryWrapper<>();
            groupWrapper.eq("seller_id", userId)
                    .eq("order_status", OrderConstant.ORDER_COMPLETED)
                    .eq("is_delete", 0)
                    .select("service_id", "COUNT(*) as sales_count", "SUM(total_amount) as total_revenue")
                    .groupBy("service_id")
                    .orderByDesc("sales_count")
                    .last("LIMIT 1");

            List<Map<String, Object>> result = serviceOrderMapper.selectMaps(groupWrapper);

            if (!result.isEmpty() && result.get(0) != null) {
                Map<String, Object> bestServiceData = result.get(0);
                Long bestServiceId = ((Number) bestServiceData.get("service_id")).longValue();
                Integer salesCount = ((Number) bestServiceData.get("sales_count")).intValue();
                Object revenueObj = bestServiceData.get("total_revenue");
                BigDecimal revenue = revenueObj != null ? new BigDecimal(revenueObj.toString()) : BigDecimal.ZERO;

                // 查询服务标题
                com.xueyifang.domain.po.Service service = serviceMapper.selectById(bestServiceId);

                SalesServiceVO bestService = new SalesServiceVO();
                bestService.setServiceId(bestServiceId);
                bestService.setServiceTitle(service != null ? service.getServiceTitle() : "未知服务");
                bestService.setSales(salesCount);
                bestService.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);

                vo.setBestService(bestService);
            } else {
                vo.setBestService(null);
            }
        } else {
            vo.setBestService(null);
        }

        // 5. 最近5条订单
        LambdaQueryWrapper<ServiceOrder> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.eq(ServiceOrder::getSellerId, userId)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED)
                .eq(ServiceOrder::getIsDelete, 0)
                .orderByDesc(ServiceOrder::getCreateTime)
                .last("LIMIT 5");

        List<ServiceOrder> recentOrders = serviceOrderMapper.selectList(recentWrapper);
        List<RecentOrderVO> recentOrderVOs = recentOrders.stream().map(order -> {
            RecentOrderVO orderVO = new RecentOrderVO();
            orderVO.setOrderId(order.getId());
            orderVO.setOrderNumber(order.getOrderNumber());
            orderVO.setTotalAmount(order.getTotalAmount());
            orderVO.setCreateTime(order.getCreateTime());
            return orderVO;
        }).collect(Collectors.toList());

        vo.setRecentOrders(recentOrderVOs);

        // 3. 写入缓存
        redisUtils.setJson(cacheKey, vo, CacheConstant.STATISTICS_USER_SALES_TTL);
        log.debug("用户销售统计写入缓存: userId={}", userId);

        return vo;
    }
}
