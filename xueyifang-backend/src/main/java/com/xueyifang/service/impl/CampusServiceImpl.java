package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.dto.admin.CampusSaveRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.vo.CampusVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.CampusMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.utils.GeofenceUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CampusServiceImpl extends ServiceImpl<CampusMapper, Campus> implements ICampusService {

    @Override
    public List<CampusVO> listActiveCampuses() {
        List<Campus> list = this.list(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getStatus, 1)
                .orderByAsc(Campus::getId));
        return toVOList(list);
    }

    @Override
    public List<CampusVO> listAllForAdmin() {
        List<Campus> list = this.list(new LambdaQueryWrapper<Campus>().orderByAsc(Campus::getId));
        return toVOList(list);
    }

    @Override
    public Long saveOrUpdateCampus(CampusSaveRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getCampusCode(), request.getCampusName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "校区编码与名称不能为空");
        }
        if (request.getCenterLat() == null || request.getCenterLng() == null
                || !GeofenceUtils.isValidCoordinate(request.getCenterLat().doubleValue(), request.getCenterLng().doubleValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写合法的校区中心经纬度");
        }
        Campus campus = request.getId() != null ? this.getById(request.getId()) : new Campus();
        if (campus == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "校区不存在");
        }
        BeanUtils.copyProperties(request, campus);
        if (campus.getSafeRadiusMeters() == null || campus.getSafeRadiusMeters() <= 0) {
            campus.setSafeRadiusMeters(1800);
        }
        if (StringUtils.isBlank(campus.getEduEmailDomains())) {
            campus.setEduEmailDomains("edu.cn");
        }
        if (campus.getStatus() == null) {
            campus.setStatus(1);
        }
        this.saveOrUpdate(campus);
        return campus.getId();
    }

    @Override
    public void validateCoordinateInCampusFence(Long campusId, Double lat, Double lng) {
        if (!GeofenceUtils.isValidCoordinate(lat, lng)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地图选点经纬度无效");
        }
        Long targetCampusId = campusId != null ? campusId : 1L;
        Campus campus = this.getById(targetCampusId);
        if (campus == null) {
            return;
        }
        Double centerLat = campus.getCenterLat() != null ? campus.getCenterLat().doubleValue() : null;
        Double centerLng = campus.getCenterLng() != null ? campus.getCenterLng().doubleValue() : null;
        boolean inside = GeofenceUtils.isInsideCampusFence(
                lat, lng,
                campus.getGeofencePolygon(),
                centerLat, centerLng,
                campus.getSafeRadiusMeters()
        );
        if (!inside) {
            throw new BusinessException(ErrorCode.GEOFENCE_OUT_OF_BOUNDS,
                    "所选会面点超出「" + campus.getCampusName() + "」校园安全电子围栏，请重新在校内区域选点");
        }
    }

    @Override
    public boolean isAllowedEduEmail(Long campusId, String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return false;
        }
        String lowerDomain = email.substring(email.indexOf('@') + 1).trim().toLowerCase();
        String configuredDomains = "edu.cn";
        if (campusId != null) {
            Campus campus = this.getById(campusId);
            if (campus != null && StringUtils.isNotBlank(campus.getEduEmailDomains())) {
                configuredDomains = campus.getEduEmailDomains();
            }
        }
        String[] parts = configuredDomains.split(",");
        for (String part : parts) {
            String suffix = part.trim().toLowerCase();
            if (StringUtils.isNotBlank(suffix) && (lowerDomain.equals(suffix) || lowerDomain.endsWith("." + suffix))) {
                return true;
            }
        }
        return false;
    }

    private List<CampusVO> toVOList(List<Campus> list) {
        List<CampusVO> result = new ArrayList<>();
        if (list == null) {
            return result;
        }
        for (Campus c : list) {
            CampusVO vo = new CampusVO();
            BeanUtils.copyProperties(c, vo);
            result.add(vo);
        }
        return result;
    }
}
