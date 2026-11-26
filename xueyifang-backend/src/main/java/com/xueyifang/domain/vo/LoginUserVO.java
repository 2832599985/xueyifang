package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已登录用户视图（脱敏，含校园实名认证、多校区与信用分）
 */
@Data
@ApiModel(value = "登录用户信息", description = "登录成功后返回的用户信息（脱敏）")
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentId;
    private String realName;
    private String nickname;
    private String phone;
    private String email;
    private String dormitory;
    private String grade;
    private Long professionalId;
    private String professionalName;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    @ApiModelProperty(value = "所属校区名称")
    private String campusName;

    private String avatar;
    private String bio;
    private Integer role;
    private Integer publishPermission;
    private Integer permissionReviewStatus;
    private BigDecimal walletBalance;
    private Integer accountStatus;

    @ApiModelProperty(value = "校园教育邮箱实名核验状态（0=未核验, 1=已核验）")
    private Integer eduVerified;

    @ApiModelProperty(value = "已核验校园邮箱")
    private String verifiedEduEmail;

    @ApiModelProperty(value = "校园履约信用分")
    private Integer creditScore;

    @ApiModelProperty(value = "卖家履约保证金")
    private BigDecimal sellerDeposit;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
