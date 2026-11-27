<template>
  <div class="admin-risk-radar-page">
    <XyfPageHeader title="异常订单监测" dense>
      <template #actions>
        <div class="xyf-segmented">
          <button
            v-for="d in [3, 7, 30]"
            :key="d"
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': scanDays === d }"
            @click="scanDays = d"
          >
            近 {{ d }} 天
          </button>
        </div>

        <el-button type="primary" :loading="scanning" @click="handleRunScan">
          立即扫描
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- 4 KPI Tiles -->
    <section class="kpi-grid">
      <XyfStatTile
        label="待核查预警"
        :value="overview.pendingAlerts"
        unit="条"
        :hint="`累计共 ${overview.totalAlerts} 条记录`"
        emphasis="primary"
      />
      <XyfStatTile
        label="高风险订单"
        :value="overview.highSeverityAlerts"
        unit="条"
        :hint="`双向互买 ${overview.bidirectionalWashCount} 条`"
        :tone="overview.highSeverityAlerts > 0 ? 'danger' : 'default'"
      />
      <XyfStatTile
        label="极速完成异常"
        :value="overview.ultraFastCompletionCount"
        unit="条"
        hint="支付到确认低于 3 分钟"
      />
      <XyfStatTile
        label="已处罚"
        :value="overview.confirmedPenalties"
        unit="条"
        :hint="`排除误报 ${overview.dismissedAlerts} 条`"
      />
    </section>

    <!-- Compact Single-Row Rule Filter Chips -->
    <section class="rule-chips-row">
      <button
        v-for="r in ruleCards"
        :key="r.code"
        type="button"
        class="xyf-chip rule-chip"
        :class="{ 'is-active': query.ruleCode === r.code }"
        :title="r.desc"
        @click="toggleRuleFilter(r.code)"
      >
        <span>{{ r.title }}</span>
        <span class="rule-chip__level">{{ r.level }}</span>
      </button>
    </section>

    <!-- Filter Bar -->
    <XyfFilterBar>
      <template #tabs>
        <div class="xyf-segmented">
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': query.handleStatus === undefined }"
            @click="setHandleStatus(undefined)"
          >
            全部
          </button>
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': query.handleStatus === 0 }"
            @click="setHandleStatus(0)"
          >
            待核查
          </button>
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': query.handleStatus === 1 }"
            @click="setHandleStatus(1)"
          >
            已处罚
          </button>
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': query.handleStatus === 2 }"
            @click="setHandleStatus(2)"
          >
            已排除
          </button>
        </div>
      </template>

      <template #fields>
        <el-select
          v-model="query.severity"
          placeholder="风险等级"
          clearable
          class="xyf-filter-select"
          @change="handleSearch"
        >
          <el-option :value="3" label="L3 高风险" />
          <el-option :value="2" label="L2 中风险" />
          <el-option :value="1" label="L1 低风险" />
        </el-select>

        <el-input
          v-model="query.keyword"
          placeholder="搜索单号、订单号或关键词"
          clearable
          :prefix-icon="Search"
          class="xyf-filter-search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </template>

      <template #actions>
        <el-button v-if="hasActiveFilter" @click="resetFilters">重置</el-button>
      </template>
    </XyfFilterBar>

    <!-- Alert Table -->
    <section class="xyf-surface-card table-card">
      <el-table :data="alertList" v-loading="loading" style="width: 100%">
        <el-table-column label="命中规则 / 单号" min-width="200">
          <template #default="{ row }">
            <div class="cell-primary">{{ row.ruleName }}</div>
            <div class="cell-sub xyf-mono">{{ row.alertNo }}</div>
          </template>
        </el-table-column>

        <el-table-column label="风险分" width="140">
          <template #default="{ row }">
            <div class="score-meter xyf-tabular">
              <div class="score-meter__head">
                <XyfStatusBadge
                  :tone="row.severity === 3 ? 'danger' : row.severity === 2 ? 'warning' : 'info'"
                  :label="`L${row.severity}`"
                />
                <strong class="xyf-mono">{{ row.riskScore }} 分</strong>
              </div>
              <div class="score-track">
                <div
                  class="score-fill"
                  :class="{ 'is-high': row.severity === 3 }"
                  :style="{ width: `${Math.min(100, Number(row.riskScore || 60))}%` }"
                />
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="涉事双方" min-width="220">
          <template #default="{ row }">
            <div class="pair-inline">
              <strong>{{ row.buyerName || `#${row.buyerId}` }}</strong>
              <span class="pair-sep">→</span>
              <strong>{{ row.sellerName || `#${row.sellerId}` }}</strong>
            </div>
            <div class="cell-sub xyf-tabular">
              7日同对 {{ row.pairOrderCount7d || 1 }} 单 · 反向 {{ row.reverseOrderCount7d || 0 }} 单
              <span v-if="row.fulfillDurationSeconds !== null && row.fulfillDurationSeconds !== undefined">
                · 履约 {{ row.fulfillDurationSeconds }}s
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="关联订单与摘要" min-width="240">
          <template #default="{ row }">
            <div class="cell-primary xyf-mono">
              {{ row.orderNumber || `订单#${row.orderId}` }} · ¥{{ Number(row.orderAmount || 0).toFixed(2) }}
            </div>
            <div class="cell-sub ellipsis">{{ row.evidenceSummary }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="135">
          <template #default="{ row }">
            <XyfStatusBadge v-if="row.handleStatus === 0" tone="warning" label="待核查" :dot="true" />
            <XyfStatusBadge
              v-else-if="row.handleStatus === 1"
              tone="danger"
              :label="`已扣罚 -${row.creditDeducted}分`"
            />
            <XyfStatusBadge v-else tone="success" label="已排除" />
          </template>
        </el-table-column>

        <el-table-column label="处置" width="150" fixed="right" align="right">
          <template #default="{ row }">
            <template v-if="row.handleStatus === 0">
              <el-button type="danger" size="small" plain @click="openResolveDialog(row, 1)">
                处罚
              </el-button>
              <el-button size="small" text @click="openResolveDialog(row, 2)">
                放行
              </el-button>
            </template>
            <span v-else class="cell-sub">已归档</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="xyf-pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchAlerts"
        />
      </div>
    </section>

    <!-- Resolve Modal -->
    <el-dialog
      v-model="resolveDialogVisible"
      :title="resolveForm.handleStatus === 1 ? '确认违规互刷并执行风控处罚' : '排除风控误报放行'"
      width="520px"
    >
      <div v-if="currentAlert" class="dialog-alert-summary">
        <strong>{{ currentAlert.ruleName }}（{{ currentAlert.alertNo }}）</strong>
        <p>{{ currentAlert.evidenceSummary }}</p>
      </div>

      <el-form label-position="top">
        <template v-if="resolveForm.handleStatus === 1">
          <el-form-item label="买卖双方各扣减校园履约信用分（分）">
            <el-slider v-model="resolveForm.creditDeductPoints" :min="5" :max="40" :step="5" show-input />
          </el-form-item>
          <el-form-item label="关联技能服务治理">
            <el-switch
              v-model="resolveForm.offlineService"
              active-text="同时强制下架涉事技能服务并推送整改通知"
              inactive-text="仅扣减双方信用分并警告"
            />
          </el-form-item>
        </template>

        <el-form-item label="处理备注">
          <el-input
            v-model="resolveForm.handleRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入核查依据或处理说明"
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
          确认执行处置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getRiskRadarOverview,
  listRiskAlerts,
  scanRiskRadarOrders,
  resolveRiskAlert,
  type RiskRadarOverviewData,
  type RiskAuditAlertItem
} from '@/api/riskRadar'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfFilterBar from '@/components/ui/XyfFilterBar.vue'

