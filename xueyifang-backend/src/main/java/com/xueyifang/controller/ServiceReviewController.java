package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.ServiceReviewCreateRequest;
import com.xueyifang.domain.vo.ServiceReviewVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 服务评价控制器
 *
 * @author xueyifang
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/review")
@Api(tags = "服务评价")
public class ServiceReviewController {

    @Resource
    private IServiceReviewService serviceReviewService;

    /**
     * 创建评价
     */
    @PostMapping("/create")
    @ApiOperation("创建评价")
    public BaseResponse<Long> createReview(@RequestBody @Valid ServiceReviewCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        Long reviewId = serviceReviewService.createReview(request, userId);
        return ResultUtils.success(reviewId);
    }

    /**
     * 查询服务的评价列表（公开接口，无需登录）
     */
    @GetMapping("/service/{serviceId}")
    @ApiOperation("查询服务评价列表")
    public BaseResponse<Page<ServiceReviewVO>> listServiceReviews(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ServiceReviewVO> page = serviceReviewService.listServiceReviews(serviceId, pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 查询订单是否已评价
     */
    @GetMapping("/order/{orderId}/status")
    @ApiOperation("查询订单是否已评价")
    public BaseResponse<Boolean> isOrderReviewed(@PathVariable Long orderId) {
        boolean reviewed = serviceReviewService.isOrderReviewed(orderId);
        return ResultUtils.success(reviewed);
    }
}
