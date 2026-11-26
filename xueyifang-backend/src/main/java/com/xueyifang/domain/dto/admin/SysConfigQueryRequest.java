package com.xueyifang.domain.dto.admin;

import lombok.Data;

/**
 * 系统配置查询请求
 */
@Data
public class SysConfigQueryRequest {
    /**
     * 配置键模糊搜索
     */
    private String keyLike;

    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}
