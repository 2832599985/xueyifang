package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.wallet.WalletRechargeRequest;
import com.xueyifang.domain.dto.wallet.WalletTransactionQueryRequest;
import com.xueyifang.domain.dto.wallet.WalletWithdrawRequest;
import com.xueyifang.domain.vo.WalletBalanceVO;
import com.xueyifang.domain.vo.WalletTransactionVO;

/**
 * 钱包服务接口
 *
 * @author xueyifang
 */
public interface IWalletService {

    /**
     * 查询当前用户钱包余额
     *
     * @return 钱包余额信息
     */
    WalletBalanceVO getCurrentUserWalletBalance();

    /**
     * 分页查询当前用户交易记录
     *
     * @param request 查询请求
     * @return 交易记录分页列表
     */
    Page<WalletTransactionVO> pageTransactions(WalletTransactionQueryRequest request);

    /**
     * 充值
     *
     * @param request 充值请求
     * @return 更新后的钱包余额信息
     */
    WalletBalanceVO recharge(WalletRechargeRequest request);

    /**
     * 提现
     *
     * @param request 提现请求
     * @return 更新后的钱包余额信息
     */
    WalletBalanceVO withdraw(WalletWithdrawRequest request);
}
