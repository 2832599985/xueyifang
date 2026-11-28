package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.RiskAlertQueryRequest;
import com.xueyifang.domain.dto.RiskAlertResolveRequest;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.po.RiskAuditAlert;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.RiskAuditAlertVO;
import com.xueyifang.domain.vo.RiskRadarOverviewVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.CreditScoreLogMapper;
import com.xueyifang.mapper.NotificationMapper;
import com.xueyifang.mapper.RiskAuditAlertMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IRiskRadarService;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.TransactionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RiskRadarServiceImpl implements IRiskRadarService {

    public static final String RULE_BIDIRECTIONAL_WASH = "BIDIRECTIONAL_WASH_TRADE";
    public static final String RULE_HIGH_FREQ_PAIR = "HIGH_FREQ_SAME_PAIR";
    public static final String RULE_ULTRA_FAST = "ULTRA_FAST_COMPLETION";
    public static final String RULE_LOW_PRICE_BRUSH = "LOW_PRICE_SCORE_BRUSHING";

    @Resource
    private RiskAuditAlertMapper riskAuditAlertMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CreditScoreLogMapper creditScoreLogMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public RiskRadarOverviewVO getRadarOverview() {
        List<RiskAuditAlert> allAlerts = riskAuditAlertMapper.selectList(
                new LambdaQueryWrapper<RiskAuditAlert>()
                        .select(RiskAuditAlert::getId, RiskAuditAlert::getHandleStatus,
                                RiskAuditAlert::getSeverity, RiskAuditAlert::getRuleCode)
                        .orderByDesc(RiskAuditAlert::getId)
                        .last("LIMIT 5000"));
        if (allAlerts == null) {
            allAlerts = Collections.emptyList();
        }

        int total = allAlerts.size();
        int pending = 0;
        int highSeverity = 0;
        int confirmed = 0;
        int dismissed = 0;
        int bidirectional = 0;
        int highFreq = 0;
        int ultraFast = 0;

        for (RiskAuditAlert a : allAlerts) {
            if (a.getHandleStatus() != null && a.getHandleStatus() == 0) {
                pending++;
            } else if (a.getHandleStatus() != null && a.getHandleStatus() == 1) {
                confirmed++;
            } else if (a.getHandleStatus() != null && a.getHandleStatus() == 2) {
                dismissed++;
            }
            if (a.getSeverity() != null && a.getSeverity() == 3) {
                highSeverity++;
            }
            if (RULE_BIDIRECTIONAL_WASH.equals(a.getRuleCode())) {
                bidirectional++;
            } else if (RULE_HIGH_FREQ_PAIR.equals(a.getRuleCode())) {
                highFreq++;
            } else if (RULE_ULTRA_FAST.equals(a.getRuleCode())) {
                ultraFast++;
            }
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<ServiceOrder> recentOrderQuery = new LambdaQueryWrapper<>();
        recentOrderQuery.eq(ServiceOrder::getIsDelete, 0)
                .ge(ServiceOrder::getCreateTime, sevenDaysAgo);
        Long recentCount = serviceOrderMapper.selectCount(recentOrderQuery);

        return new RiskRadarOverviewVO()
                .setTotalAlerts(total)
                .setPendingAlerts(pending)
                .setHighSeverityAlerts(highSeverity)
                .setConfirmedPenalties(confirmed)
                .setDismissedAlerts(dismissed)
                .setScannedOrders7d(recentCount != null ? recentCount.intValue() : 0)
                .setBidirectionalWashCount(bidirectional)
                .setHighFreqPairCount(highFreq)
                .setUltraFastCompletionCount(ultraFast);
    }

    @Override
    public Page<RiskAuditAlertVO> pageAlerts(RiskAlertQueryRequest request) {
        int pageNum = request != null && request.getPageNum() != null && request.getPageNum() > 0 ? request.getPageNum() : 1;
        int pageSize = request != null && request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 15;

        LambdaQueryWrapper<RiskAuditAlert> wrapper = new LambdaQueryWrapper<>();
        if (request != null) {
            if (StringUtils.hasText(request.getRuleCode())) {
                wrapper.eq(RiskAuditAlert::getRuleCode, request.getRuleCode());
            }
            if (request.getSeverity() != null) {
                wrapper.eq(RiskAuditAlert::getSeverity, request.getSeverity());
            }
            if (request.getHandleStatus() != null) {
                wrapper.eq(RiskAuditAlert::getHandleStatus, request.getHandleStatus());
            }
            if (StringUtils.hasText(request.getKeyword())) {
                String kw = request.getKeyword().trim();
                wrapper.and(w -> w.like(RiskAuditAlert::getAlertNo, kw)
                        .or().like(RiskAuditAlert::getOrderNumber, kw)
                        .or().like(RiskAuditAlert::getEvidenceSummary, kw));
            }
        }
        wrapper.orderByAsc(RiskAuditAlert::getHandleStatus)
                .orderByDesc(RiskAuditAlert::getSeverity)
                .orderByDesc(RiskAuditAlert::getCreateTime);

        Page<RiskAuditAlert> rawPage = riskAuditAlertMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<RiskAuditAlertVO> voPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        List<RiskAuditAlertVO> records = rawPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanRecentOrdersForRisk(int lookbackDays) {
        int days = lookbackDays > 0 ? lookbackDays : 7;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<ServiceOrder> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(ServiceOrder::getIsDelete, 0)
                .ge(ServiceOrder::getCreateTime, cutoff)
                .orderByDesc(ServiceOrder::getCreateTime)
                .last("LIMIT 300");

        List<ServiceOrder> recentOrders = serviceOrderMapper.selectList(orderQuery);
        if (recentOrders == null || recentOrders.isEmpty()) {
            return 0;
        }

        // 构建买卖对 (buyerId -> sellerId) 频次统计字典
        Map<String, Integer> pairCounts = new HashMap<>();
        for (ServiceOrder order : recentOrders) {
            if (order.getBuyerId() != null && order.getSellerId() != null) {
                String key = order.getBuyerId() + "_" + order.getSellerId();
                pairCounts.put(key, pairCounts.getOrDefault(key, 0) + 1);
            }
        }

        int newAlertsCreated = 0;
        for (ServiceOrder order : recentOrders) {
            if (order.getBuyerId() == null || order.getSellerId() == null) {
                continue;
            }
            String forwardKey = order.getBuyerId() + "_" + order.getSellerId();
            String reverseKey = order.getSellerId() + "_" + order.getBuyerId();
            int forwardCount = pairCounts.getOrDefault(forwardKey, 1);
            int reverseCount = pairCounts.getOrDefault(reverseKey, 0);

            newAlertsCreated += inspectOrderAndPersistAlerts(order, forwardCount, reverseCount).size();
        }
        return newAlertsCreated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RiskAuditAlertVO> evaluateOrderRisk(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        ServiceOrder targetOrder = serviceOrderMapper.selectById(orderId);
        if (targetOrder == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<ServiceOrder> pairQuery = new LambdaQueryWrapper<>();
        pairQuery.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getBuyerId, targetOrder.getBuyerId())
                .eq(ServiceOrder::getSellerId, targetOrder.getSellerId())
                .ge(ServiceOrder::getCreateTime, sevenDaysAgo);
        Long forwardCountLong = serviceOrderMapper.selectCount(pairQuery);
        int forwardCount = forwardCountLong != null ? Math.max(1, forwardCountLong.intValue()) : 1;

        LambdaQueryWrapper<ServiceOrder> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getBuyerId, targetOrder.getSellerId())
                .eq(ServiceOrder::getSellerId, targetOrder.getBuyerId())
                .ge(ServiceOrder::getCreateTime, sevenDaysAgo);
        Long reverseCountLong = serviceOrderMapper.selectCount(reverseQuery);
        int reverseCount = reverseCountLong != null ? reverseCountLong.intValue() : 0;

        List<RiskAuditAlert> created = inspectOrderAndPersistAlerts(targetOrder, forwardCount, reverseCount);
        return created.stream().map(this::toVO).collect(Collectors.toList());
    }

    private List<RiskAuditAlert> inspectOrderAndPersistAlerts(ServiceOrder order, int forwardCount7d, int reverseCount7d) {
        List<RiskAuditAlert> createdAlerts = new ArrayList<>();
        Long durationSec = null;
        if (order.getPaymentTime() != null && order.getBuyerConfirmTime() != null) {
            long diff = Duration.between(order.getPaymentTime(), order.getBuyerConfirmTime()).getSeconds();
            if (diff >= 0) {
                durationSec = diff;
            }
        }

        // 规则 1：双向角色互换对倒互刷 (A买B 且 B买A)
        if (reverseCount7d >= 1) {
            String evidence = String.format("检测到买卖双方近7日存在角色互换双向对倒交易：正向交易 %d 笔，反向对倒交易 %d 笔，高度疑似联合互刷校园履约信用分与成交量。",
                    forwardCount7d, reverseCount7d);
            RiskAuditAlert alert = createAlertIfNotExist(order, RULE_BIDIRECTIONAL_WASH, "买卖角色互换双向对倒互刷",
                    3, 92, forwardCount7d, reverseCount7d, durationSec, evidence);
            if (alert != null) {
                createdAlerts.add(alert);
            }
        }

        // 规则 2：同一买卖对短期高频重复交易 (7日内 >= 3笔)
        if (forwardCount7d >= 3) {
            int severity = forwardCount7d >= 4 ? 3 : 2;
            int score = Math.min(95, 70 + forwardCount7d * 5);
            String evidence = String.format("同一买家与卖家在近7日内高频重复下单达 %d 笔（当前订单金额 ¥%s），超出校园技能正常复购阈值。",
                    forwardCount7d, order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0.00");
            RiskAuditAlert alert = createAlertIfNotExist(order, RULE_HIGH_FREQ_PAIR, "同买卖对高频重复刷单预警",
                    severity, score, forwardCount7d, reverseCount7d, durationSec, evidence);
            if (alert != null) {
                createdAlerts.add(alert);
            }
        }

        // 规则 3：极速秒拍秒确异常履约 (支付到确认收货 < 180秒)
        if (durationSec != null && durationSec < 180) {
            String evidence = String.format("订单从支付成功到买方确认验收仅耗时 %d 秒（不足 3 分钟），缺乏真实技能交付耗时，疑似秒拍秒确刷单。",
                    durationSec);
            RiskAuditAlert alert = createAlertIfNotExist(order, RULE_ULTRA_FAST, "极速秒拍秒确异常履约预警",
                    2, 78, forwardCount7d, reverseCount7d, durationSec, evidence);
            if (alert != null) {
                createdAlerts.add(alert);
            }
        }

        // 规则 4：超低价完单刷信誉 (金额 <= 2.00 元且已完结)
        if (order.getTotalAmount() != null
                && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                && order.getTotalAmount().compareTo(new BigDecimal("2.00")) <= 0
                && order.getOrderStatus() != null && order.getOrderStatus() == OrderConstant.ORDER_COMPLETED) {
            String evidence = String.format("已完结订单实付金额仅为 ¥%s（≤ ¥2.00 异常微额阈值），疑似利用极低成本刷取好评与信用分。",
                    order.getTotalAmount().toPlainString());
            RiskAuditAlert alert = createAlertIfNotExist(order, RULE_LOW_PRICE_BRUSH, "超低价微额疑似刷分交易",
                    1, 64, forwardCount7d, reverseCount7d, durationSec, evidence);
            if (alert != null) {
                createdAlerts.add(alert);
            }
        }

        return createdAlerts;
    }

    private RiskAuditAlert createAlertIfNotExist(ServiceOrder order,
                                                 String ruleCode,
                                                 String ruleName,
                                                 int severity,
                                                 int riskScore,
                                                 int pairCount7d,
                                                 int reverseCount7d,
                                                 Long durationSec,
                                                 String evidence) {
        LambdaQueryWrapper<RiskAuditAlert> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(RiskAuditAlert::getOrderId, order.getId())
                .eq(RiskAuditAlert::getRuleCode, ruleCode);
        RiskAuditAlert existing = riskAuditAlertMapper.selectOne(existQuery);
        if (existing != null) {
            return null;
        }

        String alertNo = "RSK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        RiskAuditAlert alert = new RiskAuditAlert()
                .setAlertNo(alertNo)
                .setRuleCode(ruleCode)
                .setRuleName(ruleName)
                .setSeverity(severity)
                .setRiskScore(riskScore)
                .setOrderId(order.getId())
                .setOrderNumber(order.getOrderNumber())
                .setServiceId(order.getServiceId())
                .setBuyerId(order.getBuyerId())
                .setSellerId(order.getSellerId())
                .setCampusId(order.getCampusId() != null ? order.getCampusId() : 1L)
                .setOrderAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .setPairOrderCount7d(pairCount7d)
                .setReverseOrderCount7d(reverseCount7d)
                .setFulfillDurationSeconds(durationSec)
                .setEvidenceSummary(evidence)
                .setHandleStatus(0)
                .setCreditDeducted(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        riskAuditAlertMapper.insert(alert);
        return alert;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskAuditAlertVO resolveAlert(Long adminId, Long alertId, RiskAlertResolveRequest request) {
        if (alertId == null || request == null || request.getHandleStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "处置参数不完整");
        }
        RiskAuditAlert alert = riskAuditAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "风控预警记录不存在");
        }
        if (alert.getHandleStatus() != null && alert.getHandleStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该风控预警已处置完毕，请勿重复操作");
        }

        int targetStatus = request.getHandleStatus();
        if (targetStatus != 1 && targetStatus != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的处置状态");
        }

        int deductPoints = 0;
        String penaltyAction = "DISMISSED";
        boolean offlineSvc = false;
        if (targetStatus == 1) {
            deductPoints = request.getCreditDeductPoints() != null
                    ? Math.min(50, Math.max(0, request.getCreditDeductPoints()))
                    : 15;
            offlineSvc = Boolean.TRUE.equals(request.getOfflineService());
            penaltyAction = offlineSvc ? "DEDUCT_CREDIT_AND_OFFLINE" : (deductPoints > 0 ? "DEDUCT_CREDIT_ONLY" : "WARN_ONLY");
        }

        String handleRemark = StringUtils.hasText(request.getHandleRemark())
                ? request.getHandleRemark().trim()
                : (targetStatus == 1 ? "经交易关系图谱核查确认为违规互刷，已执行信用分扣减处罚" : "经核查属于正常高频辅导交易，排除误报");
        LocalDateTime now = LocalDateTime.now();

        // 先通过条件原子更新抢占 handle_status = 0 -> targetStatus，防止并发重复处置与双重扣分
        int claimed = riskAuditAlertMapper.update(null, new LambdaUpdateWrapper<RiskAuditAlert>()
                .eq(RiskAuditAlert::getId, alertId)
                .eq(RiskAuditAlert::getHandleStatus, 0)
                .set(RiskAuditAlert::getHandleStatus, targetStatus)
                .set(RiskAuditAlert::getPenaltyAction, penaltyAction)
                .set(RiskAuditAlert::getCreditDeducted, deductPoints)
                .set(RiskAuditAlert::getHandlerId, adminId)
                .set(RiskAuditAlert::getHandleRemark, handleRemark)
                .set(RiskAuditAlert::getHandleTime, now)
                .set(RiskAuditAlert::getUpdateTime, now));
        if (claimed <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该风控预警已处置完毕，请勿重复操作");
        }

        if (targetStatus == 1) {
            if (deductPoints > 0) {
                Long buyerId = alert.getBuyerId();
                Long sellerId = alert.getSellerId();
                String buyerDesc = "风控审计确认违规互刷交易（单号 " + alert.getAlertNo() + "），买方扣减信用分";
                String sellerDesc = "风控审计确认违规互刷交易（单号 " + alert.getAlertNo() + "），卖方扣减信用分";
                // 严格按 userId 升序加行锁，杜绝死锁
                if (buyerId != null && sellerId != null && buyerId > sellerId) {
                    deductUserCreditScore(sellerId, deductPoints, alert.getOrderId(), sellerDesc);
                    deductUserCreditScore(buyerId, deductPoints, alert.getOrderId(), buyerDesc);
                } else {
                    deductUserCreditScore(buyerId, deductPoints, alert.getOrderId(), buyerDesc);
                    if (sellerId != null && !sellerId.equals(buyerId)) {
                        deductUserCreditScore(sellerId, deductPoints, alert.getOrderId(), sellerDesc);
                    }
                }
            }

            if (offlineSvc && alert.getServiceId() != null) {
                Service svc = serviceMapper.selectById(alert.getServiceId());
                if (svc != null) {
                    svc.setStatus(0);
                    svc.setReviewReason("触发平台反互刷风控审计（" + alert.getRuleName() + "），已强制下架整改");
                    svc.setUpdateTime(now);
                    serviceMapper.updateById(svc);
                    if (redisUtils != null) {
                        Long svcId = alert.getServiceId();
                        TransactionUtils.afterCommit(() -> redisUtils.delete(CacheConstant.SERVICE_DETAIL_PREFIX + svcId));
                    }
                }
            }

            sendRiskPenaltyNotification(alert.getBuyerId(), alert, deductPoints, offlineSvc);
            sendRiskPenaltyNotification(alert.getSellerId(), alert, deductPoints, offlineSvc);
        }

        alert.setHandleStatus(targetStatus)
                .setPenaltyAction(penaltyAction)
                .setCreditDeducted(deductPoints)
                .setHandlerId(adminId)
                .setHandleRemark(handleRemark)
                .setHandleTime(now)
                .setUpdateTime(now);
        return toVO(alert);
    }

    private void deductUserCreditScore(Long userId, int deductPoints, Long orderId, String reasonDesc) {
        if (userId == null || deductPoints <= 0) {
            return;
        }
        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            return;
        }
        int before = user.getCreditScore() != null ? user.getCreditScore() : 100;
        int after = Math.max(0, before - deductPoints);
        user.setCreditScore(after);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        CreditScoreLog log = new CreditScoreLog()
                .setUserId(userId)
                .setDelta(-deductPoints)
                .setScoreBefore(before)
                .setScoreAfter(after)
                .setReasonCode("RISK_WASH_TRADE_PENALTY")
                .setReasonDesc(reasonDesc)
                .setRelatedOrderId(orderId)
                .setCreateTime(LocalDateTime.now());
        creditScoreLogMapper.insert(log);

        if (redisUtils != null) {
            TransactionUtils.afterCommit(() -> redisUtils.deleteUserCache(userId));
        }
    }

    private void sendRiskPenaltyNotification(Long recipientId, RiskAuditAlert alert, int deductPoints, boolean offlineSvc) {
        if (recipientId == null) {
            return;
        }
        String content = String.format("您的订单（%s）命中平台反作弊风控规则【%s】。经管理员取证核查，已扣减校园履约信用分 %d 分%s。请严格遵守校园真实担保交易规范。",
                alert.getOrderNumber() != null ? alert.getOrderNumber() : "#" + alert.getOrderId(),
                alert.getRuleName(),
                deductPoints,
                offlineSvc ? "，并已下架关联技能服务" : "");
        Notification notification = new Notification()
                .setRecipientId(recipientId)
                .setNotificationType(3)
                .setTitle("【风控审计处罚通知】违规互刷交易核查结果")
                .setContent(content)
                .setRelatedId(alert.getOrderId())
                .setIsRead(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private RiskAuditAlertVO toVO(RiskAuditAlert alert) {
        RiskAuditAlertVO vo = new RiskAuditAlertVO();
        BeanUtils.copyProperties(alert, vo);

        if (alert.getServiceId() != null) {
            Service svc = serviceMapper.selectById(alert.getServiceId());
            if (svc != null) {
                vo.setServiceTitle(svc.getServiceTitle());
            }
        }
        if (alert.getBuyerId() != null) {
            User buyer = userMapper.selectById(alert.getBuyerId());
            if (buyer != null) {
                vo.setBuyerName(StringUtils.hasText(buyer.getRealName()) ? buyer.getRealName() : buyer.getNickname());
                vo.setBuyerStudentId(buyer.getStudentId());
                vo.setBuyerCreditScore(buyer.getCreditScore());
            }
        }
        if (alert.getSellerId() != null) {
            User seller = userMapper.selectById(alert.getSellerId());
            if (seller != null) {
                vo.setSellerName(StringUtils.hasText(seller.getRealName()) ? seller.getRealName() : seller.getNickname());
                vo.setSellerStudentId(seller.getStudentId());
                vo.setSellerCreditScore(seller.getCreditScore());
            }
        }
        return vo;
    }
}
