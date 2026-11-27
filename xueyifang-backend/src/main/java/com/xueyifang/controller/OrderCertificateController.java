package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.vo.OrderDeliveryCertificateVO;
import com.xueyifang.service.ISystemHealthAuditService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/order/certificate")
@Api(tags = "用户端-校园技能交付与社会实践 SHA-256 数字存证证书")
public class OrderCertificateController {

    @Resource
    private ISystemHealthAuditService systemHealthAuditService;

    @GetMapping("/{orderId}")
    @ApiOperation("签发或调阅已完结订单的 SHA-256 数字履约与校园技能实践存证证书")
    public BaseResponse<OrderDeliveryCertificateVO> getCertificate(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getCurrentUserId();
        return ResultUtils.success(systemHealthAuditService.generateOrderDeliveryCertificate(userId, orderId));
    }

    @GetMapping("/{orderId}/verify")
    @ApiOperation("校验数字履约存证证书的 SHA-256 防伪指纹真伪")
    public BaseResponse<Map<String, Object>> verifySeal(
            @PathVariable("orderId") Long orderId,
            @RequestParam("sealHash") String sealHash) {
        return ResultUtils.success(systemHealthAuditService.verifyCertificateSeal(orderId, sealHash));
    }
}
