package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.vo.ProfessionalVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ProfessionalMapper;
import com.xueyifang.service.IProfessionalService;
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
 * 专业表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
@Service
@Slf4j
public class ProfessionalServiceImpl extends ServiceImpl<ProfessionalMapper, Professional> implements IProfessionalService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<ProfessionalVO> listAllProfessionals() {
        // 1. 尝试从缓存获取
        List<ProfessionalVO> cachedList = redisUtils.getJsonList(CacheConstant.DICT_PROFESSIONALS, ProfessionalVO.class);
        if (cachedList != null) {
            log.debug("专业列表缓存命中，数量: {}", cachedList.size());
            return cachedList;
        }

        // 2. 缓存未命中，查询数据库
        log.info("查询所有专业列表");
        QueryWrapper<Professional> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");

        List<Professional> professionalList = this.list(queryWrapper);
        log.info("查询到 {} 个专业", professionalList.size());

        // 3. 转换为VO列表
        List<ProfessionalVO> professionalVOList = professionalList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 写入缓存
        redisUtils.setJson(CacheConstant.DICT_PROFESSIONALS, professionalVOList, CacheConstant.DICT_TTL_24H);
        log.debug("专业列表写入缓存，数量: {}", professionalVOList.size());

        return professionalVOList;
    }

    /**
     * 删除专业列表缓存
     */
    private void deleteProfessionalCache() {
        redisUtils.delete(CacheConstant.DICT_PROFESSIONALS);
        log.info("专业列表缓存已删除");
    }

    /**
     * 将 Professional 实体转换为 ProfessionalVO
     *
     * @param professional 专业实体
     * @return 专业VO
     */
    private ProfessionalVO convertToVO(Professional professional) {
        ProfessionalVO vo = new ProfessionalVO();
        BeanUtils.copyProperties(professional, vo);
        return vo;
    }

    @Override
    public Professional getProfessionalById(Long id) {
        log.info("查询专业详情，id：{}", id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID不能为空");
        }

        // MyBatis-Plus 会自动过滤已删除的数据（通过 @TableLogic 注解）
        Professional professional = this.getById(id);
        if (professional == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专业不存在或已被删除");
        }

        log.info("查询专业成功，专业名称：{}", professional.getProfessionalName());
        return professional;
    }

    @Override
    public boolean addProfessional(Professional professional) {
        log.info("添加专业，专业名称：{}", professional.getProfessionalName());

        // 参数校验
        validateProfessional(professional, false);

        // 检查专业名称是否已存在（MyBatis-Plus 会自动过滤已删除的数据）
        QueryWrapper<Professional> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("professional_name", professional.getProfessionalName());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业名称已存在");
        }

        // 保存专业（不需要手动设置 isDelete，MyBatis-Plus 会自动处理）
        boolean result = this.save(professional);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加专业失败");
        }

        // 删除缓存
        deleteProfessionalCache();

        log.info("添加专业成功，id：{}", professional.getId());
        return true;
    }

    @Override
    public boolean updateProfessional(Professional professional) {
        log.info("更新专业，id：{}", professional.getId());

        // 参数校验
        validateProfessional(professional, true);

        // 检查专业是否存在（MyBatis-Plus 会自动过滤已删除的数据）
        Professional existProfessional = this.getById(professional.getId());
        if (existProfessional == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专业不存在或已被删除");
        }

        // 检查专业名称是否与其他专业重复（MyBatis-Plus 会自动过滤已删除的数据）
        if (StringUtils.isNotBlank(professional.getProfessionalName())) {
            QueryWrapper<Professional> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("professional_name", professional.getProfessionalName());
            queryWrapper.ne("id", professional.getId());
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业名称已存在");
            }
        }

        // 更新专业
        boolean result = this.updateById(professional);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新专业失败");
        }

        // 删除缓存
        deleteProfessionalCache();

        log.info("更新专业成功，专业名称：{}", professional.getProfessionalName());
        return true;
    }

    @Override
    public boolean deleteProfessional(Long id) {
        log.info("删除专业，id：{}", id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID不能为空");
        }

        // 检查专业是否存在（MyBatis-Plus 会自动过滤已删除的数据）
        Professional professional = this.getById(id);
        if (professional == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专业不存在或已被删除");
        }

        // 使用 MyBatis-Plus 的 removeById 方法进行逻辑删除
        // MyBatis-Plus 会自动将 isDelete 字段设置为 1
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除专业失败");
        }

        // 删除缓存
        deleteProfessionalCache();

        log.info("删除专业成功，专业名称：{}", professional.getProfessionalName());
        return true;
    }

    @Override
    public Page<Professional> pageProfessionals(Long current, Long pageSize, String nameLike) {
        log.info("分页查询专业，当前页：{}，每页大小：{}，名称模糊：{}", current, pageSize, nameLike);

        if (current == null || current < 1) {
            current = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        Page<Professional> page = new Page<>(current, pageSize);
        QueryWrapper<Professional> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(nameLike)) {
            queryWrapper.like("professional_name", nameLike.trim());
        }
        queryWrapper.orderByDesc("create_time");

        return this.page(page, queryWrapper);
    }

    /**
     * 校验专业参数
     *
     * @param professional 专业信息
     * @param isUpdate 是否为更新操作
     */
    private void validateProfessional(Professional professional, boolean isUpdate) {
        if (professional == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业信息不能为空");
        }

        // 更新操作需要校验ID
        if (isUpdate) {
            if (professional.getId() == null || professional.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID不能为空");
            }
        }

        // 校验专业名称
        String professionalName = professional.getProfessionalName();
        if (StringUtils.isBlank(professionalName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业名称不能为空");
        }
        if (professionalName.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业名称长度不能超过50个字符");
        }

        // 校验专业描述（可选）
        String description = professional.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业描述长度不能超过500个字符");
        }
    }

}
