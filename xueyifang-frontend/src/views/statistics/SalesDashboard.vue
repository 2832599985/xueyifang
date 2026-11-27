<template>
  <div class="sales-dashboard-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="销售统计">
        <template #actions>
          <el-button type="primary" @click="router.push('/service/publish')">发布新服务</el-button>
        </template>
      </XyfPageHeader>

      <SellerSuiteTabs />

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!stats" class="xyf-surface-card loading-card">
        <XyfEmptyState
          title="暂无可展示的销售统计数据"
          description="发布校园服务并完成首笔成交后，系统将为你汇总销售数据。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/my-services')">前往服务管理</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- 3 Core KPI Tiles (Primary + 2 Secondary) -->
        <section class="kpi-grid">
          <XyfStatTile
            label="累计服务营收"
            :value="formatRevenueNumber(stats.totalRevenue ?? 0)"
            prefix="¥"
            hint="已结算至可用钱包"
            emphasis="primary"
            clickable
            @click="router.push('/wallet')"
          >
            <template #icon><el-icon><Wallet /></el-icon></template>
          </XyfStatTile>

          <XyfStatTile
            label="累计成交单数"
            :value="stats.totalSales ?? 0"
            unit="单"
            hint="已完成验收结算"
            clickable
            @click="router.push('/orders/sales')"
          >
            <template #icon><el-icon><ShoppingBag /></el-icon></template>
          </XyfStatTile>

          <XyfStatTile
            label="平均客单价"
            :value="formatRevenueNumber(stats.averagePrice ?? 0)"
            prefix="¥"
            hint="单笔已完成交易均值"
          >
            <template #icon><el-icon><TrendCharts /></el-icon></template>
          </XyfStatTile>
        </section>

        <!-- Visual Insight Row: Honest Chart + Top Services by Sales -->
        <section class="insight-grid">
          <div class="xyf-surface-card panel-card">
            <div class="panel-head">
              <div class="xyf-section-heading" style="margin-bottom: 0">
                <h2>{{ chartModeTitle }}</h2>
              </div>
              <span class="panel-meta xyf-tabular">{{ chartModeSubtitle }}</span>
            </div>

            <div v-if="chartPoints.length > 0" ref="chartRef" class="sales-chart" />
            <XyfEmptyState
              v-else
              compact
              title="暂无已完成订单"
              description="产生首笔已完成订单后将自动展示成交金额分布。"
            />
          </div>

          <div class="xyf-surface-card panel-card">
            <div class="panel-head">
              <div class="xyf-section-heading" style="margin-bottom: 0">
                <h2>服务销量排行</h2>
              </div>
              <span class="panel-meta">按累计销量</span>
            </div>

            <div v-if="topServices.length > 0" class="rank-list">
              <button
                v-for="(svc, idx) in topServices"
                :key="svc.serviceId"
                type="button"
                class="rank-row"
                @click="router.push(`/services/${svc.serviceId}`)"
              >
                <div class="rank-row__top">
                  <span class="rank-index xyf-mono">0{{ idx + 1 }}</span>
                  <span class="rank-title ellipsis">{{ svc.serviceTitle }}</span>
                  <strong class="rank-count xyf-tabular">{{ svc.sales }} 单</strong>
                </div>
                <div class="rank-track">
                  <div
                    class="rank-track__fill"
                    :class="{ 'is-top': idx === 0 }"
                    :style="{ width: `${Math.max(8, Math.round((svc.sales / maxServiceSales) * 100))}%` }"
                  />
                </div>
              </button>
            </div>

            <XyfEmptyState
              v-else
              compact
              title="暂无服务销量记录"
              description="上架服务并成交后这里将按销量排序展示。"
            />
          </div>
        </section>

        <!-- Recent Completed Sales Orders -->
        <section class="xyf-surface-card panel-card">
          <div class="panel-head">
            <div class="xyf-section-heading" style="margin-bottom: 0">
              <h2>近期已完成成交</h2>
            </div>
            <el-button text size="small" @click="router.push('/orders/sales')">查看全部订单</el-button>
          </div>

          <XyfEmptyState
            v-if="recentRows.length === 0"
            compact
            title="暂无近期成交记录"
            description="完成销售订单验收后，近期成交明细将展示在此。"
          />

          <div v-else class="recent-list">
            <div
              v-for="row in recentRows"
              :key="row.orderId"
              class="recent-row"
              @click="router.push(`/orders/${row.orderId}`)"
            >
              <div class="recent-row__left">
                <XyfImage
                  v-if="row.serviceImage"
                  :src="buildImageUrl(row.serviceImage)"
                  :alt="row.serviceTitle"
                  width="56px"
                  height="42px"
                  :radius="6"
                  fit="cover"
                />
                <div class="recent-row__info">
                  <strong class="ellipsis">{{ row.serviceTitle || `订单 ${row.orderNumber}` }}</strong>
                  <div class="recent-row__meta">
                    <span class="xyf-mono">{{ row.orderNumber }}</span>
                    <span>·</span>
                    <span class="xyf-tabular">{{ formatDateTime(row.createTime) }}</span>
                  </div>
                </div>
              </div>
              <div class="recent-row__right xyf-tabular">
                <strong>{{ formatPrice(row.totalAmount) }}</strong>
              </div>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingBag, Wallet, TrendCharts } from '@element-plus/icons-vue'
