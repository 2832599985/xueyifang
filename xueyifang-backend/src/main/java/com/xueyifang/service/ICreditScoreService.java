package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.po.CreditScoreLog;

import java.util.List;

public interface ICreditScoreService extends IService<CreditScoreLog> {

    int adjustCreditScore(Long userId, int delta, String reasonCode, String reasonDesc, Long relatedOrderId);

    List<CreditScoreLog> listMyCreditLogs(Long userId);
}
