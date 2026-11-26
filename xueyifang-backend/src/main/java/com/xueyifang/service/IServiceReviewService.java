package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.ServiceReviewCreateRequest;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.vo.ServiceReviewVO;

/**
 * 服务评价 Service 接口
 *
 * @author xueyifang
 * @since 2025-11-30
 */
public interface IServiceReviewService extends IService<ServiceReview> {

    /**
     * 创建评价
     *
     * @param request 创建评价请求
     * @param currentUserId 当前用户ID
     * @return 评价ID
     */
    Long createReview(ServiceReviewCreateRequest request, Long currentUserId);

    /**
     * 分页查询服务的评价列表
     *
     * @param serviceId 服务ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 评价分页列表
     */
    Page<ServiceReviewVO> listServiceReviews(Long serviceId, Integer pageNum, Integer pageSize);

    /**
     * 查询订单是否已评价
     *
     * @param orderId 订单ID
     * @return true=已评价，false=未评价
     */
    boolean isOrderReviewed(Long orderId);

    /**
     * 计算卖家平均评分
     *
     * @param sellerId 卖家ID
     * @return 平均评分（1-5），无评价返回null
     */
    Double getSellerAverageRating(Long sellerId);
}