const loading = ref(false)
const scanning = ref(false)
const resolving = ref(false)
const scanDays = ref(7)

const ruleCards = [
  {
    code: 'BIDIRECTIONAL_WASH_TRADE',
    level: '高风险',
    title: '双向互买刷单',
    desc: '同一对账号短期内互为买卖双方交叉下单'
  },
  {
    code: 'HIGH_FREQ_SAME_PAIR',
    level: '中风险',
    title: '高频重复下单',
    desc: '7日内固定双方多次重复下单同一服务'
  },
  {
    code: 'ULTRA_FAST_COMPLETION',
    level: '中风险',
    title: '极速完成订单',
    desc: '付款到确认收货间隔低于 3 分钟'
  },
  {
    code: 'LOW_PRICE_SCORE_BRUSHING',
    level: '低风险',
    title: '异常低价刷分',
    desc: '成交金额明显低于同类服务正常水平'
  }
]

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

const hasActiveFilter = computed(() =>
  Boolean(query.ruleCode || query.severity || query.handleStatus !== undefined || query.keyword.trim())
)

function toggleRuleFilter(code: string) {
  query.ruleCode = query.ruleCode === code ? undefined : code
  handleSearch()
}

function setHandleStatus(st?: number) {
  query.handleStatus = st
  handleSearch()
}

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
    ElMessage.success(`扫描完成，新增 ${created} 条待核查记录`)
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

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.rule-chips-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.rule-chip {
  height: 36px;

  &__level {
    font-size: 11px;
    color: var(--xyf-ink-3);
  }

  &.is-active .rule-chip__level {
    color: rgba(255, 255, 255, 0.72);
  }
}

.table-card {
  padding: 20px 24px;
}

.cell-primary {
  font-size: 13px;
  font-weight: 600;
  color: var(--xyf-ink);
}

.cell-sub {
  font-size: 12px;
  color: var(--xyf-ink-3);
  margin-top: 3px;
}

.score-meter {
  display: flex;
  flex-direction: column;
  gap: 5px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
  }

  .score-track {
    height: 3px;
    border-radius: 999px;
    background: #ececef;
    overflow: hidden;

    .score-fill {
      height: 100%;
      background: #18181b;
      border-radius: 999px;

      &.is-high {
        background: #dc2626;
      }
    }
  }
}

.pair-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--xyf-ink);

  strong {
    font-weight: 600;
  }

  .pair-sep {
    color: var(--xyf-ink-4);
  }
}

.dialog-alert-summary {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  margin-bottom: 16px;

  strong {
    display: block;
    font-size: 13.5px;
    color: var(--xyf-ink);
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }
}
</style>
