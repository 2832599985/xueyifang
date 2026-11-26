package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 卖家发货/履约提交请求（可选携带交付说明与成果凭证图）
 */
@Data
@ApiModel(value = "SellerShipRequest", description = "卖家发货与交付凭证请求")
public class SellerShipRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "履约交付说明（如网盘/Git链接、辅导纪要、现场交接备注等，最多500字）")
    private String deliveryNote;

    @ApiModelProperty(value = "交付成果凭证图片URL列表（最多6张）")
    private List<String> deliveryImages;
}
