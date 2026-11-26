package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.dto.wallet.WalletRechargeRequest;
import com.xueyifang.domain.dto.wallet.WalletTransactionQueryRequest;
import com.xueyifang.domain.dto.wallet.WalletWithdrawRequest;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.domain.vo.WalletBalanceVO;
import com.xueyifang.domain.vo.WalletTransactionVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IUserService;
import com.xueyifang.service.IWalletService;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletServiceImpl implements IWalletService {

    private static final int DEFAULT_SINGLE_RECHARGE_LIMIT = 5000;
    private static final int DEFAULT_DAILY_RECHARGE_LIMIT = 20000;
    private static final BigDecimal MAX_SINGLE_WITHDRAW = new BigDecimal("5000.00");

    @Resource
    private IUserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IWalletTransactionService walletTransactionService;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private ISysConfigService sysConfigService;

    @Override
    public WalletBalanceVO getCurrentUserWalletBalance() {
        Long currentUserId = UserContext.getCurrentUserId();
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        WalletBalanceVO vo = new WalletBalanceVO();
        vo.setWalletBalance(user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO);
        vo.setFrozenAmount(user.getFrozenAmount() != null ? user.getFrozenAmount() : BigDecimal.ZERO);
        vo.setTotalAmount(vo.getWalletBalance().add(vo.getFrozenAmount()));
        return vo;
    }

    @Override
    public Page<WalletTransactionVO> pageTransactions(WalletTransactionQueryRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();

        QueryWrapper<WalletTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentUserId);
        if (request.getTransactionType() != null) {
            queryWrapper.eq("transaction_type", request.getTransactionType());
        }
        if (request.getStartTime() != null) {
            queryWrapper.ge("create_time", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le("create_time", request.getEndTime());
        }
        queryWrapper.orderByDesc("create_time");

        Page<WalletTransaction> page = new Page<>(request.getPageNum(), request.getPageSize());
        walletTransactionService.page(page, queryWrapper);

        Page<WalletTransactionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<WalletTransaction> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(Collections.<WalletTransactionVO>emptyList());
            return voPage;
        }

        Set<Long> orderIds = records.stream()
                .map(WalletTransaction::getRelatedOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<ServiceOrder> orders = serviceOrderService.listByIds(orderIds);
            for (ServiceOrder o : orders) {
                orderNoMap.put(o.getId(), o.getOrderNumber());
            }
        }

        List<WalletTransactionVO> voList = records.stream()
                .map(tx -> convertToVO(tx, orderNoMap))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletBalanceVO recharge(WalletRechargeRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        BigDecimal amount = request != null ? request.getAmount() : null;
        validateMonetaryAmount(amount, "充值");

        // 生产商业模式下默认关闭无真实支付渠道背书的虚拟直充（规避无牌照预付资金池风险，引导走 /payment/escrow/prepay）
        if (sysConfigService != null) {
            String demoFlag = sysConfigService.getConfigValue(SysConfigConstant.WALLET_DEMO_RECHARGE_ENABLED, "1");
            if ("0".equals(demoFlag)) {
                throw new BusinessException(ErrorCode.WALLET_DEMO_DISABLED);
            }
        }

        int singleLimitInt = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.WALLET_SINGLE_RECHARGE_LIMIT, DEFAULT_SINGLE_RECHARGE_LIMIT)
                : DEFAULT_SINGLE_RECHARGE_LIMIT;
        BigDecimal singleLimit = BigDecimal.valueOf(singleLimitInt);
        if (amount.compareTo(singleLimit) > 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID,
                    String.format("单笔充值金额不能超过 %.2f 元", singleLimit));
        }

        User user = userMapper.selectForUpdate(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (Integer.valueOf(UserConstant.ACCOUNT_DISABLED).equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账户已被限制交易，无法充值");
        }

        int dailyLimitInt = sysConfigService != null
                ? sysConfigService.getIntValue(SysConfigConstant.WALLET_DAILY_RECHARGE_LIMIT, DEFAULT_DAILY_RECHARGE_LIMIT)
                : DEFAULT_DAILY_RECHARGE_LIMIT;
        BigDecimal dailyLimit = BigDecimal.valueOf(dailyLimitInt);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<WalletTransaction> todayRecharges = walletTransactionService.list(
                new QueryWrapper<WalletTransaction>()
                        .eq("user_id", currentUserId)
                        .eq("transaction_type", WalletConstant.TRANSACTION_TYPE_RECHARGE)
                        .ge("create_time", todayStart));
        BigDecimal todayTotal = BigDecimal.ZERO;
        if (todayRecharges != null) {
            for (WalletTransaction tx : todayRecharges) {
                if (tx.getAmount() != null) {
                    todayTotal = todayTotal.add(tx.getAmount());
                }
            }
        }
        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID,
                    String.format("今日累计充值已达风控上限（单日限额 %.2f 元）", dailyLimit));
        }

        BigDecimal balanceBefore = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal frozenBefore = user.getFrozenAmount() != null ? user.getFrozenAmount() : BigDecimal.ZERO;

        BigDecimal balanceAfter = balanceBefore.add(amount);
        int updateRows = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .set(User::getWalletBalance, balanceAfter));
        if (updateRows == 0) {
            throw new BusinessException(ErrorCode.WALLET_RECHARGE_FAILED, "充值失败，请稍后重试");
        }

        recordTransaction(currentUserId, WalletConstant.TRANSACTION_TYPE_RECHARGE, amount,
                balanceBefore, balanceAfter, frozenBefore, frozenBefore, null, "沙盒联调钱包充值");

        return getCurrentUserWalletBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletBalanceVO withdraw(WalletWithdrawRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        BigDecimal amount = request != null ? request.getAmount() : null;
        validateMonetaryAmount(amount, "提现");

        if (amount.compareTo(MAX_SINGLE_WITHDRAW) > 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID,
                    String.format("单笔提现金额不能超过 %.2f 元", MAX_SINGLE_WITHDRAW));
        }

        User user = userMapper.selectForUpdate(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (Integer.valueOf(UserConstant.ACCOUNT_DISABLED).equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账户已被限制交易，无法提现");
        }

        BigDecimal balanceBefore = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal frozenBefore = user.getFrozenAmount() != null ? user.getFrozenAmount() : BigDecimal.ZERO;

        if (balanceBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH,
                    String.format("可提现收益余额不足，当前余额：%s，提现金额：%s", balanceBefore, amount));
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        int updateRows = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .set(User::getWalletBalance, balanceAfter));
        if (updateRows == 0) {
            throw new BusinessException(ErrorCode.WALLET_WITHDRAW_FAILED, "提现失败，请稍后重试");
        }

        recordTransaction(currentUserId, WalletConstant.TRANSACTION_TYPE_WITHDRAW, amount,
                balanceBefore, balanceAfter, frozenBefore, frozenBefore, null, "服务收益提现出账");

        return getCurrentUserWalletBalance();
    }

    private void validateMonetaryAmount(BigDecimal amount, String actionLabel) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID, actionLabel + "金额必须大于0");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID, actionLabel + "金额最多保留两位小数");
        }
    }

    private void recordTransaction(Long userId, Integer transactionType, BigDecimal amount,
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存钱包交易流水失败");
        }
    }

    private WalletTransactionVO convertToVO(WalletTransaction transaction, Map<Long, String> orderNoMap) {
        WalletTransactionVO vo = new WalletTransactionVO();
        vo.setTransactionId(transaction.getId());
        vo.setTransactionType(transaction.getTransactionType());
        vo.setTypeName(WalletConstant.getTransactionTypeName(transaction.getTransactionType()));
        vo.setAmount(transaction.getAmount());
        vo.setBalanceBefore(transaction.getBalanceBefore());
        vo.setBalanceAfter(transaction.getBalanceAfter());
        vo.setFrozenBefore(transaction.getFrozenBefore());
        vo.setFrozenAfter(transaction.getFrozenAfter());
        vo.setRelatedOrderId(transaction.getRelatedOrderId());
        vo.setTransactionNo(transaction.getTransactionNo());
        vo.setRemark(transaction.getRemark());
        vo.setCreateTime(transaction.getCreateTime());

        if (transaction.getRelatedOrderId() != null) {
            vo.setRelatedOrderNumber(orderNoMap.get(transaction.getRelatedOrderId()));
        }
        return vo;
    }
}
