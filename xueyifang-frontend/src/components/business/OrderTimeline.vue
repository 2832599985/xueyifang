<template>
  <div class="order-timeline-wrap">
    <!-- Monochrome Architectural Segmented Progress Track -->
    <div v-if="!isAborted" class="escrow-progress">
      <div class="escrow-progress__track">
        <div
          class="escrow-progress__fill"
          :style="{ width: `${progressPercent}%` }"
        />
      </div>

      <div class="escrow-stages">
        <div
          v-for="(step, idx) in escrowSteps"
          :key="step.label"
          class="escrow-stage"
          :class="{
            'is-done': idx < currentStepIndex,
            'is-current': idx === currentStepIndex
          }"
        >
          <span class="escrow-stage__marker" aria-hidden="true">
            <svg v-if="idx < currentStepIndex" viewBox="0 0 14 14" fill="none">
              <path
                d="M3 7.2L5.7 9.9L11 4.5"
                stroke="#FFFFFF"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span v-else class="marker-core" />
          </span>
          <span class="escrow-stage__label">{{ step.label }}</span>
        </div>
      </div>
    </div>

    <!-- Structured Monochrome Audit Ledger (No Colored Dots) -->
    <ol class="event-ledger">
      <li
        v-for="(item, index) in events"
        :key="index"
        class="event-row"
        :class="{ 'is-latest': index === events.length - 1 }"
      >
        <span class="event-row__time xyf-mono">
          {{ item.time ? formatDateTime(item.time) : '已记录' }}
        </span>

        <div class="event-row__axis" aria-hidden="true">
          <span class="axis-tick" />
        </div>

        <div class="event-row__body">
          <div class="event-row__head">
            <span class="event-row__title">{{ item.label }}</span>
            <span v-if="index === events.length - 1" class="latest-tag">当前状态</span>
          </div>
          <p v-if="item.note" class="event-row__note">{{ item.note }}</p>
        </div>
      </li>
    </ol>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDateTime } from '@/utils/format'
import { OrderStatus, PaymentStatus, RefundStatus, type OrderDetail } from '@/types/order'

interface Props {
  order: OrderDetail
}

const props = defineProps<Props>()

const escrowSteps = [
  { label: '创建订单' },
  { label: '资金托管' },
  { label: '卖家发货' },
  { label: '确认结算' }
]

const isAborted = computed(
  () =>
    props.order.orderStatus === OrderStatus.CANCELLED ||
    props.order.orderStatus === OrderStatus.FAILED
)

const currentStepIndex = computed(() => {
  switch (props.order.orderStatus) {
    case OrderStatus.UNPAID:
      return 0
    case OrderStatus.PENDING_SHIP:
      return 1
    case OrderStatus.PENDING_RECEIPT:
      return 2
    case OrderStatus.COMPLETED:
      return 4
    default:
      return 0
  }
})

const progressPercent = computed(() => {
  if (currentStepIndex.value >= escrowSteps.length) return 100
  return Math.round((currentStepIndex.value / (escrowSteps.length - 1)) * 100)
})

interface TimelineEvent {
  label: string
  time?: string
  note?: string
}

const events = computed<TimelineEvent[]>(() => {
  const o = props.order
  const items: TimelineEvent[] = [
    {
      label: '订单已创建',
      time: o.createTime
    }
  ]

  if (o.paymentTime) {
    items.push({
      label: '买家完成支付，资金进入平台担保冻结',
      time: o.paymentTime
    })
  }

  if (o.sellerShipTime) {
    items.push({
      label: '卖家已发货 / 开始履约',
      time: o.sellerShipTime,
      note: o.deliveryNote ? `交付说明：${o.deliveryNote}` : undefined
    })
  }

  if (o.refundRequestTime && o.refundStatus) {
    items.push({
      label: '买家发起退款申请',
      time: o.refundRequestTime,
      note: o.refundReason ? `原因：${o.refundReason}` : undefined
    })
  }

  if (o.refundStatus === RefundStatus.APPROVED) {
    items.push({ label: '卖家已同意退款' })
  } else if (o.refundStatus === RefundStatus.REJECTED) {
    items.push({ label: '卖家已拒绝退款申请' })
  } else if (o.refundStatus === RefundStatus.COMPLETED || o.paymentStatus === PaymentStatus.REFUNDED) {
    items.push({ label: '款项已退回买家钱包' })
  }

  if (o.buyerConfirmTime) {
    items.push({
      label: '买家确认收货，担保资金结算至卖家钱包',
      time: o.buyerConfirmTime
    })
  }

  if (o.orderStatus === OrderStatus.COMPLETED && !o.buyerConfirmTime) {
    items.push({ label: '交易已完成' })
  }

  if (o.orderStatus === OrderStatus.CANCELLED) {
    items.push({ label: '订单已取消' })
  }

  if (o.orderStatus === OrderStatus.FAILED && o.paymentStatus !== PaymentStatus.REFUNDED) {
    items.push({ label: '交易已关闭 / 失败' })
  }

  return items
})
</script>

