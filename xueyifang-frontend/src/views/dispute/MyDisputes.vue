<template>
  <div class="disputes-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="我的纠纷">
        <template #meta>
          <span class="head-count xyf-tabular">共 {{ total }} 条</span>
        </template>
        <template #actions>
          <el-button @click="router.push('/orders')">购买订单</el-button>
          <el-button type="primary" @click="router.push('/orders/sales')">销售订单</el-button>
        </template>
      </XyfPageHeader>

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
          class="xyf-surface-card dispute-card"
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
                订单 {{ item.orderNumber }}
              </button>
              <span class="meta-sep">·</span>
              <span class="dispute-time xyf-tabular">{{ formatDateTimeMinute(item.createTime) }}</span>
            </div>

            <XyfStatusBadge
              :tone="disputeTone(item.disputeStatus)"
              :label="getDisputeStatusText(item.disputeStatus)"
              :dot="item.disputeStatus === DISPUTE_STATUS.PENDING || item.disputeStatus === DISPUTE_STATUS.PROCESSING"
            />
          </div>

          <div class="dispute-card__body">
            <div class="dispute-card__info">
              <div class="dispute-reason">
                <span>{{ item.serviceTitle ? `${item.serviceTitle} · ` : '' }}{{ getDisputeTypeText(item.disputeType) }}</span>
                <span v-if="item.totalAmount !== undefined" class="dispute-amt xyf-tabular">
                  争议金额 {{ formatPrice(item.totalAmount) }}
                </span>
              </div>
              <div class="dispute-sub">{{ getStatusSummary(item.disputeStatus) }}</div>

              <!-- 3-Step Dateless Arbitration Progress Rail -->
              <div class="progress-rail" aria-label="仲裁阶段">
                <div
                  v-for="(stage, idx) in getRailStages(item.disputeStatus)"
                  :key="stage.label"
                  class="rail-step"
                  :class="{
                    'is-done': stage.state === 'done',
                    'is-current': stage.state === 'current',
                    'is-danger': stage.state === 'danger'
                  }"
                >
                  <span class="rail-dot" />
                  <span>{{ stage.label }}</span>
                  <span v-if="idx < 2" class="rail-line" />
                </div>
              </div>
            </div>

            <div class="dispute-card__actions">
              <el-button size="small" @click="router.push(`/orders/${item.orderId}`)">
                原订单
              </el-button>
              <el-button
                size="small"
                type="primary"
                @click="router.push(`/disputes/${item.disputeId}`)"
              >
                查看详情
                <el-icon style="margin-left: 4px"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > pageSize" class="xyf-pagination">
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
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const loading = ref(true)
const disputes = ref<DisputeListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

function disputeTone(status: number): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  if (status === DISPUTE_STATUS.PENDING) return 'warning'
  if (status === DISPUTE_STATUS.PROCESSING) return 'info'
  if (status === DISPUTE_STATUS.RESOLVED) return 'success'
  if (status === DISPUTE_STATUS.REJECTED) return 'danger'
  return 'neutral'
}

function getRailStages(status: number): Array<{ label: string; state: 'done' | 'current' | 'danger' | 'idle' }> {
  if (status === DISPUTE_STATUS.RESOLVED) {
    return [
      { label: '已提交', state: 'done' },
      { label: '平台核查', state: 'done' },
      { label: '已裁决', state: 'done' }
    ]
  }
  if (status === DISPUTE_STATUS.REJECTED) {
    return [
      { label: '已提交', state: 'done' },
      { label: '平台核查', state: 'done' },
      { label: '未支持', state: 'danger' }
    ]
  }
  if (status === DISPUTE_STATUS.PROCESSING) {
    return [
      { label: '已提交', state: 'done' },
      { label: '平台核查', state: 'current' },
      { label: '等待裁决', state: 'idle' }
    ]
  }
  return [
    { label: '已提交', state: 'current' },
    { label: '平台核查', state: 'idle' },
    { label: '等待裁决', state: 'idle' }
  ]
}

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
.head-count {
  font-size: 13px;
  color: var(--xyf-ink-3);
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
  padding: 18px 22px 20px;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    box-shadow: var(--xyf-shadow-sm);
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
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .dispute-id {
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .meta-sep {
    color: var(--xyf-border-strong);
  }

  .order-ref {
    border: none;
    background: transparent;
    padding: 2px 6px;
    border-radius: var(--xyf-radius-xs);
    font-size: 12px;
    color: var(--xyf-ink-2);
    cursor: pointer;

    &:hover {
      color: var(--xyf-ink);
      background: var(--xyf-surface-soft);
    }
  }

  &__body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 20px;
    padding-top: 16px;
    flex-wrap: wrap;
  }

  &__info {
    flex: 1;
    min-width: 240px;
  }

  .dispute-reason {
    display: flex;
    align-items: baseline;
    gap: 12px;
    flex-wrap: wrap;
    font-size: 16px;
    font-weight: 600;
    color: var(--xyf-ink);
    letter-spacing: -0.01em;
  }

  .dispute-amt {
    font-size: 13px;
    font-weight: 600;
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

.progress-rail {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rail-step {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--xyf-ink-4);

  .rail-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--xyf-border-strong);
  }

  .rail-line {
    width: 22px;
    height: 1px;
    background: var(--xyf-border);
    margin-left: 2px;
  }

  &.is-done {
    color: var(--xyf-ink-2);

    .rail-dot {
      background: var(--xyf-ink);
    }

    .rail-line {
      background: var(--xyf-ink);
    }
  }

  &.is-current {
    color: var(--xyf-ink);
    font-weight: 600;

    .rail-dot {
      background: var(--xyf-champagne);
    }
  }

  &.is-danger {
    color: #b91c1c;
    font-weight: 600;

    .rail-dot {
      background: var(--xyf-danger);
    }
  }
}
</style>
