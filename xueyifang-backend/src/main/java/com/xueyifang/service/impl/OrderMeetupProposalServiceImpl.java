package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.domain.dto.MeetupProposalCreateRequest;
import com.xueyifang.domain.dto.MeetupProposalRespondRequest;
import com.xueyifang.domain.po.OrderMeetupProposal;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.MeetupProposalVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.OrderMeetupProposalMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IOrderMeetupProposalService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 买卖双方双向地图选点与时间协商服务实现
 * <p>
 * 买家或卖家均可基于真实地图 SDK 选点发起会面提案，经校园电子围栏校验后推送给对方；
 * 对方点击同意即在行锁保护下原子更新订单约定坐标并锁定，全链路写入订单操作日志供纠纷仲裁举证。
 */
@Service
public class OrderMeetupProposalServiceImpl extends ServiceImpl<OrderMeetupProposalMapper, OrderMeetupProposal>
        implements IOrderMeetupProposalService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_SUPERSEDED = 3;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ICampusService campusService;

    @Resource
    private IServiceOrderLogService orderLogService;

    @Resource
    private INotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long proposeMeetupLocation(Long orderId, MeetupProposalCreateRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (orderId == null || request == null
                || request.getLatitude() == null || request.getLongitude() == null
                || StringUtils.isBlank(request.getPoiName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请在地图上选择具体会面建筑或地标");
        }

        ServiceOrder order = serviceOrderMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        boolean isBuyer = Objects.equals(currentUserId, order.getBuyerId());
        boolean isSeller = Objects.equals(currentUserId, order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅订单买卖双方可协商会面位置");
        }
        if (!Objects.equals(order.getTradeType(), OrderConstant.TRADE_TYPE_OFFLINE)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅线下履约订单支持会面选点协商");
        }
        if (Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_COMPLETED)
                || Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_CANCELLED)
                || Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_FAILED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "已终结订单无法修改会面位置");
        }

        // 校验坐标是否落在本校区电子围栏安全区域内
        campusService.validateCoordinateInCampusFence(
                order.getCampusId(),
                request.getLatitude().doubleValue(),
                request.getLongitude().doubleValue()
        );

        // 将此前未处理的旧提案标记为已覆盖，确保同一时刻最多只有一条待确认提案
        this.update(new LambdaUpdateWrapper<OrderMeetupProposal>()
                .eq(OrderMeetupProposal::getOrderId, orderId)
                .eq(OrderMeetupProposal::getProposalStatus, STATUS_PENDING)
                .set(OrderMeetupProposal::getProposalStatus, STATUS_SUPERSEDED));

        OrderMeetupProposal proposal = new OrderMeetupProposal();
        proposal.setOrderId(orderId);
        proposal.setProposerId(currentUserId);
        proposal.setProposerRole(isBuyer ? 1 : 2);
        proposal.setLatitude(request.getLatitude());
        proposal.setLongitude(request.getLongitude());
        proposal.setPoiName(request.getPoiName().trim());
        proposal.setDetailAddress(StringUtils.trimToNull(request.getDetailAddress()));
        int r = (request.getRadiusMeters() != null && request.getRadiusMeters() >= 10 && request.getRadiusMeters() <= 2000) ? request.getRadiusMeters() : 80;
        proposal.setRadiusMeters(r);
        proposal.setProposedTime(request.getProposedTime());
        proposal.setRemark(StringUtils.trimToNull(request.getRemark()));
        proposal.setProposalStatus(STATUS_PENDING);
        proposal.setCreateTime(LocalDateTime.now());
        proposal.setUpdateTime(LocalDateTime.now());
        this.save(proposal);

        String roleLabel = isBuyer ? "买家" : "卖家";
        orderLogService.recordLog(orderId, order.getOrderStatus(), order.getOrderStatus(),
                OrderLogConstant.ACTION_MEETUP_PROPOSE,
                roleLabel + "提议会面地点：" + proposal.getPoiName()
                        + (proposal.getDetailAddress() != null ? "（" + proposal.getDetailAddress() + "）" : ""));

        Long targetUserId = isBuyer ? order.getSellerId() : order.getBuyerId();
        notificationService.sendOrderNotification(targetUserId, orderId, "新会面地点协商提议",
                String.format("%s提议在「%s」线下会面交接，请前往订单详情确认或重新选点", roleLabel, proposal.getPoiName()));

        return proposal.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondMeetupProposal(Long proposalId, MeetupProposalRespondRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (proposalId == null || request == null || request.getAccept() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        OrderMeetupProposal initialProposal = this.getById(proposalId);
        if (initialProposal == null) {
            throw new BusinessException(ErrorCode.MEETUP_PROPOSAL_INVALID);
        }

        // 先对订单加行锁，再在锁内重查提案状态，防止与新提案覆盖/并发响应形成 TOCTOU 竞态
        ServiceOrder order = serviceOrderMapper.selectForUpdate(initialProposal.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_COMPLETED)
                || Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_CANCELLED)
                || Objects.equals(order.getOrderStatus(), OrderConstant.ORDER_FAILED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "已终结订单无法响应会面位置提案");
        }

        OrderMeetupProposal proposal = this.getById(proposalId);
        if (proposal == null || !Objects.equals(proposal.getProposalStatus(), STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.MEETUP_PROPOSAL_INVALID);
        }

        boolean isParticipant = Objects.equals(currentUserId, order.getBuyerId())
                || Objects.equals(currentUserId, order.getSellerId());
        if (!isParticipant || Objects.equals(currentUserId, proposal.getProposerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需由交易另一方确认该会面地点提案");
        }

        if (Boolean.TRUE.equals(request.getAccept())) {
            proposal.setProposalStatus(STATUS_ACCEPTED);
            proposal.setRespondTime(LocalDateTime.now());
            this.updateById(proposal);

            ServiceOrder updateOrder = new ServiceOrder();
            updateOrder.setId(order.getId());
            updateOrder.setMeetupLat(proposal.getLatitude());
            updateOrder.setMeetupLng(proposal.getLongitude());
            updateOrder.setMeetupPoiName(proposal.getPoiName());
            updateOrder.setMeetupAddress(proposal.getDetailAddress());
            updateOrder.setMeetupRadiusMeters(proposal.getRadiusMeters() != null ? proposal.getRadiusMeters() : 80);
            updateOrder.setMeetupConfirmed(1);
            if (proposal.getProposedTime() != null) {
                updateOrder.setScheduledStart(proposal.getProposedTime());
            }
            serviceOrderMapper.updateById(updateOrder);

            orderLogService.recordLog(order.getId(), order.getOrderStatus(), order.getOrderStatus(),
                    OrderLogConstant.ACTION_MEETUP_CONFIRMED,
                    "双方已确认锁定线下会面地点：" + proposal.getPoiName()
                            + String.format(" (%.6f, %.6f)", proposal.getLatitude(), proposal.getLongitude()));

            notificationService.sendOrderNotification(proposal.getProposerId(), order.getId(), "会面地点已锁定",
                    "对方已同意您提议的会面位置「" + proposal.getPoiName() + "」，请按约定准时前往现场当面核销");
        } else {
            proposal.setProposalStatus(STATUS_REJECTED);
            proposal.setRejectReason(StringUtils.defaultIfBlank(request.getRejectReason(), "时间或距离不便，希望重新协商位置"));
            proposal.setRespondTime(LocalDateTime.now());
            this.updateById(proposal);

            orderLogService.recordLog(order.getId(), order.getOrderStatus(), order.getOrderStatus(),
                    OrderLogConstant.ACTION_MEETUP_REJECTED,
                    "对方婉拒会面位置提案「" + proposal.getPoiName() + "」：" + proposal.getRejectReason());

            notificationService.sendOrderNotification(proposal.getProposerId(), order.getId(), "会面位置需重新协商",
                    "对方未接受「" + proposal.getPoiName() + "」（原因：" + proposal.getRejectReason() + "），可在地图重新选点");
        }
    }

    @Override
    public List<MeetupProposalVO> listOrderProposals(Long orderId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        User currentUser = UserContext.getCurrentUser();
        boolean isAdmin = currentUser != null && Objects.equals(currentUser.getRole(), 2);
        if (!isAdmin && !Objects.equals(currentUserId, order.getBuyerId()) && !Objects.equals(currentUserId, order.getSellerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        List<OrderMeetupProposal> list = this.list(new LambdaQueryWrapper<OrderMeetupProposal>()
                .eq(OrderMeetupProposal::getOrderId, orderId)
                .orderByDesc(OrderMeetupProposal::getCreateTime));
        Map<Long, String> nameCache = new HashMap<>();
        List<MeetupProposalVO> result = new ArrayList<>();
        for (OrderMeetupProposal item : list) {
            MeetupProposalVO vo = new MeetupProposalVO();
            BeanUtils.copyProperties(item, vo);
            String name = nameCache.computeIfAbsent(item.getProposerId(), uid -> {
                User u = userMapper.selectById(uid);
                return u != null ? u.getNickname() : ("用户#" + uid);
            });
            vo.setProposerName(name);
            result.add(vo);
        }
        return result;
    }
}
