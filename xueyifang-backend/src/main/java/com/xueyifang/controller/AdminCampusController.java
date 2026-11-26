package com.xueyifang.controller;

import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.admin.CampusSaveRequest;
import com.xueyifang.domain.vo.CampusVO;
import com.xueyifang.mapper.PlatformRevenueLedgerMapper;
import com.xueyifang.service.ICampusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/campus")
@Api(tags = "管理端-多校区电子围栏与商业分账看板")
public class AdminCampusController {

    @Resource
    private ICampusService campusService;

    @Resource
    private PlatformRevenueLedgerMapper revenueLedgerMapper;

    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("查询全部校区与电子围栏配置")
    public BaseResponse<List<CampusVO>> listAll() {
        return ResultUtils.success(campusService.listAllForAdmin());
    }

    @PostMapping("/save")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("新增或更新校区多边形电子围栏与教育邮箱后缀")
    public BaseResponse<Long> saveCampus(@RequestBody CampusSaveRequest request) {
        return ResultUtils.success(campusService.saveOrUpdateCampus(request));
    }

    @GetMapping("/revenue-summary")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("查询平台累计技术服务费（抽佣）分账收入")
    public BaseResponse<Map<String, Object>> getRevenueSummary() {
        BigDecimal totalCommission = revenueLedgerMapper != null
                ? revenueLedgerMapper.sumTotalPlatformRevenue()
                : BigDecimal.ZERO;
        long settledCount = revenueLedgerMapper != null ? revenueLedgerMapper.selectCount(null) : 0L;
        Map<String, Object> data = new HashMap<>();
        data.put("totalCommissionRevenue", totalCommission);
        data.put("settledOrderCount", settledCount);
        return ResultUtils.success(data);
    }
}
