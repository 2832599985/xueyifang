package com.xueyifang.domain.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 系统配置更新请求
 */
@Data
public class SysConfigUpdateRequest {
    /**
     * 配置ID（必填）
     */
    @NotNull(message = "配置ID不能为空")
    private Long id;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否启用（1=启用, 0=禁用）
     */
    private Integer isEnabled;
}
