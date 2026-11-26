package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.service.IServiceTagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员-服务标签分类管理控制器
 */
@RestController
@RequestMapping("/admin/service-tag")
@Api(tags = "管理员-服务标签管理")
public class AdminServiceTagController {

    @Resource
    private IServiceTagService serviceTagService;

    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询服务标签列表")
    public BaseResponse<Page<ServiceTag>> listServiceTags(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize,
            @RequestParam(required = false) String nameLike) {
        return ResultUtils.success(serviceTagService.pageServiceTags(pageNum, pageSize, nameLike));
    }

    @PostMapping("/add")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("新增服务标签")
    public BaseResponse<Boolean> addServiceTag(@RequestBody ServiceTag serviceTag) {
        return ResultUtils.success(serviceTagService.addServiceTag(serviceTag));
    }

    @PutMapping("/update")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("更新服务标签")
    public BaseResponse<Boolean> updateServiceTag(@RequestBody ServiceTag serviceTag) {
        return ResultUtils.success(serviceTagService.updateServiceTag(serviceTag));
    }

    @DeleteMapping("/{id}")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("删除服务标签")
    public BaseResponse<Boolean> deleteServiceTag(@PathVariable("id") Long id) {
        return ResultUtils.success(serviceTagService.deleteServiceTag(id));
    }
}
