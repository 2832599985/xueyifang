package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.ServiceReviewCreateRequest;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.ServiceReviewVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceReviewMapper;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务评价 Service 实现类
 *
 * @author xueyifang
 * @since 2025-11-30
 */
@Slf4j
@Component
public class ServiceReviewServiceImpl extends ServiceImpl<ServiceReviewMapper, ServiceReview>
        implements IServiceReviewService {

    /**
     * 直接依赖 Mapper 而非 IServiceOrderService / IServiceService，
     * 打破 Service 之间的循环依赖（此前依靠 @Lazy 规避）
     */
    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private IUserService userService;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    @Resource
    private RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(ServiceReviewCreateRequest request, Long currentUserId) {
        // 1. 查询订单
        ServiceOrder order = serviceOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        // 2. 校验当前用户是订单买家
        if (!order.getBuyerId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有买家可以评价");
        }

        // 3. 校验订单状态为已完成
        if (!Integer.valueOf(OrderConstant.ORDER_COMPLETED).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "只有已完成的订单可以评价");
        }

        // 4. 校验是否已评价（唯一索引也会拦截，但提前校验更友好）
        if (isOrderReviewed(request.getOrderId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该订单已评价");
        }

        // 5. 创建评价记录
        ServiceReview review = new ServiceReview();
        review.setServiceId(order.getServiceId());
        review.setOrderId(order.getId());
        review.setBuyerId(currentUserId);
        review.setSellerId(order.getSellerId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setIsAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        this.save(review);

        // 6. 更新服务平均评分
        updateServiceAverageRating(order.getServiceId());

        // 7. 删除卖家评分缓存
        deleteSellerRatingCache(order.getSellerId());

        // 8. 删除服务详情缓存（因为averageRating变了）
        deleteServiceDetailCache(order.getServiceId());

        log.info("用户{}对订单{}创建评价，评分{}", currentUserId, order.getId(), request.getRating());
        return review.getId();
    }

    /**
     * 更新服务平均评分（使用原子 SQL 聚合，避免并发问题）
     */
    private void updateServiceAverageRating(Long serviceId) {
        serviceMapper.updateAverageRating(serviceId);
        log.info("原子更新服务{}平均评分", serviceId);
    }

    @Override
    public Page<ServiceReviewVO> listServiceReviews(Long serviceId, Integer pageNum, Integer pageSize) {
        Page<ServiceReview> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getServiceId, serviceId);
        wrapper.eq(ServiceReview::getIsDelete, 0);
        wrapper.orderByDesc(ServiceReview::getCreateTime);

        Page<ServiceReview> reviewPage = this.page(page, wrapper);

        // 转换为VO，处理匿名逻辑
        List<ServiceReviewVO> voList = reviewPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<ServiceReviewVO> resultPage = new Page<>(pageNum, pageSize, reviewPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 将评价实体转换为VO，处理匿名逻辑
     */
    private ServiceReviewVO convertToVO(ServiceReview review) {
        ServiceReviewVO vo = new ServiceReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreateTime(review.getCreateTime());
        vo.setAnonymous(review.getIsAnonymous() == 1);

        // 处理匿名
        if (review.getIsAnonymous() == 1) {
            vo.setReviewerId(null);
            vo.setReviewerName("匿名用户");
            vo.setReviewerAvatar(null);  // 前端使用默认头像
        } else {
            User user = userService.getById(review.getBuyerId());
            if (user != null) {
                vo.setReviewerId(user.getId());
                vo.setReviewerName(user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getRealName());
                vo.setReviewerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(user.getAvatar()) : user.getAvatar());
            }
        }
        return vo;
    }

    @Override
    public Double getSellerAverageRating(Long sellerId) {
        // 1. 尝试从缓存获取
        String cacheKey = CacheConstant.SELLER_RATING_PREFIX + sellerId;
        Object cachedValue = redisUtils.get(cacheKey);
        if (cachedValue != null) {
            log.debug("卖家评分缓存命中: sellerId={}", sellerId);
            return Double.parseDouble(cachedValue.toString());
        }

        // 2. 缓存未命中，查询计算
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getSellerId, sellerId);
        wrapper.eq(ServiceReview::getIsDelete, 0);  // 排除已删除的评价
        List<ServiceReview> reviews = this.list(wrapper);
        if (reviews.isEmpty()) {
            return null;
        }
        double avg = reviews.stream().mapToInt(ServiceReview::getRating).average().orElse(0.0);
        Double result = Math.round(avg * 10) / 10.0;

        // 3. 写入缓存
        redisUtils.set(cacheKey, result.toString(), CacheConstant.SELLER_RATING_TTL);
        log.debug("卖家评分写入缓存: sellerId={}, rating={}", sellerId, result);

        return result;
    }

    /**
     * 删除卖家评分缓存
     */
    private void deleteSellerRatingCache(Long sellerId) {
        String cacheKey = CacheConstant.SELLER_RATING_PREFIX + sellerId;
        redisUtils.delete(cacheKey);
        log.info("卖家评分缓存已删除: sellerId={}", sellerId);
    }

    /**
     * 删除服务详情缓存
     */
    private void deleteServiceDetailCache(Long serviceId) {
        String cacheKey = CacheConstant.SERVICE_DETAIL_PREFIX + serviceId;
        redisUtils.delete(cacheKey);
        log.info("服务详情缓存已删除: serviceId={}", serviceId);
    }

    @Override
    public boolean isOrderReviewed(Long orderId) {
        LambdaQueryWrapper<ServiceReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceReview::getOrderId, orderId);
        wrapper.eq(ServiceReview::getIsDelete, 0);
        return this.count(wrapper) > 0;
    }
}
