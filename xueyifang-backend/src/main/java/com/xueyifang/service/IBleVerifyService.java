package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.BleVerifySubmitRequest;
import com.xueyifang.domain.po.BleVerifyRecord;
import com.xueyifang.domain.vo.BleSessionVO;

public interface IBleVerifyService extends IService<BleVerifyRecord> {

    BleSessionVO initOrJoinBleSession(Long orderId);

    void submitBleHandshakeAndSettle(Long orderId, BleVerifySubmitRequest request);
}
