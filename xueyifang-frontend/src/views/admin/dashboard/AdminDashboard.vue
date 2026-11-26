<template>
  <div class="admin-dashboard">
    <XyfPageHeader
      kicker="OPERATIONS OVERVIEW"
      title="平台运营总览"
      subtitle="监控用户注册增长、今日新增活跃指标、平台交易流水与仲裁健康度"
    >
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
      <!-- KPI Grid -->
      <section class="kpi-grid">
        <XyfStatTile
          label="注册用户总数"
          :value="stats?.totalUsers ?? 0"
          :hint="`近30天活跃 ${stats?.activeUsers ?? 0} 人 · 今日新增 ${stats?.todayNewUsers ?? 0}`"
          tone="teal"
          clickable
          @click="router.push('/admin/permissions')"
        >
          <template #icon><el-icon><User /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="平台服务总数"
          :value="stats?.totalServices ?? 0"
          :hint="`今日新增服务 ${stats?.todayNewServices ?? 0} 项`"
          tone="sage"
          clickable
          @click="router.push('/admin/services')"
        >
          <template #icon><el-icon><ShoppingBag /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="累计订单与成交额"
          :value="stats?.totalOrders ?? 0"
          :hint="`累计成交 ${formatPrice(stats?.totalTransactionAmount ?? 0)} · 已完成 ${stats?.completedOrders ?? 0}`"
          tone="amber"
        >
          <template #icon><el-icon><Wallet /></el-icon></template>
        </XyfStatTile>

        <XyfStatTile
          label="待处理订单纠纷"
          :value="stats?.pendingDisputes ?? 0"
          :hint="`今日新增订单 ${stats?.todayNewOrders ?? 0} 笔`"
          tone="danger"
          clickable
          @click="router.push('/admin/disputes')"
        >
          <template #icon><el-icon><WarnTriangleFilled /></el-icon></template>
        </XyfStatTile>
      </section>

      <!-- Action Queues + Chart Row -->
      <div class="dashboard-split">
        <!-- 7-Day Trend Chart -->
        <section class="xyf-surface-card chart-card">
          <div class="card-top">
            <div>
              <h2>近 7 日订单、服务与成交额趋势</h2>
            </div>
          </div>

          <div ref="chartRef" class="trend-chart-canvas" />
        </section>

        <!-- Urgent Governance Queues -->
        <aside class="xyf-surface-card queue-card">
          <div class="card-top">
            <div>
              <h2>审核与风控快捷工作台</h2>
            </div>
          </div>

          <div class="queue-list">
            <button
              type="button"
              class="queue-item"
              @click="router.push('/admin/permissions')"
            >
              <div class="queue-item__left">
                <span class="queue-icon tone-teal"><el-icon><Stamp /></el-icon></span>
                <div>
                  <strong>发布权限待审核</strong>
                  <p>核验同学提交的特长证明与发布资格</p>
                </div>
              </div>
              <el-icon class="queue-arrow"><ArrowRight /></el-icon>
            </button>

            <button
              type="button"
              class="queue-item"
              @click="router.push('/admin/services')"
            >
              <div class="queue-item__left">
                <span class="queue-icon tone-info"><el-icon><Checked /></el-icon></span>
                <div>
                  <strong>新发布服务审核</strong>
                  <p>审查定价合理性、图片规范与学术合规性</p>
                </div>
              </div>
              <el-icon class="queue-arrow"><ArrowRight /></el-icon>
            </button>

            <button
              type="button"
              class="queue-item"
              @click="router.push('/admin/disputes')"
            >
              <div class="queue-item__left">
                <span class="queue-icon tone-danger"><el-icon><WarnTriangleFilled /></el-icon></span>
                <div>
                  <strong>订单纠纷仲裁工作台</strong>
                  <p>当前共 {{ stats?.pendingDisputes ?? 0 }} 件待裁决纠纷单</p>
                </div>
              </div>
              <span class="queue-count xyf-tabular">{{ stats?.pendingDisputes ?? 0 }}</span>
            </button>

            <button
              type="button"
              class="queue-item"
              @click="router.push('/admin/settings')"
            >
              <div class="queue-item__left">
                <span class="queue-icon tone-amber"><el-icon><Setting /></el-icon></span>
                <div>
                  <strong>系统设置与学籍导入</strong>
                  <p>配置开放注册开关与批量导入校园用户</p>
                </div>
              </div>
              <el-icon class="queue-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User,
  ShoppingBag,
  Wallet,
  WarnTriangleFilled,
  Stamp,
  Checked,
  Setting,
  ArrowRight,
  Refresh
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { getAdminStatistics, getAdminTrend } from '@/api/admin'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import type { AdminStatistics, AdminTrend } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'

