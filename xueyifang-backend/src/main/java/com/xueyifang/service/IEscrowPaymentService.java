package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.wallet.EscrowPrepayRequest;
import com.xueyifang.domain.dto.wallet.EscrowWebhookRequest;
import com.xueyifang.domain.po.EscrowPaymentOrder;
import com.xueyifang.domain.vo.EscrowPrepayVO;

public interface IEscrowPaymentService extends IService<EscrowPaymentOrder> {

    EscrowPrepayVO createEscrowPrepay(EscrowPrepayRequest request);

    void handleVerifiedWebhook(EscrowWebhookRequest webhook);
}
