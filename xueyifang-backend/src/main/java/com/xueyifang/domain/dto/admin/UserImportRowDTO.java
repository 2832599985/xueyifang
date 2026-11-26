package com.xueyifang.domain.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入用户单行数据结构
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "用户导入行数据", description = "Excel/CSV 单行解析结构")
public class UserImportRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行号（用于错误定位）
     */
    @ApiModelProperty(value = "行号", example = "2")
    private Integer rowNum;

    /**
     * 学号（必填）
     */
    @ApiModelProperty(value = "学号", required = true)
    private String studentId;

    /**
     * 真实姓名（必填）
     */
    @ApiModelProperty(value = "真实姓名", required = true)
    private String realName;

    /**
     * 手机号（必填）
     */
    @ApiModelProperty(value = "手机号", required = true)
    private String phone;

    /**
     * 专业ID（必填）
     */
    @ApiModelProperty(value = "专业ID", required = true)
    private Long professionalId;

    /**
     * 密码（必填）
     */
    @ApiModelProperty(value = "密码", required = true)
    private String password;

    /**
     * 邮箱（可选）
     */
    @ApiModelProperty(value = "邮箱")
    private String email;

    /**
     * 寝室号（可选）
     */
    @ApiModelProperty(value = "寝室号")
    private String dormitory;

    /**
     * 年级（可选）
     */
    @ApiModelProperty(value = "年级")
    private String grade;

    /**
     * 昵称（可选）
     */
    @ApiModelProperty(value = "昵称")
    private String nickname;
}
