package com.xueyifang.service;

import com.xueyifang.domain.po.ServiceImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * 服务图片表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
public interface IServiceImageService extends IService<ServiceImage> {

    /**
     * 获取服务的封面图片URL（按sortOrder排序的第一张）
     * @param serviceId 服务ID
     * @return 封面图片URL，如果没有则返回null
     */
    String getCoverImage(Long serviceId);

    /**
     * 批量获取服务封面图（一次查询，避免 N+1）
     *
     * @param serviceIds 服务ID集合
     * @return serviceId -> 封面URL，没有图片的服务不包含在结果中
     */
    Map<Long, String> getCoverImageMap(Collection<Long> serviceIds);

}
