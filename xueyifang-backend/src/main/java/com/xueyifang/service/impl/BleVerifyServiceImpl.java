package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.domain.dto.BleVerifySubmitRequest;
import com.xueyifang.domain.po.BleVerifyRecord;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.vo.BleSessionVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.BleVerifyRecordMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.service.IBleVerifyService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.utils.BleMeetupCryptoUtils;
import com.xueyifang.utils.GeofenceUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 低功耗蓝牙（BLE）近场碰面双向挑战-应答加密核销服务实现
 */
@Service
@Slf4j
public class BleVerifyServiceImpl extends ServiceImpl<BleVerifyRecordMapper, BleVerifyRecord> implements IBleVerifyService {

    public static final String BLE_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB";
    public static final String BLE_CHAR_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB";
    private static final String REDIS_BLE_SESSION_PREFIX = "order:ble:sess:";
    private static final int SESSION_TTL_SECONDS = 90;
    private static final int DEFAULT_MIN_RSSI_DBM = -78;

    @Value("${jwt.secret:xueyifang-dev-jwt-secret-change-in-production}")
    private String verifyMasterSecret;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public BleSessionVO initOrJoinBleSession(Long orderId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        boolean isBuyer = Objects.equals(currentUserId, order.getBuyerId());
        boolean isSeller = Objects.equals(currentUserId, order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非该订单交易参与方");
        }
        if (!Objects.equals(order.getTradeType(), OrderConstant.TRADE_TYPE_OFFLINE)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅线下当面履约订单支持蓝牙近场碰面核销");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_PENDING_SHIP)
                && !Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_PENDING_RECEIPT)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不可进行蓝牙当面核销");
        }

        String redisKey = REDIS_BLE_SESSION_PREFIX + orderId;
        String cached = stringRedisTemplate != null ? stringRedisTemplate.opsForValue().get(redisKey) : null;

