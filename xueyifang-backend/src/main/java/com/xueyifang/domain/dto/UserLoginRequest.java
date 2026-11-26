package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "用户登录请求", description = "用户登录请求参数")
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "学号", required = true, example = "2021001")
    @NotBlank(message = "学号不能为空")
    private String studentId;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}

