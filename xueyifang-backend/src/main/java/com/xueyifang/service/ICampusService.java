package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.admin.CampusSaveRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.vo.CampusVO;

import java.util.List;

public interface ICampusService extends IService<Campus> {

    List<CampusVO> listActiveCampuses();

    List<CampusVO> listAllForAdmin();

    Long saveOrUpdateCampus(CampusSaveRequest request);

    void validateCoordinateInCampusFence(Long campusId, Double lat, Double lng);

    boolean isAllowedEduEmail(Long campusId, String email);
}
