package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupSessionQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 12;
    private Long campusId;
    private Long tagId;
    private Integer tradeType;
    private Integer sessionStatus;
    private String keyword;
}
