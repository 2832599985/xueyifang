package com.xueyifang.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务编辑请求DTO
 * 所有字段都是可选的，仅更新非null字段
 */
@Data
public class ServiceUpdateRequest {
    /**
     * 服务标题
     */
    private String serviceTitle;

    /**
     * 服务描述
     */
    private String serviceDescription;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 交易类型（1=线下, 2=线上担保）
     */
    private Integer tradeType;

    /**
     * 交易地点ID（仅线下交易）
     */
    private Long tradeLocationId;

    /**
     * 服务价格
     */
    private BigDecimal price;

    /**
     * 最大购买次数（-1为无限）
     */
    private Integer maxPurchases;

    /**
     * 是否实物商品（0=虚拟服务, 1=实物商品）
     */
    private Integer isPhysical;

    /**
     * 服务图片URL列表
     * null: 不更新图片
     * 空列表: 删除所有图片
     * 非空列表: 替换为新图片
     */
    private List<String> images;
}
