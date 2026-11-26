package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "用户注册请求", description = "用户注册请求参数")
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "学号", required = true, example = "20240001")
    private String studentId;

    @ApiModelProperty(value = "密码", required = true, example = "password123")
    private String password;

    @ApiModelProperty(value = "确认密码", required = true, example = "password123")
    private String confirmPassword;

    @ApiModelProperty(value = "真实姓名", required = true, example = "张三")
    private String realName;

    @ApiModelProperty(value = "手机号", required = true, example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "专业ID", required = true, example = "1")
    private Long professionalId;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    private String email;

    @ApiModelProperty(value = "寝室号", example = "宿舍1-101")
    private String dormitory;
}

