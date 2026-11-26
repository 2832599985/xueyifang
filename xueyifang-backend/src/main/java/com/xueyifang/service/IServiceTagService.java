package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.po.ServiceTag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.ServiceTagVO;

import java.util.List;

/**
 * <p>
 * 服务标签表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
public interface IServiceTagService extends IService<ServiceTag> {

    /**
     * 获取所有服务标签列表（未删除的）
     *
     * @return 服务标签VO列表
     */
    List<ServiceTagVO> listAllServiceTags();

    /**
     * 分页查询服务标签
     */
    Page<ServiceTag> pageServiceTags(Long pageNum, Long pageSize, String nameLike);

    /**
     * 新增服务标签
     */
    boolean addServiceTag(ServiceTag serviceTag);

    /**
     * 更新服务标签
     */
    boolean updateServiceTag(ServiceTag serviceTag);

    /**
     * 删除服务标签（逻辑删除）
     */
    boolean deleteServiceTag(Long id);
}
