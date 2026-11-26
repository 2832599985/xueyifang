package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.po.ServiceFavorite;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.FavoriteListItemVO;

/**
 * <p>
 * 服务收藏表 服务类
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-25
 */
public interface IServiceFavoriteService extends IService<ServiceFavorite> {

    /**
     * 收藏服务
     *
     * @param userId 用户ID
     * @param serviceId 服务ID
     */
    void collectService(Long userId, Long serviceId);

    /**
     * 取消收藏服务
     *
     * @param userId 用户ID
     * @param serviceId 服务ID
     */
    void uncollectService(Long userId, Long serviceId);

    /**
     * 分页查询我的收藏列表
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 收藏列表
     */
    Page<FavoriteListItemVO> pageMyCollections(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 判断用户是否收藏了指定服务
     *
     * @param userId 用户ID
     * @param serviceId 服务ID
     * @return 是否已收藏
     */
    boolean isCollected(Long userId, Long serviceId);
}
