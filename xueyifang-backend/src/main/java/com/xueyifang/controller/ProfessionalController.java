package com.xueyifang.controller;


import cn.hutool.core.bean.BeanUtil;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.vo.ProfessionalVO;
import com.xueyifang.service.IProfessionalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 专业表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-10-28
 */
@RestController
@RequestMapping("/professional")
@Slf4j
@Api(tags = "专业管理模块")
public class ProfessionalController {

    @Resource
    private IProfessionalService professionalService;

    /**
     * 查询所有专业
     *
     * @return 专业VO列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询所有专业", notes = "获取所有未删除的专业列表")
    public BaseResponse<List<ProfessionalVO>> listAllProfessionals() {
        log.info("查询所有专业列表");

        List<ProfessionalVO> professionalVOList = professionalService.listAllProfessionals();

        return ResultUtils.success(professionalVOList);
    }

    /**
     * 根据ID查询专业详情
     *
     * @param id 专业ID
     * @return 专业VO信息
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询单个专业", notes = "根据专业ID查询专业详细信息")
    public BaseResponse<ProfessionalVO> getProfessionalById(@PathVariable("id") Long id) {
        log.info("查询专业详情，id：{}", id);

        Professional professional = professionalService.getProfessionalById(id);

        // 转换为 VO

        return ResultUtils.success(
                BeanUtil.copyProperties(professional, ProfessionalVO.class
                ));
    }

    /**
     * 添加专业
     *
     * @param professional 专业信息
     * @return 是否成功
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加专业", notes = "添加新的专业信息")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> addProfessional(@RequestBody Professional professional) {
        log.info("添加专业，专业名称：{}", professional.getProfessionalName());

        professionalService.addProfessional(professional);

        return ResultUtils.success("添加专业成功");
    }

    /**
     * 更新专业信息
     *
     * @param professional 专业信息
     * @return 是否成功
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新专业", notes = "根据专业ID更新专业信息")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> updateProfessional(@RequestBody Professional professional) {
        log.info("更新专业，id：{}", professional.getId());

        professionalService.updateProfessional(professional);

        return ResultUtils.success("更新专业成功");
    }

    /**
     * 删除专业（逻辑删除）
     *
     * @param id 专业ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除专业", notes = "根据专业ID逻辑删除专业")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    public BaseResponse<Void> deleteProfessional(@PathVariable("id") Long id) {
        log.info("删除专业，id：{}", id);

        professionalService.deleteProfessional(id);

        return ResultUtils.success("删除专业成功");
    }

}
