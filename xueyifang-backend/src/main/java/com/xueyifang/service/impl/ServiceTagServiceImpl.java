package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.vo.ServiceTagVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceTagMapper;
import com.xueyifang.service.IServiceTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务标签表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
@Service
@Slf4j
public class ServiceTagServiceImpl extends ServiceImpl<ServiceTagMapper, ServiceTag> implements IServiceTagService {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ServiceMapper serviceMapper;

    @Override
    public List<ServiceTagVO> listAllServiceTags() {
        // 1. 尝试从缓存获取
        List<ServiceTagVO> cachedList = redisUtils.getJsonList(CacheConstant.DICT_SERVICE_TAGS, ServiceTagVO.class);
        if (cachedList != null) {
            log.debug("服务标签缓存命中，数量: {}", cachedList.size());
            return cachedList;
        }

        // 2. 缓存未命中，查询数据库
        log.info("查询所有服务标签列表");
        QueryWrapper<ServiceTag> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");

        List<ServiceTag> serviceTagList = this.list(queryWrapper);
        log.info("查询到 {} 个服务标签", serviceTagList.size());

        // 3. 转换为VO列表
        List<ServiceTagVO> serviceTagVOList = serviceTagList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 写入缓存
        redisUtils.setJson(CacheConstant.DICT_SERVICE_TAGS, serviceTagVOList, CacheConstant.DICT_TTL_12H);
        log.debug("服务标签写入缓存，数量: {}", serviceTagVOList.size());

        return serviceTagVOList;
    }

    /**
     * 将 ServiceTag 实体转换为 ServiceTagVO
     *
     * @param serviceTag 服务标签实体
     * @return 服务标签VO
     */
    private ServiceTagVO convertToVO(ServiceTag serviceTag) {
        ServiceTagVO vo = new ServiceTagVO();
        vo.setTagId(serviceTag.getId());
        vo.setTagName(serviceTag.getTagName());
        vo.setDescription(serviceTag.getDescription());
        vo.setIcon(serviceTag.getIconUrl());
        return vo;
    }

    @Override
    public Page<ServiceTag> pageServiceTags(Long pageNum, Long pageSize, String nameLike) {
        Page<ServiceTag> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceTag> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(nameLike)) {
            wrapper.like(ServiceTag::getTagName, nameLike.trim());
        }
        wrapper.orderByAsc(ServiceTag::getId);
        return this.page(page, wrapper);
    }

    @Override
    public boolean addServiceTag(ServiceTag serviceTag) {
        if (serviceTag == null || StringUtils.isBlank(serviceTag.getTagName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称不能为空");
        }
        String tagName = serviceTag.getTagName().trim();
        long count = this.count(new LambdaQueryWrapper<ServiceTag>().eq(ServiceTag::getTagName, tagName));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称已存在");
        }
        ServiceTag entity = new ServiceTag();
        entity.setTagName(tagName);
        entity.setDescription(serviceTag.getDescription());
        entity.setIconUrl(serviceTag.getIconUrl() != null ? serviceTag.getIconUrl() : "");
        boolean saved = this.save(entity);
        if (saved && redisUtils != null) {
            redisUtils.delete(CacheConstant.DICT_SERVICE_TAGS);
        }
        return saved;
    }

    @Override
    public boolean updateServiceTag(ServiceTag serviceTag) {
        if (serviceTag == null || serviceTag.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        ServiceTag existing = this.getById(serviceTag.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "服务标签不存在");
        }
        if (StringUtils.isNotBlank(serviceTag.getTagName())) {
            String tagName = serviceTag.getTagName().trim();
            long count = this.count(new LambdaQueryWrapper<ServiceTag>()
                    .eq(ServiceTag::getTagName, tagName)
                    .ne(ServiceTag::getId, serviceTag.getId()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称已存在");
            }
            existing.setTagName(tagName);
        }
        if (serviceTag.getDescription() != null) {
            existing.setDescription(serviceTag.getDescription());
        }
        if (serviceTag.getIconUrl() != null) {
            existing.setIconUrl(serviceTag.getIconUrl());
        }
        boolean updated = this.updateById(existing);
        if (updated && redisUtils != null) {
            redisUtils.delete(CacheConstant.DICT_SERVICE_TAGS);
        }
        return updated;
    }

    @Override
    public boolean deleteServiceTag(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (serviceMapper != null) {
            Long usedCount = serviceMapper.selectCount(new LambdaQueryWrapper<com.xueyifang.domain.po.Service>()
                    .eq(com.xueyifang.domain.po.Service::getTagId, id)
                    .eq(com.xueyifang.domain.po.Service::getIsDelete, 0));
            if (usedCount != null && usedCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类标签下仍有服务，无法删除");
            }
        }
        boolean removed = this.removeById(id);
        if (removed && redisUtils != null) {
            redisUtils.delete(CacheConstant.DICT_SERVICE_TAGS);
        }
        return removed;
    }
}
