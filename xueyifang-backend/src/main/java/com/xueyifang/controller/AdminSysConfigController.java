package com.xueyifang.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.admin.SysConfigQueryRequest;
import com.xueyifang.domain.dto.admin.SysConfigUpdateRequest;
import com.xueyifang.domain.po.SysConfig;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.impl.SysConfigServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员-系统配置管理控制器
 *
 * @author xueyifang
 */
@Slf4j
@RestController
@RequestMapping("/admin/sys-config")
@Api(tags = "管理员-系统配置管理")
public class AdminSysConfigController {

    @Resource
    private ISysConfigService sysConfigService;

    /**
     * 分页查询系统配置列表
     */
    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询系统配置列表")
    public BaseResponse<Page<SysConfig>> listSysConfigs(SysConfigQueryRequest request) {
        Page<SysConfig> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(request.getKeyLike())) {
            wrapper.like(SysConfig::getConfigKey, request.getKeyLike());
        }
        wrapper.orderByAsc(SysConfig::getId);

        Page<SysConfig> result = sysConfigService.page(page, wrapper);
        return ResultUtils.success(result);
    }

    /**
     * 获取单条配置详情
     */
    @GetMapping("/{id}")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取单条配置详情")
    public BaseResponse<SysConfig> getSysConfigById(@PathVariable Long id) {
        SysConfig config = sysConfigService.getById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "配置不存在");
        }
        return ResultUtils.success(config);
    }

    /**
     * 更新配置项
     */
    @PutMapping("/update")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("更新配置项")
    public BaseResponse<Boolean> updateSysConfig(@RequestBody @Valid SysConfigUpdateRequest request) {
        // 1. 检查配置是否存在
        SysConfig config = sysConfigService.getById(request.getId());
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "配置不存在");
        }

        // 2. 构建更新对象（只更新非空字段，不允许修改 configKey）
        SysConfig updateEntity = new SysConfig();
        updateEntity.setId(request.getId());
        if (request.getConfigValue() != null) {
            updateEntity.setConfigValue(request.getConfigValue());
        }
        if (request.getDescription() != null) {
            updateEntity.setDescription(request.getDescription());
        }
        if (request.getIsEnabled() != null) {
            updateEntity.setIsEnabled(request.getIsEnabled());
        }

        // 3. 执行更新
        boolean success = sysConfigService.updateById(updateEntity);

        // 4. 删除配置缓存
        if (success) {
            sysConfigService.deleteConfigCache(config.getConfigKey());
        }

        log.info("管理员更新系统配置: id={}, configKey={}", request.getId(), config.getConfigKey());
        return ResultUtils.success(success);
    }

    /**
     * 批量获取配置值（只读快捷接口）
     */
    @GetMapping("/key-values")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("批量获取配置值")
    public BaseResponse<Map<String, String>> getConfigValuesByKeys(@RequestParam List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return ResultUtils.success(Collections.emptyMap());
        }

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysConfig::getConfigKey, keys);
        wrapper.eq(SysConfig::getIsEnabled, 1); // 只返回启用的配置

        List<SysConfig> configs = sysConfigService.list(wrapper);
        Map<String, String> result = configs.stream()
                .collect(Collectors.toMap(
                        SysConfig::getConfigKey,
                        c -> c.getConfigValue() != null ? c.getConfigValue() : ""
                ));

        return ResultUtils.success(result);
    }
}
