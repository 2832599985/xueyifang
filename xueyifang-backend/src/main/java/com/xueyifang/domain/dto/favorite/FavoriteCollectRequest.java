package com.xueyifang.domain.dto.favorite;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 收藏服务请求
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "FavoriteCollectRequest", description = "收藏服务请求")
public class FavoriteCollectRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "服务ID", required = true, example = "1")
    @NotNull(message = "服务ID不能为空")
    private Long serviceId;
}