        String sessionId;
        String advShortToken;
        String buyerNonce;
        if (StringUtils.isNotBlank(cached) && cached.split("\\|").length == 3) {
            String[] parts = cached.split("\\|");
            sessionId = parts[0];
            advShortToken = parts[1];
            buyerNonce = parts[2];
        } else {
            sessionId = BleMeetupCryptoUtils.generateSessionId();
            advShortToken = BleMeetupCryptoUtils.generateShortAdvToken();
            buyerNonce = BleMeetupCryptoUtils.generateNonce();
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(
                        redisKey,
                        sessionId + "|" + advShortToken + "|" + buyerNonce,
                        SESSION_TTL_SECONDS,
                        TimeUnit.SECONDS
                );
            }
        }

        String role = isBuyer ? BleMeetupCryptoUtils.ROLE_BUYER : BleMeetupCryptoUtils.ROLE_SELLER;
        String roleSigningKey = BleMeetupCryptoUtils.deriveRoleSigningKey(verifyMasterSecret, orderId, sessionId, role);
        int minRssi = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.BLE_MIN_RSSI_DBM, DEFAULT_MIN_RSSI_DBM)
                : DEFAULT_MIN_RSSI_DBM;

        BleSessionVO vo = new BleSessionVO();
        vo.setOrderId(orderId);
        vo.setSessionId(sessionId);
        vo.setAdvShortToken(advShortToken);
        vo.setServiceUuid(BLE_SERVICE_UUID);
        vo.setCharacteristicUuid(BLE_CHAR_UUID);
        vo.setBuyerNonce(buyerNonce);
        vo.setRoleSigningKey(roleSigningKey);
        vo.setRole(role);
        vo.setMinRssiDbm(minRssi);
        vo.setExpireSeconds(SESSION_TTL_SECONDS);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBleHandshakeAndSettle(Long orderId, BleVerifySubmitRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (orderId == null || request == null
                || StringUtils.isAnyBlank(
                        request.getSessionId(),
                        request.getBuyerNonce(),
                        request.getSellerNonce(),
                        request.getBuyerSignature(),
                        request.getSellerSignature())
                || request.getRssiDbm() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "蓝牙近场握手签名参数不完整");
        }

        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(currentUserId, order.getSellerId()) && !Objects.equals(currentUserId, order.getBuyerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权提交该订单核销凭证");
        }

        // 1. 校验物理蓝牙信号强度 RSSI（过滤远程中继与非近场信号）
        int minRssi = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.BLE_MIN_RSSI_DBM, DEFAULT_MIN_RSSI_DBM)
                : DEFAULT_MIN_RSSI_DBM;
        if (request.getRssiDbm() < minRssi || request.getRssiDbm() > 0) {
            throw new BusinessException(ErrorCode.BLE_VERIFY_FAILED,
                    String.format("蓝牙近场信号过弱（当前 %d dBm，需不低于 %d dBm），请将两部手机靠近至 3 米内重试",
                            request.getRssiDbm(), minRssi));
        }

        // 2. 校验 Redis 一次性挑战会话并立即销毁防重放
        String redisKey = REDIS_BLE_SESSION_PREFIX + orderId;
        if (stringRedisTemplate != null) {
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(cached)) {
                throw new BusinessException(ErrorCode.BLE_VERIFY_FAILED, "蓝牙挑战会话已过期或已被使用，请重新开启近场感应");
            }
            String[] parts = cached.split("\\|");
            if (parts.length != 3 || !parts[0].equals(request.getSessionId()) || !parts[2].equals(request.getBuyerNonce())) {
                throw new BusinessException(ErrorCode.BLE_VERIFY_FAILED, "蓝牙挑战随机数不匹配，疑似重放请求");
            }
            stringRedisTemplate.delete(redisKey);
        }

        // 3. 恒定时间验证买方设备签名 + 卖方设备签名（确保双方真实客户端均参与了近场 Nonce 交换）
        boolean buyerOk = BleMeetupCryptoUtils.verifyHandshakeSignature(
                verifyMasterSecret, orderId, request.getSessionId(),
                request.getBuyerNonce(), request.getSellerNonce(),
                BleMeetupCryptoUtils.ROLE_BUYER, request.getBuyerSignature()
        );
        boolean sellerOk = BleMeetupCryptoUtils.verifyHandshakeSignature(
                verifyMasterSecret, orderId, request.getSessionId(),
                request.getBuyerNonce(), request.getSellerNonce(),
                BleMeetupCryptoUtils.ROLE_SELLER, request.getSellerSignature()
        );
        if (!buyerOk || !sellerOk) {
            throw new BusinessException(ErrorCode.BLE_VERIFY_FAILED, "双端加密挑战签名校验未通过，严禁伪造碰面数据");
        }

        // 4. 联合校验实时 GPS 与约定会面坐标物理误差（若订单已锁定地图坐标且设备上报了定位）
        BigDecimal distMeters = null;
        if (order.getMeetupLat() != null && order.getMeetupLng() != null
                && request.getSellerLat() != null && request.getSellerLng() != null) {
            double d = GeofenceUtils.distanceMeters(
                    order.getMeetupLat().doubleValue(), order.getMeetupLng().doubleValue(),
                    request.getSellerLat().doubleValue(), request.getSellerLng().doubleValue()
            );
            distMeters = BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
        }

        // 5. 落库防篡改蓝牙握手密码学存证
        BleVerifyRecord record = new BleVerifyRecord();
        record.setOrderId(orderId);
        record.setSessionId(request.getSessionId());
        record.setBuyerId(order.getBuyerId());
        record.setSellerId(order.getSellerId());
        record.setBuyerNonce(request.getBuyerNonce());
        record.setSellerNonce(request.getSellerNonce());
        record.setRssiDbm(request.getRssiDbm());
        record.setBuyerLat(request.getBuyerLat());
        record.setBuyerLng(request.getBuyerLng());
        record.setSellerLat(request.getSellerLat());
        record.setSellerLng(request.getSellerLng());
        record.setDistanceToMeetupM(distMeters);
        record.setBuyerSig(request.getBuyerSignature());
        record.setSellerSig(request.getSellerSignature());
        record.setCreateTime(LocalDateTime.now());
        this.save(record);

        // 6. 触发订单行锁完结与分账放款
        serviceOrderService.completeOfflineOrderByBle(orderId, request.getRssiDbm(), distMeters);
    }
}
