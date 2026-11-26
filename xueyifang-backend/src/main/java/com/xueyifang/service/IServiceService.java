package com.xueyifang.service;

import com.xueyifang.domain.po.Service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xueyifang.domain.dto.ServicePublishRequest;
import com.xueyifang.domain.dto.ServiceUpdateRequest;
import com.xueyifang.domain.vo.ServiceListItemVO;
import com.xueyifang.domain.vo.ServiceDetailVO;

/**
 * <p>
 * 服务表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
public interface IServiceService extends IService<Service> {

    Long publishService(Long userId, ServicePublishRequest request);

    IPage<ServiceListItemVO> searchServices(String keyword,
                                            Long tagId,
                                            Integer tradeType,
                                            Long sellerId,
                                            String sortBy,
                                            long pageNo,
                                            long pageSize);

    ServiceDetailVO getServiceDetail(Long serviceId);

    IPage<ServiceListItemVO> listMyServices(Long userId, Integer status, long pageNo, long pageSize);

    /**
     * 编辑服务
     * @param userId 当前用户ID
     * @param serviceId 服务ID
     * @param request 更新请求
     */
    void updateService(Long userId, Long serviceId, ServiceUpdateRequest request);

    /**
     * 下架服务
     * @param userId 当前用户ID
     * @param serviceId 服务ID
     */
    void offlineService(Long userId, Long serviceId);

    /**
     * 上架/提交审核
     * @param userId 当前用户ID
     * @param serviceId 服务ID
     */
    void onlineService(Long userId, Long serviceId);

    /**
     * 删除服务（逻辑删除）
     * @param userId 当前用户ID
     * @param serviceId 服务ID
     */
    void deleteService(Long userId, Long serviceId);
}
