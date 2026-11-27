package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.dto.GroupSessionCreateRequest;
import com.xueyifang.domain.dto.GroupSessionQueryRequest;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.GroupSession;
import com.xueyifang.domain.po.GroupSessionParticipant;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.domain.vo.GroupSessionVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.GroupSessionMapper;
import com.xueyifang.mapper.GroupSessionParticipantMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.IGroupSessionService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IUserService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.GeofenceUtils;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupSessionServiceImpl extends ServiceImpl<GroupSessionMapper, GroupSession> implements IGroupSessionService {

    public static final int STATUS_RECRUITING = 0;
    public static final int STATUS_FORMED = 1;
    public static final int STATUS_FULL = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CANCELLED = 4;

    @Resource
    private GroupSessionParticipantMapper participantMapper;

    @Resource
    private IUserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ICampusService campusService;

    @Resource
    private IServiceTagService serviceTagService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private IServiceService serviceService;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private IWalletTransactionService walletTransactionService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private ISysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroupSession(GroupSessionCreateRequest request) {
        User creator = requireLoginUser();
        if (!Integer.valueOf(1).equals(creator.getPublishPermission())) {
            throw new BusinessException(ErrorCode.USER_NO_PUBLISH_PERMISSION, "仅通过认证的技能创作者可发起拼班团课");
        }
        if (creator.getCreditScore() != null && sysConfigService != null) {
            int minPubCredit = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_PUBLISH, 70);
            if (creator.getCreditScore() < minPubCredit) {
                throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                        "您的履约信用分为 " + creator.getCreditScore() + "，低于开办拼班团课门槛 " + minPubCredit);
            }
        }
        if (request == null || StringUtils.isBlank(request.getTitle()) || StringUtils.isBlank(request.getDescription())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请完整填写拼班团课标题与课程大纲");
        }
        String title = request.getTitle().trim();
        if (title.length() < 4 || title.length() > 120) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团课标题长度需在 4~120 字之间");
        }
        if (request.getBasePrice() == null || request.getBasePrice().compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基础拼班单价不低于 1 元");
        }

        int minP = request.getMinParticipants() != null ? request.getMinParticipants() : 2;
        int maxP = request.getMaxParticipants() != null ? request.getMaxParticipants() : 6;
        if (minP < 2 || maxP < minP || maxP > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拼班人数范围不合法（最低成班 >= 2 人，上限 <= 50 人）");
        }

        int tierTarget = request.getTierTargetCount() != null ? request.getTierTargetCount() : maxP;
        if (tierTarget < minP || tierTarget > maxP) {
            tierTarget = maxP;
        }
        BigDecimal basePrice = request.getBasePrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tierPrice = request.getTierDiscountPrice() != null
                ? request.getTierDiscountPrice().setScale(2, RoundingMode.HALF_UP) : basePrice;
        if (tierPrice.compareTo(BigDecimal.ONE) < 0 || tierPrice.compareTo(basePrice) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "阶梯满员优惠价不低于 1 元且不能高于基础拼班单价");
        }

        LocalDateTime start = request.getSessionStart() != null ? request.getSessionStart() : LocalDateTime.now().plusDays(1);
        LocalDateTime end = request.getSessionEnd() != null ? request.getSessionEnd() : start.plusHours(2);
        if (!end.isAfter(start)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团课结束时间必须晚于开始时间");
        }

        Long campusId = request.getCampusId() != null ? request.getCampusId()
                : (creator.getCampusId() != null ? creator.getCampusId() : 1L);
        int tradeType = Objects.equals(request.getTradeType(), OrderConstant.TRADE_TYPE_ONLINE)
                ? OrderConstant.TRADE_TYPE_ONLINE : OrderConstant.TRADE_TYPE_OFFLINE;

        Integer radiusMeters = null;
        if (tradeType == OrderConstant.TRADE_TYPE_OFFLINE) {
            if (request.getMeetupLat() == null || request.getMeetupLng() == null || StringUtils.isBlank(request.getMeetupPoiName())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "校园线下小班团课需在真实校园地图标记授课地点");
            }
            if (!GeofenceUtils.isValidCoordinate(request.getMeetupLat().doubleValue(), request.getMeetupLng().doubleValue())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "授课坐标超出有效经纬度范围");
            }
            if (campusService != null) {
                campusService.validateCoordinateInCampusFence(
                        campusId, request.getMeetupLat().doubleValue(), request.getMeetupLng().doubleValue());
            }
            int r = request.getMeetupRadiusMeters() != null ? request.getMeetupRadiusMeters() : 80;
            if (r < 10 || r > 2000) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "线下当面签到有效核销半径需在 10~2000 米之间");
            }
            radiusMeters = r;
        }

        // 创建底层关联的担保服务模板（限购 maxP 份，支持每位参团学生生成独立担保订单与动态核销码）
        com.xueyifang.domain.po.Service templateSvc = new com.xueyifang.domain.po.Service();
        templateSvc.setServiceTitle("【拼班团课】" + title);
        templateSvc.setServiceDescription(request.getDescription().trim());
        templateSvc.setSellerId(creator.getId());
        templateSvc.setCampusId(campusId);
        templateSvc.setTagId(request.getTagId() != null ? request.getTagId() : 1L);
        templateSvc.setTradeType(tradeType);
        templateSvc.setPrice(basePrice);
        templateSvc.setMaxPurchases(maxP);
        templateSvc.setCurrentPurchaseCount(0);
        templateSvc.setIsPhysical(ServiceConstant.NOT_PHYSICAL);
        templateSvc.setRequireSlot(0);
        templateSvc.setRequireBleVerify(0);
        templateSvc.setStatus(ServiceConstant.STATUS_ONLINE);
        templateSvc.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        templateSvc.setReviewReason("拼班团课自动生成担保服务模板");
        templateSvc.setReviewTime(LocalDateTime.now());
        templateSvc.setViewCount(1);
        templateSvc.setCollectionCount(0);
        templateSvc.setCreateTime(LocalDateTime.now());
        templateSvc.setUpdateTime(LocalDateTime.now());
        templateSvc.setIsDelete(0);
        serviceService.save(templateSvc);

        GroupSession session = new GroupSession();
        session.setSessionNo(SerialNoGenerator.next("GRP"));
        session.setCreatorId(creator.getId());
        session.setCampusId(campusId);
        session.setTagId(request.getTagId());
        session.setTitle(title);
        session.setDescription(request.getDescription().trim());
        session.setBasePrice(basePrice);
        session.setTierTargetCount(tierTarget);
        session.setTierDiscountPrice(tierPrice);
        session.setMinParticipants(minP);
        session.setMaxParticipants(maxP);
        session.setJoinedCount(0);
        session.setTierRebateApplied(0);
        session.setTradeType(tradeType);
        session.setSessionStart(start);
        session.setSessionEnd(end);
        if (tradeType == OrderConstant.TRADE_TYPE_OFFLINE) {
            session.setMeetupLat(request.getMeetupLat());
            session.setMeetupLng(request.getMeetupLng());
            session.setMeetupPoiName(request.getMeetupPoiName().trim());
            session.setMeetupAddress(StringUtils.trimToNull(request.getMeetupAddress()));
            session.setMeetupRadiusMeters(radiusMeters);
        }
        session.setSessionStatus(STATUS_RECRUITING);
        session.setTemplateServiceId(templateSvc.getId());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDelete(0);

        this.baseMapper.insert(session);
        return session.getId();
    }

    @Override
    public Page<GroupSessionVO> pageGroupSessions(GroupSessionQueryRequest query) {
        int pageNum = query != null && query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query != null && query.getPageSize() != null && query.getPageSize() > 0
                ? Math.min(50, query.getPageSize()) : 12;

        LambdaQueryWrapper<GroupSession> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getSessionStatus() != null) {
                wrapper.eq(GroupSession::getSessionStatus, query.getSessionStatus());
            } else {
                wrapper.in(GroupSession::getSessionStatus, STATUS_RECRUITING, STATUS_FORMED, STATUS_FULL);
            }
            if (query.getCampusId() != null) {
                wrapper.eq(GroupSession::getCampusId, query.getCampusId());
            }
            if (query.getTagId() != null) {
                wrapper.eq(GroupSession::getTagId, query.getTagId());
            }
            if (query.getTradeType() != null) {
                wrapper.eq(GroupSession::getTradeType, query.getTradeType());
            }
            if (StringUtils.isNotBlank(query.getKeyword())) {
                String kw = query.getKeyword().trim();
                wrapper.and(w -> w.like(GroupSession::getTitle, kw).or().like(GroupSession::getDescription, kw));
            }
        } else {
            wrapper.in(GroupSession::getSessionStatus, STATUS_RECRUITING, STATUS_FORMED, STATUS_FULL);
        }
        wrapper.orderByAsc(GroupSession::getSessionStatus)
                .orderByAsc(GroupSession::getSessionStart)
                .orderByDesc(GroupSession::getCreateTime);

        Page<GroupSession> raw = this.page(new Page<>(pageNum, pageSize), wrapper);
        Page<GroupSessionVO> res = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        res.setRecords(toVOList(raw.getRecords(), false));
        return res;
    }

    @Override
    public GroupSessionVO getGroupSessionDetail(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        GroupSession s = this.getById(sessionId);
        if (s == null || Integer.valueOf(1).equals(s.getIsDelete())) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_NOT_EXIST);
        }
        return toVOList(Collections.singletonList(s), true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long joinGroupSession(Long sessionId) {
        User buyer = requireLoginUser();
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        GroupSession session = this.baseMapper.selectForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_NOT_EXIST);
        }
        if (session.getCreatorId().equals(buyer.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能报名自己开办的拼班团课");
        }
        if (session.getSessionStatus() != STATUS_RECRUITING && session.getSessionStatus() != STATUS_FORMED) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_STATUS_ERROR, "该拼班团课已满员或已结束");
        }
        int currentJoined = session.getJoinedCount() != null ? session.getJoinedCount() : 0;
        if (currentJoined >= session.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_FULL);
        }

        Long dup = participantMapper.selectCount(new LambdaQueryWrapper<GroupSessionParticipant>()
                .eq(GroupSessionParticipant::getSessionId, sessionId)
                .eq(GroupSessionParticipant::getBuyerId, buyer.getId())
                .eq(GroupSessionParticipant::getJoinStatus, 1));
        if (dup != null && dup > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMIT, "您已报名该拼班团课，请勿重复参团");
        }

        int newJoinedCount = currentJoined + 1;
        boolean tierHitNow = newJoinedCount >= session.getTierTargetCount();
        BigDecimal unitPriceForThisBuyer = tierHitNow ? session.getTierDiscountPrice() : session.getBasePrice();

        // 若当前参团人数达到阶梯优惠门槛，先更新模板服务价格使新订单直接享受阶梯价
        if (tierHitNow && session.getTemplateServiceId() != null) {
            com.xueyifang.domain.po.Service tpl = serviceService.getById(session.getTemplateServiceId());
            if (tpl != null && tpl.getPrice().compareTo(session.getTierDiscountPrice()) != 0) {
                tpl.setPrice(session.getTierDiscountPrice());
                serviceService.updateById(tpl);
            }
        }

        OrderCreateRequest orderReq = new OrderCreateRequest();
        orderReq.setServiceId(session.getTemplateServiceId());
        orderReq.setQuantity(1);
        orderReq.setTradeType(session.getTradeType());
        orderReq.setScheduledStart(session.getSessionStart());
        if (Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(session.getTradeType())) {
            orderReq.setMeetupLat(session.getMeetupLat());
            orderReq.setMeetupLng(session.getMeetupLng());
            orderReq.setMeetupPoiName(session.getMeetupPoiName());
            orderReq.setMeetupAddress(session.getMeetupAddress());
            orderReq.setMeetupRadiusMeters(session.getMeetupRadiusMeters() != null ? session.getMeetupRadiusMeters() : 80);
        }
        orderReq.setRemark(String.format("拼班团课 %s（第 %d/%d 位学员）",
                session.getSessionNo(), newJoinedCount, session.getMaxParticipants()));
        Long orderId = serviceOrderService.createOrder(orderReq);

        // 确保订单金额严格按当前阶梯价写入
        if (serviceOrderMapper != null) {
            ServiceOrder createdOrder = serviceOrderMapper.selectById(orderId);
            if (createdOrder != null && createdOrder.getTotalAmount().compareTo(unitPriceForThisBuyer) != 0) {
                createdOrder.setUnitPrice(unitPriceForThisBuyer);
                createdOrder.setTotalAmount(unitPriceForThisBuyer);
                serviceOrderMapper.updateById(createdOrder);
            }
        }

        GroupSessionParticipant participant = new GroupSessionParticipant();
        participant.setSessionId(sessionId);
        participant.setBuyerId(buyer.getId());
        participant.setOrderId(orderId);
        participant.setLockedPrice(unitPriceForThisBuyer);
        participant.setEffectivePrice(unitPriceForThisBuyer);
        participant.setRebateAmount(BigDecimal.ZERO);
        participant.setJoinStatus(1);
        participant.setCreateTime(LocalDateTime.now());
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.insert(participant);

        // 若本次刚好触发阶梯优惠且此前未执行过阶梯退差价，为所有早期高价参团学员自动折算或退回托管差价
        if (tierHitNow && !Integer.valueOf(1).equals(session.getTierRebateApplied())
                && session.getBasePrice().compareTo(session.getTierDiscountPrice()) > 0) {
            applyEarlyBirdTierRebate(session);
            session.setTierRebateApplied(1);
        }

        session.setJoinedCount(newJoinedCount);
        if (newJoinedCount >= session.getMaxParticipants()) {
            session.setSessionStatus(STATUS_FULL);
        } else if (newJoinedCount >= session.getMinParticipants()) {
            session.setSessionStatus(STATUS_FORMED);
        }
        session.setUpdateTime(LocalDateTime.now());
        this.updateById(session);

        return orderId;
    }

    private void applyEarlyBirdTierRebate(GroupSession session) {
        List<GroupSessionParticipant> existingList = participantMapper.selectList(
                new LambdaQueryWrapper<GroupSessionParticipant>()
                        .eq(GroupSessionParticipant::getSessionId, session.getId())
                        .eq(GroupSessionParticipant::getJoinStatus, 1)
        );
        if (existingList == null || existingList.isEmpty()) {
            return;
        }
        BigDecimal tierPrice = session.getTierDiscountPrice();
        for (GroupSessionParticipant p : existingList) {
            if (p.getEffectivePrice() == null || p.getEffectivePrice().compareTo(tierPrice) <= 0) {
                continue;
            }
            BigDecimal diff = p.getEffectivePrice().subtract(tierPrice).setScale(2, RoundingMode.HALF_UP);
            p.setEffectivePrice(tierPrice);
            p.setRebateAmount(diff);
            p.setUpdateTime(LocalDateTime.now());
            participantMapper.updateById(p);

            if (serviceOrderMapper != null) {
                ServiceOrder order = serviceOrderMapper.selectForUpdate(p.getOrderId());
                if (order != null) {
                    order.setUnitPrice(tierPrice);
                    order.setTotalAmount(tierPrice);
                    // 若早期学员已付款，立即从其冻结托管金额中解冻退回差价至可用钱包余额
                    if (order.getOrderStatus() == OrderConstant.ORDER_PENDING_SHIP
                            || order.getOrderStatus() == OrderConstant.ORDER_PENDING_RECEIPT) {
                        BigDecimal frozen = order.getFrozenAmount() != null ? order.getFrozenAmount() : BigDecimal.ZERO;
                        if (frozen.compareTo(diff) >= 0) {
                            order.setFrozenAmount(frozen.subtract(diff));
                            if (userMapper != null) {
                                User earlyBuyer = userMapper.selectForUpdate(p.getBuyerId());
                                if (earlyBuyer != null) {
                                    BigDecimal balBefore = earlyBuyer.getWalletBalance() != null ? earlyBuyer.getWalletBalance() : BigDecimal.ZERO;
                                    BigDecimal frzBefore = earlyBuyer.getFrozenAmount() != null ? earlyBuyer.getFrozenAmount() : BigDecimal.ZERO;
                                    BigDecimal balAfter = balBefore.add(diff);
                                    BigDecimal frzAfter = frzBefore.subtract(diff).max(BigDecimal.ZERO);
                                    earlyBuyer.setWalletBalance(balAfter);
                                    earlyBuyer.setFrozenAmount(frzAfter);
                                    userMapper.updateById(earlyBuyer);

                                    if (walletTransactionService != null) {
                                        WalletTransaction tx = new WalletTransaction();
                                        tx.setUserId(earlyBuyer.getId());
                                        tx.setTransactionType(WalletConstant.TRANSACTION_TYPE_REFUND);
                                        tx.setAmount(diff);
                                        tx.setBalanceBefore(balBefore);
                                        tx.setBalanceAfter(balAfter);
                                        tx.setFrozenBefore(frzBefore);
                                        tx.setFrozenAfter(frzAfter);
                                        tx.setRelatedOrderId(order.getId());
                                        tx.setTransactionNo(SerialNoGenerator.nextTransactionNo(earlyBuyer.getId()));
                                        tx.setRemark("拼班团课达标满员阶梯优惠自动退回差价");
                                        tx.setCreateTime(LocalDateTime.now());
                                        walletTransactionService.save(tx);
                                    }
                                }
                            }
                        }
                    }
                    serviceOrderMapper.updateById(order);
                }
            }
            if (notificationService != null) {
                notificationService.sendOrderNotification(
                        p.getBuyerId(),
                        p.getOrderId(),
                        "拼班团课满员阶梯立减生效",
                        String.format("您报名的团课「%s」人数已达阶梯优惠线，单价降至 ¥%s，立省 ¥%s",
                                session.getTitle(), tierPrice.toPlainString(), diff.toPlainString())
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelGroupSession(Long sessionId, String reason) {
        User currentUser = requireLoginUser();
        GroupSession session = this.baseMapper.selectForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_NOT_EXIST);
        }
        if (!session.getCreatorId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅开班创作者本人可取消团课");
        }
        if (session.getJoinedCount() != null && session.getJoinedCount() > 0) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_STATUS_ERROR, "已有学员报名参团，请先与学员协商处理关联订单");
        }
        session.setSessionStatus(STATUS_CANCELLED);
        session.setCloseReason(StringUtils.defaultIfBlank(reason, "创作者主动取消未开班团课"));
        session.setUpdateTime(LocalDateTime.now());
        this.updateById(session);
    }

    @Override
    public List<GroupSessionVO> listMyCreatedSessions() {
        User u = requireLoginUser();
        List<GroupSession> list = this.list(new LambdaQueryWrapper<GroupSession>()
                .eq(GroupSession::getCreatorId, u.getId())
                .orderByDesc(GroupSession::getCreateTime));
        return toVOList(list, false);
    }

    @Override
    public List<GroupSessionVO> listMyJoinedSessions() {
        User u = requireLoginUser();
        List<GroupSessionParticipant> parts = participantMapper.selectList(
                new LambdaQueryWrapper<GroupSessionParticipant>()
                        .eq(GroupSessionParticipant::getBuyerId, u.getId())
                        .eq(GroupSessionParticipant::getJoinStatus, 1)
                        .orderByDesc(GroupSessionParticipant::getCreateTime)
        );
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> sessionIds = parts.stream().map(GroupSessionParticipant::getSessionId).collect(Collectors.toList());
        List<GroupSession> sessions = this.listByIds(sessionIds);
        return toVOList(sessions, false);
    }

    @Override
    public Page<GroupSessionVO> adminPageSessions(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        int p = pageNum != null && pageNum > 0 ? pageNum : 1;
        int s = pageSize != null && pageSize > 0 ? Math.min(50, pageSize) : 15;
        LambdaQueryWrapper<GroupSession> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(GroupSession::getSessionStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(GroupSession::getTitle, kw).or().like(GroupSession::getSessionNo, kw));
        }
        wrapper.orderByDesc(GroupSession::getCreateTime);
        Page<GroupSession> raw = this.page(new Page<>(p, s), wrapper);
        Page<GroupSessionVO> res = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        res.setRecords(toVOList(raw.getRecords(), false));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCloseSession(Long sessionId, String reason) {
        GroupSession session = this.baseMapper.selectForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.GROUP_SESSION_NOT_EXIST);
        }
        session.setSessionStatus(STATUS_CANCELLED);
        session.setCloseReason(StringUtils.defaultIfBlank(reason, "管理员巡检关闭"));
        session.setUpdateTime(LocalDateTime.now());
        this.updateById(session);
    }

    private User requireLoginUser() {
        User u = UserContext.getCurrentUser();
        if (u == null || u.getId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return u;
    }

    private List<GroupSessionVO> toVOList(List<GroupSession> records, boolean includeParticipants) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> creatorIds = records.stream().map(GroupSession::getCreatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = creatorIds.isEmpty() || userService == null ? Collections.emptyMap()
                : userService.listByIds(creatorIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, String> tagMap = new HashMap<>();
        if (serviceTagService != null) {
            for (ServiceTag t : serviceTagService.list()) {
                tagMap.put(t.getId(), t.getTagName());
            }
        }
        Map<Long, String> campusMap = new HashMap<>();
        if (campusService != null) {
            for (Campus c : campusService.list()) {
                campusMap.put(c.getId(), c.getCampusName());
            }
        }
        Map<Long, String> profMap = new HashMap<>();
        if (professionalService != null) {
            for (Professional p : professionalService.list()) {
                profMap.put(p.getId(), p.getProfessionalName());
            }
        }

        List<GroupSessionVO> vos = new ArrayList<>(records.size());
        for (GroupSession r : records) {
            GroupSessionVO vo = new GroupSessionVO();
            vo.setId(r.getId());
            vo.setSessionNo(r.getSessionNo());
            vo.setCreatorId(r.getCreatorId());
            User creator = userMap.get(r.getCreatorId());
            if (creator != null) {
                vo.setCreatorName(StringUtils.defaultIfBlank(creator.getNickname(), creator.getRealName()));
                vo.setCreatorAvatar(creator.getAvatar());
                vo.setCreatorEduVerified(creator.getEduVerified());
                vo.setCreatorCreditScore(creator.getCreditScore() != null ? creator.getCreditScore() : 100);
                vo.setCreatorDeposit(creator.getSellerDeposit() != null ? creator.getSellerDeposit() : BigDecimal.ZERO);
                if (creator.getProfessionalId() != null) {
                    vo.setCreatorProfessional(profMap.get(creator.getProfessionalId()));
                }
            }
            vo.setCampusId(r.getCampusId());
            vo.setCampusName(campusMap.getOrDefault(r.getCampusId(), "主校区"));
            vo.setTagId(r.getTagId());
            vo.setTagName(r.getTagId() != null ? tagMap.get(r.getTagId()) : null);
            vo.setTitle(r.getTitle());
            vo.setDescription(r.getDescription());
            vo.setBasePrice(r.getBasePrice());
            vo.setTierTargetCount(r.getTierTargetCount());
            vo.setTierDiscountPrice(r.getTierDiscountPrice());
            int joined = r.getJoinedCount() != null ? r.getJoinedCount() : 0;
            int tierCnt = r.getTierTargetCount() != null ? r.getTierTargetCount() : 99;
            vo.setCurrentUnitPrice(joined >= tierCnt ? r.getTierDiscountPrice() : r.getBasePrice());
            vo.setMinParticipants(r.getMinParticipants());
            vo.setMaxParticipants(r.getMaxParticipants());
            vo.setJoinedCount(joined);
            vo.setTierRebateApplied(r.getTierRebateApplied());
            vo.setTradeType(r.getTradeType());
            vo.setSessionStart(r.getSessionStart());
            vo.setSessionEnd(r.getSessionEnd());
            vo.setMeetupLat(r.getMeetupLat());
            vo.setMeetupLng(r.getMeetupLng());
            vo.setMeetupPoiName(r.getMeetupPoiName());
            vo.setMeetupAddress(r.getMeetupAddress());
            vo.setMeetupRadiusMeters(r.getMeetupRadiusMeters());
            vo.setSessionStatus(r.getSessionStatus());
            vo.setTemplateServiceId(r.getTemplateServiceId());
            vo.setCloseReason(r.getCloseReason());
            vo.setCreateTime(r.getCreateTime());

            if (includeParticipants && r.getId() != null) {
                List<GroupSessionParticipant> parts = participantMapper.selectList(
                        new LambdaQueryWrapper<GroupSessionParticipant>()
                                .eq(GroupSessionParticipant::getSessionId, r.getId())
                                .eq(GroupSessionParticipant::getJoinStatus, 1)
                                .orderByAsc(GroupSessionParticipant::getCreateTime)
                );
                vo.setParticipants(toParticipantVOList(parts));
            }
            vos.add(vo);
        }
        return vos;
    }

    private List<GroupSessionVO.ParticipantItem> toParticipantVOList(List<GroupSessionParticipant> parts) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> buyerIds = new HashSet<>();
        for (GroupSessionParticipant p : parts) {
            if (p.getBuyerId() != null) {
                buyerIds.add(p.getBuyerId());
            }
        }
        Map<Long, User> buyerMap = buyerIds.isEmpty() || userService == null ? Collections.emptyMap()
                : userService.listByIds(buyerIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<GroupSessionVO.ParticipantItem> items = new ArrayList<>(parts.size());
        for (GroupSessionParticipant p : parts) {
            GroupSessionVO.ParticipantItem item = new GroupSessionVO.ParticipantItem();
            item.setId(p.getId());
            item.setBuyerId(p.getBuyerId());
            User u = buyerMap.get(p.getBuyerId());
            if (u != null) {
                item.setBuyerName(StringUtils.defaultIfBlank(u.getNickname(), "参团同学"));
                item.setBuyerAvatar(u.getAvatar());
            } else {
                item.setBuyerName("参团同学");
            }
            item.setOrderId(p.getOrderId());
            item.setLockedPrice(p.getLockedPrice());
            item.setEffectivePrice(p.getEffectivePrice());
            item.setRebateAmount(p.getRebateAmount());
            item.setCreateTime(p.getCreateTime());
            items.add(item);
        }
        return items;
    }
}
