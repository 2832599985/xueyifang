package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.service.IFinancialReconciliationService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/order/noshow")
@Api(tags = "用户端-预约订单逾期爽约自动赔付申领")
public class OrderNoShowController {

    @Resource
    private IFinancialReconciliationService financialReconciliationService;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @GetMapping("/{orderId}")
    @ApiOperation("查询订单关联的预约爽约自动赔付存证记录")
    public BaseResponse<AppointmentNoshowClaim> getOrderNoShowClaim(@PathVariable("orderId") Long orderId) {
        User caller = UserContext.getCurrentUser();
        if (caller == null || caller.getId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        boolean isAdmin = Integer.valueOf(UserConstant.ROLE_ADMIN).equals(caller.getRole());
        if (!isAdmin && !caller.getId().equals(order.getBuyerId()) && !caller.getId().equals(order.getSellerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该订单爽约赔付存证");
        }
        return ResultUtils.success(financialReconciliationService.getClaimByOrderId(orderId));
    }

    @PostMapping("/{orderId}/claim")
    @ApiOperation("买方针对已过预约结束时间仍未履约的订单一键申领爽约退本金+保证金违约金自动赔付")
    public BaseResponse<AppointmentNoshowClaim> claimSellerNoShow(
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = UserContext.getCurrentUserId();
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!userId.equals(order.getBuyerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅该订单买方可发起预约爽约自动赔付申领");
        }
        if (order.getScheduledEnd() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该订单未约定履约截止时间，不适用预约爽约自动赔付");
        }
        if (order.getScheduledEnd().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前预约时段尚未结束，请在预约结束时间逾期后再申请爽约自动赔付");
        }
        if (!Integer.valueOf(com.xueyifang.constant.OrderConstant.ORDER_PENDING_SHIP).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "卖方已交付或订单状态变更，不支持预约爽约自动赔付，如有异议请发起纠纷仲裁");
        }
        String note = body != null ? body.get("evidenceNote") : "买方申报卖方超出预约结束时间未到场/未交付，触发自动爽约赔付";
        return ResultUtils.success(financialReconciliationService.processSellerNoShowCompensation(userId, orderId, note));
    }
}
