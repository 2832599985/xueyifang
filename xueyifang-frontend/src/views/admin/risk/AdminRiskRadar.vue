<template>
  <div class="admin-risk-radar-page">
    <div class="radar-banner xyf-surface-card">
      <div class="radar-banner__info">
        <span class="radar-kicker">ANTI-WASH-TRADING &amp; GRAPH RISK ENGINE · v4.3</span>
        <h2>异常高频互刷与反作弊风控审计雷达</h2>
        <p>
          实时扫描近 7 日校内订单关系图谱，自动识别「买卖角色互换双向对倒」、「同买卖对高频重复刷单」、「3分钟内极速秒拍秒确」与「超低价微额刷信誉」行为。
        </p>
      </div>
      <div class="radar-banner__actions">
        <el-select v-model="scanDays" style="width: 130px" size="large">
          <el-option :value="3" label="回溯近 3 天" />
          <el-option :value="7" label="回溯近 7 天" />
          <el-option :value="30" label="回溯近 30 天" />
        </el-select>
        <el-button type="primary" size="large" :loading="scanning" @click="handleRunScan">
          立即执行全链路图谱扫描
        </el-button>
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">待人工核查预警</span>
        <strong class="kpi-value is-warning">{{ overview.pendingAlerts }}</strong>
        <span class="kpi-sub">累计识别 {{ overview.totalAlerts }} 条风控线索</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">高危互刷/对倒阻断 (L3)</span>
        <strong class="kpi-value is-danger">{{ overview.highSeverityAlerts }}</strong>
        <span class="kpi-sub">双向对倒 {{ overview.bidirectionalWashCount }} · 高频同对 {{ overview.highFreqPairCount }}</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">极速秒拍秒确异常</span>
        <strong class="kpi-value">{{ overview.ultraFastCompletionCount }}</strong>
        <span class="kpi-sub">支付至确认验收 &lt; 180 秒</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">已确认违规并处罚</span>
        <strong class="kpi-value is-success">{{ overview.confirmedPenalties }}</strong>
        <span class="kpi-sub">近7日覆盖订单 {{ overview.scannedOrders7d }} 笔 · 排除误报 {{ overview.dismissedAlerts }}</span>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar xyf-surface-card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索风控单号 / 订单号 / 取证关键词"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.ruleCode" placeholder="全部风控规则" clearable style="width: 210px" @change="handleSearch">
        <el-option value="BIDIRECTIONAL_WASH_TRADE" label="买卖角色互换双向对倒" />
        <el-option value="HIGH_FREQ_SAME_PAIR" label="同买卖对高频重复交易" />
        <el-option value="ULTRA_FAST_COMPLETION" label="极速秒拍秒确异常履约" />
        <el-option value="LOW_PRICE_SCORE_BRUSHING" label="超低价微额疑似刷分" />
      </el-select>
      <el-select v-model="query.severity" placeholder="风险等级" clearable style="width: 150px" @change="handleSearch">
        <el-option :value="3" label="L3 高危互刷" />
        <el-option :value="2" label="L2 中危可疑" />
        <el-option :value="1" label="L1 低风险提示" />
      </el-select>
      <el-select v-model="query.handleStatus" placeholder="处置状态" clearable style="width: 160px" @change="handleSearch">
        <el-option :value="0" label="待核查" />
        <el-option :value="1" label="已确认违规并处罚" />
        <el-option :value="2" label="已排除误报" />
      </el-select>
      <el-button type="primary" plain @click="handleSearch">筛选</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- Alert Table -->
    <div class="table-card xyf-surface-card">
      <el-table :data="alertList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="alertNo" label="风控单号 / 规则" min-width="200">
          <template #default="{ row }">
            <div class="cell-primary">{{ row.ruleName }}</div>
            <div class="cell-sub">{{ row.alertNo }} · 评分 {{ row.riskScore }}</div>
          </template>
        </el-table-column>

        <el-table-column label="风险等级" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.severity === 3" type="danger" effect="dark">L3 高危互刷</el-tag>
            <el-tag v-else-if="row.severity === 2" type="warning" effect="light">L2 中危可疑</el-tag>
            <el-tag v-else type="info" effect="plain">L1 低风险</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="涉事买卖双方" min-width="220">
          <template #default="{ row }">
            <div class="pair-line">
              <span class="role-pill is-buyer">买</span>
              <span>{{ row.buyerName || `用户#${row.buyerId}` }}</span>
              <span class="credit-chip" v-if="row.buyerCreditScore !== undefined">信用 {{ row.buyerCreditScore }}</span>
            </div>
            <div class="pair-line">
              <span class="role-pill is-seller">卖</span>
              <span>{{ row.sellerName || `用户#${row.sellerId}` }}</span>
              <span class="credit-chip" v-if="row.sellerCreditScore !== undefined">信用 {{ row.sellerCreditScore }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="关联订单与图谱特征" min-width="230">
          <template #default="{ row }">
            <div class="cell-primary">
              订单 {{ row.orderNumber || `#${row.orderId}` }}（¥{{ Number(row.orderAmount || 0).toFixed(2) }}）
            </div>
            <div class="cell-sub">
              7日同对 {{ row.pairOrderCount7d || 1 }} 笔 · 反向对倒 {{ row.reverseOrderCount7d || 0 }} 笔
              <span v-if="row.fulfillDurationSeconds !== null && row.fulfillDurationSeconds !== undefined">
                · 耗时 {{ row.fulfillDurationSeconds }}s
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="evidenceSummary" label="风控取证摘要" min-width="280" show-overflow-tooltip />

        <el-table-column label="处置状态" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.handleStatus === 0" type="warning">待管理员核查</el-tag>
            <div v-else-if="row.handleStatus === 1">
              <el-tag type="danger">已处罚 (-{{ row.creditDeducted }}分)</el-tag>
              <div class="cell-sub">{{ row.handleRemark }}</div>
            </div>
            <div v-else>
              <el-tag type="success">已排除误报</el-tag>
              <div class="cell-sub">{{ row.handleRemark }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.handleStatus === 0">
              <el-button type="danger" size="small" @click="openResolveDialog(row, 1)">
                处罚互刷
              </el-button>
              <el-button type="success" plain size="small" @click="openResolveDialog(row, 2)">
                排除误报
              </el-button>
            </template>
            <span v-else class="cell-sub">已归档</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchAlerts"
        />
      </div>
    </div>

    <!-- Resolve Modal -->
    <el-dialog
      v-model="resolveDialogVisible"
      :title="resolveForm.handleStatus === 1 ? '确认违规互刷并执行风控处罚' : '排除风控误报放行'"
      width="520px"
    >
      <div v-if="currentAlert" class="dialog-alert-summary">
        <p><strong>命中规则：</strong>{{ currentAlert.ruleName }}（{{ currentAlert.alertNo }}）</p>
        <p><strong>取证摘要：</strong>{{ currentAlert.evidenceSummary }}</p>
      </div>

      <el-form label-position="top">
        <template v-if="resolveForm.handleStatus === 1">
          <el-form-item label="买卖双方各扣减校园履约信用分（分）">
            <el-slider v-model="resolveForm.creditDeductPoints" :min="5" :max="40" :step="5" show-input />
          </el-form-item>
          <el-form-item label="关联技能服务治理">
            <el-switch
              v-model="resolveForm.offlineService"
              active-text="同时强制下架涉事技能服务并发送站内整改通知"
              inactive-text="仅扣减双方信用分并警告"
            />
          </el-form-item>
        </template>

        <el-form-item label="核查结论备注">
          <el-input
            v-model="resolveForm.handleRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入风控核查依据或处罚说明（将同步记录至审计台账）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button
          :type="resolveForm.handleStatus === 1 ? 'danger' : 'primary'"
          :loading="resolving"
          @click="submitResolve"
        >
          确认执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRiskRadarOverview,
  listRiskAlerts,
  scanRiskRadarOrders,
  resolveRiskAlert,
  type RiskRadarOverviewData,
  type RiskAuditAlertItem
} from '@/api/riskRadar'

const loading = ref(false)
const scanning = ref(false)
const resolving = ref(false)
const scanDays = ref(7)

const overview = reactive<RiskRadarOverviewData>({
  totalAlerts: 0,
  pendingAlerts: 0,
  highSeverityAlerts: 0,
  confirmedPenalties: 0,
  dismissedAlerts: 0,
  scannedOrders7d: 0,
  bidirectionalWashCount: 0,
  highFreqPairCount: 0,
  ultraFastCompletionCount: 0
})

const query = reactive({
  pageNum: 1,
  pageSize: 15,
  ruleCode: undefined as string | undefined,
  severity: undefined as number | undefined,
  handleStatus: undefined as number | undefined,
  keyword: ''
})

const alertList = ref<RiskAuditAlertItem[]>([])
const total = ref(0)

const resolveDialogVisible = ref(false)
const currentAlert = ref<RiskAuditAlertItem | null>(null)
const resolveForm = reactive({
  handleStatus: 1,
  creditDeductPoints: 15,
  offlineService: true,
  handleRemark: ''
})

const fetchOverview = async () => {
  try {
    const data = await getRiskRadarOverview()
    Object.assign(overview, data)
  } catch {
    // ignore
  }
}

const fetchAlerts = async () => {
  loading.value = true
  try {
    const res = await listRiskAlerts({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      ruleCode: query.ruleCode || undefined,
      severity: query.severity,
      handleStatus: query.handleStatus,
      keyword: query.keyword || undefined
    })
    alertList.value = res.records || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  fetchAlerts()
}

const resetFilters = () => {
  query.ruleCode = undefined
  query.severity = undefined
  query.handleStatus = undefined
  query.keyword = ''
  handleSearch()
}

const handleRunScan = async () => {
  scanning.value = true
  try {
    const created = await scanRiskRadarOrders(scanDays.value)
    ElMessage.success(`全链路图谱扫描完成，新增识别 ${created} 条可疑互刷预警`)
    await Promise.all([fetchOverview(), fetchAlerts()])
  } finally {
    scanning.value = false
  }
}

const openResolveDialog = (row: RiskAuditAlertItem, targetStatus: number) => {
  currentAlert.value = row
  resolveForm.handleStatus = targetStatus
  resolveForm.creditDeductPoints = row.severity === 3 ? 20 : 15
  resolveForm.offlineService = row.severity === 3
  resolveForm.handleRemark =
    targetStatus === 1
      ? `核实命中「${row.ruleName}」，存在刷单刷信誉行为，按平台规则扣减双方履约信用分`
      : '经核查存在真实线下/线上辅导交付记录，排除误报'
  resolveDialogVisible.value = true
}

const submitResolve = async () => {
  if (!currentAlert.value) return
  resolving.value = true
  try {
    await resolveRiskAlert(currentAlert.value.id, {
      handleStatus: resolveForm.handleStatus,
      creditDeductPoints: resolveForm.creditDeductPoints,
      offlineService: resolveForm.offlineService,
      handleRemark: resolveForm.handleRemark
    })
    ElMessage.success('风控预警处置已生效并记录审计日志')
    resolveDialogVisible.value = false
    await Promise.all([fetchOverview(), fetchAlerts()])
  } finally {
    resolving.value = false
  }
}

onMounted(() => {
  fetchOverview()
  fetchAlerts()
})
</script>

<style scoped lang="scss">
.admin-risk-radar-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.radar-banner {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;

  .radar-kicker {
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
    gap: 12px;
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

  .kpi-value {
    font-size: 26px;
    font-weight: 800;
    color: var(--xyf-ink);

    &.is-warning {
      color: #d97706;
    }
    &.is-danger {
      color: #dc2626;
    }
    &.is-success {
      color: #059669;
    }
  }

  .kpi-sub {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }
}

.filter-bar {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  padding: 16px 18px;
}

.cell-primary {
  font-weight: 600;
  color: var(--xyf-ink);
}

.cell-sub {
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.pair-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.role-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;

  &.is-buyer {
    background: #dbeafe;
    color: #1d4ed8;
  }
  &.is-seller {
    background: #fef3c7;
    color: #b45309;
  }
}

.credit-chip {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-alert-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 13px;

  p {
    margin: 4px 0;
  }
}
</style>
