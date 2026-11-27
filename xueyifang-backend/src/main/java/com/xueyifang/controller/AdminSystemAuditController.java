package com.xueyifang.controller;

import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.vo.SystemHealthAuditVO;
import com.xueyifang.service.ISystemHealthAuditService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/system-audit")
@Api(tags = "管理端-全链路资金安全、高并发索引与系统健康自检中心")
public class AdminSystemAuditController {

    @Resource
    private ISystemHealthAuditService systemHealthAuditService;

    @GetMapping("/inspect")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("执行全链路资金不变量、在途托管锚定、风控积压与复合索引健康度实时自检")
    public BaseResponse<SystemHealthAuditVO> inspectHealth() {
        return ResultUtils.success(systemHealthAuditService.inspectSystemHealth());
    }
}
