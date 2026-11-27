<template>
  <div class="admin-order-detail">
    <XyfPageHeader
      :title="orderDetail ? `平台订单核查 · ${orderDetail.orderNumber}` : '订单核查详情'"
      :show-back="true"
      back-text="返回纠纷列表"
      back-to="/admin/disputes"
    >
      <template #actions v-if="linkedDispute">
        <button
          type="button"
          class="audit-dispute-btn"
          @click="router.push(`/admin/disputes/${linkedDispute.disputeId}`)"
        >
          处理关联纠纷 #{{ linkedDispute.disputeId }}
          <el-icon><ArrowRight /></el-icon>
        </button>
      </template>
    </XyfPageHeader>

    <div v-if="loading" class="xyf-surface-card loading-card">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="!orderDetail" class="xyf-surface-card loading-card">
      <XyfEmptyState title="未找到该订单信息" />
    </div>

    <template v-else>
      <section class="xyf-surface-card summary-strip">
        <div class="summary-strip__left">
          <div class="badges-row">
            <XyfStatusBadge
              size="md"
              :tone="orderDetail.orderStatus === OrderStatus.COMPLETED ? 'success' : orderDetail.orderStatus === OrderStatus.FAILED ? 'danger' : 'info'"
              :label="getOrderStatusText(orderDetail.orderStatus)"
              :dot="orderDetail.orderStatus !== OrderStatus.COMPLETED && orderDetail.orderStatus !== OrderStatus.FAILED && orderDetail.orderStatus !== OrderStatus.CANCELLED"
            />
            <XyfStatusBadge
              size="md"
              :tone="orderDetail.paymentStatus === PaymentStatus.PAID ? 'success' : orderDetail.paymentStatus === PaymentStatus.REFUNDED ? 'danger' : 'warning'"
              :label="getPaymentStatusText(orderDetail.paymentStatus)"
            />
          </div>
          <h2>{{ orderDetail.serviceTitle }}</h2>
          <p class="order-meta xyf-tabular">
            <span>订单号 <span class="xyf-mono">{{ orderDetail.orderNumber }}</span></span>
            <span>·</span>
            <span>单价 {{ formatPrice(orderDetail.unitPrice) }} × {{ orderDetail.quantity }}</span>
            <span>·</span>
            <span>{{ getTradeTypeText(orderDetail.tradeType) }}</span>
          </p>
        </div>

        <div class="summary-strip__right">
          <span class="amt-label">订单总额</span>
          <strong class="xyf-tabular">{{ formatPrice(orderDetail.totalAmount) }}</strong>
          <span v-if="orderDetail.frozenAmount && orderDetail.frozenAmount > 0" class="frozen-hint">
            当前担保冻结 {{ formatPrice(orderDetail.frozenAmount) }}
          </span>
        </div>
      </section>

      <div class="detail-grid">
        <div class="detail-main">
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>订单担保时间轴</h2>
            </div>
            <OrderTimeline :order="orderDetail" />
          </section>

          <section
            v-if="orderDetail.deliveryNote || orderDetail.deliveryImages?.length"
            class="xyf-surface-card panel-card"
          >
            <div class="xyf-section-heading">
              <h2>卖方履约交付凭证</h2>
            </div>
            <div v-if="orderDetail.deliveryNote" class="remark-box" style="margin-top: 0">
              <strong>交付说明：</strong>{{ orderDetail.deliveryNote }}
            </div>
            <div v-if="orderDetail.deliveryImages?.length" class="voucher-grid">
              <el-image
                v-for="(img, idx) in orderDetail.deliveryImages"
                :key="img + idx"
                :src="img"
                :preview-src-list="orderDetail.deliveryImages"
                :initial-index="idx"
                fit="cover"
                class="voucher-img"
              />
            </div>
          </section>

          <section v-if="orderDetail.tradeType === TradeType.OFFLINE || orderDetail.remark" class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>线下交接地点与需求备注</h2>
            </div>

            <TradeLocationBadge
              v-if="orderDetail.tradeType === TradeType.OFFLINE"
              :location-id="orderDetail.tradeLocationId"
              :location-name="orderDetail.tradeLocationName"
              :location="orderDetail.tradeLocation"
            />

            <div v-if="orderDetail.remark" class="remark-box">
              <strong>买家下单备注：</strong>{{ orderDetail.remark }}
            </div>
          </section>
        </div>

        <aside class="detail-aside">
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>交易双方档案</h2>
            </div>

            <div class="party-row">
              <XyfAvatar :src="orderDetail.buyerAvatar" :name="orderDetail.buyerName" :size="38" />
              <div>
                <div class="party-name">{{ orderDetail.buyerName }}</div>
                <div class="party-role">买方 #{{ orderDetail.buyerId }}</div>
              </div>
            </div>

            <div class="party-divider" />

            <div class="party-row">
              <XyfAvatar :src="orderDetail.sellerAvatar" :name="orderDetail.sellerName" :size="38" />
              <div>
                <div class="party-name">{{ orderDetail.sellerName }}</div>
                <div class="party-role">卖方 #{{ orderDetail.sellerId }}</div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api/order'
import { getDisputeByOrderId } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import {
  OrderStatus,
  PaymentStatus,
  TradeType,
  getOrderStatusText,
  getPaymentStatusText,
  getTradeTypeText,
  type OrderDetail
} from '@/types/order'
import type { DisputeDetail } from '@/types/dispute'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import TradeLocationBadge from '@/components/business/TradeLocationBadge.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'

const router = useRouter()
const route = useRoute()

const orderId = computed(() => Number(route.params.id))

const loading = ref(true)
const orderDetail = ref<OrderDetail | null>(null)
const linkedDispute = ref<DisputeDetail | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    const [orderRes, disputeRes] = await Promise.all([
      getOrderDetail(orderId.value),
      getDisputeByOrderId(orderId.value).catch(() => null)
    ])
    orderDetail.value = orderRes
    linkedDispute.value = disputeRes
  } catch (err) {
    notifyFallbackError(err, '获取订单核查详情失败')
    orderDetail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.summary-strip {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;

  h2 {
    margin: 0 0 6px;
    font-size: 19px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.badges-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.order-meta {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-strip__right {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;

  .amt-label {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  strong {
    font-size: 26px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .frozen-hint {
    font-size: 12px;
    color: var(--xyf-ink-2);
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    padding: 2px 10px;
    border-radius: 6px;
  }
}

.audit-dispute-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 38px;
  padding: 0 18px;
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

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.75fr);
  gap: 20px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card {
  padding: 22px 24px;
}

.remark-box {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-bg);
  font-size: 13.5px;
  color: var(--xyf-ink-secondary);
}

.voucher-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}

.voucher-img {
  width: 100%;
  height: 88px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  overflow: hidden;
  cursor: zoom-in;
}

.party-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .party-name {
    font-size: 14.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .party-role {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.party-divider {
  height: 1px;
  background: var(--xyf-border-light);
  margin: 14px 0;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
