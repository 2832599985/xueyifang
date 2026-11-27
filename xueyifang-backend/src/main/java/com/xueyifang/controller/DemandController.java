package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.DemandBidCreateRequest;
import com.xueyifang.domain.dto.DemandCreateRequest;
import com.xueyifang.domain.dto.DemandQueryRequest;
import com.xueyifang.domain.vo.DemandBidVO;
import com.xueyifang.domain.vo.DemandRequestVO;
import com.xueyifang.service.IDemandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demand")
@Api(tags = "校内反向悬赏大厅与创作者竞标接口")
public class DemandController {

    @Resource
    private IDemandService demandService;

    @GetMapping("/list")
    @ApiOperation("分页查询校内悬赏需求大厅")
    public BaseResponse<Page<DemandRequestVO>> listDemands(DemandQueryRequest query) {
        return ResultUtils.success(demandService.pageDemands(query));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("查看悬赏需求详情与实时竞标方案列表")
    public BaseResponse<DemandRequestVO> getDemandDetail(@PathVariable("id") Long id) {
        return ResultUtils.success(demandService.getDemandDetail(id));
    }

    @PostMapping("/publish")
    @ApiOperation("发布校内反向悬赏需求（含真实地图坐标与米级半径校验）")
    public BaseResponse<Long> publishDemand(@RequestBody DemandCreateRequest request) {
        return ResultUtils.success(demandService.publishDemand(request));
    }

    @PostMapping("/{id}/bid")
    @ApiOperation("认证创作者对悬赏需求提交报价与履约方案")
    public BaseResponse<Long> submitBid(
            @PathVariable("id") Long id,
            @RequestBody DemandBidCreateRequest request) {
        return ResultUtils.success(demandService.submitBid(id, request));
    }

    @PostMapping("/{id}/accept/{bidId}")
    @ApiOperation("发布者采纳竞标报价并原子生成担保订单")
    public BaseResponse<Long> acceptBid(
            @PathVariable("id") Long id,
            @PathVariable("bidId") Long bidId) {
        return ResultUtils.success(demandService.acceptBid(id, bidId));
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("发布者取消尚未选标的悬赏需求")
    public BaseResponse<Boolean> cancelDemand(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        demandService.cancelDemand(id, reason);
        return ResultUtils.success(true);
    }

    @GetMapping("/my-published")
    @ApiOperation("查询我发布的悬赏需求列表")
    public BaseResponse<List<DemandRequestVO>> listMyPublished() {
        return ResultUtils.success(demandService.listMyPublishedDemands());
    }

    @GetMapping("/my-bids")
    @ApiOperation("查询我参与竞标的报价记录")
    public BaseResponse<List<DemandBidVO>> listMyBids() {
        return ResultUtils.success(demandService.listMyBids());
    }
}
