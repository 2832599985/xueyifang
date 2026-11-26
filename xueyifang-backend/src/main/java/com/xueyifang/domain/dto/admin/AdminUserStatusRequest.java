package com.xueyifang.domain.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理员更新用户状态/发布权限请求
 */
@Data
public class AdminUserStatusRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 账户状态（1=正常, 2=禁用）
     */
    private Integer accountStatus;

    /**
     * 发布权限（0=无, 1=有）
     */
    private Integer publishPermission;
}
