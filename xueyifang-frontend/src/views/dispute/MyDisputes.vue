<template>
  <div class="disputes-page xyf-page">
    <div class="xyf-container">
      <header class="disputes-head">
        <div>
          <h1>我的纠纷</h1>
          <p>查看已提交仲裁的订单进展与平台裁决结果</p>
        </div>
        <div class="disputes-head__actions">
          <button type="button" class="btn-secondary" @click="router.push('/orders')">
            购买订单
          </button>
          <button type="button" class="btn-primary" @click="router.push('/orders/sales')">
            销售订单
          </button>
        </div>
      </header>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="disputes.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="暂无订单纠纷记录"
          description="如在已付款订单交易中遇到争议，可在对应「订单详情页」发起平台纠纷。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/orders')">查看我的订单</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <div v-else class="disputes-stack">
        <article
          v-for="item in disputes"
          :key="item.disputeId"
          class="dispute-card"
        >
          <div class="dispute-card__head">
            <div class="dispute-card__meta">
              <span class="dispute-id xyf-mono">纠纷 #{{ item.disputeId }}</span>
              <span class="meta-sep">·</span>
              <button
                type="button"
                class="order-ref xyf-mono"
                @click="router.push(`/orders/${item.orderId}`)"
              >
                关联订单 {{ item.orderNumber }}
              </button>
              <span class="meta-sep">·</span>
              <span class="dispute-time xyf-tabular">{{ formatDateTimeMinute(item.createTime) }}</span>
            </div>

            <span class="dispute-status" :class="`state-${item.disputeStatus}`">
              {{ getDisputeStatusText(item.disputeStatus) }}
            </span>
          </div>

          <div class="dispute-card__body">
            <div class="dispute-card__info">
              <div class="dispute-reason">
                {{ item.serviceTitle ? `${item.serviceTitle} · ` : '' }}{{ getDisputeTypeText(item.disputeType) }}
                <span v-if="item.totalAmount !== undefined" class="dispute-amt xyf-tabular">
                  {{ formatPrice(item.totalAmount) }}
                </span>
              </div>
              <div class="dispute-sub">{{ getStatusSummary(item.disputeStatus) }}</div>
            </div>

            <div class="dispute-card__actions">
              <button
                type="button"
                class="card-btn-secondary"
                @click="router.push(`/orders/${item.orderId}`)"
              >
                原订单
              </button>
              <button
                type="button"
                class="card-btn-primary"
                @click="router.push(`/disputes/${item.disputeId}`)"
              >
                查看详情
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
          </div>
        </article>
      </div>

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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { getMyDisputes } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatDateTimeMinute, formatPrice } from '@/utils/format'
import {
  DISPUTE_STATUS,
  getDisputeTypeText,
  getDisputeStatusText,
  type DisputeListItem
} from '@/types/dispute'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const loading = ref(true)
const disputes = ref<DisputeListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

function getStatusSummary(status: number) {
  switch (status) {
    case DISPUTE_STATUS.PENDING:
      return '等待平台工作人员受理与核实订单记录'
    case DISPUTE_STATUS.PROCESSING:
      return '平台仲裁中，正在复核双方沟通与交付凭证'
    case DISPUTE_STATUS.RESOLVED:
      return '已完成仲裁裁决，详情内可查看最终处理结论'
    case DISPUTE_STATUS.REJECTED:
      return '经核实未支持该诉求，详情内可查看驳回说明'
    default:
      return '可在详情页查看最新受理进度'
  }
}

async function fetchDisputes() {
  loading.value = true
  try {
    const res = await getMyDisputes(pageNum.value, pageSize)
    disputes.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取纠纷记录失败')
    disputes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDisputes()
})
</script>

<style scoped lang="scss">
.disputes-page {
  padding-top: 36px;
  padding-bottom: 72px;
}

.disputes-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 28px;

  h1 {
    margin: 0;
    font-size: 30px;
    font-weight: 750;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
    line-height: 1.2;
  }

  p {
    margin: 8px 0 0;
    font-size: 14px;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.btn-primary {
  height: 40px;
  padding: 0 20px;
  border-radius: 999px;
  border: 1px solid #18181b;
  background: #18181b;
  color: #ffffff;
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: #27272a;
    border-color: #27272a;
    transform: translateY(-1px);
  }
}

.btn-secondary {
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 13.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #a1a1aa;
    background: var(--xyf-surface-soft);
  }
}

.loading-card {
  padding: 24px;
}

.disputes-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dispute-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 16px;
  padding: 18px 24px 20px;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:hover {
    border-color: #d4d4d8;
    box-shadow: 0 10px 28px -14px rgba(9, 9, 11, 0.09);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--xyf-border-subtle);
    flex-wrap: wrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  .dispute-id {
    font-weight: 650;
    color: var(--xyf-ink);
  }

  .meta-sep {
    color: #d4d4d8;
  }

  .order-ref {
    border: none;
    background: transparent;
    padding: 2px 6px;
    border-radius: 6px;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
    cursor: pointer;
    transition: all var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
      background: var(--xyf-surface-soft);
    }
  }

  .dispute-status {
    font-size: 12.5px;
    font-weight: 550;
    padding: 3px 9px;
    border-radius: 6px;
    background: #f4f4f5;
    border: 1px solid #e4e4e7;
    color: var(--xyf-ink);
    letter-spacing: -0.005em;

    &.state-3,
    &.state-4 {
      background: #fafafa;
      color: var(--xyf-ink-3);
    }
  }

  &__body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding-top: 16px;
    flex-wrap: wrap;
  }

  &__info {
    min-width: 0;
  }

  .dispute-reason {
    display: flex;
    align-items: baseline;
    gap: 10px;
    flex-wrap: wrap;
    font-size: 16.5px;
    font-weight: 650;
    color: var(--xyf-ink);
    letter-spacing: -0.015em;
  }

  .dispute-amt {
    font-size: 14px;
    font-weight: 700;
    color: var(--xyf-ink-2);
  }

  .dispute-sub {
    margin-top: 5px;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
}

.card-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid #18181b;
  background: #18181b;
  color: #ffffff;
  font-size: 13px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: #27272a;
    border-color: #27272a;
    transform: translateY(-1px);
  }
}

.card-btn-secondary {
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #a1a1aa;
    background: var(--xyf-surface-soft);
  }
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
