package com.xueyifang.controller;

import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.wallet.EscrowPrepayRequest;
import com.xueyifang.domain.dto.wallet.EscrowWebhookRequest;
import com.xueyifang.domain.vo.EscrowPrepayVO;
import com.xueyifang.service.IEscrowPaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/payment/escrow")
@Api(tags = "商业化担保支付收银台与回调网关")
public class EscrowPaymentController {

    @Resource
    private IEscrowPaymentService escrowPaymentService;

    @PostMapping("/prepay")
    @IdempotentSubmit(lockTime = 3, message = "正在拉起担保支付收银台，请勿重复点击")
    @ApiOperation("创建微信小程序/App/支付宝单笔订单担保预支付单")
    public BaseResponse<EscrowPrepayVO> prepay(@RequestBody EscrowPrepayRequest request) {
        return ResultUtils.success(escrowPaymentService.createEscrowPrepay(request));
    }

    @PostMapping("/webhook/callback")
    @ApiOperation("第三方支付网关异步签名回调通知接口（公开白名单，内部严格验签+行锁幂等）")
    public BaseResponse<Void> handleWebhook(@RequestBody EscrowWebhookRequest webhook) {
        escrowPaymentService.handleVerifiedWebhook(webhook);
        return ResultUtils.success();
    }
}