import echarts, { type ECharts, type EChartsOption } from '@/utils/echarts'
import { getUserSalesStatistics } from '@/api/statistics'
import { listSellerOrders } from '@/api/order'
import { getMyServicesEnhanced, buildImageUrl } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTime } from '@/utils/format'
import { OrderStatus, type OrderListItem } from '@/types/order'
import type { MyServiceItem } from '@/types/service'
import type { UserSalesStatistics } from '@/types/statistics'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import SellerSuiteTabs from '@/components/service/SellerSuiteTabs.vue'

interface ChartPoint {
  label: string
  amount: number
  count: number
}

interface RankServiceRow {
  serviceId: number
  serviceTitle: string
  sales: number
}

interface RecentOrderRow {
  orderId: number
  orderNumber: string
  serviceTitle: string
  serviceImage?: string
  totalAmount: number
  createTime: string
}

const router = useRouter()

const loading = ref(true)
const stats = ref<UserSalesStatistics | null>(null)
const sellerOrders = ref<OrderListItem[]>([])
const myServices = ref<MyServiceItem[]>([])

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function formatRevenueNumber(val: number | string) {
  const num = Number(val || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const completedOrders = computed(() =>
  sellerOrders.value.filter((o) => o.orderStatus === OrderStatus.COMPLETED)
)

const dailyBuckets = computed<ChartPoint[]>(() => {
  const map = new Map<string, { amount: number; count: number }>()
  for (const o of completedOrders.value) {
    if (!o.createTime) continue
    const dateKey = String(o.createTime).slice(0, 10)
    const cur = map.get(dateKey) || { amount: 0, count: 0 }
    cur.amount += Number(o.totalAmount || 0)
    cur.count += 1
    map.set(dateKey, cur)
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(-14)
    .map(([dateKey, v]) => ({
      label: dateKey.slice(5),
      amount: Number(v.amount.toFixed(2)),
      count: v.count
    }))
})

const isMultiDay = computed(() => dailyBuckets.value.length >= 2)

const chartPoints = computed<ChartPoint[]>(() => {
  if (isMultiDay.value) return dailyBuckets.value
  const src = completedOrders.value.length > 0
    ? completedOrders.value.slice(0, 8).reverse().map((o) => ({
        label: o.orderNumber.slice(-4),
        amount: Number(o.totalAmount || 0),
        count: 1
      }))
    : (stats.value?.recentOrders || []).slice().reverse().map((o) => ({
        label: o.orderNumber.slice(-4),
        amount: Number(o.totalAmount || 0),
        count: 1
      }))
  return src
})

const chartModeTitle = computed(() => (isMultiDay.value ? '按日成交营收分布' : '近期单笔成交金额'))
const chartModeSubtitle = computed(() =>
  isMultiDay.value ? `近 ${dailyBuckets.value.length} 个成交日` : `最近 ${chartPoints.value.length} 笔已完成订单`
)

const topServices = computed<RankServiceRow[]>(() => {
  const fromMy = myServices.value
    .map((s) => ({
      serviceId: s.id,
      serviceTitle: s.serviceTitle,
      sales: Number(s.salesCount ?? s.currentPurchaseCount ?? 0)
    }))
    .filter((s) => s.sales > 0)
    .sort((a, b) => b.sales - a.sales)
    .slice(0, 5)

  if (fromMy.length > 0) return fromMy
  if (stats.value?.bestService) {
    return [
      {
        serviceId: stats.value.bestService.serviceId,
        serviceTitle: stats.value.bestService.serviceTitle,
        sales: stats.value.bestService.sales
      }
    ]
  }
  return []
})

const maxServiceSales = computed(() =>
  Math.max(1, ...topServices.value.map((s) => s.sales))
)

const recentRows = computed<RecentOrderRow[]>(() => {
  if (completedOrders.value.length > 0) {
    return completedOrders.value.slice(0, 8).map((o) => ({
      orderId: o.id,
      orderNumber: o.orderNumber,
      serviceTitle: o.serviceTitle,
      serviceImage: o.serviceImage,
      totalAmount: Number(o.totalAmount || 0),
      createTime: o.createTime
    }))
  }
  return (stats.value?.recentOrders || []).map((o) => ({
    orderId: o.orderId,
    orderNumber: o.orderNumber,
    serviceTitle: '',
    totalAmount: Number(o.totalAmount || 0),
    createTime: o.createTime
  }))
})

function renderChart() {
  if (!chartRef.value || chartPoints.value.length === 0) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const labels = chartPoints.value.map((p) => p.label)
  const amounts = chartPoints.value.map((p) => p.amount)
  const counts = chartPoints.value.map((p) => p.count)

  const option: EChartsOption = {
    grid: { top: 24, right: isMultiDay.value ? 36 : 16, bottom: 28, left: 48 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#18181b',
      borderColor: '#27272a',
      textStyle: { color: '#ffffff', fontSize: 12 }
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e6e6ea' } },
      axisTick: { show: false },
      axisLabel: { color: '#71717a', fontSize: 12 }
    },
    yAxis: isMultiDay.value
      ? [
          {
            type: 'value',
            splitLine: { lineStyle: { color: '#f1f1f4', type: 'dashed' } },
            axisLabel: { color: '#71717a', fontSize: 12, formatter: '¥{value}' }
          },
          {
            type: 'value',
            minInterval: 1,
            splitLine: { show: false },
            axisLabel: { color: '#71717a', fontSize: 12, formatter: '{value}单' }
          }
        ]
      : {
          type: 'value',
          splitLine: { lineStyle: { color: '#f1f1f4', type: 'dashed' } },
          axisLabel: { color: '#71717a', fontSize: 12, formatter: '¥{value}' }
        },
    series: isMultiDay.value
      ? [
          {
            name: '日营收',
            type: 'bar',
            barMaxWidth: 26,
            itemStyle: { color: '#18181b', borderRadius: [4, 4, 0, 0] },
            data: amounts
          },
          {
            name: '成交单数',
            type: 'line',
            yAxisIndex: 1,
            smooth: 0.25,
            symbolSize: 6,
            lineStyle: { color: '#b89058', width: 2 },
            itemStyle: { color: '#b89058' },
            data: counts
          }
        ]
      : [
          {
            name: '成交金额',
            type: 'bar',
            barMaxWidth: 32,
            itemStyle: { color: '#18181b', borderRadius: [4, 4, 0, 0] },
            data: amounts
          }
        ]
  }

  chartInstance.setOption(option, true)
}

watch(
  chartPoints,
  async () => {
    await nextTick()
    renderChart()
  },
  { flush: 'post' }
)

onMounted(async () => {
  loading.value = true
  try {
    const [statRes, ordersRes, servicesRes] = await Promise.allSettled([
      getUserSalesStatistics(),
      listSellerOrders({ pageNum: 1, pageSize: 100 }),
      getMyServicesEnhanced(undefined, { pageNum: 1, pageSize: 50 })
    ])

    if (statRes.status === 'fulfilled') {
      stats.value = statRes.value
    } else {
      notifyFallbackError(statRes.reason, '获取销售统计失败')
      stats.value = null
    }

    if (ordersRes.status === 'fulfilled') {
      sellerOrders.value = ordersRes.value.records || []
    }

    if (servicesRes.status === 'fulfilled') {
      myServices.value = servicesRes.value.records || []
    }
  } finally {
    loading.value = false
    await nextTick()
    renderChart()
    if (chartRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => chartInstance?.resize())
      resizeObserver.observe(chartRef.value)
    }
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
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
  grid-template-columns: 1.35fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.insight-grid {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.panel-card {
  padding: 20px 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-meta {
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.sales-chart {
  width: 100%;
  height: 260px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rank-row {
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:hover .rank-title {
    color: var(--xyf-ink);
    text-decoration: underline;
    text-underline-offset: 3px;
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
  }

  .rank-index {
    font-size: 12px;
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }

  .rank-title {
    flex: 1;
    min-width: 0;
    font-weight: 500;
    color: var(--xyf-ink-2);
  }

  .rank-count {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
    flex-shrink: 0;
  }
}

.rank-track {
  height: 6px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  overflow: hidden;

  &__fill {
    height: 100%;
    border-radius: 999px;
    background: #3f3f46;
    transition: width 0.35s ease;

    &.is-top {
      background: #18181b;
    }
  }
}

.recent-list {
  display: flex;
  flex-direction: column;
}

.recent-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 10px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  border-radius: var(--xyf-radius-xs);
  cursor: pointer;
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
    flex: 1;
  }

  &__info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;

    strong {
      font-size: 14px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  &__right strong {
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

@media (max-width: 960px) {
  .kpi-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
