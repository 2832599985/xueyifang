package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.TradeLocationAddRequest;
import com.xueyifang.domain.dto.TradeLocationUpdateRequest;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.service.ITradeLocationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 管理员-交易地点管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/trade-location")
@Api(tags = "管理员-交易地点管理")
public class AdminTradeLocationController {

    @Resource
    private ITradeLocationService tradeLocationService;

    /**
     * 分页查询交易地点列表
     */
    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询交易地点列表")
    public BaseResponse<Page<TradeLocation>> listTradeLocations(
            @ApiParam(value = "当前页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize,
            @ApiParam(value = "是否可用（1=可用, 0=不可用）") @RequestParam(required = false) Integer isAvailable,
            @ApiParam(value = "地点名称模糊匹配") @RequestParam(required = false) String nameLike) {
        Page<TradeLocation> page = tradeLocationService.pageTradeLocationsForAdmin(pageNum, pageSize, isAvailable, nameLike);
        return ResultUtils.success(page);
    }

    /**
     * 添加交易地点
     */
    @PostMapping("/add")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("添加交易地点")
    public BaseResponse<Boolean> addTradeLocation(@Valid @RequestBody TradeLocationAddRequest addRequest) {
        boolean success = tradeLocationService.addTradeLocation(addRequest);
        return ResultUtils.success(success);
    }

    /**
     * 更新交易地点
     */
    @PutMapping("/update")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("更新交易地点")
    public BaseResponse<Boolean> updateTradeLocation(@Valid @RequestBody TradeLocationUpdateRequest updateRequest) {
        boolean success = tradeLocationService.updateTradeLocation(updateRequest);
        return ResultUtils.success(success);
    }

    /**
     * 删除交易地点（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("删除交易地点")
    public BaseResponse<Boolean> deleteTradeLocation(
            @ApiParam(value = "地点ID", required = true, example = "1") @PathVariable("id") Long id) {
        boolean success = tradeLocationService.deleteTradeLocation(id);
        return ResultUtils.success(success);
    }
}
