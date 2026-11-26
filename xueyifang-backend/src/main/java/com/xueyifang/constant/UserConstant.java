package com.xueyifang.constant;

/**
 * 用户常量
 * 
 * @author xueyifang
 */
public interface UserConstant {

    /**
     * 用户登录态 Session Key
     */
    String USER_LOGIN_STATE = "user_login_state";

    /**
     * 用户角色
     */
    int ROLE_STUDENT = 1;
    int ROLE_ADMIN = 2;

    /**
     * 发布权限
     */
    int NO_PUBLISH_PERMISSION = 0;
    int HAS_PUBLISH_PERMISSION = 1;

    /**
     * 权限审核状态
     */
    int PERMISSION_PENDING = 0;
    int PERMISSION_APPROVED = 1;
    int PERMISSION_REJECTED = 2;

    /**
     * 账户状态
     */
    int ACCOUNT_NORMAL = 1;
    int ACCOUNT_DISABLED = 2;

    /**
     * 年级常量
     */
    String GRADE_FRESHMAN = "大一";
    String GRADE_SOPHOMORE = "大二";
    String GRADE_JUNIOR = "大三";
    String GRADE_SENIOR = "大四";
    String GRADE_MASTER_1 = "研一";
    String GRADE_MASTER_2 = "研二";
    String GRADE_MASTER_3 = "研三";
}

