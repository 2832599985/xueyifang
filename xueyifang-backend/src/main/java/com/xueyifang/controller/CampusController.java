package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.vo.CampusVO;
import com.xueyifang.service.ICampusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/campus")
@Api(tags = "高校校区与电子围栏模块")
public class CampusController {

    @Resource
    private ICampusService campusService;

    @GetMapping("/list")
    @ApiOperation("获取已开放的高校校区列表及电子围栏配置（公开）")
    public BaseResponse<List<CampusVO>> listCampuses() {
        return ResultUtils.success(campusService.listActiveCampuses());
    }

    @GetMapping("/check-fence")
    @ApiOperation("实时校验地图选点是否处于指定校区安全电子围栏内（公开）")
    public BaseResponse<Map<String, Object>> checkFence(
            @RequestParam(defaultValue = "1") Long campusId,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        Map<String, Object> res = new HashMap<>();
        try {
            campusService.validateCoordinateInCampusFence(campusId, lat, lng);
            res.put("inside", true);
            res.put("message", "位于校园安全电子围栏范围内");
        } catch (Exception e) {
            res.put("inside", false);
            res.put("message", e.getMessage());
        }
        return ResultUtils.success(res);
    }
}
