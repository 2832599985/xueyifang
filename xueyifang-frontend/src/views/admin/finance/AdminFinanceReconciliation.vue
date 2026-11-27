<template>
  <div class="admin-finance-page">
    <!-- Top Banner -->
    <div class="finance-banner xyf-surface-card">
      <div class="finance-banner__info">
        <span class="finance-kicker">FINANCIAL RECONCILIATION &amp; NO-SHOW COMPENSATION · v4.4</span>
        <h2>平台财务日终自动对账报表与预约爽约自动赔付中心</h2>
        <p>
          支持按账期自动核验「担保流入 = 创作者结算 + 平台抽佣 + 退款流出 + 在途托管冻结池」总分勾稽关系，一键导出 UTF-8 BOM 标准 CSV/Excel 财务台账，并对预约逾期未履约订单执行保证金划扣自动退赔。
        </p>
      </div>
      <div class="finance-banner__actions">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择对账账期"
          style="width: 155px"
        />
        <el-button type="primary" :loading="reconciling" @click="handleReconcile">
          生成/刷新日终对账单
        </el-button>
        <el-button type="success" plain :loading="exporting" @click="handleExportCsv">
          导出 CSV / Excel 财务报表
        </el-button>
        <el-button type="warning" plain :loading="scanningNoShow" @click="handleScanNoShow">
          巡检逾期爽约并自动退赔
        </el-button>
      </div>
    </div>

    <!-- Financial Pool KPIs -->
    <div class="kpi-grid">
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">累计平台技术服务费抽佣</span>
        <strong class="kpi-val is-primary">¥{{ fmt(overview.totalPlatformCommission) }}</strong>
        <span class="kpi-sub">累计已生成 {{ overview.totalReconcileReports }} 份日终对账单</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">累计创作者结算入账净额</span>
        <strong class="kpi-val is-success">¥{{ fmt(overview.totalSellerSettled) }}</strong>
        <span class="kpi-sub">勾稽异常待复核账期：{{ overview.discrepancyReportCount }} 份</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">在途担保托管冻结池 / 保证金池</span>
        <strong class="kpi-val">¥{{ fmt(overview.currentEscrowFrozenPool) }}</strong>
        <span class="kpi-sub">创作者在池履约保证金：¥{{ fmt(overview.currentSellerDepositPool) }}</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">累计预约爽约自动赔付</span>
        <strong class="kpi-val is-danger">¥{{ fmt(overview.totalNoshowCompensationAmount) }}</strong>
        <span class="kpi-sub">已自动执行 {{ overview.totalNoshowClaims }} 笔退本金+保证金划扣</span>
      </div>
    </div>

    <!-- Tabs: Daily Reconciliation vs No-Show Claims -->
    <div class="table-panel xyf-surface-card">
      <div class="panel-toolbar">
        <el-tabs v-model="activeTab" class="finance-tabs">
          <el-tab-pane label="日终自动对账报表 (EOD Reconciliation)" name="reports" />
          <el-tab-pane label="预约爽约自动赔付台账 (No-Show Claims)" name="noshow" />
        </el-tabs>
        <el-button v-if="activeTab === 'noshow'" type="danger" plain size="small" @click="manualDialogVisible = true">
          手动指定订单执行爽约退赔
        </el-button>
      </div>

      <!-- Tab 1: Daily Reports -->
      <el-table
        v-if="activeTab === 'reports'"
        :data="overview.recentReports"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="reportDate" label="对账账期" width="120" />
        <el-table-column prop="reportNo" label="对账批次号" width="175" />
        <el-table-column label="订单笔数 (付/完/退/爽)" width="175">
          <template #default="{ row }">
            {{ row.paidOrderCount }} / {{ row.completedOrderCount }} / {{ row.refundOrderCount }} / {{ row.noshowCompensatedCount }}
          </template>
        </el-table-column>
        <el-table-column label="当日担保流入" width="130">
          <template #default="{ row }">¥{{ fmt(row.grossEscrowInflow) }}</template>
        </el-table-column>
        <el-table-column label="创作者分账净额" width="135">
          <template #default="{ row }">¥{{ fmt(row.sellerSettledAmount) }}</template>
        </el-table-column>
        <el-table-column label="平台抽佣净额" width="125">
          <template #default="{ row }">
            <strong style="color: #059669">¥{{ fmt(row.platformCommissionAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="退款与爽约赔付" width="145">
          <template #default="{ row }">
            退 ¥{{ fmt(row.refundOutflowAmount) }} / 赔 ¥{{ fmt(row.noshowPenaltyAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="日终在途托管池" width="135">
          <template #default="{ row }">¥{{ fmt(row.activeEscrowFrozenPool) }}</template>
        </el-table-column>
        <el-table-column label="总分勾稽状态" width="135">
          <template #default="{ row }">
            <el-tag v-if="row.reconcileStatus === 1" type="success" effect="dark">总分勾稽账平</el-tag>
            <el-tag v-else type="danger" effect="dark">差额 ({{ row.discrepancyCount }}笔)</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditNote" label="自动对账审计结论" min-width="260" show-overflow-tooltip />
      </el-table>

      <!-- Tab 2: No-Show Compensation Claims -->
      <el-table
        v-else
        :data="overview.recentNoshowClaims"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="claimNo" label="赔付存证单号" width="180" />
        <el-table-column label="关联订单" width="170">
          <template #default="{ row }">
            {{ row.orderNumber || `订单#${row.orderId}` }}
          </template>
        </el-table-column>
        <el-table-column label="买方 / 卖方 ID" width="140">
          <template #default="{ row }">
            买 #{{ row.buyerId }} / 卖 #{{ row.sellerId }}
          </template>
        </el-table-column>
        <el-table-column label="退回买方本金" width="130">
          <template #default="{ row }">
            <strong style="color: #059669">¥{{ fmt(row.refundToBuyerAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="违约金划扣赔付" width="140">
          <template #default="{ row }">
            <strong style="color: #dc2626">+¥{{ fmt(row.compensationAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="compensationSource" label="划扣来源" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.compensationSource === 'SELLER_DEPOSIT'" type="warning">卖方履约保证金</el-tag>
            <el-tag v-else type="info">信用分扣减处罚</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="卖方扣减信用分" width="130">
          <template #default="{ row }">-{{ row.creditDeducted }} 分</template>
        </el-table-column>
        <el-table-column prop="evidenceNote" label="爽约取证与划拨摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="执行时间" width="170" />
      </el-table>
    </div>

    <!-- Manual No-Show Compensation Modal -->
    <el-dialog v-model="manualDialogVisible" title="针对逾期未履约订单执行爽约自动退赔" width="480px">
      <el-form label-position="top">
        <el-form-item label="逾期预约担保订单 ID">
          <el-input-number v-model="manualForm.orderId" :min="1" style="width: 100%" placeholder="请输入订单ID" />
        </el-form-item>
        <el-form-item label="爽约核查存证说明">
          <el-input
            v-model="manualForm.evidenceNote"
            type="textarea"
            :rows="3"
            placeholder="例如：核实卖方超出预约结束时段仍未抵达图书馆交接点，执行自动退本金+保证金10%划扣赔付"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="manualSubmitting" @click="handleManualCompensate">
          立即执行退本金与保证金赔付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getFinanceOverview,
  reconcileDailyFinance,
  exportFinanceCsv,
  scanOverdueNoShowOrders,
  adminCompensateNoShowOrder,
  type FinancialOverviewData
} from '@/api/finance'

const loading = ref(false)
const reconciling = ref(false)
const exporting = ref(false)
const scanningNoShow = ref(false)
const activeTab = ref<'reports' | 'noshow'>('reports')
const selectedDate = ref(new Date().toISOString().slice(0, 10))

const overview = reactive<FinancialOverviewData>({
  totalPlatformCommission: 0,
  totalSellerSettled: 0,
  currentEscrowFrozenPool: 0,
  currentSellerDepositPool: 0,
  totalNoshowClaims: 0,
  totalNoshowCompensationAmount: 0,
  totalReconcileReports: 0,
  discrepancyReportCount: 0,
  recentReports: [],
  recentNoshowClaims: []
})

const manualDialogVisible = ref(false)
const manualSubmitting = ref(false)
const manualForm = reactive({
  orderId: undefined as number | undefined,
  evidenceNote: '管理员核实卖方预约逾期爽约，执行自动全额退还买方本金并从卖方保证金划扣10%违约金赔付'
})

const fmt = (val?: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toFixed(2)
}

const fetchOverview = async () => {
  loading.value = true
  try {
    const data = await getFinanceOverview()
    Object.assign(overview, data)
  } finally {
    loading.value = false
  }
}

const handleReconcile = async () => {
  reconciling.value = true
  try {
    const rep = await reconcileDailyFinance(selectedDate.value)
    ElMessage.success(`账期 ${rep.reportDate} 自动对账完成（批次号：${rep.reportNo}）`)
    await fetchOverview()
  } finally {
    reconciling.value = false
  }
}

const handleExportCsv = async () => {
  exporting.value = true
  try {
    const payload = await exportFinanceCsv()
    const blob = new Blob([payload.csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', payload.fileName || 'xueyifang_finance_reconciliation.csv')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('财务日终对账与分账/爽约赔付 CSV 报表已导出下载')
  } finally {
    exporting.value = false
  }
}

const handleScanNoShow = async () => {
  scanningNoShow.value = true
  try {
    const count = await scanOverdueNoShowOrders()
    ElMessage.success(`逾期预约订单巡检完成，本次自动执行爽约退赔 ${count} 笔`)
    await fetchOverview()
  } finally {
    scanningNoShow.value = false
  }
}

const handleManualCompensate = async () => {
  if (!manualForm.orderId) {
    ElMessage.warning('请输入有效的订单ID')
    return
  }
  manualSubmitting.value = true
  try {
    const claim = await adminCompensateNoShowOrder(manualForm.orderId, manualForm.evidenceNote)
    ElMessage.success(`爽约赔付单 ${claim.claimNo} 已生成，退回本金 ¥${fmt(claim.refundToBuyerAmount)}，赔付 ¥${fmt(claim.compensationAmount)}`)
    manualDialogVisible.value = false
    activeTab.value = 'noshow'
    await fetchOverview()
  } finally {
    manualSubmitting.value = false
  }
}

onMounted(() => {
  fetchOverview()
})
</script>

<style scoped lang="scss">
.admin-finance-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.finance-banner {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;

  .finance-kicker {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: var(--xyf-primary);
  }

  h2 {
    margin: 4px 0 6px;
    font-size: 20px;
    font-weight: 700;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
    max-width: 680px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .kpi-label {
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  .kpi-val {
    font-size: 25px;
    font-weight: 800;
    color: var(--xyf-ink);

    &.is-primary {
      color: #2563eb;
    }
    &.is-success {
      color: #059669;
    }
    &.is-danger {
      color: #dc2626;
    }
  }

  .kpi-sub {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }
}

.table-panel {
  padding: 16px 20px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
</style>
