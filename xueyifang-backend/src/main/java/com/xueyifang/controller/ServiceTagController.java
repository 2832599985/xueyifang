package com.xueyifang.controller;


import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.vo.ServiceTagVO;
import com.xueyifang.service.IServiceTagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务标签表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
@RestController
@RequestMapping("/service/tags")
@Slf4j
@Api(tags = "服务标签管理模块")
public class ServiceTagController {

    @Resource
    private IServiceTagService serviceTagService;

    /**
     * 获取服务标签列表
     *
     * @return 服务标签VO列表
     */
    @GetMapping
    @ApiOperation(value = "获取服务标签列表", notes = "获取所有可用的服务标签分类")
    public BaseResponse<List<ServiceTagVO>> listServiceTags() {
        log.info("获取服务标签列表");

        List<ServiceTagVO> serviceTagVOList = serviceTagService.listAllServiceTags();

        return ResultUtils.success(serviceTagVOList);
    }
}
