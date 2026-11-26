package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.dto.OrderPayRequest;
import com.xueyifang.domain.dto.SellerShipRequest;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.vo.OrderDetailVO;
import com.xueyifang.domain.vo.OrderListItemVO;
import com.xueyifang.domain.vo.OrderVerifyCredentialVO;
import com.xueyifang.domain.vo.SellerOrderListItemVO;

import java.math.BigDecimal;

public interface IServiceOrderService extends IService<ServiceOrder> {

    Long createOrder(OrderCreateRequest request);

    void payOrder(Long orderId, OrderPayRequest request);

    void markOrderPaidByEscrowGateway(Long orderId, Long buyerId, String payChannel, String paymentNo);

    Page<OrderListItemVO> getMyOrders(Integer pageNum, Integer pageSize, Integer orderStatus);

    Page<SellerOrderListItemVO> getMySellingOrders(Integer pageNum, Integer pageSize, Integer orderStatus);

    OrderDetailVO getOrderDetail(Long orderId);

    void sellerShip(Long orderId);

    void sellerShip(Long orderId, SellerShipRequest request);

    void verifyOfflineCode(Long orderId, String verifyCode);

    OrderVerifyCredentialVO getOfflineVerifyCredential(Long orderId);

    Long verifyOfflineQr(String qrToken, Long expectedOrderId);

    void completeOfflineOrderByBle(Long orderId, Integer rssiDbm, BigDecimal distanceMeters);

    void confirmOrder(Long orderId);

    void cancelOrder(Long orderId);

    void requestRefund(Long orderId, String reason);

    void sellerHandleRefund(Long orderId, boolean approve, String rejectReason);

    void adminRefundOrder(Long orderId, String reason);

    void autoCancelUnpaidOrders();

    void autoConfirmReceipt();

    void autoRefundTimeout();

    @Deprecated
    default void sellerConfirmOrder(Long orderId) {
        sellerShip(orderId);
    }

    @Deprecated
    default void refundOrder(Long orderId, String reason) {
        requestRefund(orderId, reason);
    }

    @Deprecated
    default void autoReleaseFrozenOrders() {
        autoConfirmReceipt();
    }
}
