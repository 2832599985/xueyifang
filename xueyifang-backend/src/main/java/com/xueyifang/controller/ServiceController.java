package com.xueyifang.controller;


import com.xueyifang.annotation.RateLimit;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xueyifang.domain.dto.ServicePublishRequest;
import com.xueyifang.domain.dto.ServiceUpdateRequest;
import com.xueyifang.domain.vo.ServiceListItemVO;
import com.xueyifang.domain.vo.ServiceDetailVO;
import com.xueyifang.service.IServiceService;
import com.xueyifang.utils.UserContext;

/**
 * <p>
 * 服务表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
@RestController
@RequestMapping("/service")
public class ServiceController {
    @Resource
    private IServiceService serviceService;

    @PostMapping("/publish")
    public BaseResponse<Long> publish(@RequestBody ServicePublishRequest request) {
        Long userId = UserContext.getCurrentUserId();
        Long id = serviceService.publishService(userId, request);
        return ResultUtils.success(id);
    }

    @GetMapping("/myServices")
    public BaseResponse<IPage<ServiceListItemVO>> myServices(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize
    ) {
        Long userId = UserContext.getCurrentUserId();
        IPage<ServiceListItemVO> page = serviceService.listMyServices(userId, status, pageNum, pageSize);
        return ResultUtils.success(page);
    }

    @GetMapping("/list")
    @RateLimit(key = "service_list", maxRequests = 30, windowSeconds = 10)
    public BaseResponse<IPage<ServiceListItemVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "tradeType", required = false) Integer tradeType,
            @RequestParam(value = "sellerId", required = false) Long sellerId,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize
    ) {
        IPage<ServiceListItemVO> page = serviceService.searchServices(keyword, tagId, tradeType, sellerId, sortBy, pageNum, pageSize);
        return ResultUtils.success(page);
    }

    @GetMapping("/{id}")
    public BaseResponse<ServiceDetailVO> detail(@PathVariable("id") Long id) {
        ServiceDetailVO vo = serviceService.getServiceDetail(id);
        return ResultUtils.success(vo);
    }

    /**
     * 编辑服务
     */
    @PutMapping("/{serviceId}")
    public BaseResponse<Void> updateService(
            @PathVariable("serviceId") Long serviceId,
            @RequestBody ServiceUpdateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        serviceService.updateService(userId, serviceId, request);
        return ResultUtils.success(null);
    }

    /**
     * 下架服务
     */
    @PutMapping("/{serviceId}/offline")
    public BaseResponse<Void> offlineService(@PathVariable("serviceId") Long serviceId) {
        Long userId = UserContext.getCurrentUserId();
        serviceService.offlineService(userId, serviceId);
        return ResultUtils.success(null);
    }

    /**
     * 上架/提交审核
     */
    @PutMapping("/{serviceId}/online")
    public BaseResponse<Void> onlineService(@PathVariable("serviceId") Long serviceId) {
        Long userId = UserContext.getCurrentUserId();
        serviceService.onlineService(userId, serviceId);
        return ResultUtils.success(null);
    }

    /**
     * 删除服务
     */
    @DeleteMapping("/{serviceId}")
    public BaseResponse<Void> deleteService(@PathVariable("serviceId") Long serviceId) {
        Long userId = UserContext.getCurrentUserId();
        serviceService.deleteService(userId, serviceId);
        return ResultUtils.success(null);
    }
}
