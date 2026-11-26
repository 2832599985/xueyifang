package com.xueyifang.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.TradeLocationAddRequest;
import com.xueyifang.domain.dto.TradeLocationUpdateRequest;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.vo.TradeLocationVO;
import com.xueyifang.service.ITradeLocationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 交易地点表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-10-29
 */
@RestController
@RequestMapping("/trade-location")
@Slf4j
@Api(tags = "交易地点管理模块")
public class TradeLocationController {

    @Resource
    private ITradeLocationService tradeLocationService;

    /**
     * 查询所有可用的交易地点
     *
     * @return 交易地点VO列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询所有可用的交易地点", notes = "获取所有可用且未删除的交易地点列表")
    public BaseResponse<List<TradeLocationVO>> listAllTradeLocations() {
        log.info("查询所有可用的交易地点列表");

        List<TradeLocationVO> tradeLocationVOList = tradeLocationService.listAllTradeLocations();

        return ResultUtils.success(tradeLocationVOList);
    }

    /**
     * 查询所有交易地点（包括不可用的）
     *
     * @return 交易地点VO列表
     */
    @GetMapping("/list/all")
    @ApiOperation(value = "查询所有交易地点", notes = "获取所有交易地点列表（包括不可用的，但不包括已删除的）")
    public BaseResponse<List<TradeLocationVO>> listAllTradeLocationsIncludingUnavailable() {
        log.info("查询所有交易地点列表（包括不可用的）");

        List<TradeLocationVO> tradeLocationVOList = tradeLocationService.listAllTradeLocationsIncludingUnavailable();

        return ResultUtils.success(tradeLocationVOList);
    }

    /**
     * 分页查询交易地点
     *
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param isAvailable 是否可用（null表示查询全部）
     * @return 分页结果
     */
    @GetMapping("/page")
    @ApiOperation(value = "分页查询交易地点", notes = "分页查询交易地点列表，支持按可用状态筛选")
    public BaseResponse<Page<TradeLocationVO>> pageTradeLocations(
            @ApiParam(value = "当前页码", example = "1") @RequestParam(defaultValue = "1") Long current,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize,
            @ApiParam(value = "是否可用（1=可用, 0=不可用，null=全部）") @RequestParam(required = false) Integer isAvailable) {
        log.info("分页查询交易地点，当前页：{}，每页大小：{}，是否可用：{}", current, pageSize, isAvailable);

        Page<TradeLocationVO> page = tradeLocationService.pageTradeLocations(current, pageSize, isAvailable);

        return ResultUtils.success(page);
    }

    /**
     * 根据ID查询交易地点详情
     *
     * @param id 地点ID
     * @return 交易地点VO信息
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询单个交易地点", notes = "根据地点ID查询交易地点详细信息")
    public BaseResponse<TradeLocationVO> getTradeLocationById(
            @ApiParam(value = "地点ID", required = true, example = "1") @PathVariable("id") Long id) {
        log.info("查询交易地点详情，id：{}", id);

        TradeLocation tradeLocation = tradeLocationService.getTradeLocationById(id);

        // 转换为 VO
        return ResultUtils.success(BeanUtil.copyProperties(tradeLocation, TradeLocationVO.class));
    }

    /**
     * 添加交易地点
     *
     * @param addRequest 添加请求
     * @return 是否成功
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加交易地点", notes = "添加新的交易地点信息")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> addTradeLocation(@Valid @RequestBody TradeLocationAddRequest addRequest) {
        log.info("添加交易地点，地点名称：{}", addRequest.getLocationName());

        tradeLocationService.addTradeLocation(addRequest);

        return ResultUtils.success("添加交易地点成功");
    }

    /**
     * 更新交易地点信息
     *
     * @param updateRequest 更新请求
     * @return 是否成功
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新交易地点", notes = "根据地点ID更新交易地点信息")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> updateTradeLocation(@Valid @RequestBody TradeLocationUpdateRequest updateRequest) {
        log.info("更新交易地点，id：{}", updateRequest.getId());

        tradeLocationService.updateTradeLocation(updateRequest);

        return ResultUtils.success("更新交易地点成功");
    }


    /**
     * 删除交易地点（逻辑删除）
     *
     * @param id 地点ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除交易地点", notes = "根据地点ID删除交易地点（逻辑删除）")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> deleteTradeLocation(
            @ApiParam(value = "地点ID", required = true, example = "1") @PathVariable("id") Long id) {
        log.info("删除交易地点，id：{}", id);

        tradeLocationService.deleteTradeLocation(id);

        return ResultUtils.success("删除交易地点成功");
    }

    /**
     * 批量删除交易地点（逻辑删除）
     *
     * @param ids 地点ID列表
     * @return 是否成功
     */
    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除交易地点", notes = "批量删除交易地点（逻辑删除）")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> batchDeleteTradeLocations(
            @ApiParam(value = "地点ID列表", required = true) @RequestBody List<Long> ids) {
        log.info("批量删除交易地点，数量：{}", ids.size());

        tradeLocationService.batchDeleteTradeLocations(ids);

        return ResultUtils.success("批量删除交易地点成功");
    }
}
