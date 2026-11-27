package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.domain.dto.DemandBidCreateRequest;
import com.xueyifang.domain.dto.DemandCreateRequest;
import com.xueyifang.domain.dto.DemandQueryRequest;
import com.xueyifang.domain.dto.OrderCreateRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.DemandBid;
import com.xueyifang.domain.po.DemandRequest;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.DemandBidVO;
import com.xueyifang.domain.vo.DemandRequestVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.DemandBidMapper;
import com.xueyifang.mapper.DemandRequestMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.IDemandService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.GeofenceUtils;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
public class DemandServiceImpl extends ServiceImpl<DemandRequestMapper, DemandRequest> implements IDemandService {

    public static final int DEMAND_STATUS_OPEN = 0;
    public static final int DEMAND_STATUS_MATCHED = 1;
    public static final int DEMAND_STATUS_CANCELLED = 2;
    public static final int DEMAND_STATUS_ADMIN_CLOSED = 3;

    public static final int BID_STATUS_PENDING = 0;
    public static final int BID_STATUS_ACCEPTED = 1;
    public static final int BID_STATUS_OUTBID = 2;

    @Resource
    private DemandBidMapper demandBidMapper;

    @Resource
    private IUserService userService;

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
    private INotificationService notificationService;

    @Resource
    private ISysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishDemand(DemandCreateRequest request) {
        User currentUser = requireLoginUser();
        if (request == null || StringUtils.isBlank(request.getTitle()) || StringUtils.isBlank(request.getDescription())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请完整填写悬赏标题与详细需求说明");
        }
        String title = request.getTitle().trim();
        if (title.length() < 4 || title.length() > 120) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "悬赏标题长度需在 4~120 字之间");
        }
        if (request.getBudgetMin() == null || request.getBudgetMax() == null
                || request.getBudgetMin().compareTo(BigDecimal.ONE) < 0
                || request.getBudgetMax().compareTo(request.getBudgetMin()) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "预算区间不合法，最低预算不少于 1 元且上限不低于下限");
        }

        if (currentUser.getCreditScore() != null && sysConfigService != null) {
            int minOrderCredit = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_ORDER, 60);
            if (currentUser.getCreditScore() < minOrderCredit) {
                throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                        "您的履约信用分为 " + currentUser.getCreditScore() + "，低于发悬赏门槛 " + minOrderCredit);
            }
        }

        Long campusId = request.getCampusId() != null ? request.getCampusId()
                : (currentUser.getCampusId() != null ? currentUser.getCampusId() : 1L);
        int tradeType = Objects.equals(request.getTradeType(), OrderConstant.TRADE_TYPE_ONLINE)
                ? OrderConstant.TRADE_TYPE_ONLINE : OrderConstant.TRADE_TYPE_OFFLINE;
        int urgency = request.getUrgencyLevel() != null && request.getUrgencyLevel() >= 1 && request.getUrgencyLevel() <= 3
                ? request.getUrgencyLevel() : 1;

        Integer radiusMeters = null;
        if (tradeType == OrderConstant.TRADE_TYPE_OFFLINE) {
            if (request.getMeetupLat() == null || request.getMeetupLng() == null || StringUtils.isBlank(request.getMeetupPoiName())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "校园线下悬赏需在真实校园地图上标记会面地点");
            }
            if (!GeofenceUtils.isValidCoordinate(request.getMeetupLat().doubleValue(), request.getMeetupLng().doubleValue())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "悬赏会面坐标超出有效经纬度范围");
            }
            if (campusService != null) {
                campusService.validateCoordinateInCampusFence(
                        campusId, request.getMeetupLat().doubleValue(), request.getMeetupLng().doubleValue());
            }
            int r = request.getMeetupRadiusMeters() != null ? request.getMeetupRadiusMeters() : 80;
            if (r < 10 || r > 2000) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当面核销有效半径需设置在 10~2000 米之间");
            }
            radiusMeters = r;
        }

        DemandRequest demand = new DemandRequest();
        demand.setDemandNo(SerialNoGenerator.next("DEMAND"));
        demand.setPublisherId(currentUser.getId());
        demand.setCampusId(campusId);
        demand.setTagId(request.getTagId());
        demand.setTitle(title);
        demand.setDescription(request.getDescription().trim());
        demand.setBudgetMin(request.getBudgetMin());
        demand.setBudgetMax(request.getBudgetMax());
        demand.setTradeType(tradeType);
        demand.setUrgencyLevel(urgency);
        demand.setDeadlineTime(request.getDeadlineTime());
        if (tradeType == OrderConstant.TRADE_TYPE_OFFLINE) {
            demand.setMeetupLat(request.getMeetupLat());
            demand.setMeetupLng(request.getMeetupLng());
            demand.setMeetupPoiName(request.getMeetupPoiName().trim());
            demand.setMeetupAddress(StringUtils.trimToNull(request.getMeetupAddress()));
            demand.setMeetupRadiusMeters(radiusMeters);
        }
        demand.setBidCount(0);
        demand.setDemandStatus(DEMAND_STATUS_OPEN);
        demand.setCreateTime(LocalDateTime.now());
        demand.setUpdateTime(LocalDateTime.now());
        demand.setIsDelete(0);

        this.baseMapper.insert(demand);
        return demand.getId();
    }

    @Override
    public Page<DemandRequestVO> pageDemands(DemandQueryRequest query) {
        int pageNum = query != null && query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query != null && query.getPageSize() != null && query.getPageSize() > 0
                ? Math.min(50, query.getPageSize()) : 12;

        LambdaQueryWrapper<DemandRequest> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getDemandStatus() != null) {
                wrapper.eq(DemandRequest::getDemandStatus, query.getDemandStatus());
            } else {
                wrapper.in(DemandRequest::getDemandStatus, DEMAND_STATUS_OPEN, DEMAND_STATUS_MATCHED);
            }
            if (query.getCampusId() != null) {
                wrapper.eq(DemandRequest::getCampusId, query.getCampusId());
            }
            if (query.getTagId() != null) {
                wrapper.eq(DemandRequest::getTagId, query.getTagId());
            }
            if (query.getTradeType() != null) {
                wrapper.eq(DemandRequest::getTradeType, query.getTradeType());
            }
            if (query.getUrgencyLevel() != null) {
                wrapper.eq(DemandRequest::getUrgencyLevel, query.getUrgencyLevel());
            }
            if (StringUtils.isNotBlank(query.getKeyword())) {
                String kw = query.getKeyword().trim();
                wrapper.and(w -> w.like(DemandRequest::getTitle, kw).or().like(DemandRequest::getDescription, kw));
            }
        } else {
            wrapper.eq(DemandRequest::getDemandStatus, DEMAND_STATUS_OPEN);
        }
        wrapper.orderByAsc(DemandRequest::getDemandStatus)
                .orderByDesc(DemandRequest::getUrgencyLevel)
                .orderByDesc(DemandRequest::getCreateTime);

        Page<DemandRequest> rawPage = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<DemandRequestVO> voList = toDemandVOList(rawPage.getRecords(), false);
        Page<DemandRequestVO> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public DemandRequestVO getDemandDetail(Long demandId) {
        if (demandId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "悬赏ID不能为空");
        }
        DemandRequest demand = this.getById(demandId);
        if (demand == null || Integer.valueOf(1).equals(demand.getIsDelete())) {
            throw new BusinessException(ErrorCode.DEMAND_NOT_EXIST);
        }
        List<DemandRequestVO> list = toDemandVOList(Collections.singletonList(demand), true);
        return list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitBid(Long demandId, DemandBidCreateRequest request) {
        User bidder = requireLoginUser();
        if (demandId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!Integer.valueOf(1).equals(bidder.getPublishPermission())) {
            throw new BusinessException(ErrorCode.USER_NO_PUBLISH_PERMISSION, "仅通过技能发布权限认证的创作者可参与竞标接单");
        }
        if (bidder.getCreditScore() != null && sysConfigService != null) {
            int minPubCredit = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_PUBLISH, 70);
            if (bidder.getCreditScore() < minPubCredit) {
                throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                        "您的履约信用分为 " + bidder.getCreditScore() + "，低于创作者接单门槛 " + minPubCredit);
            }
        }
        if (request.getBidPrice() == null || request.getBidPrice().compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报价金额需不低于 1 元");
        }
        if (StringUtils.isBlank(request.getBidPlan()) || request.getBidPlan().trim().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写至少 6 个字的接单方案与履约说明");
        }

        DemandRequest demand = this.baseMapper.selectForUpdate(demandId);
        if (demand == null) {
            throw new BusinessException(ErrorCode.DEMAND_NOT_EXIST);
        }
        if (!Integer.valueOf(DEMAND_STATUS_OPEN).equals(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.DEMAND_STATUS_ERROR, "该悬赏需求已选标或已关闭，无法继续报价");
        }
        if (demand.getPublisherId().equals(bidder.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能竞标自己发布的悬赏需求");
        }

        Long dupCount = demandBidMapper.selectCount(new LambdaQueryWrapper<DemandBid>()
                .eq(DemandBid::getDemandId, demandId)
                .eq(DemandBid::getBidderId, bidder.getId())
                .eq(DemandBid::getBidStatus, BID_STATUS_PENDING));
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMIT, "您已对该悬赏提交过有效报价，请等待需求方选标");
        }

        DemandBid bid = new DemandBid();
        bid.setDemandId(demandId);
        bid.setBidderId(bidder.getId());
        bid.setBidPrice(request.getBidPrice());
        bid.setEstimatedHours(request.getEstimatedHours() != null && request.getEstimatedHours() > 0 ? request.getEstimatedHours() : 24);
        bid.setBidPlan(request.getBidPlan().trim());
        bid.setBidStatus(BID_STATUS_PENDING);
        bid.setCreateTime(LocalDateTime.now());
        bid.setUpdateTime(LocalDateTime.now());
        bid.setIsDelete(0);
        demandBidMapper.insert(bid);

        demand.setBidCount((demand.getBidCount() == null ? 0 : demand.getBidCount()) + 1);
        demand.setUpdateTime(LocalDateTime.now());
        this.updateById(demand);

        if (notificationService != null) {
            String bidderName = StringUtils.defaultIfBlank(bidder.getNickname(), bidder.getRealName());
            notificationService.sendOrderNotification(
                    demand.getPublisherId(),
                    demand.getId(),
                    "收到新的悬赏竞标方案",
                    String.format("创作者 %s 对您的悬赏「%s」报价 ¥%s，请前往悬赏详情查看并选标",
                            StringUtils.defaultIfBlank(bidderName, "同学"), demand.getTitle(), bid.getBidPrice().toPlainString())
            );
        }
        return bid.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long acceptBid(Long demandId, Long bidId) {
        User currentUser = requireLoginUser();
        if (demandId == null || bidId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        DemandRequest demand = this.baseMapper.selectForUpdate(demandId);
        if (demand == null) {
            throw new BusinessException(ErrorCode.DEMAND_NOT_EXIST);
        }
        if (!demand.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅悬赏发布者本人可选标并生成担保订单");
        }
        if (!Integer.valueOf(DEMAND_STATUS_OPEN).equals(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.DEMAND_STATUS_ERROR, "该悬赏已完成选标或已关闭");
        }

        DemandBid winningBid = demandBidMapper.selectById(bidId);
        if (winningBid == null || !demandId.equals(winningBid.getDemandId())
                || !Integer.valueOf(BID_STATUS_PENDING).equals(winningBid.getBidStatus())) {
            throw new BusinessException(ErrorCode.DEMAND_BID_INVALID);
        }

        // 1. 生成与该悬赏一一绑定的定向履约服务快照（自动上架，限购 1 份）
        com.xueyifang.domain.po.Service dedicatedService = new com.xueyifang.domain.po.Service();
        dedicatedService.setServiceTitle("【悬赏定向单】" + demand.getTitle());
        dedicatedService.setServiceDescription(demand.getDescription() + "\n\n【中标创作者履约方案】" + winningBid.getBidPlan());
        dedicatedService.setSellerId(winningBid.getBidderId());
        dedicatedService.setCampusId(demand.getCampusId() != null ? demand.getCampusId() : 1L);
        dedicatedService.setTagId(demand.getTagId() != null ? demand.getTagId() : 1L);
        dedicatedService.setTradeType(demand.getTradeType());
        dedicatedService.setPrice(winningBid.getBidPrice());
        dedicatedService.setMaxPurchases(1);
        dedicatedService.setCurrentPurchaseCount(0);
        dedicatedService.setIsPhysical(ServiceConstant.NOT_PHYSICAL);
        dedicatedService.setRequireSlot(0);
        dedicatedService.setRequireBleVerify(0);
        dedicatedService.setStatus(ServiceConstant.STATUS_ONLINE);
        dedicatedService.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        dedicatedService.setReviewReason("C2B悬赏选标自动核准定向服务");
        dedicatedService.setReviewTime(LocalDateTime.now());
        dedicatedService.setViewCount(1);
        dedicatedService.setCollectionCount(0);
        dedicatedService.setCreateTime(LocalDateTime.now());
        dedicatedService.setUpdateTime(LocalDateTime.now());
        dedicatedService.setIsDelete(0);
        serviceService.save(dedicatedService);

        // 2. 原子创建标准担保订单，完整继承悬赏锁定的真实地图坐标与米级核销半径
        OrderCreateRequest orderReq = new OrderCreateRequest();
        orderReq.setServiceId(dedicatedService.getId());
        orderReq.setQuantity(1);
        orderReq.setTradeType(demand.getTradeType());
        if (Integer.valueOf(OrderConstant.TRADE_TYPE_OFFLINE).equals(demand.getTradeType())) {
            orderReq.setMeetupLat(demand.getMeetupLat());
            orderReq.setMeetupLng(demand.getMeetupLng());
            orderReq.setMeetupPoiName(demand.getMeetupPoiName());
            orderReq.setMeetupAddress(demand.getMeetupAddress());
            orderReq.setMeetupRadiusMeters(demand.getMeetupRadiusMeters() != null ? demand.getMeetupRadiusMeters() : 80);
        }
        orderReq.setRemark("来自悬赏单 " + demand.getDemandNo() + " · 承诺耗时 " + winningBid.getEstimatedHours() + " 小时");
        Long matchedOrderId = serviceOrderService.createOrder(orderReq);

        // 3. 更新中标与未中标报价状态并推送通知
        List<DemandBid> allBids = demandBidMapper.selectList(new LambdaQueryWrapper<DemandBid>()
                .eq(DemandBid::getDemandId, demandId)
                .eq(DemandBid::getBidStatus, BID_STATUS_PENDING));
        for (DemandBid b : allBids) {
            if (b.getId().equals(winningBid.getId())) {
                b.setBidStatus(BID_STATUS_ACCEPTED);
                b.setUpdateTime(LocalDateTime.now());
                demandBidMapper.updateById(b);
                if (notificationService != null) {
                    notificationService.sendOrderNotification(
                            b.getBidderId(),
                            matchedOrderId,
                            "恭喜！您的悬赏报价已中标",
                            String.format("您在悬赏「%s」中的 ¥%s 方案已被采纳并生成担保订单，待买家完成托管付款后即可开始履约",
                                    demand.getTitle(), b.getBidPrice().toPlainString())
                    );
                }
            } else {
                b.setBidStatus(BID_STATUS_OUTBID);
                b.setUpdateTime(LocalDateTime.now());
                demandBidMapper.updateById(b);
            }
        }

        // 4. 锁定悬赏状态与关联单据
        demand.setDemandStatus(DEMAND_STATUS_MATCHED);
        demand.setAcceptedBidId(winningBid.getId());
        demand.setMatchedServiceId(dedicatedService.getId());
        demand.setMatchedOrderId(matchedOrderId);
        demand.setUpdateTime(LocalDateTime.now());
        this.updateById(demand);

        return matchedOrderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDemand(Long demandId, String reason) {
        User currentUser = requireLoginUser();
        DemandRequest demand = this.baseMapper.selectForUpdate(demandId);
        if (demand == null) {
            throw new BusinessException(ErrorCode.DEMAND_NOT_EXIST);
        }
        if (!demand.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅发布者本人可取消悬赏");
        }
        if (!Integer.valueOf(DEMAND_STATUS_OPEN).equals(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.DEMAND_STATUS_ERROR, "已选标或已关闭的悬赏不可直接取消");
        }
        demand.setDemandStatus(DEMAND_STATUS_CANCELLED);
        demand.setCloseReason(StringUtils.defaultIfBlank(reason, "发布者主动撤回悬赏"));
        demand.setUpdateTime(LocalDateTime.now());
        this.updateById(demand);
    }

    @Override
    public List<DemandRequestVO> listMyPublishedDemands() {
        User currentUser = requireLoginUser();
        List<DemandRequest> list = this.list(new LambdaQueryWrapper<DemandRequest>()
                .eq(DemandRequest::getPublisherId, currentUser.getId())
                .orderByDesc(DemandRequest::getCreateTime));
        return toDemandVOList(list, false);
    }

    @Override
    public List<DemandBidVO> listMyBids() {
        User currentUser = requireLoginUser();
        List<DemandBid> bids = demandBidMapper.selectList(new LambdaQueryWrapper<DemandBid>()
                .eq(DemandBid::getBidderId, currentUser.getId())
                .orderByDesc(DemandBid::getCreateTime));
        if (bids == null || bids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> demandIds = bids.stream().map(DemandBid::getDemandId).collect(Collectors.toSet());
        Map<Long, DemandRequest> demandMap = this.listByIds(demandIds).stream()
                .collect(Collectors.toMap(DemandRequest::getId, d -> d, (a, b) -> a));

        List<DemandBidVO> vos = toBidVOList(bids);
        for (DemandBidVO vo : vos) {
            DemandRequest d = demandMap.get(vo.getDemandId());
            if (d != null) {
                vo.setDemandTitle(d.getTitle());
                if (Integer.valueOf(BID_STATUS_ACCEPTED).equals(vo.getBidStatus())) {
                    vo.setMatchedOrderId(d.getMatchedOrderId());
                }
            }
        }
        return vos;
    }

    @Override
    public Page<DemandRequestVO> adminPageDemands(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        int p = pageNum != null && pageNum > 0 ? pageNum : 1;
        int s = pageSize != null && pageSize > 0 ? Math.min(50, pageSize) : 15;
        LambdaQueryWrapper<DemandRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(DemandRequest::getDemandStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(DemandRequest::getTitle, kw).or().like(DemandRequest::getDemandNo, kw));
        }
        wrapper.orderByDesc(DemandRequest::getCreateTime);
        Page<DemandRequest> raw = this.page(new Page<>(p, s), wrapper);
        Page<DemandRequestVO> res = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        res.setRecords(toDemandVOList(raw.getRecords(), false));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCloseDemand(Long demandId, String reason) {
        DemandRequest demand = this.baseMapper.selectForUpdate(demandId);
        if (demand == null) {
            throw new BusinessException(ErrorCode.DEMAND_NOT_EXIST);
        }
        if (Integer.valueOf(DEMAND_STATUS_MATCHED).equals(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.DEMAND_STATUS_ERROR, "该悬赏已生成担保订单，请前往订单/纠纷仲裁模块处理");
        }
        demand.setDemandStatus(DEMAND_STATUS_ADMIN_CLOSED);
        demand.setCloseReason(StringUtils.defaultIfBlank(reason, "违反校园悬赏发布规范，管理员予以关闭"));
        demand.setUpdateTime(LocalDateTime.now());
        this.updateById(demand);
    }

    private User requireLoginUser() {
        User u = UserContext.getCurrentUser();
        if (u == null || u.getId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return u;
    }

    private List<DemandRequestVO> toDemandVOList(List<DemandRequest> records, boolean includeBids) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = records.stream().map(DemandRequest::getPublisherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() || userService == null ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, String> tagMap = new HashMap<>();
        if (serviceTagService != null) {
            for (ServiceTag tag : serviceTagService.list()) {
                tagMap.put(tag.getId(), tag.getTagName());
            }
        }
        Map<Long, String> campusMap = new HashMap<>();
        if (campusService != null) {
            for (Campus c : campusService.list()) {
                campusMap.put(c.getId(), c.getCampusName());
            }
        }

        List<DemandRequestVO> vos = new ArrayList<>(records.size());
        for (DemandRequest r : records) {
            DemandRequestVO vo = new DemandRequestVO();
            vo.setId(r.getId());
            vo.setDemandNo(r.getDemandNo());
            vo.setPublisherId(r.getPublisherId());
            User pub = userMap.get(r.getPublisherId());
            if (pub != null) {
                vo.setPublisherName(StringUtils.defaultIfBlank(pub.getNickname(), pub.getRealName()));
                vo.setPublisherAvatar(pub.getAvatar());
                vo.setPublisherEduVerified(pub.getEduVerified());
                vo.setPublisherCreditScore(pub.getCreditScore() != null ? pub.getCreditScore() : 100);
            }
            vo.setCampusId(r.getCampusId());
            vo.setCampusName(campusMap.getOrDefault(r.getCampusId(), "主校区"));
            vo.setTagId(r.getTagId());
            vo.setTagName(r.getTagId() != null ? tagMap.get(r.getTagId()) : null);
            vo.setTitle(r.getTitle());
            vo.setDescription(r.getDescription());
            vo.setBudgetMin(r.getBudgetMin());
            vo.setBudgetMax(r.getBudgetMax());
            vo.setTradeType(r.getTradeType());
            vo.setUrgencyLevel(r.getUrgencyLevel());
            vo.setDeadlineTime(r.getDeadlineTime());
            vo.setMeetupLat(r.getMeetupLat());
            vo.setMeetupLng(r.getMeetupLng());
            vo.setMeetupPoiName(r.getMeetupPoiName());
            vo.setMeetupAddress(r.getMeetupAddress());
            vo.setMeetupRadiusMeters(r.getMeetupRadiusMeters());
            vo.setBidCount(r.getBidCount() != null ? r.getBidCount() : 0);
            vo.setDemandStatus(r.getDemandStatus());
            vo.setAcceptedBidId(r.getAcceptedBidId());
            vo.setMatchedServiceId(r.getMatchedServiceId());
            vo.setMatchedOrderId(r.getMatchedOrderId());
            vo.setCloseReason(r.getCloseReason());
            vo.setCreateTime(r.getCreateTime());

            if (includeBids && r.getId() != null) {
                List<DemandBid> bids = demandBidMapper.selectList(new LambdaQueryWrapper<DemandBid>()
                        .eq(DemandBid::getDemandId, r.getId())
                        .orderByAsc(DemandBid::getBidStatus)
                        .orderByAsc(DemandBid::getBidPrice)
                        .orderByAsc(DemandBid::getCreateTime));
                vo.setBids(toBidVOList(bids));
            }
            vos.add(vo);
        }
        return vos;
    }

    private List<DemandBidVO> toBidVOList(List<DemandBid> bids) {
        if (bids == null || bids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> bidderIds = new HashSet<>();
        for (DemandBid b : bids) {
            if (b.getBidderId() != null) {
                bidderIds.add(b.getBidderId());
            }
        }
        Map<Long, User> userMap = bidderIds.isEmpty() || userService == null ? Collections.emptyMap()
                : userService.listByIds(bidderIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, String> profMap = new HashMap<>();
        if (professionalService != null) {
            for (Professional p : professionalService.list()) {
                profMap.put(p.getId(), p.getProfessionalName());
            }
        }

        List<DemandBidVO> list = new ArrayList<>(bids.size());
        for (DemandBid b : bids) {
            DemandBidVO vo = new DemandBidVO();
            vo.setId(b.getId());
            vo.setDemandId(b.getDemandId());
            vo.setBidderId(b.getBidderId());
            User u = userMap.get(b.getBidderId());
            if (u != null) {
                vo.setBidderName(StringUtils.defaultIfBlank(u.getNickname(), u.getRealName()));
                vo.setBidderAvatar(u.getAvatar());
                vo.setBidderEduVerified(u.getEduVerified());
                vo.setBidderCreditScore(u.getCreditScore() != null ? u.getCreditScore() : 100);
                if (u.getProfessionalId() != null) {
                    vo.setBidderProfessional(profMap.get(u.getProfessionalId()));
                }
            }
            vo.setBidPrice(b.getBidPrice());
            vo.setEstimatedHours(b.getEstimatedHours());
            vo.setBidPlan(b.getBidPlan());
            vo.setBidStatus(b.getBidStatus());
            vo.setCreateTime(b.getCreateTime());
            list.add(vo);
        }
        return list;
    }
}
