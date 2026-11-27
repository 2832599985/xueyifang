package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.WalletConstant;
import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.domain.po.EscrowPaymentOrder;
import com.xueyifang.domain.po.FinancialReconciliationDaily;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.po.PlatformRevenueLedger;
import com.xueyifang.domain.po.SellerDepositLedger;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.WalletTransaction;
import com.xueyifang.domain.vo.FinancialOverviewVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.AppointmentNoshowClaimMapper;
import com.xueyifang.mapper.CreditScoreLogMapper;
import com.xueyifang.mapper.EscrowPaymentOrderMapper;
import com.xueyifang.mapper.FinancialReconciliationDailyMapper;
import com.xueyifang.mapper.NotificationMapper;
import com.xueyifang.mapper.PlatformRevenueLedgerMapper;
import com.xueyifang.mapper.SellerDepositLedgerMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.mapper.WalletTransactionMapper;
import com.xueyifang.service.IFinancialReconciliationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class FinancialReconciliationServiceImpl implements IFinancialReconciliationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private FinancialReconciliationDailyMapper reconciliationDailyMapper;

    @Resource
    private AppointmentNoshowClaimMapper noshowClaimMapper;

    @Resource
    private PlatformRevenueLedgerMapper platformRevenueLedgerMapper;

    @Resource
    private SellerDepositLedgerMapper sellerDepositLedgerMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private WalletTransactionMapper walletTransactionMapper;

    @Resource
    private CreditScoreLogMapper creditScoreLogMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private EscrowPaymentOrderMapper escrowPaymentOrderMapper;

    @Override
    public FinancialOverviewVO getFinancialOverview() {
        List<PlatformRevenueLedger> revenueList = platformRevenueLedgerMapper.selectList(new LambdaQueryWrapper<>());
        if (revenueList == null) {
            revenueList = Collections.emptyList();
        }
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalSellerNet = BigDecimal.ZERO;
        for (PlatformRevenueLedger r : revenueList) {
            if (r.getCommissionAmount() != null) {
                totalCommission = totalCommission.add(r.getCommissionAmount());
            }
            if (r.getSellerNetAmount() != null) {
                totalSellerNet = totalSellerNet.add(r.getSellerNetAmount());
            }
        }

        // 计算全平台在途托管冻结资金池
        BigDecimal activeEscrowPool = computeCurrentEscrowFrozenPool();

        // 计算全平台创作者保证金在池总额
        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getIsDelete, 0));
        BigDecimal depositPool = BigDecimal.ZERO;
        if (allUsers != null) {
            for (User u : allUsers) {
                if (u.getSellerDeposit() != null && u.getSellerDeposit().compareTo(BigDecimal.ZERO) > 0) {
                    depositPool = depositPool.add(u.getSellerDeposit());
                }
            }
        }

        // 统计预约爽约自动赔付
        List<AppointmentNoshowClaim> allClaims = noshowClaimMapper.selectList(
                new LambdaQueryWrapper<AppointmentNoshowClaim>().orderByDesc(AppointmentNoshowClaim::getCreateTime));
        if (allClaims == null) {
            allClaims = Collections.emptyList();
        }
        BigDecimal totalNoshowComp = BigDecimal.ZERO;
        for (AppointmentNoshowClaim c : allClaims) {
            if (c.getCompensationAmount() != null) {
                totalNoshowComp = totalNoshowComp.add(c.getCompensationAmount());
            }
        }

        // 查询近期日终对账单
        List<FinancialReconciliationDaily> reports = reconciliationDailyMapper.selectList(
                new LambdaQueryWrapper<FinancialReconciliationDaily>()
                        .orderByDesc(FinancialReconciliationDaily::getReportDate)
                        .last("LIMIT 30"));
        if (reports == null) {
            reports = Collections.emptyList();
        }
        int discrepancyReports = 0;
        for (FinancialReconciliationDaily rep : reports) {
            if (rep.getReconcileStatus() != null && rep.getReconcileStatus() == 2) {
                discrepancyReports++;
            }
        }

        List<AppointmentNoshowClaim> recentClaims = allClaims.size() > 15 ? allClaims.subList(0, 15) : allClaims;

        return new FinancialOverviewVO()
                .setTotalPlatformCommission(totalCommission.setScale(2, RoundingMode.HALF_UP))
                .setTotalSellerSettled(totalSellerNet.setScale(2, RoundingMode.HALF_UP))
                .setCurrentEscrowFrozenPool(activeEscrowPool.setScale(2, RoundingMode.HALF_UP))
                .setCurrentSellerDepositPool(depositPool.setScale(2, RoundingMode.HALF_UP))
                .setTotalNoshowClaims(allClaims.size())
                .setTotalNoshowCompensationAmount(totalNoshowComp.setScale(2, RoundingMode.HALF_UP))
                .setTotalReconcileReports(reports.size())
                .setDiscrepancyReportCount(discrepancyReports)
                .setRecentReports(reports)
                .setRecentNoshowClaims(recentClaims);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancialReconciliationDaily generateOrRefreshDailyReport(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        String dateStr = date.format(DATE_FMT);
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // 1. 查询当日支付订单流入
        LambdaQueryWrapper<ServiceOrder> paidQuery = new LambdaQueryWrapper<>();
        paidQuery.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID)
                .ge(ServiceOrder::getPaymentTime, startOfDay)
                .le(ServiceOrder::getPaymentTime, endOfDay);
        List<ServiceOrder> paidOrders = serviceOrderMapper.selectList(paidQuery);
        if (paidOrders == null) {
            paidOrders = Collections.emptyList();
        }

        BigDecimal grossInflow = BigDecimal.ZERO;
        for (ServiceOrder o : paidOrders) {
            if (o.getTotalAmount() != null) {
                grossInflow = grossInflow.add(o.getTotalAmount());
            }
        }

        // 2. 查询当日完结结算订单并执行总分勾稽审计 (totalAmount == sellerSettleAmount + platformFee)
        LambdaQueryWrapper<ServiceOrder> completedQuery = new LambdaQueryWrapper<>();
        completedQuery.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED)
                .ge(ServiceOrder::getBuyerConfirmTime, startOfDay)
                .le(ServiceOrder::getBuyerConfirmTime, endOfDay);
        List<ServiceOrder> completedOrders = serviceOrderMapper.selectList(completedQuery);
        if (completedOrders == null) {
            completedOrders = Collections.emptyList();
        }

        BigDecimal sellerSettled = BigDecimal.ZERO;
        BigDecimal platformCommission = BigDecimal.ZERO;
        int discrepancyCount = 0;
        BigDecimal discrepancyAmount = BigDecimal.ZERO;

        for (ServiceOrder o : completedOrders) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal fee = o.getPlatformFee() != null ? o.getPlatformFee() : BigDecimal.ZERO;
            BigDecimal settle = o.getSellerSettleAmount() != null ? o.getSellerSettleAmount() : total.subtract(fee);

            sellerSettled = sellerSettled.add(settle);
            platformCommission = platformCommission.add(fee);

            BigDecimal diff = total.subtract(settle.add(fee)).abs();
            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                discrepancyCount++;
                discrepancyAmount = discrepancyAmount.add(diff);
            }
        }

        // 3. 查询当日退款订单流出
        LambdaQueryWrapper<ServiceOrder> refundQuery = new LambdaQueryWrapper<>();
        refundQuery.eq(ServiceOrder::getIsDelete, 0)
                .in(ServiceOrder::getRefundStatus, OrderConstant.REFUND_APPROVED, OrderConstant.REFUND_COMPLETED)
                .ge(ServiceOrder::getUpdateTime, startOfDay)
                .le(ServiceOrder::getUpdateTime, endOfDay);
        List<ServiceOrder> refundedOrders = serviceOrderMapper.selectList(refundQuery);
        if (refundedOrders == null) {
            refundedOrders = Collections.emptyList();
        }
        BigDecimal refundOutflow = BigDecimal.ZERO;
        for (ServiceOrder o : refundedOrders) {
            if (o.getTotalAmount() != null) {
                refundOutflow = refundOutflow.add(o.getTotalAmount());
            }
        }

        // 4. 查询当日预约爽约赔付单
        LambdaQueryWrapper<AppointmentNoshowClaim> claimQuery = new LambdaQueryWrapper<>();
        claimQuery.ge(AppointmentNoshowClaim::getCreateTime, startOfDay)
                .le(AppointmentNoshowClaim::getCreateTime, endOfDay);
        List<AppointmentNoshowClaim> dayClaims = noshowClaimMapper.selectList(claimQuery);
        if (dayClaims == null) {
            dayClaims = Collections.emptyList();
        }
        BigDecimal noshowPenalty = BigDecimal.ZERO;
        for (AppointmentNoshowClaim c : dayClaims) {
            if (c.getCompensationAmount() != null) {
                noshowPenalty = noshowPenalty.add(c.getCompensationAmount());
            }
        }

        BigDecimal activeEscrowPool = computeCurrentEscrowFrozenPool();
        int reconcileStatus = discrepancyCount == 0 ? 1 : 2;
        String auditNote = discrepancyCount == 0
                ? String.format("账期 %s 自动对账通过：担保流入 ¥%s，创作者分账 ¥%s + 平台抽佣 ¥%s，总分勾稽零差额。",
                dateStr,
                grossInflow.setScale(2, RoundingMode.HALF_UP),
                sellerSettled.setScale(2, RoundingMode.HALF_UP),
                platformCommission.setScale(2, RoundingMode.HALF_UP))
                : String.format("账期 %s 检测到 %d 笔订单分账勾稽差额合计 ¥%s，请财务复核。",
                dateStr, discrepancyCount, discrepancyAmount.setScale(2, RoundingMode.HALF_UP));

        LambdaQueryWrapper<FinancialReconciliationDaily> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(FinancialReconciliationDaily::getReportDate, dateStr);
        FinancialReconciliationDaily report = reconciliationDailyMapper.selectOne(existQuery);

        if (report == null) {
            String reportNo = "FIN" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            report = new FinancialReconciliationDaily()
                    .setReportNo(reportNo)
                    .setReportDate(dateStr)
                    .setCreateTime(LocalDateTime.now());
        }

        report.setPaidOrderCount(paidOrders.size())
                .setCompletedOrderCount(completedOrders.size())
                .setRefundOrderCount(refundedOrders.size())
                .setNoshowCompensatedCount(dayClaims.size())
                .setGrossEscrowInflow(grossInflow.setScale(2, RoundingMode.HALF_UP))
                .setSellerSettledAmount(sellerSettled.setScale(2, RoundingMode.HALF_UP))
                .setPlatformCommissionAmount(platformCommission.setScale(2, RoundingMode.HALF_UP))
                .setRefundOutflowAmount(refundOutflow.setScale(2, RoundingMode.HALF_UP))
                .setNoshowPenaltyAmount(noshowPenalty.setScale(2, RoundingMode.HALF_UP))
                .setActiveEscrowFrozenPool(activeEscrowPool.setScale(2, RoundingMode.HALF_UP))
                .setDiscrepancyCount(discrepancyCount)
                .setDiscrepancyAmount(discrepancyAmount.setScale(2, RoundingMode.HALF_UP))
                .setReconcileStatus(reconcileStatus)
                .setAuditNote(auditNote)
                .setUpdateTime(LocalDateTime.now());

        if (report.getId() == null) {
            reconciliationDailyMapper.insert(report);
        } else {
            reconciliationDailyMapper.updateById(report);
        }
        return report;
    }

    private BigDecimal computeCurrentEscrowFrozenPool() {
        LambdaQueryWrapper<ServiceOrder> activeQuery = new LambdaQueryWrapper<>();
        activeQuery.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID)
                .in(ServiceOrder::getOrderStatus, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.ORDER_PENDING_RECEIPT);
        List<ServiceOrder> activeOrders = serviceOrderMapper.selectList(activeQuery);
        BigDecimal pool = BigDecimal.ZERO;
        if (activeOrders != null) {
            for (ServiceOrder o : activeOrders) {
                BigDecimal frozen = o.getFrozenAmount() != null ? o.getFrozenAmount() : o.getTotalAmount();
                if (frozen != null && frozen.compareTo(BigDecimal.ZERO) > 0) {
                    pool = pool.add(frozen);
                }
            }
        }
        return pool;
    }

    @Override
    public String exportFinancialReconciliationCsv(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        String startStr = start.format(DATE_FMT);
        String endStr = end.format(DATE_FMT);

        LambdaQueryWrapper<FinancialReconciliationDaily> reportQuery = new LambdaQueryWrapper<>();
        reportQuery.ge(FinancialReconciliationDaily::getReportDate, startStr)
                .le(FinancialReconciliationDaily::getReportDate, endStr)
                .orderByDesc(FinancialReconciliationDaily::getReportDate);
        List<FinancialReconciliationDaily> reports = reconciliationDailyMapper.selectList(reportQuery);
        if (reports == null) {
            reports = Collections.emptyList();
        }

        LocalDateTime startDt = LocalDateTime.of(start, LocalTime.MIN);
        LocalDateTime endDt = LocalDateTime.of(end, LocalTime.MAX);

        List<PlatformRevenueLedger> ledgers = platformRevenueLedgerMapper.selectList(
                new LambdaQueryWrapper<PlatformRevenueLedger>()
                        .ge(PlatformRevenueLedger::getCreateTime, startDt)
                        .le(PlatformRevenueLedger::getCreateTime, endDt)
                        .orderByDesc(PlatformRevenueLedger::getCreateTime));
        if (ledgers == null) {
            ledgers = Collections.emptyList();
        }

        List<AppointmentNoshowClaim> claims = noshowClaimMapper.selectList(
                new LambdaQueryWrapper<AppointmentNoshowClaim>()
                        .ge(AppointmentNoshowClaim::getCreateTime, startDt)
                        .le(AppointmentNoshowClaim::getCreateTime, endDt)
                        .orderByDesc(AppointmentNoshowClaim::getCreateTime));
        if (claims == null) {
            claims = Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM 确保 Microsoft Excel / WPS 打开中文零乱码
        sb.append('\uFEFF');
        sb.append("【一、学艺坊平台财务日终对账汇总表】(").append(startStr).append(" 至 ").append(endStr).append(")\n");
        sb.append("账期,对账批次号,支付订单数,完结结算数,退款订单数,爽约赔付单数,担保流入总额(元),创作者结算净额(元),平台抽佣净额(元),退回买方总额(元),爽约违约赔付额(元),在途托管冻结池(元),勾稽差异笔数,勾稽差额(元),对账结论,审计摘要\n");
        for (FinancialReconciliationDaily r : reports) {
            sb.append(csvCell(r.getReportDate())).append(',')
                    .append(csvCell(r.getReportNo())).append(',')
                    .append(r.getPaidOrderCount() != null ? r.getPaidOrderCount() : 0).append(',')
                    .append(r.getCompletedOrderCount() != null ? r.getCompletedOrderCount() : 0).append(',')
                    .append(r.getRefundOrderCount() != null ? r.getRefundOrderCount() : 0).append(',')
                    .append(r.getNoshowCompensatedCount() != null ? r.getNoshowCompensatedCount() : 0).append(',')
                    .append(fmtMoney(r.getGrossEscrowInflow())).append(',')
                    .append(fmtMoney(r.getSellerSettledAmount())).append(',')
                    .append(fmtMoney(r.getPlatformCommissionAmount())).append(',')
                    .append(fmtMoney(r.getRefundOutflowAmount())).append(',')
                    .append(fmtMoney(r.getNoshowPenaltyAmount())).append(',')
                    .append(fmtMoney(r.getActiveEscrowFrozenPool())).append(',')
                    .append(r.getDiscrepancyCount() != null ? r.getDiscrepancyCount() : 0).append(',')
                    .append(fmtMoney(r.getDiscrepancyAmount())).append(',')
                    .append(r.getReconcileStatus() != null && r.getReconcileStatus() == 1 ? "总分账平" : "存在差额预警").append(',')
                    .append(csvCell(r.getAuditNote())).append('\n');
        }

        sb.append("\n【二、平台技术服务费抽佣与创作者分账明细台账】\n");
        sb.append("分账流水号,关联订单ID,创作者ID,订单总额(元),抽佣费率(BPS),平台技术服务费(元),创作者实得净额(元),结算时间\n");
        for (PlatformRevenueLedger l : ledgers) {
            sb.append(csvCell(l.getLedgerNo())).append(',')
                    .append(l.getOrderId() != null ? l.getOrderId() : "").append(',')
                    .append(l.getSellerId() != null ? l.getSellerId() : "").append(',')
                    .append(fmtMoney(l.getOrderTotalAmount())).append(',')
                    .append(l.getCommissionBps() != null ? l.getCommissionBps() : 0).append(',')
                    .append(fmtMoney(l.getCommissionAmount())).append(',')
                    .append(fmtMoney(l.getSellerNetAmount())).append(',')
                    .append(l.getCreateTime() != null ? l.getCreateTime().format(DT_FMT) : "").append('\n');
        }

        sb.append("\n【三、预约爽约自动赔付存证与保证金划扣明细】\n");
        sb.append("赔付单号,关联订单号,买方ID,卖方ID,原订单金额(元),退回买方本金(元),违约赔付买方金额(元),赔付资金来源,扣减卖方信用分,赔付存证说明,执行时间\n");
        for (AppointmentNoshowClaim c : claims) {
            sb.append(csvCell(c.getClaimNo())).append(',')
                    .append(csvCell(c.getOrderNumber())).append(',')
                    .append(c.getBuyerId() != null ? c.getBuyerId() : "").append(',')
                    .append(c.getSellerId() != null ? c.getSellerId() : "").append(',')
                    .append(fmtMoney(c.getOrderAmount())).append(',')
                    .append(fmtMoney(c.getRefundToBuyerAmount())).append(',')
                    .append(fmtMoney(c.getCompensationAmount())).append(',')
                    .append(csvCell(c.getCompensationSource())).append(',')
                    .append(c.getCreditDeducted() != null ? c.getCreditDeducted() : 0).append(',')
                    .append(csvCell(c.getEvidenceNote())).append(',')
                    .append(c.getCreateTime() != null ? c.getCreateTime().format(DT_FMT) : "").append('\n');
        }
        return sb.toString();
    }

    private String fmtMoney(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private String csvCell(String val) {
        if (val == null) {
            return "";
        }
        String cleaned = val.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        return "\"" + cleaned + "\"";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentNoshowClaim processSellerNoShowCompensation(Long operatorId, Long orderId, String evidenceNote) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        ServiceOrder order = serviceOrderMapper.selectForUpdate(orderId);
        if (order == null || (order.getIsDelete() != null && order.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        // 幂等防重检查
        AppointmentNoshowClaim existing = getClaimByOrderId(orderId);
        if (existing != null) {
            return existing;
        }

        if (!Integer.valueOf(OrderConstant.PAYMENT_PAID).equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "未支付或已退款订单不适用预约爽约赔付");
        }
        if (!Integer.valueOf(OrderConstant.ORDER_PENDING_SHIP).equals(order.getOrderStatus())
                && !Integer.valueOf(OrderConstant.ORDER_PENDING_RECEIPT).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持预约爽约赔付");
        }

        Long firstUserId = Math.min(order.getBuyerId(), order.getSellerId());
        Long secondUserId = Math.max(order.getBuyerId(), order.getSellerId());
        User firstUser = userMapper.selectForUpdate(firstUserId);
        User secondUser = userMapper.selectForUpdate(secondUserId);
        User buyer = order.getBuyerId().equals(firstUserId) ? firstUser : secondUser;
        User seller = order.getSellerId().equals(firstUserId) ? firstUser : secondUser;
        if (buyer == null || seller == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        BigDecimal orderPrincipal = order.getFrozenAmount() != null && order.getFrozenAmount().compareTo(BigDecimal.ZERO) > 0
                ? order.getFrozenAmount()
                : (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);

        boolean isEscrowGateway = Integer.valueOf(2).equals(order.getPaymentMethod());

        // 1. 全额解冻并退回买方托管本金（第三方托管直付仅核减冻结并原路退回渠道，禁止误入可提现钱包余额）
        BigDecimal buyerBalBefore = buyer.getWalletBalance() != null ? buyer.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal buyerFrozenBefore = buyer.getFrozenAmount() != null ? buyer.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal buyerBalAfterRefund = isEscrowGateway ? buyerBalBefore : buyerBalBefore.add(orderPrincipal);
        BigDecimal buyerFrozenAfterRefund = buyerFrozenBefore.subtract(orderPrincipal).max(BigDecimal.ZERO);

        buyer.setWalletBalance(buyerBalAfterRefund);
        buyer.setFrozenAmount(buyerFrozenAfterRefund);

        WalletTransaction refundTx = new WalletTransaction()
                .setUserId(buyer.getId())
                .setTransactionType(WalletConstant.TRANSACTION_TYPE_REFUND)
                .setAmount(orderPrincipal)
                .setBalanceBefore(buyerBalBefore)
                .setBalanceAfter(buyerBalAfterRefund)
                .setFrozenBefore(buyerFrozenBefore)
                .setFrozenAfter(buyerFrozenAfterRefund)
                .setRelatedOrderId(order.getId())
                .setTransactionNo("TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .setRemark(isEscrowGateway
                        ? "卖方预约爽约，第三方担保支付本金原路退回"
                        : "卖方预约爽约，系统自动全额解冻退回订单托管本金")
                .setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(refundTx);

        if (isEscrowGateway && escrowPaymentOrderMapper != null) {
            escrowPaymentOrderMapper.update(null, new LambdaUpdateWrapper<EscrowPaymentOrder>()
                    .eq(EscrowPaymentOrder::getOrderId, order.getId())
                    .eq(EscrowPaymentOrder::getPayStatus, 1)
                    .set(EscrowPaymentOrder::getPayStatus, 3)
                    .set(EscrowPaymentOrder::getUpdateTime, LocalDateTime.now()));
        }

        // 2. 计算爽约违约赔偿金（订单金额 10%，最低 5 元，以卖方在池履约保证金余额为上限自动划扣）
        BigDecimal targetPenalty = orderPrincipal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        if (targetPenalty.compareTo(new BigDecimal("5.00")) < 0) {
            targetPenalty = new BigDecimal("5.00");
        }
        BigDecimal sellerDepositBefore = seller.getSellerDeposit() != null ? seller.getSellerDeposit() : BigDecimal.ZERO;
        BigDecimal actualCompensation = sellerDepositBefore.min(targetPenalty).max(BigDecimal.ZERO);
        String compensationSource = "SELLER_DEPOSIT";

        if (actualCompensation.compareTo(BigDecimal.ZERO) > 0) {
            // 从卖方履约保证金专户划扣
            BigDecimal sellerDepositAfter = sellerDepositBefore.subtract(actualCompensation);
            seller.setSellerDeposit(sellerDepositAfter);

            SellerDepositLedger depositLedger = new SellerDepositLedger()
                    .setLedgerNo("DEP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                    .setUserId(seller.getId())
                    .setActionType(3) // 3=违约仲裁/爽约自动划扣赔付
                    .setAmount(actualCompensation)
                    .setDepositBefore(sellerDepositBefore)
                    .setDepositAfter(sellerDepositAfter)
                    .setRemark("订单 " + order.getOrderNumber() + " 预约逾期未履约爽约，自动划扣保证金赔付买方")
                    .setCreateTime(LocalDateTime.now());
            sellerDepositLedgerMapper.insert(depositLedger);

            // 赔付金直接打入买方可用钱包余额
            BigDecimal buyerBalBeforeComp = buyer.getWalletBalance();
            BigDecimal buyerBalAfterComp = buyerBalBeforeComp.add(actualCompensation);
            buyer.setWalletBalance(buyerBalAfterComp);

            WalletTransaction compTx = new WalletTransaction()
                    .setUserId(buyer.getId())
                    .setTransactionType(WalletConstant.TRANSACTION_TYPE_INCOME)
                    .setAmount(actualCompensation)
                    .setBalanceBefore(buyerBalBeforeComp)
                    .setBalanceAfter(buyerBalAfterComp)
                    .setFrozenBefore(buyer.getFrozenAmount())
                    .setFrozenAfter(buyer.getFrozenAmount())
                    .setRelatedOrderId(order.getId())
                    .setTransactionNo("TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                    .setRemark("卖方预约爽约违约金自动赔付入账（来自创作者履约保证金专户）")
                    .setCreateTime(LocalDateTime.now());
            walletTransactionMapper.insert(compTx);
        } else {
            compensationSource = "CREDIT_PENALTY_ONLY";
        }

        // 3. 扣减卖方履约信用分 10 分
        int creditDeduct = 10;
        int scoreBefore = seller.getCreditScore() != null ? seller.getCreditScore() : 100;
        int scoreAfter = Math.max(0, scoreBefore - creditDeduct);
        seller.setCreditScore(scoreAfter);
        seller.setUpdateTime(LocalDateTime.now());
        buyer.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(buyer);
        userMapper.updateById(seller);

        CreditScoreLog creditLog = new CreditScoreLog()
                .setUserId(seller.getId())
                .setDelta(-creditDeduct)
                .setScoreBefore(scoreBefore)
                .setScoreAfter(scoreAfter)
                .setReasonCode("SELLER_NOSHOW_PENALTY")
                .setReasonDesc("预约订单 " + order.getOrderNumber() + " 逾期未履约爽约，自动扣减信用分")
                .setRelatedOrderId(order.getId())
                .setCreateTime(LocalDateTime.now());
        creditScoreLogMapper.insert(creditLog);

        // 4. 更新订单状态为交易失败退款并清零冻结
        order.setOrderStatus(OrderConstant.ORDER_FAILED)
                .setPaymentStatus(OrderConstant.PAYMENT_REFUNDED)
                .setRefundStatus(OrderConstant.REFUND_COMPLETED)
                .setFrozenAmount(BigDecimal.ZERO)
                .setRefundReason("卖方预约逾期爽约，系统已执行自动退本赔付（保证金划扣 ¥" + actualCompensation + "）")
                .setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);

        // 5. 写入爽约自动赔付存证表
        String claimNo = "NSH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String finalNote = StringUtils.hasText(evidenceNote)
                ? evidenceNote.trim()
                : String.format("预约档期已逾期未履约，系统自动解冻退还买方本金 ¥%s，并从卖方履约保证金划扣 ¥%s 赔付买方，扣减卖方信用分 %d 分。",
                orderPrincipal.toPlainString(), actualCompensation.toPlainString(), creditDeduct);

        AppointmentNoshowClaim claim = new AppointmentNoshowClaim()
                .setClaimNo(claimNo)
                .setOrderId(order.getId())
                .setOrderNumber(order.getOrderNumber())
                .setServiceId(order.getServiceId())
                .setBuyerId(order.getBuyerId())
                .setSellerId(order.getSellerId())
                .setCampusId(order.getCampusId() != null ? order.getCampusId() : 1L)
                .setNoshowType(1)
                .setScheduledStart(order.getScheduledStart())
                .setScheduledEnd(order.getScheduledEnd())
                .setOrderAmount(orderPrincipal)
                .setRefundToBuyerAmount(orderPrincipal)
                .setCompensationAmount(actualCompensation)
                .setCompensationSource(compensationSource)
                .setCreditDeducted(creditDeduct)
                .setClaimStatus(1)
                .setEvidenceNote(finalNote)
                .setCreateTime(LocalDateTime.now());
        noshowClaimMapper.insert(claim);

        // 6. 发送双向站内通知
        sendNoShowNotification(buyer.getId(), order.getId(), "【爽约自动赔付到账】预约订单已全额退款并获赔",
                String.format("您预约的订单（%s）因卖方逾期未履约，系统已自动退还托管本金 ¥%s，并划扣卖方保证金 ¥%s 赔付至您的钱包余额。",
                        order.getOrderNumber(), orderPrincipal.toPlainString(), actualCompensation.toPlainString()));
        sendNoShowNotification(seller.getId(), order.getId(), "【预约爽约违约处罚通知】保证金划扣与信用分扣减",
                String.format("您接单的预约订单（%s）因逾期未履约触发爽约自动赔付，已从您的履约保证金划扣 ¥%s 补偿买方，并扣减校园履约信用分 %d 分。",
                        order.getOrderNumber(), actualCompensation.toPlainString(), creditDeduct));

        return claim;
    }

    @Override
    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public int scanAndCompensateOverdueNoShowOrders() {
        LocalDateTime overdueThreshold = LocalDateTime.now().minusMinutes(30);
        LambdaQueryWrapper<ServiceOrder> query = new LambdaQueryWrapper<>();
        query.eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_PENDING_SHIP)
                .isNotNull(ServiceOrder::getScheduledEnd)
                .lt(ServiceOrder::getScheduledEnd, overdueThreshold)
                .last("LIMIT 50");

        List<ServiceOrder> overdueOrders = serviceOrderMapper.selectList(query);
        if (overdueOrders == null || overdueOrders.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ServiceOrder order : overdueOrders) {
            try {
                processSellerNoShowCompensation(0L, order.getId(),
                        "定时巡检发现预约结束时间已逾期超过 30 分钟仍未签到/交付，触发自动爽约退赔");
                count++;
            } catch (Exception ignored) {
                // 单笔异常不影响其余订单赔付
            }
        }
        return count;
    }

    @Override
    public AppointmentNoshowClaim getClaimByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        LambdaQueryWrapper<AppointmentNoshowClaim> query = new LambdaQueryWrapper<>();
        query.eq(AppointmentNoshowClaim::getOrderId, orderId);
        return noshowClaimMapper.selectOne(query);
    }

    @Override
    public List<AppointmentNoshowClaim> listRecentNoshowClaims(int limit) {
        int lim = limit > 0 ? Math.min(100, limit) : 20;
        List<AppointmentNoshowClaim> list = noshowClaimMapper.selectList(
                new LambdaQueryWrapper<AppointmentNoshowClaim>()
                        .orderByDesc(AppointmentNoshowClaim::getCreateTime)
                        .last("LIMIT " + lim));
        return list != null ? list : Collections.emptyList();
    }

    private void sendNoShowNotification(Long recipientId, Long orderId, String title, String content) {
        if (recipientId == null) {
            return;
        }
        Notification n = new Notification()
                .setRecipientId(recipientId)
                .setNotificationType(3)
                .setTitle(title)
                .setContent(content)
                .setRelatedId(orderId)
                .setIsRead(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }
}
