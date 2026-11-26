package com.xueyifang.service;

import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.vo.ProfessionalVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * <p>
 * 专业表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
public interface IProfessionalService extends IService<Professional> {

    /**
     * 查询所有专业（未删除的）
     *
     * @return 专业VO列表
     */
    List<ProfessionalVO> listAllProfessionals();

    /**
     * 根据ID查询专业详情
     *
     * @param id 专业ID
     * @return 专业信息
     */
    Professional getProfessionalById(Long id);

    /**
     * 添加专业
     *
     * @param professional 专业信息
     * @return 是否成功
     */
    boolean addProfessional(Professional professional);

    /**
     * 更新专业信息
     *
     * @param professional 专业信息
     * @return 是否成功
     */
    boolean updateProfessional(Professional professional);

    /**
     * 删除专业（逻辑删除）
     *
     * @param id 专业ID
     * @return 是否成功
     */
    boolean deleteProfessional(Long id);

    /**
     * 分页查询专业列表
     *
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param nameLike 专业名称模糊匹配
     * @return 分页结果
     */
    Page<Professional> pageProfessionals(Long current, Long pageSize, String nameLike);

}
