package com.xueyifang.controller;


import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 系统配置表 前端控制器（公开接口）
 * </p>
 *
 * @author luoyi
 * @since 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/sys-config")
@Api(tags = "系统配置-公共接口")
public class SysConfigController {

    @Resource
    private ISysConfigService sysConfigService;

    /**
     * 获取注册开关状态
     * 公开接口，无需登录
     *
     * @return { registerEnabled: true/false }
     */
    @GetMapping("/register-status")
    @ApiOperation(value = "获取注册开关状态", notes = "公开接口，前端读取以控制注册入口显示")
    public BaseResponse<Map<String, Object>> getRegisterStatus() {
        String registerEnabled = sysConfigService.getConfigValue(
                SysConfigConstant.REGISTER_ENABLED,
                SysConfigConstant.REGISTER_ENABLED_DEFAULT
        );

        Map<String, Object> result = new HashMap<>(2);
        result.put("registerEnabled", "1".equals(registerEnabled) || "true".equalsIgnoreCase(registerEnabled));

        return ResultUtils.success(result);
    }
}
