package com.xueyifang.domain.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingUserVO {
    private Long userId;
    private String studentId;
    private String realName;
    private String phone;
    private String email;
    private String grade;
    private String professionalName;
    private Integer permissionReviewStatus;
    private String applyReason;
    private LocalDateTime createTime;
}
