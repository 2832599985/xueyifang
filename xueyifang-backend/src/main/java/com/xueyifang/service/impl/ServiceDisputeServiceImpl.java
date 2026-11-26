package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.ChatConstant;
import com.xueyifang.constant.DisputeConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.domain.dto.DisputeCreateRequest;
import com.xueyifang.domain.dto.DisputeHandleRequest;
import com.xueyifang.domain.dto.DisputeQueryRequest;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.UserChat;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.domain.vo.DisputeDetailVO;
import com.xueyifang.domain.vo.DisputeListItemVO;
import com.xueyifang.domain.vo.UserSimpleVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserChatMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceDisputeService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务纠纷表 服务实现类
 * <p>
 * 支持图文举证、订单行锁防并发结算/重复建单、买卖双方沟通留痕调阅与批量无 N+1 查询。
 */
@org.springframework.stereotype.Service
public class ServiceDisputeServiceImpl extends ServiceImpl<ServiceDisputeMapper, ServiceDispute> implements IServiceDisputeService {

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private IServiceOrderLogService orderLogService;

    @Resource
    private IUserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IServiceService serviceService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private UserChatMapper userChatMapper;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDispute(DisputeCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 1. 先对父订单加行锁，防止并发重复建单或与确认收货/退款穿插
        ServiceOrder order = serviceOrderMapper.selectForUpdate(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
        }

        // 2. 验证用户是否是订单的买家或卖家
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该订单的参与者");
        }

        // 3. 验证订单状态（待付款、已失败、已取消的订单不能发起纠纷）
        if (order.getOrderStatus().equals(OrderConstant.ORDER_UNPAID)
                || order.getOrderStatus().equals(OrderConstant.ORDER_FAILED)
                || order.getOrderStatus().equals(OrderConstant.ORDER_CANCELLED)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该订单状态不允许发起纠纷");
        }

