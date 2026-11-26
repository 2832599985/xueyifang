package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已登录用户视图（脱敏）
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "登录用户信息", description = "登录成功后返回的用户信息（脱敏）")
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "学号")
    private String studentId;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "寝室号")
    private String dormitory;

    @ApiModelProperty(value = "年级")
    private String grade;

    @ApiModelProperty(value = "专业ID")
    private Long professionalId;

    @ApiModelProperty(value = "专业名称")
    private String professionalName;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人简介")
    private String bio;

    @ApiModelProperty(value = "用户角色（1=学生, 2=管理员）")
    private Integer role;

    @ApiModelProperty(value = "发布权限（0=无, 1=有）")
    private Integer publishPermission;

    @ApiModelProperty(value = "权限审核状态（0=未审核, 1=已批准, 2=已拒绝）")
    private Integer permissionReviewStatus;

    @ApiModelProperty(value = "钱包余额")
    private BigDecimal walletBalance;

    @ApiModelProperty(value = "账户状态（1=正常, 2=禁用）")
    private Integer accountStatus;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    // 注意：密码、冻结金额等敏感信息不在此VO中返回
}

