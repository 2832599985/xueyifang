package com.xueyifang.service;

import com.xueyifang.domain.vo.statistics.UserSalesStatisticsVO;

/**
 * 统计服务接口
 *
 * @author xueyifang
 */
public interface IStatisticsService {

    /**
     * 获取用户销售统计
     *
     * @param userId 用户ID
     * @return 销售统计数据
     */
    UserSalesStatisticsVO getUserSalesStatistics(Long userId);
}
