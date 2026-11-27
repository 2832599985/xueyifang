package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DemandQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 12;
    private Long campusId;
    private Long tagId;
    private Integer tradeType;
    private Integer urgencyLevel;
    private Integer demandStatus;
    private String keyword;
}
