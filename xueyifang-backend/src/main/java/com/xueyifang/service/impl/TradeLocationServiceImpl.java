package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.dto.TradeLocationAddRequest;
import com.xueyifang.domain.dto.TradeLocationUpdateRequest;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.vo.TradeLocationVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.TradeLocationMapper;
import com.xueyifang.service.ITradeLocationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 交易地点表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-29
 */
@Service
@Slf4j
public class TradeLocationServiceImpl extends ServiceImpl<TradeLocationMapper, TradeLocation> implements ITradeLocationService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<TradeLocationVO> listAllTradeLocations() {
        // 1. 尝试从缓存获取
        List<TradeLocationVO> cachedList = redisUtils.getJsonList(CacheConstant.DICT_TRADE_LOCATIONS, TradeLocationVO.class);
        if (cachedList != null) {
            log.debug("交易地点缓存命中，数量: {}", cachedList.size());
            return cachedList;
        }

        // 2. 缓存未命中，查询数据库
        log.info("查询所有可用的交易地点列表");
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_available", 1);
        queryWrapper.orderByDesc("create_time");

        List<TradeLocation> tradeLocationList = this.list(queryWrapper);
        log.info("查询到 {} 个可用的交易地点", tradeLocationList.size());

        // 3. 转换为VO列表
        List<TradeLocationVO> tradeLocationVOList = tradeLocationList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 写入缓存
        redisUtils.setJson(CacheConstant.DICT_TRADE_LOCATIONS, tradeLocationVOList, CacheConstant.DICT_TTL_12H);
        log.debug("交易地点写入缓存，数量: {}", tradeLocationVOList.size());