        // 4. 验证是否已存在未解决的纠纷
        LambdaQueryWrapper<ServiceDispute> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceDispute::getOrderId, request.getOrderId())
                .in(ServiceDispute::getDisputeStatus,
                        DisputeConstant.STATUS_PENDING,
                        DisputeConstant.STATUS_PROCESSING)
                .eq(ServiceDispute::getIsDelete, 0);
        long existingDisputeCount = this.count(queryWrapper);
        if (existingDisputeCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该订单已存在未解决的纠纷");
        }

        // 5. 处理举证图片（最多6张，入库相对路径）
        String storedEvidence = null;
        if (request.getEvidenceImages() != null && !request.getEvidenceImages().isEmpty()) {
            if (request.getEvidenceImages().size() > 6) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "举证图片最多上传6张");
            }
            List<String> keys = new ArrayList<>();
            for (String img : request.getEvidenceImages()) {
                if (StringUtils.isNotBlank(img)) {
                    keys.add(fileUrlResolver != null ? fileUrlResolver.toRelativeKey(img) : img.trim());
                }
            }
            if (!keys.isEmpty()) {
                storedEvidence = String.join(",", keys);
            }
        }

        // 6. 创建纠纷记录
        ServiceDispute dispute = new ServiceDispute();
        dispute.setOrderId(request.getOrderId());
        dispute.setDisputeInitiatorId(userId);
        dispute.setDisputeType(request.getDisputeType());
        dispute.setDescription(request.getDescription());
        dispute.setEvidenceImages(storedEvidence);
        dispute.setDisputeStatus(DisputeConstant.STATUS_PENDING);
        dispute.setCreateTime(LocalDateTime.now());
        dispute.setUpdateTime(LocalDateTime.now());
        dispute.setIsDelete(0);

        this.save(dispute);

        if (orderLogService != null) {
            orderLogService.recordLog(order.getId(), order.getOrderStatus(), order.getOrderStatus(),
                    OrderLogConstant.ACTION_DISPUTE_CREATE, "发起平台仲裁纠纷：" + request.getDescription());
        }

        // 7. 通知对方：有人发起了纠纷
        Long recipientId = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        User initiator = userService.getById(userId);
        String initiatorName = initiator != null ? initiator.getNickname() : ("用户" + userId);
        String notificationContent = String.format("用户 %s 对订单 %s 发起了纠纷，平台已冻结自动确认收货计时，请及时关注",
                initiatorName, order.getOrderNumber());
        notificationService.sendDisputeNotification(recipientId, dispute.getId(), "纠纷通知", notificationContent);

        return dispute.getId();
    }

    @Override
    public Page<DisputeListItemVO> getMyDisputes(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        LambdaQueryWrapper<ServiceOrder> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.and(wrapper -> wrapper
                .eq(ServiceOrder::getBuyerId, userId)
                .or()
                .eq(ServiceOrder::getSellerId, userId));
        orderQueryWrapper.eq(ServiceOrder::getIsDelete, 0);
        List<ServiceOrder> orders = serviceOrderService.list(orderQueryWrapper);

        if (orders.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        List<Long> orderIds = orders.stream().map(ServiceOrder::getId).collect(Collectors.toList());

        Page<ServiceDispute> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceDispute> disputeQueryWrapper = new LambdaQueryWrapper<>();
        disputeQueryWrapper.in(ServiceDispute::getOrderId, orderIds)
                .eq(ServiceDispute::getIsDelete, 0)
                .orderByDesc(ServiceDispute::getCreateTime);

        Page<ServiceDispute> disputePage = this.page(page, disputeQueryWrapper);
        return buildDisputeListPage(disputePage, pageNum, pageSize);
    }

    @Override
    public DisputeDetailVO getDisputeDetail(Long disputeId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        ServiceDispute dispute = this.getById(disputeId);
        if (dispute == null || dispute.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "纠纷不存在");
        }

        ServiceOrder order = serviceOrderService.getById(dispute.getOrderId());
        if (order == null || order.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
        }

        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限查看该纠纷");
        }

        return assembleDisputeDetailVO(dispute, order);
    }

    // ==================== 管理员方法 ====================

    @Override
    public Page<DisputeListItemVO> listDisputesForAdmin(DisputeQueryRequest request) {
        Integer pageNum = request.getPageNum();
        Integer pageSize = request.getPageSize();

        Page<ServiceDispute> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceDispute> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceDispute::getIsDelete, 0);

        if (request.getDisputeStatus() != null) {
            queryWrapper.eq(ServiceDispute::getDisputeStatus, request.getDisputeStatus());
        }
        if (request.getDisputeType() != null) {
            queryWrapper.eq(ServiceDispute::getDisputeType, request.getDisputeType());
        }
        if (request.getInitiatorId() != null) {
            queryWrapper.eq(ServiceDispute::getDisputeInitiatorId, request.getInitiatorId());
        }
        if (request.getOrderNumber() != null && !request.getOrderNumber().isEmpty()) {
            LambdaQueryWrapper<ServiceOrder> orderQueryWrapper = new LambdaQueryWrapper<>();
            orderQueryWrapper.like(ServiceOrder::getOrderNumber, request.getOrderNumber())
                    .eq(ServiceOrder::getIsDelete, 0);
            List<ServiceOrder> matchedOrders = serviceOrderService.list(orderQueryWrapper);
            if (matchedOrders.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            List<Long> orderIds = matchedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList());
            queryWrapper.in(ServiceDispute::getOrderId, orderIds);
        }

        queryWrapper.orderByDesc(ServiceDispute::getCreateTime);
        Page<ServiceDispute> disputePage = this.page(page, queryWrapper);
        return buildDisputeListPage(disputePage, pageNum, pageSize);
    }

    @Override
    public DisputeDetailVO getDisputeDetailForAdmin(Long disputeId) {
        ServiceDispute dispute = this.getById(disputeId);
        if (dispute == null || dispute.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_EXIST);
        }

        ServiceOrder order = serviceOrderService.getById(dispute.getOrderId());
        if (order == null || order.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        return assembleDisputeDetailVO(dispute, order);
    }

    @Override
    public DisputeDetailVO getDisputeDetailByOrderId(Long orderId) {
        LambdaQueryWrapper<ServiceDispute> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServiceDispute::getOrderId, orderId)
                .eq(ServiceDispute::getIsDelete, 0)
                .orderByAsc(ServiceDispute::getDisputeStatus)
                .orderByDesc(ServiceDispute::getCreateTime)
                .last("LIMIT 1");

        ServiceDispute dispute = this.getOne(queryWrapper);
        if (dispute == null) {
            return null;
        }
        return getDisputeDetailForAdmin(dispute.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleDisputeByAdmin(Long disputeId, DisputeHandleRequest request) {
        Long adminId = UserContext.getCurrentUserId();
        if (adminId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 1. 先读取纠纷以定位订单ID
        ServiceDispute initialDispute = this.getById(disputeId);
        if (initialDispute == null || initialDispute.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_EXIST);
        }

        // 2. 严格按统一锁顺序加锁：先锁 ServiceOrder，再锁 ServiceDispute，防止与买家确认收货并发死锁或重复退钱
        ServiceOrder order = serviceOrderMapper.selectForUpdate(initialDispute.getOrderId());
        if (order == null || (order.getIsDelete() != null && order.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        ServiceDispute dispute = baseMapper.selectForUpdate(disputeId);
        if (dispute == null || dispute.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_EXIST);
        }

        // 3. 在行锁保护下校验纠纷状态
        Integer currentStatus = dispute.getDisputeStatus();
        if (!DisputeConstant.STATUS_PENDING.equals(currentStatus)
                && !DisputeConstant.STATUS_PROCESSING.equals(currentStatus)) {
            throw new BusinessException(ErrorCode.DISPUTE_STATUS_ERROR, "该纠纷已处理完成，无法再次处理");
        }

        // 4. 根据处理动作更新纠纷状态
        Integer actionType = request.getActionType();
        Integer newStatus;
        if (DisputeHandleRequest.ACTION_RESOLVE == actionType) {
            newStatus = DisputeConstant.STATUS_RESOLVED;
        } else if (DisputeHandleRequest.ACTION_REJECT == actionType) {
            newStatus = DisputeConstant.STATUS_REJECTED;
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的处理动作类型");
        }

        // 5. 若裁决支持退款，执行原路解冻退回
        boolean refundExecuted = false;
        if (Boolean.TRUE.equals(request.getNeedRefund()) && DisputeHandleRequest.ACTION_RESOLVE == actionType) {
            if (OrderConstant.PAYMENT_PAID == order.getPaymentStatus()
                    && order.getFrozenAmount() != null
                    && order.getFrozenAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                    && (OrderConstant.ORDER_PENDING_SHIP == order.getOrderStatus()
                        || OrderConstant.ORDER_PENDING_RECEIPT == order.getOrderStatus())) {
                String refundReason = "管理员仲裁退款：" + request.getResolution();
                serviceOrderService.adminRefundOrder(order.getId(), refundReason);
                refundExecuted = true;
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该订单担保资金已结算或无可退冻结余额，无法执行原路退款（请取消勾选退款仅作裁决）");
            }
        }

        // 6. 更新纠纷记录
        dispute.setDisputeStatus(newStatus);
        dispute.setAdminReply(request.getAdminReply());
        dispute.setResolution(request.getResolution());
        dispute.setHandledBy(adminId);
        dispute.setUpdateTime(LocalDateTime.now());
        this.updateById(dispute);

        // 7. 发送通知给买家和卖家
        String notificationTitle;
        if (DisputeHandleRequest.ACTION_RESOLVE == actionType) {
            notificationTitle = refundExecuted ? "纠纷处理结果：已支持退款" : "纠纷处理结果：纠纷已解决";
        } else {
            notificationTitle = "纠纷处理结果：申请被驳回";
        }

        String notificationContent = String.format(
                "管理员已处理订单 %s 的纠纷。\n解决方案：%s\n处理意见：%s",
                order.getOrderNumber(),
                request.getResolution(),
                request.getAdminReply()
        );

        notificationService.sendDisputeNotification(order.getBuyerId(), disputeId, notificationTitle, notificationContent);
        notificationService.sendDisputeNotification(order.getSellerId(), disputeId, notificationTitle, notificationContent);
    }

    @Override
    public List<ChatMessageVO> getDisputeChatLogsForAdmin(Long disputeId) {
        ServiceDispute dispute = this.getById(disputeId);
        if (dispute == null || dispute.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_EXIST);
        }
        ServiceOrder order = serviceOrderService.getById(dispute.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        Long buyerId = order.getBuyerId();
        Long sellerId = order.getSellerId();
        LambdaQueryWrapper<UserChat> chatQuery = new LambdaQueryWrapper<>();
        chatQuery.eq(UserChat::getIsDelete, 0)
                .and(w -> w
                        .and(w1 -> w1.eq(UserChat::getSenderId, buyerId).eq(UserChat::getReceiverId, sellerId))
                        .or(w2 -> w2.eq(UserChat::getSenderId, sellerId).eq(UserChat::getReceiverId, buyerId)))
                .orderByDesc(UserChat::getCreateTime)
                .last("LIMIT 60");

        List<UserChat> recentDesc = userChatMapper.selectList(chatQuery);
        if (recentDesc == null || recentDesc.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.reverse(recentDesc);

        List<User> partyUsers = userMapper.selectBatchIds(Arrays.asList(buyerId, sellerId));
        Map<Long, User> userMap = new HashMap<>();
        if (partyUsers != null) {
            for (User u : partyUsers) {
                userMap.put(u.getId(), u);
            }
        }

        List<ChatMessageVO> result = new ArrayList<>();
        for (UserChat chat : recentDesc) {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setMessageId(chat.getId());
            if (Integer.valueOf(ChatConstant.MESSAGE_TYPE_IMAGE).equals(chat.getMessageType()) && fileUrlResolver != null) {
                vo.setContent(fileUrlResolver.toPublicUrl(chat.getContent()));
            } else {
                vo.setContent(chat.getContent());
            }
            vo.setMessageType(chat.getMessageType());
            vo.setIsRead(chat.getIsRead());
            vo.setRelatedOrderId(chat.getRelatedOrderId());
            vo.setRelatedServiceId(chat.getRelatedServiceId());
            vo.setCreateTime(chat.getCreateTime());

            User sender = userMap.get(chat.getSenderId());
            if (sender != null) {
                UserSimpleVO senderVO = new UserSimpleVO();
                senderVO.setUserId(sender.getId());
                senderVO.setRealName(StringUtils.isNotBlank(sender.getNickname()) ? sender.getNickname() : sender.getRealName());
                senderVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(sender.getAvatar()) : sender.getAvatar());
                vo.setSender(senderVO);
            }
            User receiver = userMap.get(chat.getReceiverId());
            if (receiver != null) {
                UserSimpleVO receiverVO = new UserSimpleVO();
                receiverVO.setUserId(receiver.getId());
                receiverVO.setRealName(StringUtils.isNotBlank(receiver.getNickname()) ? receiver.getNickname() : receiver.getRealName());
                receiverVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(receiver.getAvatar()) : receiver.getAvatar());
                vo.setReceiver(receiverVO);
            }
            result.add(vo);
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private Page<DisputeListItemVO> buildDisputeListPage(Page<ServiceDispute> disputePage, Integer pageNum, Integer pageSize) {
        Page<DisputeListItemVO> voPage = new Page<>(pageNum, pageSize, disputePage.getTotal());
        List<ServiceDispute> records = disputePage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(Collections.<DisputeListItemVO>emptyList());
            return voPage;
        }

        Set<Long> orderIds = records.stream().map(ServiceDispute::getOrderId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, ServiceOrder> orderMap = new HashMap<>();
        Set<Long> serviceIds = new HashSet<>();
        if (!orderIds.isEmpty()) {
            List<ServiceOrder> orderList = serviceOrderService.listByIds(orderIds);
            for (ServiceOrder o : orderList) {
                orderMap.put(o.getId(), o);
                if (o.getServiceId() != null) {
                    serviceIds.add(o.getServiceId());
                }
            }
        }

        Map<Long, Service> serviceMap = new HashMap<>();
        if (!serviceIds.isEmpty()) {
            List<Service> services = serviceService.listByIds(serviceIds);
            for (Service s : services) {
                serviceMap.put(s.getId(), s);
            }
        }

        List<DisputeListItemVO> voList = new ArrayList<>();
        for (ServiceDispute dispute : records) {
            DisputeListItemVO vo = new DisputeListItemVO();
            vo.setDisputeId(dispute.getId());
            vo.setOrderId(dispute.getOrderId());
            vo.setDisputeType(dispute.getDisputeType());
            vo.setDisputeStatus(dispute.getDisputeStatus());
            vo.setCreateTime(dispute.getCreateTime());

            ServiceOrder order = orderMap.get(dispute.getOrderId());
            if (order != null) {
                vo.setOrderNumber(order.getOrderNumber());
                vo.setTotalAmount(order.getTotalAmount());
                Service svc = serviceMap.get(order.getServiceId());
                if (svc != null) {
                    vo.setServiceTitle(svc.getServiceTitle());
                }
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    private DisputeDetailVO assembleDisputeDetailVO(ServiceDispute dispute, ServiceOrder order) {
        User buyer = userService.getById(order.getBuyerId());
        User seller = userService.getById(order.getSellerId());

        DisputeDetailVO vo = new DisputeDetailVO();
        BeanUtils.copyProperties(dispute, vo);
        vo.setDisputeId(dispute.getId());

        vo.setOrderId(order.getId());
        vo.setOrderNumber(order.getOrderNumber());
        Service service = serviceService.getById(order.getServiceId());
        vo.setServiceTitle(service != null ? service.getServiceTitle() : "未知服务");
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFrozenAmount(order.getFrozenAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTradeType(order.getTradeType());
        vo.setDeliveryNote(order.getDeliveryNote());
        vo.setDeliveryImages(toPublicUrlList(order.getDeliveryImages()));
        vo.setEvidenceImages(toPublicUrlList(dispute.getEvidenceImages()));

        vo.setBuyerId(order.getBuyerId());
        vo.setBuyerName(buyer != null ? buyer.getNickname() : "未知");
        vo.setBuyerAvatar(buyer != null ? (fileUrlResolver != null ? fileUrlResolver.toPublicUrl(buyer.getAvatar()) : buyer.getAvatar()) : null);
        vo.setSellerId(order.getSellerId());
        vo.setSellerName(seller != null ? seller.getNickname() : "未知");
        vo.setSellerAvatar(seller != null ? (fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar()) : null);

        return vo;
    }

    private List<String> toPublicUrlList(String storedCsv) {
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
}
