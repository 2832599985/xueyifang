package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "交易地点添加请求")
public class TradeLocationAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long campusId;

    @NotBlank(message = "地点名称不能为空")
    private String locationName;

    private String locationDescription;
    private String locationAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer validRadiusMeters;
    private Integer hasCctv;
    private Integer isAvailable;
}
