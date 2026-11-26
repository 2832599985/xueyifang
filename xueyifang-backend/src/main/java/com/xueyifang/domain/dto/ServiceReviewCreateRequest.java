package com.xueyifang.domain.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 创建评价请求
 *
 * @author xueyifang
 * @since 2025-11-30
 */
@Data
public class ServiceReviewCreateRequest {

    /**
     * 订单ID（必填）
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 评分（1-5）
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;

    /**
     * 评价内容（必填，10-500字）
     */
    @NotBlank(message = "评价内容不能为空")
    @Size(min = 10, max = 500, message = "评价内容需在10-500字之间")
    private String content;

    /**
     * 是否匿名，默认false
     */
    private Boolean anonymous = false;
}
