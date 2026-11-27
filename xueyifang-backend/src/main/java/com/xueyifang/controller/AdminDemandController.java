package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.vo.DemandRequestVO;
import com.xueyifang.service.IDemandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/admin/demand")
@Api(tags = "管理端-校内悬赏需求巡检与治理")
public class AdminDemandController {

    @Resource
    private IDemandService demandService;

    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("管理员分页巡检全校悬赏需求")
    public BaseResponse<Page<DemandRequestVO>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ResultUtils.success(demandService.adminPageDemands(pageNum, pageSize, status, keyword));
    }

    @PostMapping("/{id}/close")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("管理员违规下架关闭悬赏需求")
    public BaseResponse<Boolean> adminClose(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        demandService.adminCloseDemand(id, reason);
        return ResultUtils.success(true);
    }
}
