package com.xueyifang.domain.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员用户列表项 VO
 */
@Data
public class AdminUserVO {
    private Long userId;
    private String studentId;
    private String realName;
    private String nickname;
    private String phone;
    private String email;
    private String dormitory;
    private String grade;
    private Long professionalId;
    private String professionalName;
    private Integer role;
    private Integer publishPermission;
    private Integer permissionReviewStatus;
    private String applyReason;
    private BigDecimal walletBalance;
    private BigDecimal frozenAmount;
    private Integer accountStatus;
    private LocalDateTime createTime;
}
