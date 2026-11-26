package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.dto.TradeLocationAddRequest;
import com.xueyifang.domain.dto.TradeLocationUpdateRequest;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.vo.TradeLocationVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.TradeLocationMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.utils.GeofenceUtils;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TradeLocationServiceImpl extends ServiceImpl<TradeLocationMapper, TradeLocation> implements ITradeLocationService {

    public static final int DEFAULT_VALID_RADIUS_METERS = 80;
    public static final int MIN_VALID_RADIUS_METERS = 10;
    public static final int MAX_VALID_RADIUS_METERS = 2000;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ICampusService campusService;

    @Override
    public List<TradeLocationVO> listAllTradeLocations() {
        List<TradeLocationVO> cachedList = redisUtils != null
                ? redisUtils.getJsonList(CacheConstant.DICT_TRADE_LOCATIONS, TradeLocationVO.class)
                : null;
        if (cachedList != null) {
            return cachedList;
        }

        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_available", 1);
        queryWrapper.orderByDesc("create_time");

        List<TradeLocationVO> tradeLocationVOList = this.list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        if (redisUtils != null) {
            redisUtils.setJson(CacheConstant.DICT_TRADE_LOCATIONS, tradeLocationVOList, CacheConstant.DICT_TTL_12H);
        }
        return tradeLocationVOList;
    }

    private void deleteTradeLocationCache() {
        if (redisUtils != null) {
            redisUtils.delete(CacheConstant.DICT_TRADE_LOCATIONS);
        }
    }

    @Override
    public List<TradeLocationVO> listAllTradeLocationsIncludingUnavailable() {
        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<TradeLocationVO> pageTradeLocations(Long current, Long pageSize, Integer isAvailable) {
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
        queryWrapper.orderByDesc("create_time");

        Page<TradeLocation> tradeLocationPage = this.page(page, queryWrapper);
        Page<TradeLocationVO> voPage = new Page<>(tradeLocationPage.getCurrent(), tradeLocationPage.getSize(), tradeLocationPage.getTotal());
        voPage.setRecords(tradeLocationPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<TradeLocation> pageTradeLocationsForAdmin(Long current, Long pageSize, Integer isAvailable, String nameLike) {
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
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }
        TradeLocation tradeLocation = this.getById(id);
        if (tradeLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }
        return tradeLocation;
    }

    @Override
    public boolean addTradeLocation(TradeLocationAddRequest addRequest) {
        validateAddRequest(addRequest);

        QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("location_name", addRequest.getLocationName().trim());
        if (this.count(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点名称已存在");
        }

        TradeLocation tradeLocation = new TradeLocation();
        BeanUtils.copyProperties(addRequest, tradeLocation);
        tradeLocation.setLocationName(addRequest.getLocationName().trim());
        if (tradeLocation.getCampusId() == null) {
            tradeLocation.setCampusId(1L);
        }
        if (tradeLocation.getValidRadiusMeters() == null) {
            tradeLocation.setValidRadiusMeters(DEFAULT_VALID_RADIUS_METERS);
        }
        if (tradeLocation.getHasCctv() == null) {
            tradeLocation.setHasCctv(1);
        }
        if (tradeLocation.getIsAvailable() == null) {
            tradeLocation.setIsAvailable(1);
        }

        if (!this.save(tradeLocation)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加交易地点失败");
        }
        deleteTradeLocationCache();
        return true;
    }

    @Override
    public boolean updateTradeLocation(TradeLocationUpdateRequest updateRequest) {
        validateUpdateRequest(updateRequest);

        TradeLocation existingLocation = this.getById(updateRequest.getId());
        if (existingLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }

        if (StringUtils.isNotBlank(updateRequest.getLocationName())
                && !updateRequest.getLocationName().trim().equals(existingLocation.getLocationName())) {
            QueryWrapper<TradeLocation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("location_name", updateRequest.getLocationName().trim());
            queryWrapper.ne("id", updateRequest.getId());
            if (this.count(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点名称已存在");
            }
        }

        TradeLocation tradeLocation = new TradeLocation();
        BeanUtils.copyProperties(updateRequest, tradeLocation);
        if (StringUtils.isNotBlank(updateRequest.getLocationName())) {
            tradeLocation.setLocationName(updateRequest.getLocationName().trim());
        }

        if (!this.updateById(tradeLocation)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新交易地点失败");
        }
        deleteTradeLocationCache();
        return true;
    }

    @Override
    public boolean deleteTradeLocation(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "交易地点不存在或已被删除");
        }
        if (!this.removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除交易地点失败");
        }
        deleteTradeLocationCache();
        return true;
    }

    @Override
    public boolean batchDeleteTradeLocations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID列表不能为空");
        }
        if (!this.removeByIds(ids)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量删除交易地点失败");
        }
        deleteTradeLocationCache();
        return true;
    }

    private TradeLocationVO convertToVO(TradeLocation tradeLocation) {
        TradeLocationVO vo = new TradeLocationVO();
        BeanUtils.copyProperties(tradeLocation, vo);
        if (vo.getValidRadiusMeters() == null) {
            vo.setValidRadiusMeters(DEFAULT_VALID_RADIUS_METERS);
        }
        return vo;
    }

    private void validateGeoAndRadius(Long campusId, java.math.BigDecimal lat, java.math.BigDecimal lng, Integer radiusMeters) {
        if (lat != null || lng != null) {
            if (lat == null || lng == null || !GeofenceUtils.isValidCoordinate(lat.doubleValue(), lng.doubleValue())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请在真实地图上标记合法的经纬度坐标");
            }
            if (campusService != null) {
                campusService.validateCoordinateInCampusFence(campusId != null ? campusId : 1L, lat.doubleValue(), lng.doubleValue());
            }
        }
        if (radiusMeters != null && (radiusMeters < MIN_VALID_RADIUS_METERS || radiusMeters > MAX_VALID_RADIUS_METERS)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("有效核销范围半径需设置在 %d 米至 %d 米之间", MIN_VALID_RADIUS_METERS, MAX_VALID_RADIUS_METERS));
        }
    }

    private void validateAddRequest(TradeLocationAddRequest addRequest) {
        if (addRequest == null || StringUtils.isBlank(addRequest.getLocationName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点名称不能为空");
        }
        if (addRequest.getLocationName().trim().length() > 100) {
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
        validateGeoAndRadius(addRequest.getCampusId(), addRequest.getLatitude(), addRequest.getLongitude(), addRequest.getValidRadiusMeters());
    }

    private void validateUpdateRequest(TradeLocationUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地点ID不能为空");
        }
        if (updateRequest.getLocationName() != null && updateRequest.getLocationName().trim().length() > 100) {
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
        validateGeoAndRadius(updateRequest.getCampusId(), updateRequest.getLatitude(), updateRequest.getLongitude(), updateRequest.getValidRadiusMeters());
    }
}
