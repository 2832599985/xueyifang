package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.RiskAlertQueryRequest;
import com.xueyifang.domain.dto.RiskAlertResolveRequest;
import com.xueyifang.domain.vo.RiskAuditAlertVO;
import com.xueyifang.domain.vo.RiskRadarOverviewVO;

import java.util.List;

public interface IRiskRadarService {

    /**
     * 获取风控审计雷达全局态势指标
     */
    RiskRadarOverviewVO getRadarOverview();

    /**
     * 分页查询风控审计预警列表
     */
    Page<RiskAuditAlertVO> pageAlerts(RiskAlertQueryRequest request);

    /**
     * 扫描近期订单交易关系图谱，识别双向对倒互刷、同对高频交易与秒拍秒确异常
     *
     * @param lookbackDays 回溯天数（默认 7 天）
     * @return 本次新发现并落库的风控预警条数
     */
    int scanRecentOrdersForRisk(int lookbackDays);

    /**
     * 针对单笔订单实时执行风控规则检测
     */
    List<RiskAuditAlertVO> evaluateOrderRisk(Long orderId);

    /**
     * 管理员核查处置风控预警（支持双向扣减信用分、强制下架服务并发送站内警示通知）
     */
    RiskAuditAlertVO resolveAlert(Long adminId, Long alertId, RiskAlertResolveRequest request);
}
