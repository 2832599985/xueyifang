package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户表（v3.0 商业版：含多校区、教育邮箱实名核验、履约信用分与保证金）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@ApiModel(value = "User对象", description = "用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "学号，唯一，注册后不可修改")
    private String studentId;

    @ApiModelProperty(value = "密码，BCrypt 加密存储")
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

    @ApiModelProperty(value = "年级")
    private String grade;

    @ApiModelProperty(value = "专业ID")
    private Long professionalId;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人简介")
    private String bio;

    @ApiModelProperty(value = "用户角色（1=学生, 2=管理员）")
    private Integer role;

    @ApiModelProperty(value = "发布权限（0=无, 1=有）")
    private Integer publishPermission;

    @ApiModelProperty(value = "权限审核状态（NULL=未申请, 0=审核中, 1=已批准, 2=已拒绝）")
    private Integer permissionReviewStatus;

    @ApiModelProperty(value = "发布权限申请理由")
    private String permissionApplyReason;

    @ApiModelProperty(value = "钱包余额（结算可提现收益）")
    private BigDecimal walletBalance;

    @ApiModelProperty(value = "冻结金额（担保交易）")
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "账户状态（1=正常, 2=禁用）")
    private Integer accountStatus;

    @ApiModelProperty(value = "校园教育邮箱实名核验状态（0=未核验, 1=已核验）")
    private Integer eduVerified;

    @ApiModelProperty(value = "已核验校园邮箱")
    private String verifiedEduEmail;

    @ApiModelProperty(value = "校园履约信用分（初始100，上限120）")
    private Integer creditScore;

    @ApiModelProperty(value = "卖家履约保证金")
    private BigDecimal sellerDeposit;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;
}
