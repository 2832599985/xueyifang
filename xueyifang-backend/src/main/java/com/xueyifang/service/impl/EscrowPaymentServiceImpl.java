package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.wallet.EscrowPrepayRequest;
import com.xueyifang.domain.dto.wallet.EscrowWebhookRequest;
import com.xueyifang.domain.po.EscrowPaymentOrder;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.vo.EscrowPrepayVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.EscrowPaymentOrderMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.service.IEscrowPaymentService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.utils.BleMeetupCryptoUtils;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 商业化单笔担保支付网关服务（对接微信小程序支付 / 微信App支付 / 支付宝）
 * <p>
 * 彻底替代无支付牌照下的“虚拟预充值资金池”，实现合规的单笔订单担保支付：
 * 1. 客户端调用 prepay 生成支付单并获取微信/支付宝客户端唤起签名参数；
 * 2. 支付渠道异步回调（或开发联调环境携带 HMAC 验签回调）幂等锁定支付单并冻结订单托管款。
 */
@Service
@Slf4j
public class EscrowPaymentServiceImpl extends ServiceImpl<EscrowPaymentOrderMapper, EscrowPaymentOrder>
        implements IEscrowPaymentService {

    public static final int PAY_STATUS_PENDING = 0;
    public static final int PAY_STATUS_PAID = 1;

    @Value("${payment.webhook-secret:xueyifang-escrow-webhook-secret}")
    private String webhookSecret;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EscrowPrepayVO createEscrowPrepay(EscrowPrepayRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        ServiceOrder order = serviceOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(order.getBuyerId(), currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅买方本人可发起订单支付");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_UNPAID)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单不是待支付状态");
        }

        String channel = StringUtils.defaultIfBlank(request.getPayChannel(), "WECHAT_MINI").toUpperCase();
        String paymentNo = SerialNoGenerator.nextPaymentNo(order.getId());
        BigDecimal amount = order.getTotalAmount();

        EscrowPaymentOrder payOrder = new EscrowPaymentOrder();
        payOrder.setPaymentNo(paymentNo);
        payOrder.setOrderId(order.getId());
        payOrder.setUserId(currentUserId);
        payOrder.setPayChannel(channel);
        payOrder.setAmount(amount);
        payOrder.setPayStatus(PAY_STATUS_PENDING);
        payOrder.setCreateTime(LocalDateTime.now());
        payOrder.setUpdateTime(LocalDateTime.now());
        this.save(payOrder);

        long epochSec = Instant.now().getEpochSecond();
        String nonceStr = BleMeetupCryptoUtils.generateNonce();
        String prepayPkg = "prepay_id=wx_" + paymentNo;
        String signPayload = paymentNo + "|" + amount.toPlainString() + "|" + epochSec;
        String paySign = BleMeetupCryptoUtils.hmacSha256Hex(webhookSecret, signPayload);

        Map<String, String> sdkParams = new HashMap<>();
        sdkParams.put("timeStamp", String.valueOf(epochSec));
        sdkParams.put("nonceStr", nonceStr);
        sdkParams.put("package", prepayPkg);
        sdkParams.put("signType", "HMAC-SHA256");
        sdkParams.put("paySign", paySign);

        EscrowPrepayVO vo = new EscrowPrepayVO();
        vo.setPaymentNo(paymentNo);
        vo.setOrderId(order.getId());
        vo.setPayChannel(channel);
        vo.setAmount(amount);
        vo.setSdkPayParams(sdkParams);
        if (!"prod".equalsIgnoreCase(activeProfile)) {
            vo.setSandboxCallbackSign(computeWebhookSign(paymentNo, amount));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleVerifiedWebhook(EscrowWebhookRequest webhook) {
        if (webhook == null || StringUtils.isAnyBlank(webhook.getPaymentNo(), webhook.getSignature())
                || webhook.getPaidAmount() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付回调参数缺失");
        }
        EscrowPaymentOrder payOrder = this.baseMapper.selectByPaymentNoForUpdate(webhook.getPaymentNo());
        if (payOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "支付单不存在");
        }
        // 幂等放行：第三方支付网关重复通知直接返回成功
        if (Objects.equals(payOrder.getPayStatus(), PAY_STATUS_PAID)) {
            return;
        }
        if (payOrder.getAmount().compareTo(webhook.getPaidAmount()) != 0) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_ERROR, "回调实付金额与订单应付金额不一致");
        }
        String expectedSign = computeWebhookSign(payOrder.getPaymentNo(), payOrder.getAmount());
        if (!BleMeetupCryptoUtils.constantTimeEquals(expectedSign, webhook.getSignature().trim().toLowerCase())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "支付回调签名核验失败");
        }

        payOrder.setPayStatus(PAY_STATUS_PAID);
        payOrder.setChannelTradeNo(StringUtils.defaultIfBlank(webhook.getChannelTradeNo(), "WX_" + payOrder.getPaymentNo()));
        payOrder.setPaidTime(LocalDateTime.now());
        payOrder.setCallbackDigest(expectedSign);
        payOrder.setUpdateTime(LocalDateTime.now());
        this.updateById(payOrder);

        serviceOrderService.markOrderPaidByEscrowGateway(
                payOrder.getOrderId(),
                payOrder.getUserId(),
                payOrder.getPayChannel(),
                payOrder.getPaymentNo()
        );
    }

    public String computeWebhookSign(String paymentNo, BigDecimal amount) {
        String normalized = amount.stripTrailingZeros().toPlainString();
        return BleMeetupCryptoUtils.hmacSha256Hex(webhookSecret, "ESCROW_WEBHOOK|" + paymentNo + "|" + normalized);
    }
}
