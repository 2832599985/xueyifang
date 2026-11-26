package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueyifang.domain.po.ServiceImage;
import com.xueyifang.mapper.ServiceImageMapper;
import com.xueyifang.service.IServiceImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.FileUrlResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务图片表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
@Service
public class ServiceImageServiceImpl extends ServiceImpl<ServiceImageMapper, ServiceImage> implements IServiceImageService {

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Override
    public String getCoverImage(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        LambdaQueryWrapper<ServiceImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceImage::getServiceId, serviceId)
                .eq(ServiceImage::getIsDelete, 0)
                .orderByAsc(ServiceImage::getSortOrder)
                .orderByAsc(ServiceImage::getId)
                .last("LIMIT 1");
        ServiceImage image = this.getOne(queryWrapper);
        if (image == null) {
            return null;
        }
        return fileUrlResolver != null ? fileUrlResolver.toPublicUrl(image.getImageUrl()) : image.getImageUrl();
    }

    @Override
    public Map<Long, String> getCoverImageMap(Collection<Long> serviceIds) {
        Map<Long, String> coverMap = new HashMap<>();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return coverMap;
        }
        LambdaQueryWrapper<ServiceImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ServiceImage::getServiceId, serviceIds)
                .eq(ServiceImage::getIsDelete, 0)
                .orderByAsc(ServiceImage::getSortOrder)
                .orderByAsc(ServiceImage::getId);
        List<ServiceImage> images = this.list(queryWrapper);
        // 已按 sortOrder 升序，每个服务保留第一张并统一解析为完整对象存储 URL
        for (ServiceImage image : images) {
            String publicUrl = fileUrlResolver != null ? fileUrlResolver.toPublicUrl(image.getImageUrl()) : image.getImageUrl();
            coverMap.putIfAbsent(image.getServiceId(), publicUrl);
        }
        return coverMap;
    }
}
