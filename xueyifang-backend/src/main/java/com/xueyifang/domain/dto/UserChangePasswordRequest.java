package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户修改密码请求
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "用户修改密码请求", description = "用户修改密码请求参数")
public class UserChangePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "旧密码", required = true, example = "password123")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true, example = "newpassword456")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @ApiModelProperty(value = "确认新密码", required = true, example = "newpassword456")
    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;
}

