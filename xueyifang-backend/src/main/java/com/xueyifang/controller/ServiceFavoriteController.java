package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.favorite.FavoriteCollectRequest;
import com.xueyifang.domain.vo.FavoriteListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IServiceFavoriteService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * <p>
 * 服务收藏 前端控制器
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-25
 */
@RestController
@RequestMapping("/favorite")
@Api(tags = "收藏管理")
public class ServiceFavoriteController {

    @Resource
    private IServiceFavoriteService serviceFavoriteService;

    @PostMapping("/collect")
    @ApiOperation("收藏服务")
    @IdempotentSubmit(lockTime = 2)
    public BaseResponse<Void> collectService(@RequestBody @Valid FavoriteCollectRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        serviceFavoriteService.collectService(userId, request.getServiceId());
        return ResultUtils.success();
    }

    @DeleteMapping("/collect/{serviceId}")
    @ApiOperation("取消收藏服务")
    public BaseResponse<Void> uncollectService(@PathVariable Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务ID不能为空");
        }
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        serviceFavoriteService.uncollectService(userId, serviceId);
        return ResultUtils.success();
    }

    @GetMapping("/myCollections")
    @ApiOperation("获取我的收藏列表")
    public BaseResponse<Page<FavoriteListItemVO>> getMyCollections(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        Page<FavoriteListItemVO> result = serviceFavoriteService.pageMyCollections(userId, pageNum, pageSize);
        return ResultUtils.success(result);
    }
}
