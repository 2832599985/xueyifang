package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.dto.OrderPayRequest;
import com.xueyifang.domain.dto.SellerShipRequest;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.vo.OrderDetailVO;
import com.xueyifang.domain.vo.OrderListItemVO;
import com.xueyifang.domain.vo.OrderVerifyCredentialVO;
import com.xueyifang.domain.vo.SellerOrderListItemVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * <p>
 * 服务订单表 服务类
 * </p>
 *
 * 交易流程 v2.0:
 * 1. 买家下单 → 待支付(1)
 * 2. 买家支付 → 待发货(2)，资金冻结
 * 3. 卖家发货 → 待收货(3)
 * 4. 买家确认收货 → 已完成(4)，资金释放给卖家
 *
 * @author luoyi
 * @since 2025-11-19
 */
public interface IServiceOrderService extends IService<ServiceOrder> {

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单ID
     */
    Long createOrder(OrderCreateRequest request);

    /**
     * 支付订单
     * 支付后订单状态变为待发货(2)，资金冻结
     *
     * @param orderId 订单ID
     * @param request 支付请求
     */
    void payOrder(Long orderId, OrderPayRequest request);

    /**
     * 获取我的订单列表（买家视角）
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param orderStatus 订单状态
     * @return 订单列表
     */
    Page<OrderListItemVO> getMyOrders(Integer pageNum, Integer pageSize, Integer orderStatus);

    /**
     * 获取我的售出订单列表（卖家视角）
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param orderStatus 订单状态
     * @return 卖家订单列表
     */
    Page<SellerOrderListItemVO> getMySellingOrders(Integer pageNum, Integer pageSize, Integer orderStatus);

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailVO getOrderDetail(Long orderId);

    // ==================== 交易流程操作 ====================

    /**
     * 卖家发货
     * 订单状态: 待发货(2) → 待收货(3)
     *
     * @param orderId 订单ID
     */
    void sellerShip(Long orderId);

    /**
     * 卖家发货（支持提交交付说明与成果凭证图片）
     *
     * @param orderId 订单ID
     * @param request 交付凭证参数（可为 null）
     */
    void sellerShip(Long orderId, SellerShipRequest request);

    /**
     * 校园线下交易当面核销码结单（卖家输入买家出示的30秒动态6位核销码，立即完成交接并结算担保资金）
     *
     * @param orderId    订单ID
     * @param verifyCode 6位动态数字核销码
     */
    void verifyOfflineCode(Long orderId, String verifyCode);

    /**
     * 买方获取线下订单的实时动态核销凭证（30s动态数字码 + 10s防截图动态二维码载荷）
     *
     * @param orderId 订单ID
     * @return 动态凭证与服务端倒计时
     */
    OrderVerifyCredentialVO getOfflineVerifyCredential(Long orderId);

    /**
     * 卖方通过实时摄像头扫描买方 10 秒动态二维码完成当面核销结单
     *
     * @param qrToken         实时扫出的动态签名载荷
     * @param expectedOrderId 可选：限定必须为指定订单
     * @return 完成核销的订单ID
     */
    Long verifyOfflineQr(String qrToken, Long expectedOrderId);

    /**
     * 买家确认收货
     * 订单状态: 待收货(3) → 已完成(4)，资金释放给卖家
     *
     * @param orderId 订单ID
     */
    void confirmOrder(Long orderId);

    /**
     * 取消订单（仅待支付状态）
     *
     * @param orderId 订单ID
     */
    void cancelOrder(Long orderId);

    // ==================== 退款相关操作 ====================

    /**
     * 买家申请退款
     * - 待发货(2)状态：直接退款，无需卖家同意
     * - 待收货(3)状态：创建退款申请，等待卖家处理
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     */
    void requestRefund(Long orderId, String reason);

    /**
     * 卖家处理退款申请
     *
     * @param orderId 订单ID
     * @param approve 是否同意退款
     * @param rejectReason 拒绝原因（拒绝时必填）
     */
    void sellerHandleRefund(Long orderId, boolean approve, String rejectReason);

    // ==================== 管理员方法 ====================

    /**
     * 管理员强制退款订单（用于纠纷处理）
     *
     * @param orderId 订单ID
     * @param reason  退款原因
     */
    void adminRefundOrder(Long orderId, String reason);

    // ==================== 定时任务方法 ====================

    /**
     * 自动取消长期未支付的订单
     * 定时任务调用，超时时间由 ORDER_UNPAID_TIMEOUT_HOURS 配置
     */
    void autoCancelUnpaidOrders();

    /**
     * 自动确认收货
     * 定时任务调用，发货后超过 AUTO_CONFIRM_RECEIPT_DAYS 天自动确认
     */
    void autoConfirmReceipt();

    /**
     * 自动退款超时未处理的退款申请
     * 定时任务调用，卖家超过 SELLER_REFUND_TIMEOUT_DAYS 天未处理自动退款
     */
    void autoRefundTimeout();

    // ==================== 废弃方法（兼容旧代码）====================

    /**
     * @deprecated 使用 {@link #sellerShip(Long)} 代替
     */
    @Deprecated
    default void sellerConfirmOrder(Long orderId) {
        sellerShip(orderId);
    }

    /**
     * @deprecated 使用 {@link #requestRefund(Long, String)} 代替
     */
    @Deprecated
    default void refundOrder(Long orderId, String reason) {
        requestRefund(orderId, reason);
    }

    /**
     * @deprecated 使用 {@link #autoConfirmReceipt()} 代替
     */
    @Deprecated
    default void autoReleaseFrozenOrders() {
        autoConfirmReceipt();
    }
}
