<template>
  <div class="sales-dashboard-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="销售统计"
        :show-back="true"
        back-text="返回我的服务"
        back-to="/my-services"
      >
        <template #actions>
          <el-button @click="router.push('/orders/sales')">查看销售订单</el-button>
          <el-button type="primary" @click="router.push('/service/publish')">发布新服务</el-button>
        </template>
      </XyfPageHeader>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!stats" class="xyf-surface-card loading-card">
        <XyfEmptyState
          title="暂无可展示的销售统计数据"
          description="发布校园服务并完成首笔成交后，系统将为你汇总多维度销售看板。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/my-services')">前往服务管理</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- KPI Cards -->
        <section class="kpi-grid">
          <XyfStatTile
            label="累计成交单数"
            :value="stats.totalSales ?? 0"
            hint="已完成验收结算订单总计"
            tone="teal"
            clickable
            @click="router.push('/orders/sales')"
          >
            <template #icon><el-icon><ShoppingBag /></el-icon></template>
          </XyfStatTile>

          <XyfStatTile
            label="累计服务营收"
            :value="formatPrice(stats.totalRevenue ?? 0)"
            hint="已结算进可用钱包总额"
            tone="amber"
            clickable
            @click="router.push('/wallet')"
          >
            <template #icon><el-icon><Wallet /></el-icon></template>
          </XyfStatTile>

          <XyfStatTile
            label="平均客单价"
            :value="formatPrice(stats.averagePrice ?? 0)"
            hint="单笔已完成交易均值"
            tone="info"
          >
            <template #icon><el-icon><TrendCharts /></el-icon></template>
          </XyfStatTile>

          <XyfStatTile
            label="最畅销服务"
            :value="stats.bestService ? `${stats.bestService.sales} 单` : '暂无'"
            :hint="stats.bestService?.serviceTitle || '等待首笔成交'"
            tone="sage"
            :clickable="Boolean(stats.bestService)"
            @click="stats.bestService && router.push(`/services/${stats.bestService.serviceId}`)"
          >
            <template #icon><el-icon><Medal /></el-icon></template>
          </XyfStatTile>
        </section>

        <!-- Recent Completed Sales Orders -->
        <section class="xyf-surface-card table-card">
          <div class="xyf-section-heading">
            <h2>近期已完成成交记录</h2>
          </div>

          <XyfEmptyState
            v-if="!stats.recentOrders || stats.recentOrders.length === 0"
            title="暂无近期成交记录"
            description="完成销售订单验收后，最近成交单号与金额将展示在此。"
          />

          <el-table v-else :data="stats.recentOrders" stripe>
            <el-table-column label="订单编号" min-width="180">
              <template #default="{ row }">
                <button
                  type="button"
                  class="svc-link xyf-mono"
                  @click="router.push(`/orders/${row.orderId}`)"
                >
                  {{ row.orderNumber }}
                </button>
              </template>
            </el-table-column>

            <el-table-column label="成交时间" min-width="170">
              <template #default="{ row }">
                <span class="xyf-tabular">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="入账金额" width="140" align="right">
              <template #default="{ row }">
                <span class="revenue-cell xyf-tabular">
                  {{ formatPrice(row.totalAmount) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" align="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  plain
                  @click="router.push(`/orders/${row.orderId}`)"
                >
                  订单详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingBag, Wallet, TrendCharts, Medal } from '@element-plus/icons-vue'
import { getUserSalesStatistics } from '@/api/statistics'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTime } from '@/utils/format'
import type { UserSalesStatistics } from '@/types/statistics'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const loading = ref(true)
const stats = ref<UserSalesStatistics | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await getUserSalesStatistics()
  } catch (err) {
    notifyFallbackError(err, '获取销售统计失败')
    stats.value = null
  } finally {
    loading.value = false
  }
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

.table-card {
  padding: 22px 24px;
}

.svc-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--xyf-primary);
  font-weight: 600;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.revenue-cell {
  font-weight: 700;
  color: var(--xyf-accent);
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
