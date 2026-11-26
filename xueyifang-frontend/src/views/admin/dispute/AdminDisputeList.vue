<template>
  <div class="admin-dispute-list">
    <XyfPageHeader
      title="订单纠纷仲裁台"
      subtitle="按处理状态与订单号检索平台订单争议，进入详情查看凭据并作出裁决"
    >
      <template #actions>
        <el-button @click="fetchDisputes" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新队列
        </el-button>
      </template>
    </XyfPageHeader>

    <section class="xyf-surface-card filter-card">
      <div class="filter-bar">
        <div class="xyf-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.label"
            type="button"
            class="xyf-tab"
            :class="{ 'is-active': selectedStatus === tab.value }"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <el-input
          v-model="orderNumberFilter"
          placeholder="按订单号检索..."
          clearable
          class="order-search-input"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">检索</el-button>
          </template>
        </el-input>
      </div>
    </section>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="disputes.length === 0"
        title="当前筛选条件下暂无纠纷记录"
        description="尝试切换状态标签或清空订单号搜索关键词。"
      />

      <el-table v-else :data="disputes" stripe>
        <el-table-column prop="disputeId" label="纠纷单号" width="100">
          <template #default="{ row }">
            <span class="xyf-mono">#{{ row.disputeId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="关联订单号" min-width="170">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              class="xyf-mono"
              @click="router.push(`/admin/orders/${row.orderId}`)"
            >
              {{ row.orderNumber }}
            </el-button>
          </template>
        </el-table-column>

        <el-table-column label="争议类型" width="130">
          <template #default="{ row }">
            <XyfStatusBadge
              tone="accent"
              :label="getDisputeTypeText(row.disputeType)"
              :dot="false"
            />
          </template>
        </el-table-column>

        <el-table-column label="处理状态" width="130">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="statusTone(row.disputeStatus)"
              :label="getDisputeStatusText(row.disputeStatus)"
            />
          </template>
        </el-table-column>

        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">
            <span class="xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" align="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="router.push(`/admin/orders/${row.orderId}`)"
            >
              核查订单
            </el-button>
            <el-button
              size="small"
              type="primary"
              @click="router.push(`/admin/disputes/${row.disputeId}`)"
            >
              {{
                row.disputeStatus === DISPUTE_STATUS.PENDING ||
                row.disputeStatus === DISPUTE_STATUS.PROCESSING
                  ? '立即仲裁'
                  : '查看详情'
              }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchDisputes"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getAdminDisputeList } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatDateTimeMinute } from '@/utils/format'
import {
  DISPUTE_STATUS,
  getDisputeTypeText,
  getDisputeStatusText,
  type DisputeListItem
} from '@/types/dispute'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const statusTabs: Array<{ label: string; value: number | null }> = [
  { label: '全部记录', value: null },
  { label: '待审核', value: DISPUTE_STATUS.PENDING },
  { label: '处理中', value: DISPUTE_STATUS.PROCESSING },
  { label: '已解决', value: DISPUTE_STATUS.RESOLVED },
  { label: '已拒绝', value: DISPUTE_STATUS.REJECTED }
]

const loading = ref(true)
const selectedStatus = ref<number | null>(null)
const orderNumberFilter = ref('')
const disputes = ref<DisputeListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

function statusTone(status: number) {
  if (status === DISPUTE_STATUS.RESOLVED) return 'success'
  if (status === DISPUTE_STATUS.REJECTED) return 'danger'
  if (status === DISPUTE_STATUS.PROCESSING) return 'info'
  return 'warning'
}

async function fetchDisputes() {
  loading.value = true
  try {
    const res = await getAdminDisputeList({
      pageNum: pageNum.value,
      pageSize,
      disputeStatus: selectedStatus.value,
      orderNumber: orderNumberFilter.value.trim() || undefined
    })
    disputes.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取纠纷列表失败')
    disputes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchStatus(status: number | null) {
  selectedStatus.value = status
  pageNum.value = 1
  fetchDisputes()
}

function handleSearch() {
  pageNum.value = 1
  fetchDisputes()
}

onMounted(() => {
  fetchDisputes()
})
</script>

<style scoped lang="scss">
.filter-card {
  padding: 16px 20px;
  margin-bottom: 18px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.order-search-input {
  width: 280px;
  max-width: 100%;
}

.table-card {
  padding: 20px 24px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
