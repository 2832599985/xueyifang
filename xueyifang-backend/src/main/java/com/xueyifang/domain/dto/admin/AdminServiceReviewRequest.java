package com.xueyifang.domain.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminServiceReviewRequest {
    @NotNull(message = "服务ID不能为空")
    private Long serviceId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String reason;
}
