package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 专业视图对象
 * 用于返回给前端的专业信息（不包含内部字段）
 *
 * @author luoyi
 */
@Data
@ApiModel(value = "专业信息", description = "专业视图对象，用于返回专业列表和详情")
public class ProfessionalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "专业ID")
    private Long id;

    @ApiModelProperty(value = "专业名称")
    private String professionalName;

    @ApiModelProperty(value = "专业描述")
    private String description;
}

