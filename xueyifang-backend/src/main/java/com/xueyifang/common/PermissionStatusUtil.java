package com.xueyifang.common;

import com.xueyifang.constant.UserConstant;

public final class PermissionStatusUtil {

    private PermissionStatusUtil() {
    }

    public static String toEnStatus(Integer publishPermission, Integer reviewStatus) {
        if (publishPermission != null && publishPermission == UserConstant.HAS_PUBLISH_PERMISSION) {
            return "approved";
        }
        if (reviewStatus != null) {
            if (reviewStatus == UserConstant.PERMISSION_APPROVED) {
                return "approved";
            }
            if (reviewStatus == UserConstant.PERMISSION_REJECTED) {
                return "rejected";
            }
            if (reviewStatus == UserConstant.PERMISSION_PENDING) {
                return "pending";
            }
        }
        return null;
    }

    public static String toZhText(Integer publishPermission, Integer reviewStatus) {
        String status = toEnStatus(publishPermission, reviewStatus);
        if ("approved".equals(status)) {
            return "已批准";
        }
        if ("rejected".equals(status)) {
            return "已拒绝";
        }
        if ("pending".equals(status)) {
            return "审核中";
        }
        return "未申请";
    }
}