const router = useRouter()

const loading = ref(true)
const stats = ref<AdminStatistics | null>(null)
const trendData = ref<AdminTrend | null>(null)
const chartRef = ref<HTMLDivElement | null>(null)

let chartInstance: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

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
      backgroundColor: '#14191D',
      borderColor: '#14191D',
      textStyle: { color: '#F8FAFC', fontSize: 12 }
    },
    legend: {
      data: ['日新增订单', '日新增服务', '日成交金额 (元)'],
      bottom: 0,
      textStyle: { color: '#4B5563', fontSize: 12 }
    },
    grid: { top: 24, left: 42, right: 48, bottom: 42 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E3DFD5' } },
      axisLabel: { color: '#6B7280', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '数量',
        minInterval: 1,
        axisLabel: { color: '#6B7280', fontSize: 11 },
        splitLine: { lineStyle: { color: '#F1EDE4', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '金额 (元)',
        axisLabel: { color: '#6B7280', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '日新增订单',
        type: 'bar',
        barMaxWidth: 22,
        itemStyle: { color: '#18181B', borderRadius: [5, 5, 0, 0] },
        data: orderCounts
      },
      {
        name: '日新增服务',
        type: 'bar',
        barMaxWidth: 22,
        itemStyle: { color: '#B45309', borderRadius: [5, 5, 0, 0] },
        data: serviceCounts
      },
      {
        name: '日成交金额 (元)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: '#C2410C' },
        lineStyle: { width: 2.5, color: '#C2410C' },
        areaStyle: { color: 'rgba(194, 65, 12, 0.08)' },
        data: amounts
      }
    ]
  })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [s, t] = await Promise.all([
      getAdminStatistics(),
      getAdminTrend().catch(() => null)
    ])
    stats.value = s
    trendData.value = t
    await nextTick()
    renderChart()
  } catch (err) {
    notifyFallbackError(err, '加载运营总览数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDashboard()
  if (chartRef.value) {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.dashboard-split {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.9fr);
  gap: 20px;
  align-items: start;
}

.chart-card,
.queue-card {
  padding: 22px 24px;
}

.card-top {
  margin-bottom: 16px;

  h2 {
    margin: 4px 0 0;
    font-size: 17px;
    font-weight: 750;
    color: var(--xyf-ink);
  }
}

.trend-chart-canvas {
  width: 100%;
  height: 310px;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.queue-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 14px;
  border-radius: var(--xyf-radius-md);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-bg);
  cursor: pointer;
  text-align: left;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-primary);
    background: var(--xyf-surface);
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;

    strong {
      display: block;
      font-size: 14px;
      color: var(--xyf-ink);
      margin-bottom: 2px;
    }

    p {
      margin: 0;
      font-size: 12px;
      color: var(--xyf-ink-muted);
    }
  }
}

.queue-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--xyf-radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;

  &.tone-teal,
  &.tone-info,
  &.tone-amber {
    background: #f4f4f5;
    border: 1px solid #e4e4e7;
    color: #18181b;
  }
  &.tone-danger {
    background: #faf5f5;
    border: 1px solid #eed6d6;
    color: #b91c1c;
  }
}

.queue-count {
  min-width: 28px;
  height: 26px;
  padding: 0 9px;
  border-radius: var(--xyf-radius-pill);
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  font-size: 13px;
  font-weight: 700;
  color: var(--xyf-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.queue-arrow {
  color: var(--xyf-ink-muted);
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
