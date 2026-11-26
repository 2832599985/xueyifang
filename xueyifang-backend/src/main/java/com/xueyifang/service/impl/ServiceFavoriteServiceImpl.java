package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceFavorite;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.FavoriteListItemVO;
import com.xueyifang.domain.vo.ServiceListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceFavoriteMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IServiceFavoriteService;
import com.xueyifang.service.IServiceImageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务收藏表 服务实现类
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-25
 */
@Component
public class ServiceFavoriteServiceImpl extends ServiceImpl<ServiceFavoriteMapper, ServiceFavorite>
        implements IServiceFavoriteService {

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IServiceImageService serviceImageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectService(Long userId, Long serviceId) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 检查服务是否存在
        Service service = serviceMapper.selectById(serviceId);
        if (service == null || service.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务不存在");
        }

        // 3. 检查是否已收藏
        LambdaQueryWrapper<ServiceFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceFavorite::getUserId, userId)
                    .eq(ServiceFavorite::getServiceId, serviceId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            // 已收藏，直接返回（幂等性）
            return;
        }

        // 4. 插入收藏记录
        ServiceFavorite favorite = new ServiceFavorite();
        favorite.setUserId(userId);
        favorite.setServiceId(serviceId);
        favorite.setCreateTime(LocalDateTime.now());
        favorite.setUpdateTime(LocalDateTime.now());
        this.save(favorite);

        // 5. 更新服务收藏数 +1
        serviceMapper.update(null,
            new UpdateWrapper<Service>()
                .eq("id", serviceId)
                .setSql("collection_count = collection_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectService(Long userId, Long serviceId) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询收藏记录
        LambdaQueryWrapper<ServiceFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceFavorite::getUserId, userId)
                    .eq(ServiceFavorite::getServiceId, serviceId);
        ServiceFavorite favorite = this.getOne(queryWrapper);

        if (favorite == null) {
            // 未收藏，直接返回（幂等性）
            return;
        }

        // 3. 删除收藏记录（硬删除）
        this.removeById(favorite.getId());

        // 4. 更新服务收藏数 -1（但不能小于0）
        serviceMapper.update(null,
            new UpdateWrapper<Service>()
                .eq("id", serviceId)
                .setSql("collection_count = GREATEST(collection_count - 1, 0)"));
    }

    @Override
    public Page<FavoriteListItemVO> pageMyCollections(Long userId, Integer pageNum, Integer pageSize) {
        // 1. 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 2. 分页查询收藏记录
        Page<ServiceFavorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceFavorite::getUserId, userId)
                    .orderByDesc(ServiceFavorite::getCreateTime);
        Page<ServiceFavorite> favoritePage = this.page(page, queryWrapper);

        // 3. 提取 serviceId 列表
        List<Long> serviceIds = favoritePage.getRecords().stream()
                .map(ServiceFavorite::getServiceId)
                .collect(Collectors.toList());

        // 4. 如果没有收藏记录，返回空结果
        if (serviceIds.isEmpty()) {
            Page<FavoriteListItemVO> result = new Page<>(pageNum, pageSize);
            result.setTotal(0);
            result.setRecords(new ArrayList<>());
            return result;
        }

        // 5. 批量查询服务信息
        LambdaQueryWrapper<Service> serviceQuery = new LambdaQueryWrapper<>();
        serviceQuery.in(Service::getId, serviceIds)
                    .eq(Service::getIsDelete, 0);
        List<Service> services = serviceMapper.selectList(serviceQuery);
        Map<Long, Service> serviceMap = services.stream()
                .collect(Collectors.toMap(Service::getId, s -> s));

        // 6. 批量查询卖家信息
        List<Long> sellerIds = services.stream()
                .map(Service::getSellerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> sellerMap = sellerIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 7. 组装 VO
        List<FavoriteListItemVO> voList = favoritePage.getRecords().stream()
                .map(favorite -> {
                    FavoriteListItemVO vo = new FavoriteListItemVO();
                    vo.setFavoriteId(favorite.getId());
                    vo.setCreateTime(favorite.getCreateTime());

                    Service service = serviceMap.get(favorite.getServiceId());
                    if (service != null) {
                        ServiceListItemVO serviceVO = new ServiceListItemVO();
                        BeanUtils.copyProperties(service, serviceVO);

                        // 设置服务封面图
                        serviceVO.setCoverImage(serviceImageService.getCoverImage(service.getId()));

                        // 设置卖家名称
                        User seller = sellerMap.get(service.getSellerId());
                        if (seller != null) {
                            vo.setSellerName(seller.getRealName());
                        }

                        vo.setService(serviceVO);
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        // 8. 封装分页结果
        Page<FavoriteListItemVO> result = new Page<>(pageNum, pageSize);
        result.setTotal(favoritePage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public boolean isCollected(Long userId, Long serviceId) {
        if (userId == null || serviceId == null) {
            return false;
        }

        LambdaQueryWrapper<ServiceFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceFavorite::getUserId, userId)
                    .eq(ServiceFavorite::getServiceId, serviceId);
        return this.count(queryWrapper) > 0;
    }
}
