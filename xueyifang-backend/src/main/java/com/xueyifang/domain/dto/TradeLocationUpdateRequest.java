package com.xueyifang.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "交易地点更新请求")
public class TradeLocationUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "地点ID不能为空")
    private Long id;

    private Long campusId;
    private String locationName;
    private String locationDescription;
    private String locationAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer hasCctv;
    private Integer isAvailable;
}
