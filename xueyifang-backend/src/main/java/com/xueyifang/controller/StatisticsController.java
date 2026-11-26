package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.vo.statistics.UserSalesStatisticsVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IStatisticsService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 数据统计控制器
 *
 * @author xueyifang
 */
@RestController
@RequestMapping("/statistics")
@Api(tags = "数据统计")
public class StatisticsController {

    @Resource
    private IStatisticsService statisticsService;

    @GetMapping("/sales")
    @ApiOperation("获取我的销售统计")
    public BaseResponse<UserSalesStatisticsVO> getSalesStatistics() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        UserSalesStatisticsVO result = statisticsService.getUserSalesStatistics(userId);
        return ResultUtils.success(result);
    }
}
