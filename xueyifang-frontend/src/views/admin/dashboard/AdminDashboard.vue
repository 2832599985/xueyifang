<template>
  <div class="admin-dashboard">
    <XyfPageHeader title="数据概览" dense>
      <template #actions>
        <el-button @click="loadDashboard" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </template>
    </XyfPageHeader>

    <div v-if="loading && !stats" class="xyf-surface-card loading-card">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- Row 1: 1 Primary + 3 Secondary KPI Tiles -->
      <section class="kpi-grid">
        <XyfStatTile
          label="累计成交额"
          :value="formatMoneyCompact(stats?.totalTransactionAmount ?? 0)"
          prefix="¥"
          :hint="`已完成 ${stats?.completedOrders ?? 0} / ${stats?.totalOrders ?? 0} 笔订单`"
          :delta="{ value: `今日 +${stats?.todayNewOrders ?? 0} 单`, direction: (stats?.todayNewOrders ?? 0) > 0 ? 'up' : 'flat' }"
          emphasis="primary"
          clickable
          @click="router.push('/admin/finance')"
        >
          <template #icon><el-icon><Wallet /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="注册用户"
          :value="stats?.totalUsers ?? 0"
          unit="人"
          :hint="`近 30 天活跃 ${stats?.activeUsers ?? 0} 人`"
          :delta="{ value: `今日 +${stats?.todayNewUsers ?? 0}`, direction: (stats?.todayNewUsers ?? 0) > 0 ? 'up' : 'flat' }"
          clickable
          @click="router.push('/admin/users')"
        >
          <template #icon><el-icon><User /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="平台服务"
          :value="stats?.totalServices ?? 0"
          unit="项"
          :hint="`今日新增 ${stats?.todayNewServices ?? 0} 项`"
          clickable
          @click="router.push('/admin/services')"
        >
          <template #icon><el-icon><ShoppingBag /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="待处理纠纷"
          :value="stats?.pendingDisputes ?? 0"
          unit="件"
          hint="需管理员核查裁决"
          :tone="(stats?.pendingDisputes ?? 0) > 0 ? 'danger' : 'default'"
          clickable
          @click="router.push('/admin/disputes')"
        >
          <template #icon><el-icon><WarnTriangleFilled /></el-icon></template>
        </XyfStatTile>
      </section>

      <!-- Row 2: 1.5fr Chart + 1fr Real-Count Governance Queue -->
      <div class="dashboard-split">
        <section class="xyf-surface-card chart-card">
          <div class="card-top">
            <h2>近 7 日成交与新增</h2>
            <span class="card-meta">按日汇总</span>
          </div>

          <div ref="chartRef" class="trend-chart-canvas" />
        </section>

        <aside class="xyf-surface-card queue-card">
          <div class="card-top">
            <h2>待办与治理队列</h2>
            <span class="card-meta">实时汇总</span>
          </div>

          <div class="queue-list">
            <button
              v-for="item in todoItems"
              :key="item.path"
              type="button"
              class="queue-row"
              @click="router.push(item.path)"
            >
              <div class="queue-row__left">
                <el-icon class="queue-row__icon"><component :is="item.icon" /></el-icon>
                <div class="queue-row__text">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.sub }}</span>
                </div>
              </div>
              <div class="queue-row__right">
                <span
                  class="queue-count xyf-tabular"
                  :class="{
                    'is-alert': item.alert && typeof item.count === 'number' && item.count > 0,
                    'is-muted': item.count === null
                  }"
                >
                  {{ item.count === null ? '—' : item.count }}
                </span>
                <el-icon class="queue-arrow"><ArrowRight /></el-icon>
              </div>
            </button>
          </div>
        </aside>
      </div>

      <!-- Row 3: Lightweight Bottom Governance Strip (No auto heavy audit on mount) -->
      <footer class="status-strip">
        <div class="status-strip__items xyf-tabular">
          <span>托管冻结池 <strong>{{ financeFrozenPool !== null ? formatPrice(financeFrozenPool) : '—' }}</strong></span>
          <span class="strip-sep" />
          <span>开放悬赏 <strong>{{ openDemandCount !== null ? `${openDemandCount} 条` : '—' }}</strong></span>
        </div>
        <div class="status-strip__links">
          <el-button text size="small" @click="router.push('/admin/system-health')">前往系统检查</el-button>
          <el-button text size="small" @click="router.push('/admin/settings')">参数与学籍导入</el-button>
        </div>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User,
  ShoppingBag,
  Wallet,
  WarnTriangleFilled,
  Stamp,
  Checked,
  Compass,
  Aim,
  ArrowRight,
  Refresh
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { getAdminStatistics, getAdminTrend, getPendingUsers } from '@/api/admin'
import { getPendingServices } from '@/api/adminService'
import { adminListDemands } from '@/api/demand'
import { getRiskRadarOverview } from '@/api/riskRadar'
import { getFinanceOverview } from '@/api/finance'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import type { AdminStatistics, AdminTrend } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'

