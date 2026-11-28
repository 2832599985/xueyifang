package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.dto.OrderPayRequest;
import com.xueyifang.domain.dto.OrderRefundRequest;
import com.xueyifang.domain.dto.OrderVerifyCodeRequest;
import com.xueyifang.domain.dto.OrderVerifyQrRequest;
import com.xueyifang.domain.dto.SellerHandleRefundRequest;
import com.xueyifang.domain.dto.SellerShipRequest;
import com.xueyifang.domain.vo.OrderDetailVO;
import com.xueyifang.domain.vo.OrderListItemVO;
import com.xueyifang.domain.vo.OrderVerifyCredentialVO;
import com.xueyifang.domain.vo.SellerOrderListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 服务订单表 前端控制器
 *
 * 交易流程 v2.0:
 * 1. 买家下单 → 待支付(1)
 * 2. 买家支付 → 待发货(2)
 * 3. 卖家发货 → 待收货(3)
 * 4. 买家确认收货 → 已完成(4)
 *
 * @author luoyi
 * @since 2025-11-19
 */
@RestController
@RequestMapping("/order")
@Api(tags = "订单管理")
public class ServiceOrderController {

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private IServiceService serviceService;

    // ==================== 订单基础操作 ====================

    @PostMapping("/create")
    @ApiOperation("创建订单")
    public BaseResponse<Long> createOrder(@RequestBody OrderCreateRequest request) {
        if (request == null || request.getServiceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        com.xueyifang.domain.po.Service svc = serviceService.getById(request.getServiceId());
        if (svc != null && svc.getServiceTitle() != null
                && (svc.getServiceTitle().startsWith("【悬赏定向单】") || svc.getServiceTitle().startsWith("【拼班团课】"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该服务为悬赏定向单或拼班团课专用模板，请通过对应业务入口下单");
        }
        Long orderId = serviceOrderService.createOrder(request);
        return ResultUtils.success(orderId);
    }

    @PostMapping("/{orderId}/pay")
    @ApiOperation("支付订单")
    @IdempotentSubmit(lockTime = 5, useFirstParam = true, message = "支付处理中，请勿重复提交")
    public BaseResponse<Void> payOrder(@PathVariable Long orderId, @RequestBody OrderPayRequest request) {
        if (request == null || orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.payOrder(orderId, request);
        return ResultUtils.success();
    }

    @PostMapping("/{orderId}/cancel")
    @ApiOperation("取消订单（仅待支付状态）")
    public BaseResponse<Void> cancelOrder(@PathVariable Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.cancelOrder(orderId);
        return ResultUtils.success();
    }

    // ==================== 交易流程操作 ====================

    @PostMapping("/{orderId}/ship")
    @ApiOperation("卖家发货（可选携带交付说明与凭证截图）")
    public BaseResponse<Void> sellerShip(
            @PathVariable Long orderId,
            @RequestBody(required = false) SellerShipRequest request) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.sellerShip(orderId, request);
        return ResultUtils.success();
    }

    @GetMapping("/{orderId}/verify-credential")
    @ApiOperation("买方获取线下订单实时动态核销凭证（30s动态码 + 10s防截图动态二维码）")
    public BaseResponse<OrderVerifyCredentialVO> getOfflineVerifyCredential(@PathVariable Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(serviceOrderService.getOfflineVerifyCredential(orderId));
    }

    @PostMapping("/{orderId}/verify-code")
    @ApiOperation("校内线下交易当面核销码结单（卖家输入买家30秒动态6位核销码）")
    public BaseResponse<Void> verifyOfflineCode(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderVerifyCodeRequest request) {
        if (orderId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.verifyOfflineCode(orderId, request.getVerifyCode(), request.getVerifierLat(), request.getVerifierLng());
        return ResultUtils.success();
    }

    @PostMapping("/verify-qr")
    @ApiOperation("校内线下交易实时摄像头动态二维码核销结单（10秒有效期，防截图）")
    public BaseResponse<Long> verifyOfflineQr(@Valid @RequestBody OrderVerifyQrRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long verifiedOrderId = serviceOrderService.verifyOfflineQr(request.getQrToken(), request.getExpectedOrderId(), request.getVerifierLat(), request.getVerifierLng());
        return ResultUtils.success(verifiedOrderId);
    }

    @PostMapping("/{orderId}/confirm")
    @ApiOperation("买家确认收货")
    public BaseResponse<Void> confirmOrder(@PathVariable Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.confirmOrder(orderId);
        return ResultUtils.success();
    }

    // ==================== 退款相关操作 ====================

    @PostMapping("/{orderId}/refund")
    @ApiOperation(value = "买家申请退款", notes = "待发货状态直接退款，待收货状态需等待卖家处理")
    public BaseResponse<Void> requestRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRefundRequest request) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.requestRefund(orderId, request.getReason());
        return ResultUtils.success();
    }

    @PostMapping("/{orderId}/handleRefund")
    @ApiOperation(value = "卖家处理退款申请", notes = "同意或拒绝买家的退款申请")
    public BaseResponse<Void> sellerHandleRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody SellerHandleRefundRequest request) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.sellerHandleRefund(orderId, request.getApprove(), request.getRejectReason());
        return ResultUtils.success();
    }

    // ==================== 订单查询 ====================

    @GetMapping("/myOrders")
    @ApiOperation("获取我的订单列表（买家视角）")
    public BaseResponse<Page<OrderListItemVO>> getMyOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(value = "订单状态：1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败")
            @RequestParam(required = false) Integer orderStatus) {
        Page<OrderListItemVO> result = serviceOrderService.getMyOrders(pageNum, pageSize, orderStatus);
        return ResultUtils.success(result);
    }

    @GetMapping("/mySellingOrders")
    @ApiOperation("获取我的售出订单列表（卖家视角）")
    public BaseResponse<Page<SellerOrderListItemVO>> getMySellingOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(value = "订单状态：1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败")
            @RequestParam(required = false) Integer orderStatus) {
        Page<SellerOrderListItemVO> result = serviceOrderService.getMySellingOrders(pageNum, pageSize, orderStatus);
        return ResultUtils.success(result);
    }

    @GetMapping("/{orderId}")
    @ApiOperation("获取订单详情")
    public BaseResponse<OrderDetailVO> getOrderDetail(@PathVariable Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        OrderDetailVO result = serviceOrderService.getOrderDetail(orderId);
        return ResultUtils.success(result);
    }

    // ==================== 兼容旧接口（已废弃）====================

    @Deprecated
    @PostMapping("/{orderId}/sellerConfirm")
    @ApiOperation(value = "卖家确认订单（已废弃）", notes = "请使用 /ship 接口代替")
    public BaseResponse<Void> sellerConfirmOrder(@PathVariable Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        serviceOrderService.sellerShip(orderId);
        return ResultUtils.success();
    }
}
