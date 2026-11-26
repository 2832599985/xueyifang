package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息更新请求
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "用户信息更新请求", description = "用户更新个人信息请求参数")
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "昵称", example = "小张")
    private String nickname;

    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "邮箱", example = "new@example.com")
    private String email;

    @ApiModelProperty(value = "寝室号", example = "宿舍2-202")
    private String dormitory;

    @ApiModelProperty(value = "年级（可选值：大一、大二、大三、大四）", example = "大三")
    private String grade;

    @ApiModelProperty(value = "专业ID", example = "1")
    private Long professionalId;

    @ApiModelProperty(value = "头像URL", example = "https://example.com/new_avatar.jpg")
    private String avatar;

    @ApiModelProperty(value = "个人简介", example = "热爱编程和设计")
    private String bio;
}

