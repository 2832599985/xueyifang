package com.xueyifang.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author luoyi
 * @since 2025-10-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@ApiModel(value="User对象", description="用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "学号，唯一，注册后不可修改")
    private String studentId;

    @ApiModelProperty(value = "密码，加密存储")
    private String password;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "手机号，必填")
    private String phone;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "寝室号")
    private String dormitory;

    @ApiModelProperty(value = "年级（大一、大二、大三、大四）")
    private String grade;

    @ApiModelProperty(value = "专业ID")
    private Long professionalId;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人简介")
    private String bio;

    @ApiModelProperty(value = "用户角色（1=学生, 2=管理员）")
    private Integer role;

    @ApiModelProperty(value = "发布权限（0=无, 1=有）")
    private Integer publishPermission;

    @ApiModelProperty(value = "权限审核状态（NULL=未申请, 0=未审核/审核中, 1=已批准, 2=已拒绝）")
    private Integer permissionReviewStatus;

    @ApiModelProperty(value = "发布权限申请理由")
    private String permissionApplyReason;

    @ApiModelProperty(value = "钱包余额")
    private BigDecimal walletBalance;

    @ApiModelProperty(value = "冻结金额（担保交易）")
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "账户状态（1=正常, 2=禁用）")
    private Integer accountStatus;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


}
