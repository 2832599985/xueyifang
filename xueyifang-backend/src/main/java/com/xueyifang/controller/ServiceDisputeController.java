package com.xueyifang.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.DisputeCreateRequest;
import com.xueyifang.domain.vo.DisputeDetailVO;
import com.xueyifang.domain.vo.DisputeListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IServiceDisputeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * <p>
 * 服务纠纷表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
@RestController
@RequestMapping("/dispute")
@Api(tags = "服务纠纷管理")
public class ServiceDisputeController {

    @Resource
    private IServiceDisputeService serviceDisputeService;

    /**
     * 创建纠纷
     *
     * @param request 创建纠纷请求
     * @return 纠纷ID
     */
    @PostMapping("/create")
    @ApiOperation("创建纠纷")
    public BaseResponse<Long> createDispute(@Valid @RequestBody DisputeCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long disputeId = serviceDisputeService.createDispute(request);
        return ResultUtils.success(disputeId);
    }

    /**
     * 获取我的纠纷列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 纠纷列表
     */
    @GetMapping("/my")
    @ApiOperation("获取我的纠纷列表")
    public BaseResponse<Page<DisputeListItemVO>> getMyDisputes(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<DisputeListItemVO> result = serviceDisputeService.getMyDisputes(pageNum, pageSize);
        return ResultUtils.success(result);
    }

    /**
     * 获取纠纷详情
     *
     * @param disputeId 纠纷ID
     * @return 纠纷详情
     */
    @GetMapping("/{disputeId}")
    @ApiOperation("获取纠纷详情")
    public BaseResponse<DisputeDetailVO> getDisputeDetail(
            @ApiParam("纠纷ID") @PathVariable Long disputeId) {
        if (disputeId == null || disputeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DisputeDetailVO result = serviceDisputeService.getDisputeDetail(disputeId);
        return ResultUtils.success(result);
    }
}
