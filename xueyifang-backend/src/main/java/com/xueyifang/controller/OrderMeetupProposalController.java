package com.xueyifang.controller;

import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.MeetupProposalCreateRequest;
import com.xueyifang.domain.dto.MeetupProposalRespondRequest;
import com.xueyifang.domain.vo.MeetupProposalVO;
import com.xueyifang.service.IOrderMeetupProposalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/order")
@Api(tags = "订单双向地图选点协商模块")
public class OrderMeetupProposalController {

    @Resource
    private IOrderMeetupProposalService meetupProposalService;

    @PostMapping("/{orderId}/meetup/propose")
    @IdempotentSubmit(lockTime = 3, useFirstParam = true, message = "会面提案发送中，请勿重复点击")
    @ApiOperation("买卖任意一方在地图上选点并发起会面位置/时间提案")
    public BaseResponse<Long> proposeMeetup(
            @PathVariable Long orderId,
            @RequestBody MeetupProposalCreateRequest request) {
        return ResultUtils.success(meetupProposalService.proposeMeetupLocation(orderId, request));
    }

    @PostMapping("/meetup/{proposalId}/respond")
    @IdempotentSubmit(lockTime = 3, useFirstParam = true, message = "正在处理确认，请勿重复提交")
    @ApiOperation("交易另一方同意锁定或婉拒会面位置提案")
    public BaseResponse<Void> respondMeetup(
            @PathVariable Long proposalId,
            @RequestBody MeetupProposalRespondRequest request) {
        meetupProposalService.respondMeetupProposal(proposalId, request);
        return ResultUtils.success();
    }

    @GetMapping("/{orderId}/meetup/proposals")
    @ApiOperation("查询订单历史会面位置协商记录")
    public BaseResponse<List<MeetupProposalVO>> listProposals(@PathVariable Long orderId) {
        return ResultUtils.success(meetupProposalService.listOrderProposals(orderId));
    }
}