        return tradeLocationVOList;
    }

    /**
     * 删除交易地点缓存
     */
    private void deleteTradeLocationCache() {
        redisUtils.delete(CacheConstant.DICT_TRADE_LOCATIONS);
        log.info("交易地点缓存已删除");
    }

    @Override
    public List<TradeLocationVO> listAllTradeLocationsIncludingUnavailable() {
        log.info("查询所有交易地点列表（包括不可用的）");

        // MyBatis-Plus 会自动过滤已删除的数据（通过 @TableLogic 注解）
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");

        List<TradeLocation> tradeLocationList = this.list(queryWrapper);
        log.info("查询到 {} 个交易地点", tradeLocationList.size());

        // 将 TradeLocation 实体列表转换为 TradeLocationVO 列表
        List<TradeLocationVO> tradeLocationVOList = tradeLocationList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return tradeLocationVOList;
    }

    @Override
    public Page<TradeLocationVO> pageTradeLocations(Long current, Long pageSize, Integer isAvailable) {
        log.info("分页查询交易地点，当前页：{}，每页大小：{}，是否可用：{}", current, pageSize, isAvailable);

        // 参数校验
        if (current == null || current < 1) {
            current = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        // 构建分页对象
        Page<TradeLocation> page = new Page<>(current, pageSize);

        // 构建查询条件
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        if (isAvailable != null) {
            queryWrapper.eq("is_available", isAvailable);
        }
        queryWrapper.orderByDesc("create_time");

        // 执行分页查询
        Page<TradeLocation> tradeLocationPage = this.page(page, queryWrapper);

        // 转换为 VO 分页对象
        Page<TradeLocationVO> voPage = new Page<>(tradeLocationPage.getCurrent(), tradeLocationPage.getSize(), tradeLocationPage.getTotal());
        List<TradeLocationVO> voList = tradeLocationPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        log.info("分页查询成功，总记录数：{}，当前页记录数：{}", voPage.getTotal(), voList.size());
        return voPage;
    }

    @Override
    public Page<TradeLocation> pageTradeLocationsForAdmin(Long current, Long pageSize, Integer isAvailable, String nameLike) {
        log.info("管理员分页查询交易地点，当前页：{}，每页大小：{}，是否可用：{}，名称模糊：{}", current, pageSize, isAvailable, nameLike);

        if (current == null || current < 1) {
            current = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        Page<TradeLocation> page = new Page<>(current, pageSize);
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        if (isAvailable != null) {
            queryWrapper.eq("is_available", isAvailable);
        }
        if (StringUtils.isNotBlank(nameLike)) {
            queryWrapper.like("location_name", nameLike.trim());
        }
        queryWrapper.orderByDesc("create_time");

        return this.page(page, queryWrapper);
    }

    @Override
    public TradeLocation getTradeLocationById(Long id) {
        log.info("查询交易地点详情，id：{}", id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }

        // MyBatis-Plus 会自动过滤已删除的数据（通过 @TableLogic 注解）
        TradeLocation tradeLocation = this.getById(id);
        if (tradeLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }

        log.info("查询交易地点成功，地点名称：{}", tradeLocation.getLocationName());
        return tradeLocation;
    }

    @Override
    public boolean addTradeLocation(TradeLocationAddRequest addRequest) {
        log.info("添加交易地点，地点名称：{}", addRequest.getLocationName());

        // 参数校验
        validateAddRequest(addRequest);

        // 检查地点名称是否已存在（MyBatis-Plus 会自动过滤已删除的数据）
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("location_name", addRequest.getLocationName());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点名称已存在");
        }

        // 构建实体对象
        TradeLocation tradeLocation = new TradeLocation();
        BeanUtils.copyProperties(addRequest, tradeLocation);

        // 设置默认值
        if (tradeLocation.getIsAvailable() == null) {
            tradeLocation.setIsAvailable(1); // 默认可用
        }

        // 保存交易地点
        boolean result = this.save(tradeLocation);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加交易地点失败");
        }

        // 删除缓存
        deleteTradeLocationCache();

        log.info("添加交易地点成功，id：{}", tradeLocation.getId());
        return true;
    }


    @Override
    public boolean updateTradeLocation(TradeLocationUpdateRequest updateRequest) {
        log.info("更新交易地点，id：{}", updateRequest.getId());

        // 参数校验
        validateUpdateRequest(updateRequest);

        // 检查交易地点是否存在
        TradeLocation existingLocation = this.getById(updateRequest.getId());
        if (existingLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }

        // 如果修改了地点名称，检查新名称是否已存在
        if (StringUtils.isNotBlank(updateRequest.getLocationName())
                && !updateRequest.getLocationName().equals(existingLocation.getLocationName())) {
            QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("location_name", updateRequest.getLocationName());
            queryWrapper.ne("id", updateRequest.getId());
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点名称已存在");
            }
        }

        // 构建更新对象
        TradeLocation tradeLocation = new TradeLocation();
        BeanUtils.copyProperties(updateRequest, tradeLocation);

        // 执行更新
        boolean result = this.updateById(tradeLocation);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新交易地点失败");
        }

        // 删除缓存
        deleteTradeLocationCache();

        log.info("更新交易地点成功，id：{}", updateRequest.getId());
        return true;
    }

    @Override
    public boolean deleteTradeLocation(Long id) {
        log.info("删除交易地点，id：{}", id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }

        // 检查交易地点是否存在
        TradeLocation tradeLocation = this.getById(id);
        if (tradeLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }

        // 执行逻辑删除（MyBatis-Plus 会自动处理）
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除交易地点失败");
        }

        // 删除缓存
        deleteTradeLocationCache();

        log.info("删除交易地点成功，id：{}", id);
        return true;
    }

    @Override
    public boolean batchDeleteTradeLocations(List<Long> ids) {
        log.info("批量删除交易地点，数量：{}", ids.size());

        // 参数校验
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID列表不能为空");
        }

        // 执行批量逻辑删除（MyBatis-Plus 会自动处理）
        boolean result = this.removeByIds(ids);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量删除交易地点失败");
        }

        // 删除缓存
        deleteTradeLocationCache();

        log.info("批量删除交易地点成功，数量：{}", ids.size());
        return true;
    }

    /**
     * 将 TradeLocation 实体转换为 TradeLocationVO
     *
     * @param tradeLocation 交易地点实体
     * @return 交易地点VO
     */
    private TradeLocationVO convertToVO(TradeLocation tradeLocation) {
        TradeLocationVO vo = new TradeLocationVO();
        BeanUtils.copyProperties(tradeLocation, vo);
        return vo;
    }

    /**
     * 校验添加请求参数
     *
     * @param addRequest 添加请求
     */
    private void validateAddRequest(TradeLocationAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String locationName = addRequest.getLocationName();
        if (StringUtils.isBlank(locationName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点名称不能为空");
        }

        if (locationName.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点名称长度不能超过100个字符");
        }

        if (addRequest.getLocationDescription() != null && addRequest.getLocationDescription().length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点描述长度不能超过500个字符");
        }

        if (addRequest.getLocationAddress() != null && addRequest.getLocationAddress().length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "详细地址长度不能超过200个字符");
        }

        if (addRequest.getIsAvailable() != null && addRequest.getIsAvailable() != 0 && addRequest.getIsAvailable() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否可用参数错误，只能为0或1");
        }
    }

    /**
     * 校验更新请求参数
     *
     * @param updateRequest 更新请求
     */
    private void validateUpdateRequest(TradeLocationUpdateRequest updateRequest) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (updateRequest.getId() == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }

        if (updateRequest.getLocationName() != null && updateRequest.getLocationName().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点名称长度不能超过100个字符");
        }

        if (updateRequest.getLocationDescription() != null && updateRequest.getLocationDescription().length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点描述长度不能超过500个字符");
        }

        if (updateRequest.getLocationAddress() != null && updateRequest.getLocationAddress().length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "详细地址长度不能超过200个字符");
        }

        if (updateRequest.getIsAvailable() != null && updateRequest.getIsAvailable() != 0 && updateRequest.getIsAvailable() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否可用参数错误，只能为0或1");
        }
    }
}
