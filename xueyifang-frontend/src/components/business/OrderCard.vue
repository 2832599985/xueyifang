<template>
  <article class="order-card">
    <div class="order-card__head">
      <div class="order-card__head-left">
        <div class="order-card__party" v-if="counterpartyName">
          <XyfAvatar :src="counterpartyAvatar" :name="counterpartyName" :size="22" />
          <span class="party-name">{{ counterpartyName }}</span>
        </div>
        <span class="meta-sep">·</span>
        <span class="order-card__time xyf-tabular">{{ formatDateTimeMinute(order.createTime) }}</span>
        <button type="button" class="order-card__no xyf-mono" title="点击复制订单号" @click="copyOrderNo">
          <span>{{ order.orderNumber }}</span>
          <el-icon><DocumentCopy /></el-icon>
        </button>
      </div>

      <div class="order-card__statuses">
        <XyfStatusBadge :tone="orderTone" :label="getOrderStatusText(order.orderStatus)" />
        <XyfStatusBadge
          v-if="order.paymentStatus === PaymentStatus.REFUNDED"
          tone="danger"
          label="已退款"
        />
        <XyfStatusBadge
          v-else-if="order.refundStatus && getRefundStatusText(order.refundStatus)"
          :tone="refundTone"
          :label="getRefundStatusText(order.refundStatus)"
        />
      </div>
    </div>

    <div class="order-card__body">
      <div class="order-card__main">
        <div class="order-card__thumb" @click="router.push(`/services/${order.serviceId}`)">
          <XyfImage
            :src="buildImageUrl(order.serviceImage)"
            :alt="order.serviceTitle"
            :lazy="false"
            width="124px"
            height="88px"
            :radius="12"
            fit="cover"
          />
        </div>

        <div class="order-card__info">
          <h4 class="order-card__title ellipsis" @click="router.push(`/services/${order.serviceId}`)">
            {{ order.serviceTitle }}
          </h4>
          <div class="order-card__sub xyf-tabular">
            <span>{{ formatCleanPrice(order.unitPrice) }} × {{ order.quantity }}</span>
          </div>
          <p v-if="order.remark" class="order-card__remark ellipsis">
            “{{ order.remark }}”
          </p>
        </div>
      </div>

      <div class="order-card__right">
        <div class="order-card__amount xyf-tabular">
          <span class="amount-label">实付</span>
          <strong class="amount-value">{{ formatCleanPrice(order.totalAmount) }}</strong>
        </div>

        <div class="order-card__actions">
          <slot name="actions" />
          <button
            type="button"
            class="detail-link-btn"
            @click="router.push(`/orders/${order.id}`)"
          >
            详情
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy, ArrowRight } from '@element-plus/icons-vue'
import { buildImageUrl } from '@/api/service'
import { formatDateTimeMinute } from '@/utils/format'
import {
  OrderStatus,
  PaymentStatus,
  RefundStatus,
  getOrderStatusText,
  getRefundStatusText,
  type OrderListItem,
  type OrderRole
} from '@/types/order'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'

interface Props {
  order: OrderListItem
  role: OrderRole
}

const props = defineProps<Props>()
const router = useRouter()

const counterpartyName = computed(() =>
  props.role === 'BUYER' ? props.order.sellerName : props.order.buyerName
)

const counterpartyAvatar = computed(() =>
  props.role === 'BUYER' ? props.order.sellerAvatar : props.order.buyerAvatar
)

const orderTone = computed(() => {
  switch (props.order.orderStatus) {
    case OrderStatus.UNPAID:
      return 'warning'
    case OrderStatus.PENDING_SHIP:
      return 'teal'
    case OrderStatus.PENDING_RECEIPT:
      return 'info'
    case OrderStatus.COMPLETED:
      return 'success'
    case OrderStatus.FAILED:
      return 'danger'
    default:
      return 'neutral'
  }
})

const refundTone = computed(() => {
  switch (props.order.refundStatus) {
    case RefundStatus.PENDING:
      return 'warning'
    case RefundStatus.APPROVED:
      return 'success'
    case RefundStatus.REJECTED:
    case RefundStatus.COMPLETED:
      return 'danger'
    default:
      return 'neutral'
  }
})

function formatCleanPrice(val: number | string) {
  const num = Number(val)
  if (Number.isNaN(num)) return '¥0'
  return Number.isInteger(num) ? `¥${num}` : `¥${num.toFixed(2)}`
}

async function copyOrderNo() {
  try {
    await navigator.clipboard.writeText(props.order.orderNumber)
    ElMessage.success('订单号已复制')
  } catch {
    ElMessage.info(`订单号：${props.order.orderNumber}`)
  }
}
</script>

<style scoped lang="scss">
.order-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 16px;
  padding: 18px 24px 20px;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:hover {
    border-color: #d4d4d8;
    box-shadow: 0 10px 28px -14px rgba(9, 9, 11, 0.1);
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

  &__head-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  &__party {
    display: inline-flex;
    align-items: center;
    gap: 7px;

    .party-name {
      font-size: 13.5px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  .meta-sep {
    color: #d4d4d8;
  }

  &__no {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--xyf-ink-3);
    background: transparent;
    border: none;
    padding: 2px 6px;
    border-radius: 6px;
    cursor: pointer;
    transition: all var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
      background: var(--xyf-surface-soft);
    }
  }

  &__statuses {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    padding-top: 18px;

    @media (max-width: 780px) {
      flex-direction: column;
      align-items: stretch;
    }
  }

  &__main {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;
    min-width: 0;
  }

  &__thumb {
    cursor: pointer;
    flex-shrink: 0;
    border-radius: 12px;
    overflow: hidden;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.9;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  &__title {
    margin: 0;
    font-size: 16px;
    font-weight: 650;
    color: var(--xyf-ink);
    letter-spacing: -0.01em;
    cursor: pointer;
    transition: opacity 0.18s ease;

    &:hover {
      opacity: 0.72;
    }
  }

  &__sub {
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__remark {
    margin: 2px 0 0;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
    max-width: 540px;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 14px;
    flex-shrink: 0;

    @media (max-width: 780px) {
      flex-direction: row;
      align-items: center;
      justify-content: space-between;
      padding-top: 12px;
      border-top: 1px solid var(--xyf-border-subtle);
    }
  }

  &__amount {
    display: inline-flex;
    align-items: baseline;
    gap: 6px;

    .amount-label {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }

    .amount-value {
      font-size: 20px;
      font-weight: 750;
      color: var(--xyf-ink);
      letter-spacing: -0.02em;
    }
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .detail-link-btn {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    height: 34px;
    padding: 0 10px;
    border: none;
    background: transparent;
    color: var(--xyf-ink-2);
    font-size: 13px;
    font-weight: 500;
    font-family: inherit;
    border-radius: 8px;
    cursor: pointer;
    transition: all var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
      background: var(--xyf-surface-soft);
    }
  }
}
</style>
