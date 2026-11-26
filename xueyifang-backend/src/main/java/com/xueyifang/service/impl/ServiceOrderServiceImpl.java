package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.DisputeConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.dto.OrderPayRequest;
import com.xueyifang.domain.dto.SellerShipRequest;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.domain.vo.OrderDetailVO;
import com.xueyifang.domain.vo.OrderListItemVO;
import com.xueyifang.domain.vo.OrderVerifyCredentialVO;
import com.xueyifang.domain.vo.SellerOrderListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.service.IUserService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.OfflineVerifyCodeUtils;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.TransactionUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务订单表 服务实现类
 *
 * <h3>交易流程 v2.0</h3>
 * <ol>
 *   <li>买家下单 → 待支付(1)</li>
 *   <li>买家支付 → 待发货(2)，资金冻结在买家账户，占用库存</li>
 *   <li>卖家发货 → 待收货(3)</li>
 *   <li>买家确认收货 → 已完成(4)，资金释放给卖家</li>
 * </ol>
 *
 * <h3>退款流程</h3>
 * <ul>
 *   <li>待发货(2)：买家可直接退款，无需卖家同意</li>
 *   <li>待收货(3)：买家申请 → 卖家同意/拒绝 → 超时自动退款</li>
 *   <li>退款成功后解冻资金退回买家，并归还库存</li>
 * </ul>
 *
 * <h3>并发控制约定</h3>
 * <ul>
 *   <li>所有修改订单状态/资金的操作，先 {@code SELECT ... FOR UPDATE} 锁订单行，再锁用户行</li>
 *   <li>同时涉及买卖双方资金时，按 userId 升序加锁，避免死锁</li>
 *   <li>库存通过带条件的原子 UPDATE 占用，杜绝超卖</li>
 * </ul>
 *
 * @author luoyi
 * @since 2025-11-19
 */
@Slf4j
@org.springframework.stereotype.Service
public class ServiceOrderServiceImpl extends ServiceImpl<ServiceOrderMapper, ServiceOrder> implements IServiceOrderService {

    private static final String DEFAULT_VERIFY_MASTER_SECRET = "xueyifang-dev-jwt-secret-change-in-production";
    private static final String VERIFY_FAIL_KEY_PREFIX = "order:verify:fail:";
    private static final int MAX_VERIFY_FAIL_ATTEMPTS = 5;
    private static final long VERIFY_FAIL_WINDOW_SECONDS = 900L;

    @Value("${jwt.secret:xueyifang-dev-jwt-secret-change-in-production}")
    private String verifyMasterSecret;

    @Resource
    private IServiceService serviceService;

    @Resource
    private IUserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private ServiceDisputeMapper serviceDisputeMapper;

    @Resource
    private ITradeLocationService tradeLocationService;

    @Resource
    private IServiceOrderLogService orderLogService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private IServiceImageService serviceImageService;

