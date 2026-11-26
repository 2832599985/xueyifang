package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.wallet.WalletRechargeRequest;
import com.xueyifang.domain.dto.wallet.WalletTransactionQueryRequest;
import com.xueyifang.domain.dto.wallet.WalletWithdrawRequest;
import com.xueyifang.domain.vo.WalletBalanceVO;
import com.xueyifang.domain.vo.WalletTransactionVO;
import com.xueyifang.service.IWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 钱包管理 前端控制器
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@RestController
@RequestMapping("/wallet")
@Api(tags = "钱包管理")
@Slf4j
public class WalletController {

    @Resource
    private IWalletService walletService;

    @GetMapping("/balance")
    @ApiOperation("查询钱包余额")
    public BaseResponse<WalletBalanceVO> getBalance() {
        log.info("收到查询钱包余额请求");
        WalletBalanceVO result = walletService.getCurrentUserWalletBalance();
        return ResultUtils.success(result);
    }

    @GetMapping("/transactions")
    @ApiOperation("查询交易记录")
    public BaseResponse<Page<WalletTransactionVO>> pageTransactions(WalletTransactionQueryRequest request) {
        log.info("收到查询交易记录请求，请求参数：{}", request);
        Page<WalletTransactionVO> result = walletService.pageTransactions(request);
        return ResultUtils.success(result);
    }

    @PostMapping("/recharge")
    @ApiOperation("钱包充值")
    public BaseResponse<WalletBalanceVO> recharge(@Valid @RequestBody WalletRechargeRequest request) {
        log.info("收到钱包充值请求，充值金额：{}", request.getAmount());
        WalletBalanceVO result = walletService.recharge(request);
        return ResultUtils.success(result);
    }

    @PostMapping("/withdraw")
    @ApiOperation("钱包提现")
    public BaseResponse<WalletBalanceVO> withdraw(@Valid @RequestBody WalletWithdrawRequest request) {
        log.info("收到钱包提现请求，提现金额：{}", request.getAmount());
        WalletBalanceVO result = walletService.withdraw(request);
        return ResultUtils.success(result);
    }
}
