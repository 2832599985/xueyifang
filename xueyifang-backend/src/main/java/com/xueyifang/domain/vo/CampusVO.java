package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CampusVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String campusCode;
    private String campusName;
    private String city;
    private BigDecimal centerLat;
    private BigDecimal centerLng;
    private String geofencePolygon;
    private Integer safeRadiusMeters;
    private String eduEmailDomains;
    private Integer status;
}
