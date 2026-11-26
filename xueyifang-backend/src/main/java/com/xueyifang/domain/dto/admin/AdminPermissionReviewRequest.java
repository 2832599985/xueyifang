package com.xueyifang.domain.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminPermissionReviewRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String reason;
}
