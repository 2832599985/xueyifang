package com.xueyifang.service.impl;

import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.mapper.WalletTransactionMapper;
import com.xueyifang.service.IWalletTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 钱包流水表 服务实现类
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@Service
@Slf4j
public class WalletTransactionServiceImpl extends ServiceImpl<WalletTransactionMapper, WalletTransaction> implements IWalletTransactionService {

}
