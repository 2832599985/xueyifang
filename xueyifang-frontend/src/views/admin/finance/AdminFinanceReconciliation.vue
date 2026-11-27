<template>
  <div class="admin-finance-page">
    <XyfPageHeader title="财务对账">
      <template #actions>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择对账日期"
          style="width: 152px"
        />
        <el-button type="primary" :loading="reconciling" @click="handleReconcile">
          生成对账单
        </el-button>
        <el-button plain :loading="exporting" @click="handleExportCsv">
          导出 CSV
        </el-button>
        <el-button plain :loading="scanningNoShow" @click="handleScanNoShow">
          扫描逾期爽约订单
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- 4 Financial Pool KPIs -->
    <section class="kpi-grid">
      <XyfStatTile
        label="累计平台服务费"
        :value="`¥${fmt(overview.totalPlatformCommission)}`"
        :hint="`已生成 ${overview.totalReconcileReports} 份对账单`"
      />
      <XyfStatTile
        label="累计创作者结算"
        :value="`¥${fmt(overview.totalSellerSettled)}`"
        :hint="`差额异常：${overview.discrepancyReportCount} 份`"
      />
      <XyfStatTile
        label="在途托管余额"
        :value="`¥${fmt(overview.currentEscrowFrozenPool)}`"
        :hint="`创作者保证金：¥${fmt(overview.currentSellerDepositPool)}`"
      />
      <XyfStatTile
        label="累计爽约赔付"
        :value="`¥${fmt(overview.totalNoshowCompensationAmount)}`"
        :hint="`已处理 ${overview.totalNoshowClaims} 笔爽约退赔`"
        tone="danger"
      />
    </section>

    <!-- Visual Accounting Equation Flow + ECharts Cashflow Split -->
    <div class="finance-visual-split">
      <!-- Left: Total-to-Subledger Balance Flow Diagram -->
      <section class="xyf-surface-card equation-card xyf-tabular">
        <div class="xyf-section-heading">
          <h2>收支平衡核对</h2>
        </div>

        <div class="equation-flow">
          <div class="eq-node is-dark">
            <span>托管流入总额</span>
            <strong>¥{{ fmt(latestReport?.grossEscrowInflow ?? overview.currentEscrowFrozenPool) }}</strong>
            <small>{{ latestReport ? `日期 ${latestReport.reportDate}` : '实时快照' }}</small>
          </div>

          <span class="eq-symbol">=</span>

          <div class="eq-breakdown">
            <div class="eq-sub">
              <span>创作者结算</span>
              <strong>¥{{ fmt(latestReport?.sellerSettledAmount ?? overview.totalSellerSettled) }}</strong>
            </div>
            <div class="eq-sub is-gold">
              <span>平台服务费</span>
              <strong>¥{{ fmt(latestReport?.platformCommissionAmount ?? overview.totalPlatformCommission) }}</strong>
            </div>
            <div class="eq-sub">
              <span>退款与违约流出</span>
              <strong>¥{{ fmt(latestReport?.refundOutflowAmount ?? 0) }}</strong>
            </div>
            <div class="eq-sub">
              <span>在途冻结资金</span>
              <strong>¥{{ fmt(latestReport?.activeEscrowFrozenPool ?? overview.currentEscrowFrozenPool) }}</strong>
            </div>
          </div>
        </div>

        <div class="equation-status">
          <span class="xyf-live-dot" />
          <span>
            {{
              overview.discrepancyReportCount === 0
                ? '账目核对一致，无差额'
                : `发现 ${overview.discrepancyReportCount} 个账期存在差额，请核查下方记录`
            }}
          </span>
        </div>
      </section>

      <!-- Right: ECharts Reconcile Cashflow Chart -->
      <section class="xyf-surface-card chart-card">
        <div class="xyf-section-heading">
          <h2>近期收支趋势</h2>
        </div>
        <div ref="chartRef" class="finance-chart-stage" />
      </section>
    </div>

    <!-- Ledger Tables -->
    <section class="xyf-surface-card table-panel">
      <div class="panel-toolbar">
        <div class="xyf-segmented">
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': activeTab === 'reports' }"
            @click="activeTab = 'reports'"
          >
            <span>每日对账单</span>
            <span class="xyf-segmented__count">{{ overview.recentReports.length }}</span>
          </button>
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': activeTab === 'noshow' }"
            @click="activeTab = 'noshow'"
          >
            <span>爽约赔付记录</span>
            <span class="xyf-segmented__count">{{ overview.recentNoshowClaims.length }}</span>
          </button>
        </div>

        <el-button
          v-if="activeTab === 'noshow'"
          type="danger"
          plain
          size="small"
          @click="manualDialogVisible = true"
        >
          指定订单手动执行爽约退赔
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
        <el-table-column label="对账批次号" width="180">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.reportNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="笔数 (付/完/退/爽)" width="160">
          <template #default="{ row }">
            <span class="xyf-tabular">
              {{ row.paidOrderCount }} / {{ row.completedOrderCount }} / {{ row.refundOrderCount }} / {{ row.noshowCompensatedCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当日担保流入" width="130">
          <template #default="{ row }"><span class="xyf-tabular">¥{{ fmt(row.grossEscrowInflow) }}</span></template>
        </el-table-column>
        <el-table-column label="创作者分账" width="130">
          <template #default="{ row }"><span class="xyf-tabular">¥{{ fmt(row.sellerSettledAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="平台抽佣" width="125">
          <template #default="{ row }">
            <strong class="xyf-tabular">¥{{ fmt(row.platformCommissionAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="退款 / 爽约赔付" width="150">
          <template #default="{ row }">
            <span class="xyf-tabular">¥{{ fmt(row.refundOutflowAmount) }} / ¥{{ fmt(row.noshowPenaltyAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="日终在途托管池" width="135">
          <template #default="{ row }"><span class="xyf-tabular">¥{{ fmt(row.activeEscrowFrozenPool) }}</span></template>
        </el-table-column>
        <el-table-column label="勾稽结论" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.reconcileStatus === 1" type="success">总分账平</el-tag>
            <el-tag v-else type="danger">差额 ({{ row.discrepancyCount }}笔)</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditNote" label="自动审计摘要" min-width="240" show-overflow-tooltip />
      </el-table>

      <!-- Tab 2: No-Show Compensation Claims -->
      <el-table
        v-else
        :data="overview.recentNoshowClaims"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="赔付存证单号" width="185">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.claimNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联担保单" width="170">
          <template #default="{ row }">
            {{ row.orderNumber || `订单#${row.orderId}` }}
          </template>
        </el-table-column>
        <el-table-column label="涉事双方" width="140">
          <template #default="{ row }">
            买 #{{ row.buyerId }} ⇄ 卖 #{{ row.sellerId }}
          </template>
        </el-table-column>
        <el-table-column label="退回买方本金" width="130">
          <template #default="{ row }">
            <strong class="xyf-tabular">¥{{ fmt(row.refundToBuyerAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="违约金赔付" width="130">
          <template #default="{ row }">
            <strong class="xyf-tabular" style="color: #b91c1c">+¥{{ fmt(row.compensationAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="划扣来源" width="145">
          <template #default="{ row }">
            <el-tag v-if="row.compensationSource === 'SELLER_DEPOSIT'" type="warning">卖方保证金池</el-tag>
            <el-tag v-else type="info">信用分扣罚</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信用扣减" width="110">
          <template #default="{ row }">-{{ row.creditDeducted }} 分</template>
        </el-table-column>
        <el-table-column prop="evidenceNote" label="爽约取证摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createTime" label="执行时间" width="170" />
      </el-table>
    </section>

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
            placeholder="核实卖方超出预约结束时段仍未抵达约定交接点，执行自动退本金+保证金10%划扣赔付"
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
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import echarts from '@/utils/echarts'
import {
  getFinanceOverview,
  reconcileDailyFinance,
  exportFinanceCsv,
  scanOverdueNoShowOrders,
  adminCompensateNoShowOrder,
  type FinancialOverviewData
} from '@/api/finance'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'

const loading = ref(false)
const reconciling = ref(false)
const exporting = ref(false)
const scanningNoShow = ref(false)
const activeTab = ref<'reports' | 'noshow'>('reports')
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: ReturnType<typeof echarts.init> | null = null

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

const latestReport = computed(() => overview.recentReports?.[0] || null)

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

function renderCashflowChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const list = [...(overview.recentReports || [])].slice(0, 7).reverse()
  const dates = list.length ? list.map(r => r.reportDate.slice(5)) : [selectedDate.value.slice(5)]
  const settled = list.length ? list.map(r => Number(r.sellerSettledAmount || 0)) : [Number(overview.totalSellerSettled || 0)]
  const commission = list.length ? list.map(r => Number(r.platformCommissionAmount || 0)) : [Number(overview.totalPlatformCommission || 0)]

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#18181b',
      borderColor: '#18181b',
      textStyle: { color: '#ffffff', fontSize: 12 }
    },
    legend: {
      data: ['创作者分账净额', '平台技术抽佣'],
      bottom: 0,
      textStyle: { color: '#52525b', fontSize: 12 }
    },
    grid: { top: 18, left: 46, right: 20, bottom: 34 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e4e4e7' } },
      axisLabel: { color: '#71717a', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f1f4' } },
      axisLabel: { color: '#71717a', fontSize: 11 }
    },
    series: [
      {
        name: '创作者分账净额',
        type: 'bar',
        barMaxWidth: 24,
        itemStyle: { color: '#18181b', borderRadius: [5, 5, 0, 0] },
        data: settled
      },
      {
        name: '平台技术抽佣',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#b89058' },
        lineStyle: { width: 2.5, color: '#b89058' },
        data: commission
      }
    ]
  })
}

const fetchOverview = async () => {
  loading.value = true
  try {
    const data = await getFinanceOverview()
    Object.assign(overview, data)
    await nextTick()
    renderCashflowChart()
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

const onResize = () => chartInstance?.resize()

onMounted(() => {
  window.addEventListener('resize', onResize)
  fetchOverview()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartInstance?.dispose()
})
</script>

<style scoped lang="scss">
.admin-finance-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.finance-visual-split {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 18px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.equation-card,
.chart-card {
  padding: 22px 24px;
}

.equation-flow {
  display: flex;
  align-items: center;
  gap: 14px;

  @media (max-width: 680px) {
    flex-direction: column;
    align-items: stretch;
  }

  .eq-node {
    padding: 18px 20px;
    border-radius: 14px;
    background: #18181b;
    color: #ffffff;
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 170px;

    span {
      font-size: 11.5px;
      color: rgba(255, 255, 255, 0.65);
    }

    strong {
      font-size: 22px;
      font-weight: 800;
    }

    small {
      font-size: 11px;
      color: #e5c79e;
    }
  }

  .eq-symbol {
    font-size: 22px;
    font-weight: 700;
    color: var(--xyf-ink-3);
    text-align: center;
  }

  .eq-breakdown {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .eq-sub {
    padding: 11px 14px;
    border-radius: 12px;
    border: 1px solid var(--xyf-border);
    background: var(--xyf-surface-soft);
    display: flex;
    flex-direction: column;
    gap: 2px;

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }

    strong {
      font-size: 15px;
      font-weight: 750;
      color: var(--xyf-ink);
    }

    &.is-gold {
      background: var(--xyf-champagne-soft);
      border-color: var(--xyf-champagne-border);

      strong {
        color: #9a5b13;
      }
    }
  }
}

.equation-status {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--xyf-ink-2);
}

.finance-chart-stage {
  width: 100%;
  height: 210px;
}

.table-panel {
  padding: 20px 22px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
</style>