const router = useRouter()

const loading = ref(true)
const stats = ref<AdminStatistics | null>(null)
const trendData = ref<AdminTrend | null>(null)
const pendingServiceCount = ref<number | null>(null)
const pendingPermissionCount = ref<number | null>(null)
const pendingRiskCount = ref<number | null>(null)
const financeDiscrepancyCount = ref<number | null>(null)
const financeFrozenPool = ref<number | null>(null)
const openDemandCount = ref<number | null>(null)

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

function formatMoneyCompact(val: number | string) {
  const num = Number(val || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const todoItems = computed(() => [
  {
    path: '/admin/services',
    icon: Checked,
    title: '待审核服务',
    sub: '新发布或修改待上架项目',
    count: pendingServiceCount.value,
    alert: false
  },
  {
    path: '/admin/permissions',
    icon: Stamp,
    title: '发布权限申请',
    sub: '核验学生特长与发布资格',
    count: pendingPermissionCount.value,
    alert: false
  },
  {
    path: '/admin/disputes',
    icon: WarnTriangleFilled,
    title: '待裁决纠纷',
    sub: '订单争议举证与资金裁决',
    count: stats.value ? stats.value.pendingDisputes : null,
    alert: true
  },
  {
    path: '/admin/risk-radar',
    icon: Compass,
    title: '异常订单待核查',
    sub: '风控规则命中的待核实记录',
    count: pendingRiskCount.value,
    alert: true
  },
  {
    path: '/admin/finance',
    icon: Wallet,
    title: '财务对账差额',
    sub: '存在账目偏差的对账单数',
    count: financeDiscrepancyCount.value,
    alert: true
  },
  {
    path: '/admin/demands',
    icon: Aim,
    title: '悬赏招募巡检',
    sub: '当前正在公开招募的需求',
    count: openDemandCount.value,
    alert: false
  }
])

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const dates = trendData.value?.dates?.length ? trendData.value.dates : ['暂无日期']
  const orderCounts = trendData.value?.orderCounts || [0]
  const serviceCounts = trendData.value?.serviceCounts || [0]
  const amounts = trendData.value?.transactionAmounts || [0]

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#18181b',
      borderColor: '#27272a',
      textStyle: { color: '#ffffff', fontSize: 12 }
    },
    legend: {
      data: ['日新增订单', '日新增服务', '日成交金额 (元)'],
      bottom: 0,
      textStyle: { color: '#52525b', fontSize: 12 }
    },
    grid: { top: 24, left: 42, right: 48, bottom: 42 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e6e6ea' } },
      axisTick: { show: false },
      axisLabel: { color: '#71717a', fontSize: 12 }
    },
    yAxis: [
      {
        type: 'value',
        name: '数量',
        nameTextStyle: { color: '#71717a', fontSize: 12 },
        minInterval: 1,
        axisLabel: { color: '#71717a', fontSize: 12 },
        splitLine: { lineStyle: { color: '#f1f1f4', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '金额 (元)',
        nameTextStyle: { color: '#71717a', fontSize: 12 },
        axisLabel: { color: '#71717a', fontSize: 12 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '日新增订单',
        type: 'bar',
        barMaxWidth: 20,
        itemStyle: { color: '#18181b', borderRadius: [4, 4, 0, 0] },
        data: orderCounts
      },
      {
        name: '日新增服务',
        type: 'bar',
        barMaxWidth: 20,
        itemStyle: { color: '#d4d4d8', borderRadius: [4, 4, 0, 0] },
        data: serviceCounts
      },
      {
        name: '日成交金额 (元)',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.25,
        symbolSize: 6,
        itemStyle: { color: '#b89058' },
        lineStyle: { width: 2, color: '#b89058' },
        areaStyle: { color: 'rgba(184, 144, 88, 0.06)' },
        data: amounts
      }
    ]
  })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [statRes, trendRes, svcRes, permRes, demandRes, riskRes, finRes] =
      await Promise.allSettled([
        getAdminStatistics(),
        getAdminTrend(),
        getPendingServices(1, 1),
        getPendingUsers(1, 1),
        adminListDemands({ pageNum: 1, pageSize: 1, status: 0 }),
        getRiskRadarOverview(),
        getFinanceOverview()
      ])

    if (statRes.status === 'fulfilled') {
      stats.value = statRes.value
    } else {
      notifyFallbackError(statRes.reason, '加载运营总览数据失败')
    }

    trendData.value = trendRes.status === 'fulfilled' ? trendRes.value : null
    pendingServiceCount.value = svcRes.status === 'fulfilled' ? (svcRes.value.total ?? 0) : null
    pendingPermissionCount.value = permRes.status === 'fulfilled' ? (permRes.value.total ?? 0) : null
    openDemandCount.value = demandRes.status === 'fulfilled' ? (demandRes.value.total ?? 0) : null
    pendingRiskCount.value = riskRes.status === 'fulfilled' ? (riskRes.value.pendingAlerts ?? 0) : null
    financeDiscrepancyCount.value =
      finRes.status === 'fulfilled' ? (finRes.value.discrepancyReportCount ?? 0) : null
    financeFrozenPool.value =
      finRes.status === 'fulfilled' ? Number(finRes.value.currentEscrowFrozenPool ?? 0) : null

    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDashboard()
  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize()
    })
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.dashboard-split {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  align-items: start;
}