<style scoped lang="scss">
.order-timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ==================== Top Segmented Stage Track ==================== */
.escrow-progress {
  position: relative;
  padding: 18px 20px 16px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &__track {
    position: relative;
    height: 2px;
    background: #e4e4e7;
    border-radius: 2px;
    margin: 10px 12.5% 0;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: #18181b;
    border-radius: 2px;
    transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  }
}

.escrow-stages {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: -12px;
}

.escrow-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;

  &__marker {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #ffffff;
    border: 1.5px solid #d4d4d8;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all var(--xyf-transition);

    svg {
      width: 12px;
      height: 12px;
    }

    .marker-core {
      width: 6px;
      height: 6px;
      border-radius: 2px;
      background: #d4d4d8;
    }
  }

  &__label {
    font-size: 12.5px;
    font-weight: 500;
    color: var(--xyf-ink-3);
  }

  &.is-done {
    .escrow-stage__marker {
      background: #18181b;
      border-color: #18181b;
    }

    .escrow-stage__label {
      color: var(--xyf-ink);
      font-weight: 600;
    }
  }

  &.is-current {
    .escrow-stage__marker {
      background: #ffffff;
      border: 2px solid #18181b;
      box-shadow: 0 0 0 3px rgba(24, 24, 27, 0.08);

      .marker-core {
        width: 7px;
        height: 7px;
        border-radius: 2px;
        background: #18181b;
      }
    }

    .escrow-stage__label {
      color: #18181b;
      font-weight: 600;
    }
  }
}

/* ==================== Monochrome Event Ledger ==================== */
.event-ledger {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.event-row {
  display: grid;
  grid-template-columns: 150px 24px minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
  min-height: 44px;

  @media (max-width: 640px) {
    grid-template-columns: 20px minmax(0, 1fr);

    &__time {
      display: none;
    }
  }

  &__time {
    padding-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-3);
    text-align: right;
    white-space: nowrap;
  }

  &__axis {
    position: relative;
    display: flex;
    justify-content: center;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      bottom: 0;
      left: 50%;
      width: 1px;
      transform: translateX(-50%);
      background: #e4e4e7;
    }

    .axis-tick {
      position: relative;
      z-index: 1;
      margin-top: 7px;
      width: 7px;
      height: 7px;
      border-radius: 2px;
      background: #ffffff;
      border: 1.5px solid #a1a1aa;
    }
  }

  &:first-child .event-row__axis::before {
    top: 10px;
  }

  &:last-child .event-row__axis::before {
    bottom: calc(100% - 10px);
  }

  &__body {
    padding-bottom: 18px;
    min-width: 0;
  }

  &:last-child .event-row__body {
    padding-bottom: 2px;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  &__title {
    font-size: 14px;
    font-weight: 500;
    color: var(--xyf-ink-2);
    line-height: 1.45;
  }

  &__note {
    margin: 7px 0 0;
    padding: 9px 12px;
    border-radius: 8px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    font-size: 13px;
    line-height: 1.6;
    color: var(--xyf-ink-2);
  }

  &.is-latest {
    .axis-tick {
      width: 8px;
      height: 8px;
      background: #18181b;
      border-color: #18181b;
    }

    .event-row__time {
      color: var(--xyf-ink);
      font-weight: 600;
    }

    .event-row__title {
      color: var(--xyf-ink);
      font-weight: 600;
    }
  }
}

.latest-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 5px;
  background: #18181b;
  color: #ffffff;
  letter-spacing: 0.01em;
}
</style>