    @Resource
    private IServiceReviewService serviceReviewService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private IWalletTransactionService walletTransactionService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    // ==================== 订单创建与支付 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateRequest request) {
        Long userId = requireLogin();
        Long serviceId = request.getServiceId();
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "购买数量必须大于0");
        }

        // 1. 验证服务是否存在且上架
        Service service = serviceService.getById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!Objects.equals(service.getStatus(), ServiceConstant.STATUS_ONLINE)) {
            throw new BusinessException(ErrorCode.SERVICE_OFFLINE);
        }

        // 2. 不能购买自己的服务
        if (service.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能购买自己发布的服务");
        }

        // 3. 库存预检查（仅用于友好提示，真正的库存占用在支付时原子完成）
        if (!hasStock(service, quantity)) {
            throw new BusinessException(ErrorCode.SERVICE_SOLD_OUT, "服务库存不足");
        }

        // 4. 交易类型以服务发布时的设定为准；线下交易必须具有有效地点（默认继承服务预设地点）
        Integer effectiveTradeType = service.getTradeType() != null ? service.getTradeType() : request.getTradeType();
        Long effectiveLocationId = null;
        if (Objects.equals(effectiveTradeType, OrderConstant.TRADE_TYPE_OFFLINE)) {
            effectiveLocationId = request.getTradeLocationId() != null
                    ? request.getTradeLocationId()
                    : service.getTradeLocationId();
            if (effectiveLocationId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "线下交易必须选择交易地点");
            }
            TradeLocation location = tradeLocationService.getById(effectiveLocationId);
            if (location == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点不存在");
            }
        }

        // 5. 生成订单
        ServiceOrder order = new ServiceOrder();
        BeanUtils.copyProperties(request, order);
        order.setTradeType(effectiveTradeType);
        order.setTradeLocationId(effectiveLocationId);
        order.setOrderNumber(SerialNoGenerator.nextOrderNo());
        order.setBuyerId(userId);
        order.setSellerId(service.getSellerId());
        order.setUnitPrice(service.getPrice());
        order.setTotalAmount(service.getPrice().multiply(new BigDecimal(quantity)));
        order.setOrderStatus(OrderConstant.ORDER_UNPAID);
        order.setPaymentStatus(OrderConstant.PAYMENT_UNPAID);
        order.setRefundStatus(OrderConstant.REFUND_NONE);
        this.save(order);

        orderLogService.recordLog(order.getId(), null, OrderConstant.ORDER_UNPAID,
                OrderLogConstant.ACTION_CREATE, "创建订单");

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, OrderPayRequest request) {
        Long userId = requireLogin();

        // 1. 锁定订单并校验
        ServiceOrder order = lockOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能支付自己的订单");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待支付");
        }

        // 2. 原子占用库存（并发下不会超卖；失败时整个事务回滚）
        int stockUpdated = serviceMapper.incrementPurchaseCountIfAvailable(order.getServiceId(), order.getQuantity());
        if (stockUpdated == 0) {
            throw new BusinessException(ErrorCode.SERVICE_SOLD_OUT, "服务库存不足，无法支付");
        }

        // 3. 锁定买家账户并校验余额
        User buyer = userMapper.selectForUpdate(userId);
        BigDecimal amount = order.getTotalAmount();
        if (buyer.getWalletBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.ORDER_BALANCE_NOT_ENOUGH);
        }

        // 4. 扣减可用余额，增加冻结金额（无论线上线下统一走担保冻结）
        BigDecimal balanceBefore = buyer.getWalletBalance();
        BigDecimal frozenBefore = buyer.getFrozenAmount();
        BigDecimal newBalance = balanceBefore.subtract(amount);
        BigDecimal newFrozen = frozenBefore.add(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getWalletBalance, newBalance)
                .set(User::getFrozenAmount, newFrozen));

        recordOrderTransaction(userId, WalletConstant.TRANSACTION_TYPE_PAYMENT, amount,
                balanceBefore, newBalance, frozenBefore, frozenBefore,
                order.getId(), "支付订单 " + order.getOrderNumber());
        recordOrderTransaction(userId, WalletConstant.TRANSACTION_TYPE_FREEZE, amount,
                newBalance, newBalance, frozenBefore, newFrozen,
                order.getId(), "冻结订单资金 " + order.getOrderNumber());

        // 5. 更新订单状态为待发货（线下交易同步生成高熵核销种子，用于派生 30s 动态码与 10s 动态二维码）
        order.setFrozenAmount(amount);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_SHIP);
        order.setPaymentStatus(OrderConstant.PAYMENT_PAID);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentTime(LocalDateTime.now());
        if (Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(order.getTradeType())) {
            order.setVerifyCode(OfflineVerifyCodeUtils.generateOrderSeed());
        }
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_PENDING_SHIP,
                OrderLogConstant.ACTION_PAY, "买家支付，资金冻结，等待卖家发货");

        evictServiceDetailCacheAfterCommit(order.getServiceId());

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "新订单通知",
                String.format("买家 %s 已支付订单 %s，服务：%s，金额：%.2f 元，请尽快发货",
                        buyer.getNickname(), order.getOrderNumber(), getServiceTitle(order.getServiceId()), amount));
    }

    // ==================== 交易流程操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerShip(Long orderId) {
        sellerShip(orderId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerShip(Long orderId, SellerShipRequest request) {
        Long userId = requireLogin();

        ServiceOrder order = lockOrder(orderId);
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能操作自己的订单");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待发货，无法发货");
        }
        if (order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单有待处理的退款申请，请先处理");
        }

        String deliveryNote = null;
        if (request != null) {
            if (StringUtils.isNotBlank(request.getDeliveryNote())) {
                deliveryNote = request.getDeliveryNote().trim();
                if (deliveryNote.length() > 500) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "交付说明不能超过500字");
                }
                order.setDeliveryNote(deliveryNote);
            }
            if (request.getDeliveryImages() != null && !request.getDeliveryImages().isEmpty()) {
                if (request.getDeliveryImages().size() > 6) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "交付凭证图片最多上传6张");
                }
                List<String> relativeKeys = new ArrayList<>();
                for (String rawUrl : request.getDeliveryImages()) {
                    if (StringUtils.isBlank(rawUrl)) {
                        continue;
                    }
                    relativeKeys.add(fileUrlResolver != null ? fileUrlResolver.toRelativeKey(rawUrl) : rawUrl.trim());
                }
                if (!relativeKeys.isEmpty()) {
                    order.setDeliveryImages(String.join(",", relativeKeys));
                }
            }
        }

        order.setOrderStatus(OrderConstant.ORDER_PENDING_RECEIPT);
        order.setSellerShipTime(LocalDateTime.now());
        this.updateById(order);

        String logDesc = StringUtils.isNotBlank(deliveryNote)
                ? "卖家已提交交付凭证并发货：" + deliveryNote
                : "卖家已发货，等待买家确认收货";
        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_PENDING_SHIP, OrderConstant.ORDER_PENDING_RECEIPT,
                OrderLogConstant.ACTION_SELLER_SHIP, logDesc);

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "发货通知",
                String.format("卖家 %s 已完成交付并发货，订单 %s，服务：%s，请及时查收验收",
                        getNickname(userId), order.getOrderNumber(), getServiceTitle(order.getServiceId())));
    }

    @Override
    public OrderVerifyCredentialVO getOfflineVerifyCredential(Long orderId) {
        Long userId = requireLogin();
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        ServiceOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        // 严格权限隔离：仅买方本人可拉取实时动态核销凭证，严禁泄露给卖方或第三方
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅买方本人可查看动态当面核销凭证");
        }
        if (!Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(order.getTradeType())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅校内线下交易订单支持动态当面核销");
        }

        long nowEpoch = Instant.now().getEpochSecond();
        OrderVerifyCredentialVO vo = new OrderVerifyCredentialVO();
        vo.setOrderId(order.getId());
        vo.setOrderNumber(order.getOrderNumber());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCodePeriodSeconds((int) OfflineVerifyCodeUtils.CODE_PERIOD_SECONDS);
        vo.setQrPeriodSeconds((int) OfflineVerifyCodeUtils.QR_PERIOD_SECONDS);
        vo.setServerTimestamp(nowEpoch);

        // 若已被卖方扫码/输码核销完结，平稳返回终态状态供买方端即时感知刷新，不抛报错弹窗
        if (Integer.valueOf(OrderConstant.ORDER_COMPLETED).equals(order.getOrderStatus())) {
            vo.setCodeRemainingSeconds(0);
            vo.setQrRemainingSeconds(0);
            return vo;
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态暂不可生成当面核销凭证");
        }
        if (StringUtils.isBlank(order.getVerifyCode())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单核销密钥未初始化，请直接点击确认收货");
        }

        String secret = getVerifyMasterSecret();
        vo.setVerifyCode(OfflineVerifyCodeUtils.generateCode(order.getId(), order.getVerifyCode(), secret, nowEpoch));
        vo.setCodeRemainingSeconds(OfflineVerifyCodeUtils.remainingSeconds(nowEpoch, OfflineVerifyCodeUtils.CODE_PERIOD_SECONDS));
        vo.setQrToken(OfflineVerifyCodeUtils.generateQrToken(order.getId(), order.getVerifyCode(), secret, nowEpoch));
        vo.setQrRemainingSeconds(OfflineVerifyCodeUtils.remainingSeconds(nowEpoch, OfflineVerifyCodeUtils.QR_PERIOD_SECONDS));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyOfflineCode(Long orderId, String verifyCode) {
        Long userId = requireLogin();
        if (StringUtils.isBlank(verifyCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入6位动态核销码");
        }
        String inputCode = verifyCode.trim();

        String failKey = VERIFY_FAIL_KEY_PREFIX + orderId;
        checkVerifyRateLimit(failKey);

        ServiceOrder order = lockOrder(orderId);
        validateOfflineVerifyEligibility(order, userId);

        long nowEpoch = Instant.now().getEpochSecond();
        boolean matched = OfflineVerifyCodeUtils.verifyCode(
                order.getId(), order.getVerifyCode(), getVerifyMasterSecret(), inputCode, nowEpoch);
        if (!matched) {
            recordVerifyFailure(failKey);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动态核销码不正确或已过期（每30秒自动刷新），请核对买家最新6位数字");
        }

        finishOfflineVerification(order, failKey,
                "校内当面 30s 动态核销码验证通过，当面交接完成并结算资金",
                "动态核销码");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long verifyOfflineQr(String qrToken, Long expectedOrderId) {
        Long userId = requireLogin();
        OfflineVerifyCodeUtils.ParsedQrToken parsed = OfflineVerifyCodeUtils.parseQrToken(qrToken);
        if (parsed == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法识别的二维码，请现场扫描学艺坊买家端实时动态核销码");
        }
        if (expectedOrderId != null && !expectedOrderId.equals(parsed.getOrderId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所扫二维码不属于当前订单，请核对订单后再扫");
        }

        String failKey = VERIFY_FAIL_KEY_PREFIX + parsed.getOrderId();
        checkVerifyRateLimit(failKey);

        ServiceOrder order = lockOrder(parsed.getOrderId());
        validateOfflineVerifyEligibility(order, userId);

        long nowEpoch = Instant.now().getEpochSecond();
        OfflineVerifyCodeUtils.QrVerifyStatus status = OfflineVerifyCodeUtils.verifyQrToken(
                order.getId(), order.getVerifyCode(), getVerifyMasterSecret(), parsed, nowEpoch);
        if (status == OfflineVerifyCodeUtils.QrVerifyStatus.EXPIRED) {
            recordVerifyFailure(failKey);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动态核销二维码已过期（10秒有效期），请现场扫描买家最新动态码，不支持截图核销");
        }
        if (status != OfflineVerifyCodeUtils.QrVerifyStatus.VALID) {
            recordVerifyFailure(failKey);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动态核销二维码签名无效，请确认扫描的是真实订单实时动态码");
        }

        finishOfflineVerification(order, failKey,
                "校内当面 10s 动态二维码实时扫码核销通过，当面交接完成并结算资金",
                "现场动态二维码");
        return order.getId();
    }

    private void checkVerifyRateLimit(String failKey) {
        if (redisUtils != null) {
            Object failObj = redisUtils.get(failKey);
            if (failObj != null && Integer.parseInt(failObj.toString()) >= MAX_VERIFY_FAIL_ATTEMPTS) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "核销连续失败次数过多，请15分钟后再试或请买家直接点击确认收货");
            }
        }
    }

    private void recordVerifyFailure(String failKey) {
        if (redisUtils != null) {
            redisUtils.incrementWithExpire(failKey, VERIFY_FAIL_WINDOW_SECONDS);
        }
    }

    private void validateOfflineVerifyEligibility(ServiceOrder order, Long sellerUserId) {
        if (!order.getSellerId().equals(sellerUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能核销自己的销售订单");
        }
        if (!Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(order.getTradeType())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅校内线下交易订单支持当面核销");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持核销结单");
        }
        if (order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单存在待处理的退款申请，请先协商处理");
        }
    }

    private void finishOfflineVerification(ServiceOrder order, String failKey, String logDesc, String methodLabel) {
        if (redisUtils != null) {
            redisUtils.delete(failKey);
        }
        if (order.getSellerShipTime() == null) {
            order.setSellerShipTime(LocalDateTime.now());
        }
        closeActiveDisputesForOrder(order.getId(), "线下当面核销验证通过，双方当面完成交接，关联纠纷自动结案");
        completeOrder(order, OrderLogConstant.ACTION_OFFLINE_VERIFY, logDesc);

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "当面核销完成通知",
                String.format("您的线下订单 %s（服务：%s）已通过%s完成当面交接，担保资金 %.2f 元已结算给卖家",
                        order.getOrderNumber(), getServiceTitle(order.getServiceId()), methodLabel, order.getTotalAmount()));
    }

    private String getVerifyMasterSecret() {
        return StringUtils.isNotBlank(verifyMasterSecret) ? verifyMasterSecret : DEFAULT_VERIFY_MASTER_SECRET;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId) {
        Long userId = requireLogin();

        ServiceOrder order = lockOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能确认自己的订单");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待收货，无法确认收货");
        }

        // 买家主动确认收货视为撤回退款申请及已解决关联纠纷
        closeActiveDisputesForOrder(order.getId(), "买方已主动确认收货并完成担保结算，平台纠纷自动结案");
        completeOrder(order, OrderLogConstant.ACTION_BUYER_CONFIRM, "买家确认收货，交易完成，资金已结算给卖家");

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "交易完成通知",
                String.format("买家 %s 已确认收货，订单 %s 交易完成，金额 %.2f 元已结算到您的钱包",
                        getNickname(userId), order.getOrderNumber(), order.getTotalAmount()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Long userId = requireLogin();

        ServiceOrder order = lockOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能取消自己的订单");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "只能取消待支付状态的订单");
        }

        order.setOrderStatus(OrderConstant.ORDER_CANCELLED);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_CANCELLED,
                OrderLogConstant.ACTION_CANCEL, "买家取消未支付订单");
    }

    // ==================== 退款相关操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(Long orderId, String reason) {
        Long userId = requireLogin();

        ServiceOrder order = lockOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能申请自己订单的退款");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持退款");
        }
        if (order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有退款申请正在处理中");
        }
        if (order.getRefundStatus() == OrderConstant.REFUND_COMPLETED) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该订单已退款，无法重复申请");
        }

        if (order.getOrderStatus() == OrderConstant.ORDER_PENDING_SHIP) {
            // 待发货：直接退款，无需卖家同意
            processRefund(order, reason, OrderLogConstant.ACTION_REFUND, "买家在发货前申请退款，系统自动退款");
            return;
        }

        // 待收货：创建退款申请，等待卖家处理
        order.setRefundStatus(OrderConstant.REFUND_PENDING);
        order.setRefundReason(reason);
        order.setRefundRequestTime(LocalDateTime.now());
        this.updateById(order);

        orderLogService.recordLog(order.getId(), order.getOrderStatus(), order.getOrderStatus(),
                OrderLogConstant.ACTION_REFUND_REQUEST, "买家申请退款：" + reason);

        int timeoutDays = sysConfigService.getIntValue(SysConfigConstant.SELLER_REFUND_TIMEOUT_DAYS, 3);
        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "退款申请通知",
                String.format("买家 %s 对订单 %s（服务：%s）申请退款，原因：%s，请在%d天内处理",
                        getNickname(userId), order.getOrderNumber(), getServiceTitle(order.getServiceId()),
                        reason, timeoutDays));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerHandleRefund(Long orderId, boolean approve, String rejectReason) {
        Long userId = requireLogin();

        ServiceOrder order = lockOrder(orderId);
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能处理自己订单的退款申请");
        }
        if (order.getRefundStatus() != OrderConstant.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有待处理的退款申请");
        }

        if (approve) {
            // processRefund 内部会记录日志并通知买家
            processRefund(order, order.getRefundReason(), OrderLogConstant.ACTION_SELLER_APPROVE_REFUND, "卖家同意退款");
            return;
        }

        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝退款时必须填写原因");
        }

        order.setRefundStatus(OrderConstant.REFUND_REJECTED);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), order.getOrderStatus(), order.getOrderStatus(),
                OrderLogConstant.ACTION_SELLER_REJECT_REFUND, "卖家拒绝退款：" + rejectReason);

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "退款申请被拒绝",
                String.format("您对订单 %s（服务：%s）的退款申请被卖家 %s 拒绝，原因：%s。如有异议，可发起纠纷",
                        order.getOrderNumber(), getServiceTitle(order.getServiceId()), getNickname(userId), rejectReason));
    }

    // ==================== 管理员方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefundOrder(Long orderId, String reason) {
        ServiceOrder order = lockOrder(orderId);

        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持退款");
        }

        processRefund(order, reason, OrderLogConstant.ACTION_ADMIN_REFUND, "管理员强制退款：" + reason);

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "订单退款通知",
                String.format("订单 %s 已由管理员处理退款，金额 %.2f 元已退回买家。原因：%s",
                        order.getOrderNumber(), order.getTotalAmount(), reason));
    }

    // ==================== 定时任务方法 ====================
    // 注意：批处理方法本身不开启事务，每个订单在独立事务中处理，
    // 单个订单失败只回滚自身，不会拖垮整批，也不会长时间持有大量行锁。

    @Override
    public void autoCancelUnpaidOrders() {
        int timeoutHours = sysConfigService.getIntValue(SysConfigConstant.ORDER_UNPAID_TIMEOUT_HOURS, 24);
        LocalDateTime timeoutPoint = LocalDateTime.now().minusHours(timeoutHours);

        List<ServiceOrder> timeoutOrders = this.list(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_UNPAID)
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_UNPAID)
                .le(ServiceOrder::getCreateTime, timeoutPoint));

        int success = runEachInTransaction(timeoutOrders, order -> processAutoCancel(order.getId(), timeoutHours), "自动取消");
        log.info("自动取消未支付订单完成：候选{}个，成功{}个", timeoutOrders.size(), success);
    }

    @Override
    public void autoConfirmReceipt() {
        int timeoutDays = sysConfigService.getIntValue(SysConfigConstant.AUTO_CONFIRM_RECEIPT_DAYS, 7);
        LocalDateTime timeoutPoint = LocalDateTime.now().minusDays(timeoutDays);

        List<ServiceOrder> timeoutOrders = this.list(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_PENDING_RECEIPT)
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID)
                .isNotNull(ServiceOrder::getSellerShipTime)
                .le(ServiceOrder::getSellerShipTime, timeoutPoint)
                .ne(ServiceOrder::getRefundStatus, OrderConstant.REFUND_PENDING));

        int success = runEachInTransaction(timeoutOrders, order -> processAutoConfirmReceipt(order.getId(), timeoutDays), "自动确认收货");
        log.info("自动确认收货完成：候选{}个，成功{}个", timeoutOrders.size(), success);
    }

    @Override
    public void autoRefundTimeout() {
        int timeoutDays = sysConfigService.getIntValue(SysConfigConstant.SELLER_REFUND_TIMEOUT_DAYS, 3);
        LocalDateTime timeoutPoint = LocalDateTime.now().minusDays(timeoutDays);

        List<ServiceOrder> timeoutOrders = this.list(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getRefundStatus, OrderConstant.REFUND_PENDING)
                .isNotNull(ServiceOrder::getRefundRequestTime)
                .le(ServiceOrder::getRefundRequestTime, timeoutPoint));

        int success = runEachInTransaction(timeoutOrders, order -> processAutoRefund(order.getId(), timeoutDays), "自动退款");
        log.info("超时自动退款完成：候选{}个，成功{}个", timeoutOrders.size(), success);
    }

    // ==================== 查询方法 ====================

    @Override
    public Page<OrderListItemVO> getMyOrders(Integer pageNum, Integer pageSize, Integer orderStatus) {
        Long userId = requireLogin();

        Page<ServiceOrder> orderPage = this.page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getBuyerId, userId)
                .eq(orderStatus != null, ServiceOrder::getOrderStatus, orderStatus)
                .orderByDesc(ServiceOrder::getCreateTime));

        OrderRelations rel = loadRelations(orderPage.getRecords());
        List<OrderListItemVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderListItemVO vo = new OrderListItemVO();
            BeanUtils.copyProperties(order, vo);
            Service service = rel.services.get(order.getServiceId());
            if (service != null) {
                vo.setServiceTitle(service.getServiceTitle());
            }
            vo.setServiceImage(rel.covers.get(order.getServiceId()));
            User seller = rel.users.get(order.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
            }
            User buyer = rel.users.get(order.getBuyerId());
            if (buyer != null) {
                vo.setBuyerName(buyer.getNickname());
                vo.setBuyerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(buyer.getAvatar()) : buyer.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<OrderListItemVO> resultPage = new Page<>(pageNum, pageSize, orderPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Page<SellerOrderListItemVO> getMySellingOrders(Integer pageNum, Integer pageSize, Integer orderStatus) {
        Long userId = requireLogin();

        Page<ServiceOrder> orderPage = this.page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getSellerId, userId)
                .eq(orderStatus != null, ServiceOrder::getOrderStatus, orderStatus)
                .orderByDesc(ServiceOrder::getCreateTime));

        OrderRelations rel = loadRelations(orderPage.getRecords());
        List<SellerOrderListItemVO> voList = orderPage.getRecords().stream().map(order -> {
            SellerOrderListItemVO vo = new SellerOrderListItemVO();
            BeanUtils.copyProperties(order, vo);
            Service service = rel.services.get(order.getServiceId());
            if (service != null) {
                vo.setServiceTitle(service.getServiceTitle());
            }
            vo.setServiceImage(rel.covers.get(order.getServiceId()));
            User buyer = rel.users.get(order.getBuyerId());
            if (buyer != null) {
                vo.setBuyerName(buyer.getNickname());
                vo.setBuyerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(buyer.getAvatar()) : buyer.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<SellerOrderListItemVO> resultPage = new Page<>(pageNum, pageSize, orderPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        Long userId = requireLogin();

        ServiceOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        // 买家、卖家或管理员可查看
        if (!UserContext.isAdmin() && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该订单");
        }

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);

        Service service = serviceService.getById(order.getServiceId());
        if (service != null) {
            vo.setServiceTitle(service.getServiceTitle());
            vo.setServiceDescription(service.getServiceDescription());
        }
        vo.setServiceImage(serviceImageService.getCoverImage(order.getServiceId()));

        Map<Long, User> users = toMap(userService.listByIds(java.util.Arrays.asList(order.getBuyerId(), order.getSellerId())), User::getId);
        Set<Long> professionalIds = users.values().stream()
                .map(User::getProfessionalId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Professional> professionals = professionalIds.isEmpty()
                ? Collections.<Long, Professional>emptyMap()
                : toMap(professionalService.listByIds(professionalIds), Professional::getId);

        User buyer = users.get(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerName(buyer.getNickname());
            vo.setBuyerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(buyer.getAvatar()) : buyer.getAvatar());
            vo.setBuyerProfessional(professionalName(professionals, buyer.getProfessionalId()));
        }
        User seller = users.get(order.getSellerId());
        if (seller != null) {
            vo.setSellerName(seller.getNickname());
            vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
            vo.setSellerProfessional(professionalName(professionals, seller.getProfessionalId()));
        }

        if (order.getTradeLocationId() != null) {
            TradeLocation location = tradeLocationService.getById(order.getTradeLocationId());
            if (location != null) {
                vo.setTradeLocationName(location.getLocationName());
            }
        }

        vo.setDeliveryImages(parsePublicUrlList(order.getDeliveryImages()));
        // 严格权限隔离：仅买方本人在待发货/待收货阶段派生实时 30s 动态 6 位核销码，永不暴露库内密钥种子，卖方始终为 null
        boolean showVerifyCode = order.getBuyerId().equals(userId)
                && Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(order.getTradeType())
                && (Integer.valueOf(OrderConstant.ORDER_PENDING_SHIP).equals(order.getOrderStatus())
                    || Integer.valueOf(OrderConstant.ORDER_PENDING_RECEIPT).equals(order.getOrderStatus()));
        if (showVerifyCode && StringUtils.isNotBlank(order.getVerifyCode())) {
            vo.setVerifyCode(OfflineVerifyCodeUtils.generateCode(
                    order.getId(), order.getVerifyCode(), getVerifyMasterSecret(), Instant.now().getEpochSecond()));
        } else {
            vo.setVerifyCode(null);
        }

        vo.setIsReviewed(serviceReviewService.isOrderReviewed(order.getId()));
        return vo;
    }

    // ==================== 私有辅助方法：状态流转 ====================

    /**
     * 锁定订单行（FOR UPDATE），订单不存在则抛异常
     */
    private ServiceOrder lockOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        ServiceOrder order = baseMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        return order;
    }

    /**
     * 完成订单：释放资金给卖家 + 更新订单状态 + 记录日志
     * 调用方必须已持有订单行锁且已校验状态
     */
    private void completeOrder(ServiceOrder order, String action, String logMessage) {
        Integer previousStatus = order.getOrderStatus();
        releaseFundsToSeller(order);

        order.setOrderStatus(OrderConstant.ORDER_COMPLETED);
        order.setBuyerConfirmTime(LocalDateTime.now());
        order.setFrozenAmount(BigDecimal.ZERO);
        order.setRefundStatus(OrderConstant.REFUND_NONE);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), previousStatus, OrderConstant.ORDER_COMPLETED,
                action, logMessage);

        evictServiceDetailCacheAfterCommit(order.getServiceId());
        if (redisUtils != null) {
            Long sellerId = order.getSellerId();
            TransactionUtils.afterCommit(() -> redisUtils.delete(CacheConstant.STATISTICS_USER_SALES_PREFIX + sellerId));
        }
    }

    /**
     * 执行退款：解冻资金退回买家、归还库存、更新订单、记录日志、通知买家
     * 调用方必须已持有订单行锁
     *
     * @param action     订单日志操作类型（区分买家直退/卖家同意/管理员/系统自动）
     * @param logMessage 日志描述
     */
    private void processRefund(ServiceOrder order, String reason, String action, String logMessage) {
        BigDecimal refundAmount = order.getFrozenAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单没有可退款的冻结金额");
        }
        // 记录变更前状态，用于日志
        Integer previousStatus = order.getOrderStatus();

        // 1. 锁定买家账户，解冻并退回余额
        User buyer = userMapper.selectForUpdate(order.getBuyerId());
        if (buyer.getFrozenAmount().compareTo(refundAmount) < 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "买家冻结金额不足");
        }
        BigDecimal balanceBefore = buyer.getWalletBalance();
        BigDecimal frozenBefore = buyer.getFrozenAmount();
        BigDecimal newFrozen = frozenBefore.subtract(refundAmount);
        BigDecimal newBalance = balanceBefore.add(refundAmount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, order.getBuyerId())
                .set(User::getFrozenAmount, newFrozen)
                .set(User::getWalletBalance, newBalance));

        recordOrderTransaction(order.getBuyerId(), WalletConstant.TRANSACTION_TYPE_UNFREEZE, refundAmount,
                balanceBefore, balanceBefore, frozenBefore, newFrozen,
                order.getId(), "退款解冻订单资金 " + order.getOrderNumber());
        recordOrderTransaction(order.getBuyerId(), WalletConstant.TRANSACTION_TYPE_REFUND, refundAmount,
                balanceBefore, newBalance, newFrozen, newFrozen,
                order.getId(), "订单退款 " + order.getOrderNumber());

        // 2. 归还库存（支付时占用的购买名额）
        serviceMapper.decrementPurchaseCount(order.getServiceId(), order.getQuantity());

        // 3. 更新订单状态（保留买家原始下单备注 remark，将退款/仲裁原因写入 refundReason）
        order.setPaymentStatus(OrderConstant.PAYMENT_REFUNDED);
        order.setOrderStatus(OrderConstant.ORDER_FAILED);
        order.setFrozenAmount(BigDecimal.ZERO);
        order.setRefundStatus(OrderConstant.REFUND_COMPLETED);
        if (reason != null && !reason.trim().isEmpty()) {
            order.setRefundReason(reason);
        }
        this.updateById(order);

        // 4. 记录日志（使用变更前的状态）
        orderLogService.recordLog(order.getId(), previousStatus, OrderConstant.ORDER_FAILED, action, logMessage);

        // 非管理员仲裁触发的退款（如卖家同意退款、系统超时自动退款）自动完结该单关联的待处理纠纷
        if (!OrderLogConstant.ACTION_ADMIN_REFUND.equals(action)) {
            closeActiveDisputesForOrder(order.getId(), "订单已完成全额退款，关联纠纷自动结案");
        }

        evictServiceDetailCacheAfterCommit(order.getServiceId());

        // 5. 通知买家
        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "退款成功通知",
                String.format("订单 %s（服务：%s）退款成功，金额 %.2f 元已退回您的钱包",
                        order.getOrderNumber(), getServiceTitle(order.getServiceId()), refundAmount));
    }

    /**
     * 释放冻结资金给卖家（按 userId 升序加锁，避免死锁）
     */
    private void releaseFundsToSeller(ServiceOrder order) {
        BigDecimal amount = order.getFrozenAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long buyerId = order.getBuyerId();
        Long sellerId = order.getSellerId();
        User buyer;
        User seller;
        if (buyerId < sellerId) {
            buyer = userMapper.selectForUpdate(buyerId);
            seller = userMapper.selectForUpdate(sellerId);
        } else {
            seller = userMapper.selectForUpdate(sellerId);
            buyer = userMapper.selectForUpdate(buyerId);
        }

        if (buyer.getFrozenAmount().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "买家冻结金额不足");
        }

        BigDecimal buyerBalanceBefore = buyer.getWalletBalance();
        BigDecimal buyerFrozenBefore = buyer.getFrozenAmount();
        BigDecimal sellerBalanceBefore = seller.getWalletBalance();
        BigDecimal sellerFrozenBefore = seller.getFrozenAmount();

        BigDecimal buyerFrozenAfter = buyerFrozenBefore.subtract(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, buyerId)
                .set(User::getFrozenAmount, buyerFrozenAfter));

        BigDecimal sellerBalanceAfter = sellerBalanceBefore.add(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, sellerId)
                .set(User::getWalletBalance, sellerBalanceAfter));

        recordOrderTransaction(buyerId, WalletConstant.TRANSACTION_TYPE_UNFREEZE, amount,
                buyerBalanceBefore, buyerBalanceBefore, buyerFrozenBefore, buyerFrozenAfter,
                order.getId(), "确认收货解冻资金 " + order.getOrderNumber());
        recordOrderTransaction(sellerId, WalletConstant.TRANSACTION_TYPE_INCOME, amount,
                sellerBalanceBefore, sellerBalanceAfter, sellerFrozenBefore, sellerFrozenBefore,
                order.getId(), "订单收入 " + order.getOrderNumber());
    }

    // ==================== 私有辅助方法：定时任务 ====================

    /**
     * 对每个订单开启独立事务执行处理逻辑，返回成功数量
     */
    private int runEachInTransaction(List<ServiceOrder> orders, java.util.function.Consumer<ServiceOrder> handler, String taskName) {
        int success = 0;
        for (ServiceOrder order : orders) {
            try {
                Boolean done = transactionTemplate.execute(status -> {
                    handler.accept(order);
                    return Boolean.TRUE;
                });
                if (Boolean.TRUE.equals(done)) {
                    success++;
                }
            } catch (Exception e) {
                log.error("[定时任务-{}] 订单{}处理失败", taskName, order.getId(), e);
            }
        }
        return success;
    }

    private void processAutoCancel(Long orderId, int timeoutHours) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getOrderStatus() != OrderConstant.ORDER_UNPAID
                || order.getPaymentStatus() != OrderConstant.PAYMENT_UNPAID) {
            log.info("订单{}状态已变更，跳过自动取消", orderId);
            return;
        }

        order.setOrderStatus(OrderConstant.ORDER_CANCELLED);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_CANCELLED,
                OrderLogConstant.ACTION_AUTO_CANCEL,
                String.format("系统自动取消：订单超过%d小时未支付", timeoutHours));

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "订单自动取消通知",
                String.format("订单 %s 因超过 %d 小时未支付，已被系统自动取消", order.getOrderNumber(), timeoutHours));
    }

    private void processAutoConfirmReceipt(Long orderId, int timeoutDays) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT
                || order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            log.info("订单{}状态已变更或有待处理退款，跳过自动确认收货", orderId);
            return;
        }
        if (hasActiveDispute(orderId)) {
            log.info("订单{}存在未解决的纠纷，跳过自动确认收货", orderId);
            return;
        }

        BigDecimal amount = order.getFrozenAmount();
        completeOrder(order, OrderLogConstant.ACTION_AUTO_CONFIRM_RECEIPT,
                String.format("系统自动确认收货：发货超过%d天", timeoutDays));

        String serviceTitle = getServiceTitle(order.getServiceId());
        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "订单自动确认收货",
                String.format("订单 %s（服务：%s）发货超过%d天，系统已自动确认收货",
                        order.getOrderNumber(), serviceTitle, timeoutDays));
        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "订单完成通知",
                String.format("订单 %s 系统自动确认收货，金额 %.2f 元已结算到您的钱包",
                        order.getOrderNumber(), amount == null ? order.getTotalAmount() : amount));
    }

    private void processAutoRefund(Long orderId, int timeoutDays) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getRefundStatus() != OrderConstant.REFUND_PENDING) {
            log.info("订单{}退款申请已被处理，跳过自动退款", orderId);
            return;
        }

        processRefund(order, order.getRefundReason(), OrderLogConstant.ACTION_AUTO_REFUND,
                String.format("卖家超过%d天未处理退款申请，系统自动退款", timeoutDays));

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "退款申请自动处理通知",
                String.format("订单 %s（服务：%s）的退款申请因您超过%d天未处理，系统已自动退款给买家",
                        order.getOrderNumber(), getServiceTitle(order.getServiceId()), timeoutDays));
    }

    private boolean hasActiveDispute(Long orderId) {
        if (serviceDisputeMapper == null || orderId == null) {
            return false;
        }
        Long count = serviceDisputeMapper.selectCount(new LambdaQueryWrapper<ServiceDispute>()
                .eq(ServiceDispute::getOrderId, orderId)
                .in(ServiceDispute::getDisputeStatus, DisputeConstant.STATUS_PENDING, DisputeConstant.STATUS_PROCESSING)
                .eq(ServiceDispute::getIsDelete, 0));
        return count != null && count > 0;
    }

    private void closeActiveDisputesForOrder(Long orderId, String resolutionNote) {
        if (serviceDisputeMapper == null || orderId == null) {
            return;
        }
        serviceDisputeMapper.update(null, new LambdaUpdateWrapper<ServiceDispute>()
                .eq(ServiceDispute::getOrderId, orderId)
                .in(ServiceDispute::getDisputeStatus, DisputeConstant.STATUS_PENDING, DisputeConstant.STATUS_PROCESSING)
                .eq(ServiceDispute::getIsDelete, 0)
                .set(ServiceDispute::getDisputeStatus, DisputeConstant.STATUS_RESOLVED)
                .set(ServiceDispute::getResolution, resolutionNote)
                .set(ServiceDispute::getUpdateTime, LocalDateTime.now()));
    }

    private List<String> parsePublicUrlList(String storedCsv) {
        if (StringUtils.isBlank(storedCsv)) {
            return Collections.emptyList();
        }
        String[] parts = storedCsv.split(",");
        List<String> urls = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                String clean = part.trim();
                urls.add(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(clean) : clean);
            }
        }
        return urls;
    }

    // ==================== 私有辅助方法：通用 ====================

    private void evictServiceDetailCacheAfterCommit(Long serviceId) {
        if (redisUtils == null || serviceId == null) {
            return;
        }
        TransactionUtils.afterCommit(() -> redisUtils.delete(CacheConstant.SERVICE_DETAIL_PREFIX + serviceId));
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return userId;
    }

    private boolean hasStock(Service service, int quantity) {
        Integer max = service.getMaxPurchases();
        if (max == null || max == -1) {
            return true;
        }
        int current = service.getCurrentPurchaseCount() == null ? 0 : service.getCurrentPurchaseCount();
        return current + quantity <= max;
    }

    private String getServiceTitle(Long serviceId) {
        Service service = serviceService.getById(serviceId);
        return service != null ? service.getServiceTitle() : "未知服务";
    }

    private String getNickname(Long userId) {
        User user = userService.getById(userId);
        return user != null ? user.getNickname() : "用户" + userId;
    }

    private static String professionalName(Map<Long, Professional> professionals, Long professionalId) {
        if (professionalId == null) {
            return null;
        }
        Professional p = professionals.get(professionalId);
        return p != null ? p.getProfessionalName() : null;
    }

    private static <T> Map<Long, T> toMap(List<T> list, Function<T, Long> keyMapper) {
        return list.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a));
    }

    /**
     * 订单列表关联数据（服务、用户、封面），一次性批量加载，避免 N+1 查询
     */
    private static class OrderRelations {
        Map<Long, Service> services = Collections.emptyMap();
        Map<Long, User> users = Collections.emptyMap();
        Map<Long, String> covers = Collections.emptyMap();
    }

    private OrderRelations loadRelations(List<ServiceOrder> orders) {
        OrderRelations rel = new OrderRelations();
        if (orders == null || orders.isEmpty()) {
            return rel;
        }
        Set<Long> serviceIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (ServiceOrder order : orders) {
            serviceIds.add(order.getServiceId());
            userIds.add(order.getBuyerId());
            userIds.add(order.getSellerId());
        }
        rel.services = toMap(serviceService.listByIds(serviceIds), Service::getId);
        rel.users = toMap(userService.listByIds(userIds), User::getId);
        rel.covers = serviceImageService.getCoverImageMap(serviceIds);
        return rel;
    }

    /**
     * 记录订单相关的钱包交易流水
     */
    private void recordOrderTransaction(Long userId, Integer transactionType, BigDecimal amount,
                                        BigDecimal balanceBefore, BigDecimal balanceAfter,
                                        BigDecimal frozenBefore, BigDecimal frozenAfter,
                                        Long relatedOrderId, String remark) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(frozenBefore);
        transaction.setFrozenAfter(frozenAfter);
        transaction.setRelatedOrderId(relatedOrderId);
        transaction.setRemark(remark);
        transaction.setTransactionNo(SerialNoGenerator.nextTransactionNo(userId));
        transaction.setCreateTime(LocalDateTime.now());

        if (!walletTransactionService.save(transaction)) {
            log.error("保存订单交易流水失败，userId：{}，transactionType：{}，orderId：{}",
                    userId, transactionType, relatedOrderId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存交易流水失败");
        }

        log.info("记录订单交易流水成功，流水号：{}，交易类型：{}，金额：{}，订单ID：{}",
                transaction.getTransactionNo(), WalletConstant.getTransactionTypeName(transactionType), amount, relatedOrderId);
    }
}