.chart-card,
.queue-card {
  padding: 20px 24px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .card-meta {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.trend-chart-canvas {
  width: 100%;
  height: 320px;
}

.queue-list {
  display: flex;
  flex-direction: column;
}

.queue-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 8px;
  border: none;
  border-bottom: 1px solid var(--xyf-border-subtle);
  background: transparent;
  border-radius: var(--xyf-radius-xs);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background-color var(--xyf-transition);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--xyf-surface-soft);
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  &__icon {
    font-size: 16px;
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }

  &__text {
    min-width: 0;

    strong {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: var(--xyf-ink);
    }

    span {
      display: block;
      font-size: 12px;
      color: var(--xyf-ink-3);
      margin-top: 2px;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
}

.queue-count {
  min-width: 26px;
  height: 24px;
  padding: 0 8px;
  border-radius: var(--xyf-radius-xs);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;

  &.is-alert {
    background: var(--xyf-danger-soft);
    border-color: #f5caca;
    color: #b91c1c;
  }

  &.is-muted {
    color: var(--xyf-ink-4);
  }
}

.queue-arrow {
  color: var(--xyf-ink-4);
  font-size: 14px;
}

.status-strip {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;

  &__items {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
    font-size: 13px;
    color: var(--xyf-ink-3);

    strong {
      color: var(--xyf-ink);
      font-weight: 600;
      margin-left: 4px;
    }
  }

  .strip-sep {
    width: 1px;
    height: 12px;
    background: var(--xyf-border-strong);
  }

  &__links {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

@media (max-width: 1120px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
