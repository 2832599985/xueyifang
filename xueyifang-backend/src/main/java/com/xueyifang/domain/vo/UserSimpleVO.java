package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户简略信息视图
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "用户简略信息", description = "用户的简略信息（用于嵌套展示）")
public class UserSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "头像URL")
    private String avatar;
}
