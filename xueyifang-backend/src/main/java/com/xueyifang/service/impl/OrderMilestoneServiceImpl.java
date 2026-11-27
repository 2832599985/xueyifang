package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.OrderLogConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.dto.OrderMilestonePlanRequest;
import com.xueyifang.domain.po.OrderMilestone;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.OrderMilestoneMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICreditScoreService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IOrderMilestoneService;
import com.xueyifang.service.IServiceOrderLogService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IWalletTransactionService;
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
import java.util.List;

@Service
public class OrderMilestoneServiceImpl extends ServiceImpl<OrderMilestoneMapper, OrderMilestone> implements IOrderMilestoneService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUBMITTED = 1;
    public static final int STATUS_RELEASED = 2;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IWalletTransactionService walletTransactionService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private IServiceOrderLogService orderLogService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private ICreditScoreService creditScoreService;

    @Override
    public List<OrderMilestone> listByOrderId(Long orderId) {
        Long userId = requireLoginId();
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        User u = UserContext.getCurrentUser();
        boolean isAdmin = u != null && Integer.valueOf(2).equals(u.getRole());
        if (!isAdmin && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该订单里程碑计划");
        }
        return this.list(new LambdaQueryWrapper<OrderMilestone>()
                .eq(OrderMilestone::getOrderId, orderId)
                .orderByAsc(OrderMilestone::getStageIndex));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderMilestone> saveMilestonePlan(Long orderId, OrderMilestonePlanRequest request) {
        Long userId = requireLoginId();
        if (orderId == null || request == null || request.getStages() == null || request.getStages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少配置 1 个阶段里程碑");
        }
        if (request.getStages().size() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单笔订单最多支持拆分 5 个阶段里程碑");
        }

        ServiceOrder order = serviceOrderMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅订单买卖双方可制定阶段里程碑计划");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅已托管付款且未完结的担保订单可设置分阶段里程碑");
        }

        List<OrderMilestone> existing = this.list(new LambdaQueryWrapper<OrderMilestone>()
                .eq(OrderMilestone::getOrderId, orderId));
        for (OrderMilestone m : existing) {
            if (Integer.valueOf(STATUS_RELEASED).equals(m.getMilestoneStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有阶段完成资金释放，无法重置里程碑计划");
            }
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (OrderMilestonePlanRequest.StageItem item : request.getStages()) {
            if (item == null || StringUtils.isBlank(item.getTitle()) || item.getAmount() == null
                    || item.getAmount().compareTo(new BigDecimal("1.00")) < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "每个里程碑需填写名称且金额不低于 1 元");
            }
            sum = sum.add(item.getAmount().setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal frozen = order.getFrozenAmount() != null ? order.getFrozenAmount() : BigDecimal.ZERO;
        if (sum.compareTo(frozen) != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("各阶段金额总和（¥%s）必须等于当前订单剩余托管冻结金额（¥%s）",
                            sum.toPlainString(), frozen.toPlainString()));
        }

        if (!existing.isEmpty()) {
            this.remove(new LambdaQueryWrapper<OrderMilestone>().eq(OrderMilestone::getOrderId, orderId));
        }

        List<OrderMilestone> created = new ArrayList<>(request.getStages().size());
        int idx = 1;
        for (OrderMilestonePlanRequest.StageItem item : request.getStages()) {
            OrderMilestone m = new OrderMilestone();
            m.setOrderId(orderId);
            m.setStageIndex(idx++);
            m.setTitle(item.getTitle().trim());
            m.setAmount(item.getAmount().setScale(2, RoundingMode.HALF_UP));
            m.setMilestoneStatus(STATUS_PENDING);
            m.setPlatformFee(BigDecimal.ZERO);
            m.setSellerNetAmount(BigDecimal.ZERO);
            m.setCreateTime(LocalDateTime.now());
            m.setUpdateTime(LocalDateTime.now());
            this.baseMapper.insert(m);
            created.add(m);
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitMilestoneProof(Long milestoneId, String proofNote) {
        Long userId = requireLoginId();
        if (StringUtils.isBlank(proofNote)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写本阶段交付成果说明");
        }
        OrderMilestone milestone = this.baseMapper.selectForUpdate(milestoneId);
        if (milestone == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "里程碑阶段不存在");
        }
        ServiceOrder order = serviceOrderMapper.selectForUpdate(milestone.getOrderId());
        if (order == null || !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅该订单卖方可提交阶段交付成果");
        }
        if (Integer.valueOf(STATUS_RELEASED).equals(milestone.getMilestoneStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该阶段已完成验收结算");
        }

        milestone.setProofNote(proofNote.trim());
        milestone.setMilestoneStatus(STATUS_SUBMITTED);
        milestone.setSubmittedTime(LocalDateTime.now());
        milestone.setUpdateTime(LocalDateTime.now());
        this.updateById(milestone);

        if (order.getOrderStatus() == OrderConstant.ORDER_PENDING_SHIP) {
            order.setOrderStatus(OrderConstant.ORDER_PENDING_RECEIPT);
            order.setSellerShipTime(LocalDateTime.now());
            serviceOrderMapper.updateById(order);
        }

        if (notificationService != null) {
            notificationService.sendOrderNotification(
                    order.getBuyerId(),
                    order.getId(),
                    "卖方已提交阶段里程碑成果",
                    String.format("订单 %s 第 %d 阶段「%s」已提交交付说明，请查收验收并释放对应托管款项",
                            order.getOrderNumber(), milestone.getStageIndex(), milestone.getTitle())
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseMilestone(Long milestoneId) {
        Long userId = requireLoginId();
        OrderMilestone milestone = this.baseMapper.selectForUpdate(milestoneId);
        if (milestone == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "里程碑阶段不存在");
        }
        if (Integer.valueOf(STATUS_RELEASED).equals(milestone.getMilestoneStatus())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMIT, "该阶段款项已释放，请勿重复操作");
        }

        ServiceOrder order = serviceOrderMapper.selectForUpdate(milestone.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅买方本人可验收并释放阶段托管资金");
        }
        if (order.getOrderStatus() != OrderConstant.ORDER_PENDING_SHIP
                && order.getOrderStatus() != OrderConstant.ORDER_PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持释放里程碑款项");
        }
        if (Integer.valueOf(OrderConstant.REFUND_PENDING).equals(order.getRefundStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单存在待处理退款，请先处理后再验收里程碑");
        }

        BigDecimal stageAmt = milestone.getAmount();
        BigDecimal orderFrozen = order.getFrozenAmount() != null ? order.getFrozenAmount() : BigDecimal.ZERO;
        if (orderFrozen.compareTo(stageAmt) < 0) {
            throw new BusinessException(ErrorCode.WALLET_FROZEN_NOT_ENOUGH, "订单剩余托管冻结金额不足");
        }

        // 按 ID 升序加悲观行锁，杜绝死锁
        Long firstId = Math.min(order.getBuyerId(), order.getSellerId());
        Long secondId = Math.max(order.getBuyerId(), order.getSellerId());
        User firstUser = userMapper.selectForUpdate(firstId);
        User secondUser = userMapper.selectForUpdate(secondId);
        User buyer = order.getBuyerId().equals(firstId) ? firstUser : secondUser;
        User seller = order.getSellerId().equals(firstId) ? firstUser : secondUser;

        if (buyer == null || seller == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        int bps = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.PLATFORM_COMMISSION_BPS, 500) : 500;
        BigDecimal fee = bps > 0
                ? stageAmt.multiply(BigDecimal.valueOf(bps)).divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal sellerNet = stageAmt.subtract(fee);

        // 扣减买方冻结金额
        BigDecimal buyerFrozenBefore = buyer.getFrozenAmount() != null ? buyer.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal buyerFrozenAfter = buyerFrozenBefore.subtract(stageAmt).max(BigDecimal.ZERO);
        buyer.setFrozenAmount(buyerFrozenAfter);
        userMapper.updateById(buyer);

        WalletTransaction unfreezeTx = new WalletTransaction();
        unfreezeTx.setUserId(buyer.getId());
        unfreezeTx.setTransactionType(WalletConstant.TRANSACTION_TYPE_UNFREEZE);
        unfreezeTx.setAmount(stageAmt);
        unfreezeTx.setBalanceBefore(buyer.getWalletBalance());
        unfreezeTx.setBalanceAfter(buyer.getWalletBalance());
        unfreezeTx.setFrozenBefore(buyerFrozenBefore);
        unfreezeTx.setFrozenAfter(buyerFrozenAfter);
        unfreezeTx.setRelatedOrderId(order.getId());
        unfreezeTx.setTransactionNo(SerialNoGenerator.nextTransactionNo(buyer.getId()));
        unfreezeTx.setRemark(String.format("验收释放里程碑 #%d「%s」托管款", milestone.getStageIndex(), milestone.getTitle()));
        unfreezeTx.setCreateTime(LocalDateTime.now());
        walletTransactionService.save(unfreezeTx);

        // 增加卖方可用余额
        BigDecimal sellerBalBefore = seller.getWalletBalance() != null ? seller.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal sellerBalAfter = sellerBalBefore.add(sellerNet);
        seller.setWalletBalance(sellerBalAfter);
        userMapper.updateById(seller);

        WalletTransaction incomeTx = new WalletTransaction();
        incomeTx.setUserId(seller.getId());
        incomeTx.setTransactionType(WalletConstant.TRANSACTION_TYPE_INCOME);
        incomeTx.setAmount(sellerNet);
        incomeTx.setBalanceBefore(sellerBalBefore);
        incomeTx.setBalanceAfter(sellerBalAfter);
        incomeTx.setFrozenBefore(seller.getFrozenAmount());
        incomeTx.setFrozenAfter(seller.getFrozenAmount());
        incomeTx.setRelatedOrderId(order.getId());
        incomeTx.setTransactionNo(SerialNoGenerator.nextTransactionNo(seller.getId()));
        incomeTx.setRemark(String.format("里程碑 #%d「%s」验收结算入账（服务费 ¥%s）",
                milestone.getStageIndex(), milestone.getTitle(), fee.toPlainString()));
        incomeTx.setCreateTime(LocalDateTime.now());
        walletTransactionService.save(incomeTx);

        // 更新里程碑状态
        milestone.setMilestoneStatus(STATUS_RELEASED);
        milestone.setReleasedTime(LocalDateTime.now());
        milestone.setPlatformFee(fee);
        milestone.setSellerNetAmount(sellerNet);
        milestone.setUpdateTime(LocalDateTime.now());
        this.updateById(milestone);

        // 更新订单剩余托管冻结额与累计分账额
        BigDecimal remainingFrozen = orderFrozen.subtract(stageAmt);
        order.setFrozenAmount(remainingFrozen);
        order.setPlatformFee((order.getPlatformFee() != null ? order.getPlatformFee() : BigDecimal.ZERO).add(fee));
        order.setSellerSettleAmount((order.getSellerSettleAmount() != null ? order.getSellerSettleAmount() : BigDecimal.ZERO).add(sellerNet));

        int prevStatus = order.getOrderStatus();
        if (remainingFrozen.compareTo(BigDecimal.ZERO) == 0) {
            order.setOrderStatus(OrderConstant.ORDER_COMPLETED);
            order.setBuyerConfirmTime(LocalDateTime.now());
            if (order.getSellerShipTime() == null) {
                order.setSellerShipTime(LocalDateTime.now());
            }
            serviceOrderMapper.updateById(order);
            if (orderLogService != null) {
                orderLogService.recordLog(order.getId(), prevStatus, OrderConstant.ORDER_COMPLETED,
                        OrderLogConstant.ACTION_BUYER_CONFIRM,
                        "全部阶段里程碑均已验收释放，担保订单圆满完结");
            }
            if (creditScoreService != null) {
                creditScoreService.adjustCreditScore(seller.getId(), 2, "ORDER_COMPLETE",
                        "完成分阶段里程碑担保履约 " + order.getOrderNumber(), order.getId());
            }
        } else {
            if (order.getOrderStatus() == OrderConstant.ORDER_PENDING_SHIP) {
                order.setOrderStatus(OrderConstant.ORDER_PENDING_RECEIPT);
            }
            serviceOrderMapper.updateById(order);
        }

        if (notificationService != null) {
            notificationService.sendOrderNotification(
                    seller.getId(),
                    order.getId(),
                    "收到里程碑阶段结算款",
                    String.format("买家已验收里程碑「%s」，净入账 ¥%s 已转入您的可用钱包",
                            milestone.getTitle(), sellerNet.toPlainString())
            );
        }
    }

    private Long requireLoginId() {
        User u = UserContext.getCurrentUser();
        if (u == null || u.getId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return u.getId();
    }
}
