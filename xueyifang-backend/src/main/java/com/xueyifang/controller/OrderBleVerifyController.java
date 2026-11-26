package com.xueyifang.controller;

import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.annotation.RateLimit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.BleVerifySubmitRequest;
import com.xueyifang.domain.vo.BleSessionVO;
import com.xueyifang.service.IBleVerifyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/order")
@Api(tags = "低功耗蓝牙(BLE)近场加密碰面核销模块")
public class OrderBleVerifyController {

    @Resource
    private IBleVerifyService bleVerifyService;

    @PostMapping("/{orderId}/ble/session")
    @RateLimit(maxRequests = 15, windowSeconds = 60, limitType = RateLimit.LimitType.USER)
    @ApiOperation("开启或加入订单低功耗蓝牙近场挑战握手会话，下发角色临时签名密钥")
    public BaseResponse<BleSessionVO> openBleSession(@PathVariable Long orderId) {
        return ResultUtils.success(bleVerifyService.initOrJoinBleSession(orderId));
    }

    @PostMapping("/{orderId}/ble/verify")
    @IdempotentSubmit(lockTime = 5, useFirstParam = true, message = "蓝牙核销结算中，请勿重复提交")
    @ApiOperation("提交双端蓝牙近场挑战签名与定位数据，完成防伪造线下核销结单")
    public BaseResponse<Void> verifyBleMeetup(
            @PathVariable Long orderId,
            @RequestBody BleVerifySubmitRequest request) {
        bleVerifyService.submitBleHandshakeAndSettle(orderId, request);
        return ResultUtils.success();
    }
}
