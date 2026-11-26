package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.service.IProfessionalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员-专业管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/professional")
@Api(tags = "管理员-专业管理")
public class AdminProfessionalController {

    @Resource
    private IProfessionalService professionalService;

    /**
     * 分页查询专业列表
     */
    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询专业列表")
    public BaseResponse<Page<Professional>> listProfessionals(
            @ApiParam(value = "当前页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize,
            @ApiParam(value = "专业名称模糊匹配") @RequestParam(required = false) String nameLike) {
        Page<Professional> page = professionalService.pageProfessionals(pageNum, pageSize, nameLike);
        return ResultUtils.success(page);
    }

    /**
     * 添加专业
     */
    @PostMapping("/add")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("添加专业")
    public BaseResponse<Boolean> addProfessional(@RequestBody Professional professional) {
        boolean success = professionalService.addProfessional(professional);
        return ResultUtils.success(success);
    }

    /**
     * 更新专业
     */
    @PutMapping("/update")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("更新专业")
    public BaseResponse<Boolean> updateProfessional(@RequestBody Professional professional) {
        boolean success = professionalService.updateProfessional(professional);
        return ResultUtils.success(success);
    }

    /**
     * 删除专业（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("删除专业")
    public BaseResponse<Boolean> deleteProfessional(
            @ApiParam(value = "专业ID", required = true, example = "1") @PathVariable("id") Long id) {
        boolean success = professionalService.deleteProfessional(id);
        return ResultUtils.success(success);
    }
}
