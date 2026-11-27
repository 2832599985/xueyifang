package com.xueyifang.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.po.SellerDepositLedger;
import com.xueyifang.domain.po.ServiceDispute;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.SellerDepositLedgerMapper;
import com.xueyifang.mapper.ServiceDisputeMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IWalletTransactionService;
import com.xueyifang.utils.SerialNoGenerator;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wallet/deposit")
@Api(tags = "创作者履约保证金专户管理接口")
public class SellerDepositController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SellerDepositLedgerMapper depositLedgerMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceDisputeMapper serviceDisputeMapper;

    @Resource
    private IWalletTransactionService walletTransactionService;

    @GetMapping("/info")
    @ApiOperation("查询当前用户创作者履约保证金余额与变动台账")
    public BaseResponse<Map<String, Object>> getDepositInfo() {
        Long userId = requireLoginId();
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        List<SellerDepositLedger> logs = depositLedgerMapper.selectList(
                new LambdaQueryWrapper<SellerDepositLedger>()
                        .eq(SellerDepositLedger::getUserId, userId)
                        .orderByDesc(SellerDepositLedger::getCreateTime)
                        .last("LIMIT 20")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("sellerDeposit", u.getSellerDeposit() != null ? u.getSellerDeposit() : BigDecimal.ZERO);
        data.put("walletBalance", u.getWalletBalance() != null ? u.getWalletBalance() : BigDecimal.ZERO);
        data.put("logs", logs);
        return ResultUtils.success(data);
    }

    @PostMapping("/pay")
    @Transactional(rollbackFor = Exception.class)
    @ApiOperation("从钱包可用余额划转缴纳创作者履约保证金")
    public BaseResponse<Boolean> payDeposit(@RequestBody Map<String, Object> body) {
        Long userId = requireLoginId();
        BigDecimal amount = parseAmount(body);
        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        BigDecimal balBefore = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        if (balBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH, "钱包可用余额不足，请先完成订单收款或充值");
        }
        BigDecimal depBefore = user.getSellerDeposit() != null ? user.getSellerDeposit() : BigDecimal.ZERO;
        BigDecimal balAfter = balBefore.subtract(amount);
        BigDecimal depAfter = depBefore.add(amount);

        user.setWalletBalance(balAfter);
        user.setSellerDeposit(depAfter);
        userMapper.updateById(user);

        SellerDepositLedger ledger = new SellerDepositLedger();
        ledger.setLedgerNo(SerialNoGenerator.next("DEP"));
        ledger.setUserId(userId);
        ledger.setActionType(1);
        ledger.setAmount(amount);
        ledger.setDepositBefore(depBefore);
        ledger.setDepositAfter(depAfter);
        ledger.setRemark("主动缴纳创作者履约保证金");
        ledger.setCreateTime(LocalDateTime.now());
        depositLedgerMapper.insert(ledger);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setTransactionType(WalletConstant.TRANSACTION_TYPE_DEPOSIT);
        tx.setAmount(amount);
        tx.setBalanceBefore(balBefore);
        tx.setBalanceAfter(balAfter);
        tx.setFrozenBefore(user.getFrozenAmount());
        tx.setFrozenAfter(user.getFrozenAmount());
        tx.setTransactionNo(SerialNoGenerator.nextTransactionNo(userId));
        tx.setRemark("划转缴纳创作者履约保证金");
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionService.save(tx);

        return ResultUtils.success(true);
    }

    @PostMapping("/withdraw")
    @Transactional(rollbackFor = Exception.class)
    @ApiOperation("无在途订单与未结纠纷时申请退还履约保证金至可用余额")
    public BaseResponse<Boolean> withdrawDeposit(@RequestBody Map<String, Object> body) {
        Long userId = requireLoginId();
        BigDecimal amount = parseAmount(body);
        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        BigDecimal depBefore = user.getSellerDeposit() != null ? user.getSellerDeposit() : BigDecimal.ZERO;
        if (depBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "退还金额超出当前保证金余额");
        }

        // 校验是否存在未完结的在途销售订单
        Long activeSellOrders = serviceOrderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getSellerId, userId)
                .in(ServiceOrder::getOrderStatus, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.ORDER_PENDING_RECEIPT));
        if (activeSellOrders != null && activeSellOrders > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "您当前仍有 " + activeSellOrders + " 笔进行中的销售担保订单，需全部履约完结后方可退还保证金");
        }

        // 校验关联销售单是否存在未结案纠纷（disputeStatus = 1 或 2）
        List<ServiceOrder> sellerOrders = serviceOrderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .select(ServiceOrder::getId)
                .eq(ServiceOrder::getSellerId, userId));
        if (sellerOrders != null && !sellerOrders.isEmpty()) {
            List<Long> orderIds = sellerOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList());
            Long openDisputes = serviceDisputeMapper.selectCount(new LambdaQueryWrapper<ServiceDispute>()
                    .in(ServiceDispute::getOrderId, orderIds)
                    .in(ServiceDispute::getDisputeStatus, Arrays.asList(1, 2)));
            if (openDisputes != null && openDisputes > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "存在尚未结案的订单仲裁纠纷，暂不可退还履约保证金");
            }
        }

        BigDecimal balBefore = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal depAfter = depBefore.subtract(amount);
        BigDecimal balAfter = balBefore.add(amount);

        user.setSellerDeposit(depAfter);
        user.setWalletBalance(balAfter);
        userMapper.updateById(user);

        SellerDepositLedger ledger = new SellerDepositLedger();
        ledger.setLedgerNo(SerialNoGenerator.next("DEP"));
        ledger.setUserId(userId);
        ledger.setActionType(2);
        ledger.setAmount(amount);
        ledger.setDepositBefore(depBefore);
        ledger.setDepositAfter(depAfter);
        ledger.setRemark("无责退还履约保证金至钱包可用余额");
        ledger.setCreateTime(LocalDateTime.now());
        depositLedgerMapper.insert(ledger);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setTransactionType(WalletConstant.TRANSACTION_TYPE_REFUND);
        tx.setAmount(amount);
        tx.setBalanceBefore(balBefore);
        tx.setBalanceAfter(balAfter);
        tx.setFrozenBefore(user.getFrozenAmount());
        tx.setFrozenAfter(user.getFrozenAmount());
        tx.setTransactionNo(SerialNoGenerator.nextTransactionNo(userId));
        tx.setRemark("退还创作者履约保证金入账");
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionService.save(tx);

        return ResultUtils.success(true);
    }

    private BigDecimal parseAmount(Map<String, Object> body) {
        if (body == null || body.get("amount") == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入有效金额");
        }
        try {
            BigDecimal amt = new BigDecimal(body.get("amount").toString()).setScale(2, RoundingMode.HALF_UP);
            if (amt.compareTo(new BigDecimal("10.00")) < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "保证金单笔操作金额不低于 10 元");
            }
            return amt;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "金额格式无效");
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
