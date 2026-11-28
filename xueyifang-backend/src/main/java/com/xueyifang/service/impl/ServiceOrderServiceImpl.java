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
import com.xueyifang.domain.po.EscrowPaymentOrder;
import com.xueyifang.domain.po.GroupSession;
import com.xueyifang.domain.po.GroupSessionParticipant;
import com.xueyifang.domain.po.PlatformRevenueLedger;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceTimeSlot;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.domain.vo.OrderDetailVO;
import com.xueyifang.domain.vo.OrderListItemVO;
import com.xueyifang.domain.vo.OrderVerifyCredentialVO;
import com.xueyifang.domain.vo.SellerOrderListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.EscrowPaymentOrderMapper;
import com.xueyifang.mapper.GroupSessionMapper;
import com.xueyifang.mapper.GroupSessionParticipantMapper;
import com.xueyifang.mapper.PlatformRevenueLedgerMapper;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceTimeSlotMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.ICreditScoreService;
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
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.GeofenceUtils;
import com.xueyifang.utils.OfflineVerifyCodeUtils;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.TransactionUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private FileUrlResolver fileUrlResolver;

    @Resource
    private ICampusService campusService;

    @Resource
    private ServiceTimeSlotMapper serviceTimeSlotMapper;

    @Resource
    private PlatformRevenueLedgerMapper platformRevenueLedgerMapper;

    @Resource
    private EscrowPaymentOrderMapper escrowPaymentOrderMapper;

    @Resource
    private ICreditScoreService creditScoreService;

    @Resource
    private GroupSessionMapper groupSessionMapper;

    @Resource
    private GroupSessionParticipantMapper groupSessionParticipantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateRequest request) {
        Long userId = requireLogin();
        Long serviceId = request.getServiceId();
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "购买数量必须大于0");
        }

        // 校验买方最低校园履约信用分
        User currentUser = UserContext.getCurrentUser();
        if (currentUser != null && currentUser.getCreditScore() != null && sysConfigService != null) {
            int minCredit = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_ORDER, 60);
            if (currentUser.getCreditScore() < minCredit) {
                throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                        "您的履约信用分为 " + currentUser.getCreditScore() + "，低于下单门槛 " + minCredit);
            }
        }

        Service service = serviceService.getById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!Objects.equals(service.getStatus(), ServiceConstant.STATUS_ONLINE)) {
            throw new BusinessException(ErrorCode.SERVICE_OFFLINE);
        }
        if (service.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能购买自己发布的服务");
        }
        if (!hasStock(service, quantity)) {
            throw new BusinessException(ErrorCode.SERVICE_SOLD_OUT, "服务库存不足");
        }

        Long campusId = service.getCampusId() != null ? service.getCampusId() : 1L;
        Integer effectiveTradeType = service.getTradeType() != null ? service.getTradeType() : request.getTradeType();
        Long effectiveLocationId = null;
        BigDecimal meetupLat = request.getMeetupLat();
        BigDecimal meetupLng = request.getMeetupLng();
        String meetupPoiName = request.getMeetupPoiName();
        String meetupAddress = request.getMeetupAddress();
        Integer meetupRadiusMeters = request.getMeetupRadiusMeters();

        if (Objects.equals(effectiveTradeType, OrderConstant.TRADE_TYPE_OFFLINE)) {
            boolean hasCustomMapPoint = meetupLat != null && meetupLng != null && StringUtils.isNotBlank(meetupPoiName);
            if (hasCustomMapPoint) {
                if (campusService != null) {
                    campusService.validateCoordinateInCampusFence(
                            campusId, meetupLat.doubleValue(), meetupLng.doubleValue());
                }
            } else {
                effectiveLocationId = request.getTradeLocationId() != null
                        ? request.getTradeLocationId()
                        : service.getTradeLocationId();
                if (effectiveLocationId == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "线下交易请在地图上选点或选择预设校内地点");
                }
                TradeLocation location = tradeLocationService.getById(effectiveLocationId);
                if (location == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易地点不存在");
                }
                meetupLat = location.getLatitude();
                meetupLng = location.getLongitude();
                meetupPoiName = location.getLocationName();
                meetupAddress = location.getLocationAddress();
                meetupRadiusMeters = location.getValidRadiusMeters() != null ? location.getValidRadiusMeters() : 80;
            }
        }

        // 若服务开启了时间段档期预约，原子占用档期名额
        LocalDateTime scheduledStart = request.getScheduledStart();
        LocalDateTime scheduledEnd = null;
        if (Integer.valueOf(1).equals(service.getRequireSlot()) || request.getSlotId() != null) {
            if (request.getSlotId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该技能服务需选择预约档期时段");
            }
            if (serviceTimeSlotMapper != null) {
                ServiceTimeSlot slot = serviceTimeSlotMapper.selectById(request.getSlotId());
                if (slot == null || !Objects.equals(slot.getServiceId(), serviceId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "预约档期不存在");
                }
                int occupied = serviceTimeSlotMapper.occupySlotIfAvailable(request.getSlotId(), quantity);
                if (occupied == 0) {
                    throw new BusinessException(ErrorCode.SERVICE_SLOT_FULL);
                }
                scheduledStart = slot.getSlotStart();
                scheduledEnd = slot.getSlotEnd();
            }
        }

        ServiceOrder order = new ServiceOrder();
        BeanUtils.copyProperties(request, order);
        order.setCampusId(campusId);
        order.setTradeType(effectiveTradeType);
        order.setTradeLocationId(effectiveLocationId);
        order.setScheduledStart(scheduledStart);
        order.setScheduledEnd(scheduledEnd);
        order.setMeetupLat(meetupLat);
        order.setMeetupLng(meetupLng);
        order.setMeetupPoiName(meetupPoiName);
        order.setMeetupAddress(meetupAddress);
        order.setMeetupRadiusMeters(meetupRadiusMeters != null ? Math.max(10, Math.min(2000, meetupRadiusMeters)) : 80);
        order.setMeetupConfirmed(0);
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

        ServiceOrder order = lockOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能支付自己的订单");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待支付");
        }

        int stockUpdated = serviceMapper.incrementPurchaseCountIfAvailable(order.getServiceId(), order.getQuantity());
        if (stockUpdated == 0) {
            throw new BusinessException(ErrorCode.SERVICE_SOLD_OUT, "服务库存不足，无法支付");
        }

        User buyer = userMapper.selectForUpdate(userId);
        BigDecimal amount = order.getTotalAmount();
        if (buyer.getWalletBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.ORDER_BALANCE_NOT_ENOUGH);
        }

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

        order.setFrozenAmount(amount);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_SHIP);
        order.setPaymentStatus(OrderConstant.PAYMENT_PAID);
        // 钱包支付强制记录为钱包支付方式，防止客户端篡改 paymentMethod=2 导致退款时跳过退回余额
        order.setPaymentMethod(OrderConstant.PAYMENT_METHOD_WALLET);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaidByEscrowGateway(Long orderId, Long buyerId, String payChannel, String paymentNo) {
        ServiceOrder order = lockOrder(orderId);
        if (Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_PENDING_SHIP)
                && Objects.equals(order.getPaymentStatus(), OrderConstant.PAYMENT_PAID)) {
            return;
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态异常，无法入账托管款");
        }

        int stockUpdated = serviceMapper.incrementPurchaseCountIfAvailable(order.getServiceId(), order.getQuantity());
        if (stockUpdated == 0) {
            throw new BusinessException(ErrorCode.SERVICE_SOLD_OUT, "服务库存不足，无法完成托管入账");
        }

        User buyer = userMapper.selectForUpdate(buyerId);
        BigDecimal amount = order.getTotalAmount();
        BigDecimal balanceBefore = buyer.getWalletBalance() != null ? buyer.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal frozenBefore = buyer.getFrozenAmount() != null ? buyer.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal newFrozen = frozenBefore.add(amount);

        // 第三方担保直付：资金直接进入买方担保冻结账目，不污染可用充值余额池
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, buyerId)
                .set(User::getFrozenAmount, newFrozen));

        recordOrderTransaction(buyerId, WalletConstant.TRANSACTION_TYPE_FREEZE, amount,
                balanceBefore, balanceBefore, frozenBefore, newFrozen,
                order.getId(), payChannel + " 单笔担保托管冻结（单号 " + paymentNo + "）");

        order.setFrozenAmount(amount);
        order.setOrderStatus(OrderConstant.ORDER_PENDING_SHIP);
        order.setPaymentStatus(OrderConstant.PAYMENT_PAID);
        order.setPaymentMethod(2);
        order.setPaymentTime(LocalDateTime.now());
        if (Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(order.getTradeType())) {
            order.setVerifyCode(OfflineVerifyCodeUtils.generateOrderSeed());
        }
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_PENDING_SHIP,
                OrderLogConstant.ACTION_ESCROW_PAY,
                "买方通过 " + payChannel + " 完成单笔担保支付（支付单 " + paymentNo + "），资金已托管冻结");

        evictServiceDetailCacheAfterCommit(order.getServiceId());
        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "新担保订单通知",
                String.format("买家 %s 已通过担保收银台支付订单 %s，金额 %.2f 元，请确认会面地点并履约",
                        buyer.getNickname(), order.getOrderNumber(), amount));
    }

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
        verifyOfflineCode(orderId, verifyCode, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyOfflineCode(Long orderId, String verifyCode, BigDecimal verifierLat, BigDecimal verifierLng) {
        Long userId = requireLogin();
        if (StringUtils.isBlank(verifyCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入6位动态核销码");
        }
        String inputCode = verifyCode.trim();

        String failKey = VERIFY_FAIL_KEY_PREFIX + orderId;
        checkVerifyRateLimit(failKey);

        ServiceOrder order = lockOrder(orderId);
        validateOfflineVerifyEligibility(order, userId);
        validateVerifierWithinLocationRadius(order, verifierLat, verifierLng);

        long nowEpoch = Instant.now().getEpochSecond();
        boolean matched = OfflineVerifyCodeUtils.verifyCode(
                order.getId(), order.getVerifyCode(), getVerifyMasterSecret(), inputCode, nowEpoch);
        if (!matched) {
            recordVerifyFailure(failKey);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动态核销码不正确或已过期（每30秒自动刷新），请核对买家最新6位数字");
        }

        order.setVerifyChannel("TOTP_30S");
        finishOfflineVerification(order, failKey,
                "校内当面 30s 动态核销码验证通过，当面交接完成并结算资金",
                "动态核销码");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long verifyOfflineQr(String qrToken, Long expectedOrderId) {
        return verifyOfflineQr(qrToken, expectedOrderId, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long verifyOfflineQr(String qrToken, Long expectedOrderId, BigDecimal verifierLat, BigDecimal verifierLng) {
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
        validateVerifierWithinLocationRadius(order, verifierLat, verifierLng);

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

        order.setVerifyChannel("QR_10S");
        finishOfflineVerification(order, failKey,
                "校内当面 10s 动态二维码实时扫码核销通过，当面交接完成并结算资金",
                "现场动态二维码");
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOfflineOrderByBle(Long orderId, Integer rssiDbm, BigDecimal distanceMeters) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持蓝牙核销结单");
        }
        if (order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单存在待处理的退款申请，请先协商处理");
        }
        if (order.getSellerShipTime() == null) {
            order.setSellerShipTime(LocalDateTime.now());
        }
        order.setVerifyChannel("BLE_CRYPTO");
        closeActiveDisputesForOrder(order.getId(), "低功耗蓝牙近场加密碰面核销通过，关联纠纷自动结案");

        String distInfo = distanceMeters != null ? String.format("，距约定点 %.1f 米", distanceMeters) : "";
        completeOrder(order, OrderLogConstant.ACTION_BLE_VERIFY,
                String.format("低功耗蓝牙(BLE)双端加密挑战签名碰面核销通过（RSSI %d dBm%s），资金已分账结算", rssiDbm, distInfo));

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "蓝牙近场碰面核销完成",
                String.format("您的线下订单 %s 已通过低功耗蓝牙近场加密感应完成交接结算", order.getOrderNumber()));
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

    private void validateVerifierWithinLocationRadius(ServiceOrder order, BigDecimal verifierLat, BigDecimal verifierLng) {
        BigDecimal targetLat = order.getMeetupLat();
        BigDecimal targetLng = order.getMeetupLng();
        Integer allowedRadius = order.getMeetupRadiusMeters();
        String spotName = order.getMeetupPoiName();

        if ((targetLat == null || targetLng == null) && order.getTradeLocationId() != null && tradeLocationService != null) {
            TradeLocation loc = tradeLocationService.getById(order.getTradeLocationId());
            if (loc != null) {
                targetLat = loc.getLatitude();
                targetLng = loc.getLongitude();
                if (allowedRadius == null) {
                    allowedRadius = loc.getValidRadiusMeters();
                }
                if (StringUtils.isBlank(spotName)) {
                    spotName = loc.getLocationName();
                }
            }
        }
        if (targetLat != null && targetLng != null) {
            if (verifierLat == null || verifierLng == null) {
                throw new BusinessException(ErrorCode.GEOFENCE_OUT_OF_BOUNDS,
                        "该线下订单已设定交接电子围栏，请先授权获取定位后再进行当面核销");
            }
            if (!GeofenceUtils.isValidCoordinate(verifierLat.doubleValue(), verifierLng.doubleValue())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "上报的核销定位坐标无效");
            }
            int maxMeters = (allowedRadius != null && allowedRadius > 0)
                    ? allowedRadius
                    : (sysConfigService != null ? sysConfigService.getIntValue(SysConfigConstant.MEETUP_GEOFENCE_TOLERANCE_METERS, 150) : 150);
            double dist = GeofenceUtils.distanceMeters(
                    targetLat.doubleValue(), targetLng.doubleValue(),
                    verifierLat.doubleValue(), verifierLng.doubleValue()
            );
            if (dist > maxMeters) {
                throw new BusinessException(ErrorCode.GEOFENCE_OUT_OF_BOUNDS,
                        String.format("超出约定交接地点有效范围：当前距「%s」约 %.0f 米，需进入 %d 米范围内当面核销",
                                StringUtils.defaultIfBlank(spotName, "约定交接点"), dist, maxMeters));
            }
        } else if (verifierLat != null && verifierLng != null
                && !GeofenceUtils.isValidCoordinate(verifierLat.doubleValue(), verifierLng.doubleValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上报的核销定位坐标无效");
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

        if (order.getVerifyChannel() == null) {
            order.setVerifyChannel("ONLINE");
        }
        closeActiveDisputesForOrder(order.getId(), "买方已主动确认收货并完成担保结算，平台纠纷自动结案");
        completeOrder(order, OrderLogConstant.ACTION_BUYER_CONFIRM, "买家确认收货，交易完成，资金已结算给卖家");

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "交易完成通知",
                String.format("买家 %s 已确认收货，订单 %s 交易完成，净收入 %.2f 元已结算到您的钱包",
                        getNickname(userId), order.getOrderNumber(),
                        order.getSellerSettleAmount() != null ? order.getSellerSettleAmount() : order.getTotalAmount()));
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

        if (order.getSlotId() != null && serviceTimeSlotMapper != null) {
            serviceTimeSlotMapper.releaseSlot(order.getSlotId(), order.getQuantity() != null ? order.getQuantity() : 1);
        }
        releaseGroupSessionSeatIfApplicable(order.getId());

        order.setOrderStatus(OrderConstant.ORDER_CANCELLED);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_CANCELLED,
                OrderLogConstant.ACTION_CANCEL, "买家取消未支付订单");
    }

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
            processRefund(order, reason, OrderLogConstant.ACTION_REFUND, "买家在发货前申请退款，系统自动退款");
            return;
        }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefundOrder(Long orderId, String reason) {
        ServiceOrder order = lockOrder(orderId);

        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持退款");
        }

        processRefund(order, reason, OrderLogConstant.ACTION_ADMIN_REFUND, "管理员强制退款：" + reason);

        // 纠纷败诉强制退款扣减卖家信用分 5 分
        if (creditScoreService != null) {
            creditScoreService.adjustCreditScore(order.getSellerId(), -5, "DISPUTE_LOST",
                    "订单仲裁强制退款扣除履约信用分", order.getId());
        }

        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "订单退款通知",
                String.format("订单 %s 已由管理员处理退款，金额 %.2f 元已退回买家。原因：%s",
                        order.getOrderNumber(), order.getTotalAmount(), reason));
    }

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
        if (!UserContext.isAdmin() && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该订单");
        }

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);

        Service service = serviceService.getById(order.getServiceId());
        if (service != null) {
            vo.setServiceTitle(service.getServiceTitle());
            vo.setServiceDescription(service.getServiceDescription());
            vo.setRequireBleVerify(service.getRequireBleVerify());
        }
        vo.setServiceImage(serviceImageService.getCoverImage(order.getServiceId()));

        Map<Long, User> users = toMap(userService.listByIds(Arrays.asList(order.getBuyerId(), order.getSellerId())), User::getId);
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
                if (vo.getMeetupPoiName() == null) {
                    vo.setMeetupPoiName(location.getLocationName());
                    vo.setMeetupLat(location.getLatitude());
                    vo.setMeetupLng(location.getLongitude());
                }
            }
        } else if (vo.getMeetupPoiName() != null) {
            vo.setTradeLocationName(vo.getMeetupPoiName());
        }

        vo.setDeliveryImages(parsePublicUrlList(order.getDeliveryImages()));
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

        // 履约完成双向奖励信用分 +2（上限120）
        if (creditScoreService != null) {
            creditScoreService.adjustCreditScore(order.getSellerId(), 2, "ORDER_COMPLETE",
                    "按约完成担保服务履约奖励", order.getId());
            creditScoreService.adjustCreditScore(order.getBuyerId(), 1, "ORDER_COMPLETE",
                    "诚信验收担保订单奖励", order.getId());
        }

        evictServiceDetailCacheAfterCommit(order.getServiceId());
        if (redisUtils != null) {
            Long sellerId = order.getSellerId();
            TransactionUtils.afterCommit(() -> redisUtils.delete(CacheConstant.STATISTICS_USER_SALES_PREFIX + sellerId));
        }
    }

    private void processRefund(ServiceOrder order, String reason, String action, String logMessage) {
        BigDecimal refundAmount = order.getFrozenAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单没有可退款的冻结金额");
        }
        Integer previousStatus = order.getOrderStatus();

        User buyer = userMapper.selectForUpdate(order.getBuyerId());
        if (buyer.getFrozenAmount().compareTo(refundAmount) < 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "买家冻结金额不足");
        }
        BigDecimal balanceBefore = buyer.getWalletBalance();
        BigDecimal frozenBefore = buyer.getFrozenAmount();
        BigDecimal newFrozen = frozenBefore.subtract(refundAmount);
        boolean isEscrowGateway = Integer.valueOf(2).equals(order.getPaymentMethod());
        // 第三方担保直付退款仅核减托管冻结并原路退回支付渠道，严禁将渠道资金误入账为可提现钱包余额
        BigDecimal newBalance = isEscrowGateway ? balanceBefore : balanceBefore.add(refundAmount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, order.getBuyerId())
                .set(User::getFrozenAmount, newFrozen)
                .set(User::getWalletBalance, newBalance));

        recordOrderTransaction(order.getBuyerId(), WalletConstant.TRANSACTION_TYPE_UNFREEZE, refundAmount,
                balanceBefore, balanceBefore, frozenBefore, newFrozen,
                order.getId(), "退款解冻订单资金 " + order.getOrderNumber());
        recordOrderTransaction(order.getBuyerId(), WalletConstant.TRANSACTION_TYPE_REFUND, refundAmount,
                balanceBefore, newBalance, newFrozen, newFrozen,
                order.getId(), (isEscrowGateway ? "第三方担保支付原路退回 " : "订单退款 ") + order.getOrderNumber());

        if (isEscrowGateway && escrowPaymentOrderMapper != null) {
            escrowPaymentOrderMapper.update(null, new LambdaUpdateWrapper<EscrowPaymentOrder>()
                    .eq(EscrowPaymentOrder::getOrderId, order.getId())
                    .eq(EscrowPaymentOrder::getPayStatus, 1)
                    .set(EscrowPaymentOrder::getPayStatus, 3)
                    .set(EscrowPaymentOrder::getUpdateTime, LocalDateTime.now()));
        }

        serviceMapper.decrementPurchaseCount(order.getServiceId(), order.getQuantity());
        if (order.getSlotId() != null && serviceTimeSlotMapper != null) {
            serviceTimeSlotMapper.releaseSlot(order.getSlotId(), order.getQuantity() != null ? order.getQuantity() : 1);
        }
        releaseGroupSessionSeatIfApplicable(order.getId());

        order.setPaymentStatus(OrderConstant.PAYMENT_REFUNDED);
        order.setOrderStatus(OrderConstant.ORDER_FAILED);
        order.setFrozenAmount(BigDecimal.ZERO);
        order.setRefundStatus(OrderConstant.REFUND_COMPLETED);
        if (reason != null && !reason.trim().isEmpty()) {
            order.setRefundReason(reason);
        }
        this.updateById(order);

        orderLogService.recordLog(order.getId(), previousStatus, OrderConstant.ORDER_FAILED, action, logMessage);

        if (!OrderLogConstant.ACTION_ADMIN_REFUND.equals(action)) {
            closeActiveDisputesForOrder(order.getId(), "订单已完成全额退款，关联纠纷自动结案");
        }

        evictServiceDetailCacheAfterCommit(order.getServiceId());
        String refundChannelDesc = isEscrowGateway ? "已原路退回您的原支付渠道" : "已退回您的钱包账户";
        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "退款成功通知",
                String.format("订单 %s（服务：%s）退款成功，金额 %.2f 元%s",
                        order.getOrderNumber(), getServiceTitle(order.getServiceId()), refundAmount, refundChannelDesc));
    }

    /**
     * 释放冻结资金给卖家并执行商业化平台抽佣分账（严格按 userId 升序加锁防死锁）
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

        // 计算平台技术服务费（基点制，例如 500 bps = 5.00%；未配置或单测默认 0 时全额结算）
        int commissionBps = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.PLATFORM_COMMISSION_BPS, 0)
                : 0;
        commissionBps = Math.max(0, Math.min(3000, commissionBps));
        BigDecimal platformFee = BigDecimal.ZERO;
        if (commissionBps > 0) {
            platformFee = amount.multiply(BigDecimal.valueOf(commissionBps))
                    .divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
        }
        BigDecimal sellerNet = amount.subtract(platformFee);

        BigDecimal existingFee = order.getPlatformFee() != null ? order.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal existingSettle = order.getSellerSettleAmount() != null ? order.getSellerSettleAmount() : BigDecimal.ZERO;
        order.setPlatformFee(existingFee.add(platformFee));
        order.setSellerSettleAmount(existingSettle.add(sellerNet));

        BigDecimal buyerBalanceBefore = buyer.getWalletBalance();
        BigDecimal buyerFrozenBefore = buyer.getFrozenAmount();
        BigDecimal sellerBalanceBefore = seller.getWalletBalance();
        BigDecimal sellerFrozenBefore = seller.getFrozenAmount();

        BigDecimal buyerFrozenAfter = buyerFrozenBefore.subtract(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, buyerId)
                .set(User::getFrozenAmount, buyerFrozenAfter));

        BigDecimal sellerBalanceAfter = sellerBalanceBefore.add(sellerNet);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, sellerId)
                .set(User::getWalletBalance, sellerBalanceAfter));

        recordOrderTransaction(buyerId, WalletConstant.TRANSACTION_TYPE_UNFREEZE, amount,
                buyerBalanceBefore, buyerBalanceBefore, buyerFrozenBefore, buyerFrozenAfter,
                order.getId(), "确认收货解冻资金 " + order.getOrderNumber());
        recordOrderTransaction(sellerId, WalletConstant.TRANSACTION_TYPE_INCOME, sellerNet,
                sellerBalanceBefore, sellerBalanceAfter, sellerFrozenBefore, sellerFrozenBefore,
                order.getId(), platformFee.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("订单分账净收入 %s（已扣平台服务费 %.2f 元）", order.getOrderNumber(), platformFee)
                        : "订单收入 " + order.getOrderNumber());

        if (platformFee.compareTo(BigDecimal.ZERO) > 0 && platformRevenueLedgerMapper != null) {
            PlatformRevenueLedger ledger = new PlatformRevenueLedger();
            ledger.setLedgerNo(SerialNoGenerator.nextRevenueLedgerNo(order.getId()));
            ledger.setOrderId(order.getId());
            ledger.setSellerId(sellerId);
            ledger.setOrderTotalAmount(amount);
            ledger.setCommissionBps(commissionBps);
            ledger.setCommissionAmount(platformFee);
            ledger.setSellerNetAmount(sellerNet);
            ledger.setCreateTime(LocalDateTime.now());
            platformRevenueLedgerMapper.insert(ledger);
        }
    }

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
            return;
        }
        if (order.getSlotId() != null && serviceTimeSlotMapper != null) {
            serviceTimeSlotMapper.releaseSlot(order.getSlotId(), order.getQuantity() != null ? order.getQuantity() : 1);
        }
        releaseGroupSessionSeatIfApplicable(order.getId());
        order.setOrderStatus(OrderConstant.ORDER_CANCELLED);
        this.updateById(order);

        orderLogService.recordLog(order.getId(), OrderConstant.ORDER_UNPAID, OrderConstant.ORDER_CANCELLED,
                OrderLogConstant.ACTION_AUTO_CANCEL,
                String.format("系统自动取消：订单超过%d小时未支付", timeoutHours));

        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "订单自动取消通知",
                String.format("订单 %s 因超过 %d 小时未支付，已被系统自动取消", order.getOrderNumber(), timeoutHours));
    }

    private void releaseGroupSessionSeatIfApplicable(Long orderId) {
        if (groupSessionParticipantMapper == null || groupSessionMapper == null || orderId == null) {
            return;
        }
        GroupSessionParticipant participant = groupSessionParticipantMapper.selectOne(
                new LambdaQueryWrapper<GroupSessionParticipant>()
                        .eq(GroupSessionParticipant::getOrderId, orderId)
                        .eq(GroupSessionParticipant::getJoinStatus, 1)
                        .last("LIMIT 1"));
        if (participant == null) {
            return;
        }
        participant.setJoinStatus(0);
        participant.setUpdateTime(LocalDateTime.now());
        groupSessionParticipantMapper.updateById(participant);

        GroupSession session = groupSessionMapper.selectForUpdate(participant.getSessionId());
        if (session == null) {
            return;
        }
        int currentJoined = session.getJoinedCount() != null ? session.getJoinedCount() : 0;
        int updatedJoined = Math.max(0, currentJoined - 1);
        session.setJoinedCount(updatedJoined);
        if (Objects.equals(session.getSessionStatus(), GroupSessionServiceImpl.STATUS_FULL)
                || Objects.equals(session.getSessionStatus(), GroupSessionServiceImpl.STATUS_FORMED)) {
            int minP = session.getMinParticipants() != null ? session.getMinParticipants() : 2;
            session.setSessionStatus(updatedJoined >= minP
                    ? GroupSessionServiceImpl.STATUS_FORMED
                    : GroupSessionServiceImpl.STATUS_RECRUITING);
        }
        session.setUpdateTime(LocalDateTime.now());
        groupSessionMapper.updateById(session);
    }

    private void processAutoConfirmReceipt(Long orderId, int timeoutDays) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT
                || order.getRefundStatus() == OrderConstant.REFUND_PENDING) {
            return;
        }
        if (hasActiveDispute(orderId)) {
            return;
        }

        completeOrder(order, OrderLogConstant.ACTION_AUTO_CONFIRM_RECEIPT,
                String.format("系统自动确认收货：发货超过%d天", timeoutDays));

        String serviceTitle = getServiceTitle(order.getServiceId());
        notificationService.sendOrderNotification(order.getBuyerId(), order.getId(), "订单自动确认收货",
                String.format("订单 %s（服务：%s）发货超过%d天，系统已自动确认收货",
                        order.getOrderNumber(), serviceTitle, timeoutDays));
        notificationService.sendOrderNotification(order.getSellerId(), order.getId(), "订单完成通知",
                String.format("订单 %s 系统自动确认收货，净收入 %.2f 元已结算到您的钱包",
                        order.getOrderNumber(),
                        order.getSellerSettleAmount() != null ? order.getSellerSettleAmount() : order.getTotalAmount()));
    }

    private void processAutoRefund(Long orderId, int timeoutDays) {
        ServiceOrder order = lockOrder(orderId);
        if (order.getRefundStatus() != OrderConstant.REFUND_PENDING) {
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存交易流水失败");
        }
    }
}
