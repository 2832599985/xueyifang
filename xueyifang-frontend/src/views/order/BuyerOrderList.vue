<template>
  <div class="orders-page xyf-page">
    <div class="xyf-container">
      <header class="orders-head">
        <div>
          <h1>购买订单</h1>
          <p>查看待支付、资金托管中、待收货与历史服务订单</p>
        </div>
        <div class="orders-head__actions">
          <button type="button" class="btn-secondary" @click="router.push('/disputes')">
            我的纠纷
          </button>
          <button type="button" class="btn-primary" @click="router.push('/services')">
            继续逛服务
          </button>
        </div>
      </header>

      <div class="toolbar-row">
        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.label"
            type="button"
            class="status-tab"
            :class="{ 'is-active': activeStatus === tab.value }"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <span class="total-hint xyf-tabular">
          共 {{ total }} 笔订单
        </span>
      </div>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="orders.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="暂无相关订单记录"
          description="前往服务大厅挑选心仪的校园技能服务，下单后将由平台全程托管担保。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/services')">浏览服务大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <div v-else class="orders-stack">
        <OrderCard v-for="order in orders" :key="order.id" :order="order" role="BUYER">
          <template #actions>
            <button
              v-if="canPayOrder(order.orderStatus)"
              type="button"
              class="card-btn-primary"
              :disabled="operatingId === order.id"
              @click="handlePay(order)"
            >
              立即支付
            </button>

            <button
              v-if="canCancelOrder(order.orderStatus)"
              type="button"
              class="card-btn-secondary"
              :disabled="operatingId === order.id"
              @click="handleCancel(order)"
            >
              取消订单
            </button>

            <button
              v-if="canBuyerConfirm(order.orderStatus, order.refundStatus)"
              type="button"
              class="card-btn-primary"
              :disabled="operatingId === order.id"
              @click="handleConfirmReceipt(order)"
            >
              确认收货
            </button>

            <button
              v-if="canApplyRefund(order.orderStatus, order.refundStatus)"
              type="button"
              class="card-btn-secondary"
              @click="openRefundDialog(order)"
            >
              申请退款
            </button>

            <button
              v-if="order.orderStatus === OrderStatus.COMPLETED"
              type="button"
              class="card-btn-secondary"
              @click="router.push(`/orders/${order.id}`)"
            >
              评价 / 售后
            </button>
          </template>
        </OrderCard>
      </div>

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchOrders"
        />
      </div>
    </div>

    <!-- Apply Refund Dialog -->
    <el-dialog v-model="refundDialogVisible" title="申请订单退款" width="440px">
      <el-form label-position="top">
        <el-form-item label="退款原因（请简要说明情况以便卖家确认）">
          <el-input
            v-model="refundReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="例如：双方沟通后无法按期交付 / 需求变更协商退款..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">
          提交退款申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listBuyerOrders,
  payOrder,
  cancelOrder,
  buyerConfirm,
  applyRefund
} from '@/api/order'
import { notifyFallbackError } from '@/api/request'
import {
  OrderStatus,
  PaymentMethod,
  canPayOrder,
  canCancelOrder,
  canBuyerConfirm,
  canApplyRefund,
  type OrderListItem
} from '@/types/order'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import OrderCard from '@/components/business/OrderCard.vue'

const router = useRouter()

const statusTabs: Array<{ label: string; value: OrderStatus | undefined }> = [
  { label: '全部订单', value: undefined },
  { label: '待支付', value: OrderStatus.UNPAID },
  { label: '待发货', value: OrderStatus.PENDING_SHIP },
  { label: '待收货', value: OrderStatus.PENDING_RECEIPT },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED },
  { label: '交易失败', value: OrderStatus.FAILED }
]

const loading = ref(false)
const operatingId = ref<number | null>(null)
const activeStatus = ref<OrderStatus | undefined>(undefined)
const orders = ref<OrderListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const refundDialogVisible = ref(false)
const refundTargetOrder = ref<OrderListItem | null>(null)
const refundReason = ref('')
const refundSubmitting = ref(false)

async function fetchOrders() {
  loading.value = true
  try {
    const res = await listBuyerOrders({
      pageNum: pageNum.value,
      pageSize,
      orderStatus: activeStatus.value
    })
    orders.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取买方订单列表失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchStatus(status: OrderStatus | undefined) {
  activeStatus.value = status
  pageNum.value = 1
  fetchOrders()
}

async function handlePay(order: OrderListItem) {
  try {
    await ElMessageBox.confirm(
      `确认从钱包支付 ¥${order.totalAmount.toFixed(2)} 到平台托管吗？`,
      '支付确认',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'info' }
    )
    operatingId.value = order.id
    await payOrder(order.id, { orderId: order.id, paymentMethod: PaymentMethod.WALLET })
    ElMessage.success('支付成功，资金已冻结担保')
    await fetchOrders()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '支付失败')
  } finally {
    operatingId.value = null
  }
}

async function handleCancel(order: OrderListItem) {
  try {
    await ElMessageBox.confirm('确认取消这笔待支付订单吗？', '取消确认', {
      confirmButtonText: '确认取消',
      cancelButtonText: '保留订单',
      type: 'warning'
    })
    operatingId.value = order.id
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await fetchOrders()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '取消订单失败')
  } finally {
    operatingId.value = null
  }
}

async function handleConfirmReceipt(order: OrderListItem) {
  try {
    await ElMessageBox.confirm(
      '确认已收到约定交付物吗？确认后托管资金将立即结算给卖家。',
      '确认收货',
      { confirmButtonText: '确认结算', cancelButtonText: '再检查一下', type: 'success' }
    )
    operatingId.value = order.id
    await buyerConfirm(order.id)
    ElMessage.success('交易已完成，欢迎留下真实评价')
    await fetchOrders()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '确认收货失败')
  } finally {
    operatingId.value = null
  }
}

function openRefundDialog(order: OrderListItem) {
  refundTargetOrder.value = order
  refundReason.value = ''
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!refundTargetOrder.value) return
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }

  refundSubmitting.value = true
  try {
    await applyRefund(refundTargetOrder.value.id, refundReason.value.trim())
    ElMessage.success('已提交退款申请')
    refundDialogVisible.value = false
    await fetchOrders()
  } catch (err) {
    notifyFallbackError(err, '申请退款失败')
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped lang="scss">
.orders-page {
  padding-top: 36px;
  padding-bottom: 72px;
}

.orders-head {
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
    box-shadow: 0 8px 20px -6px rgba(9, 9, 11, 0.28);
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

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--xyf-border);
  margin-bottom: 24px;
}

.status-tabs {
  display: flex;
  align-items: center;
  gap: 24px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.status-tab {
  position: relative;
  padding: 0 2px 14px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--xyf-ink-3);
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--xyf-transition);

  &:hover {
    color: var(--xyf-ink);
  }

  &.is-active {
    color: var(--xyf-ink);
    font-weight: 650;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 2px;
      background: #18181b;
      border-radius: 2px;
    }
  }
}

.total-hint {
  font-size: 13px;
  color: var(--xyf-ink-3);
  padding-bottom: 14px;
}

.loading-card {
  padding: 24px;
}

.orders-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-btn-primary {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid #18181b;
  background: #18181b;
  color: #ffffff;
  font-size: 13px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover:not(:disabled) {
    background: #2e2e34;
    border-color: #2e2e34;
    transform: translateY(-1px);
    box-shadow: 0 6px 14px -4px rgba(9, 9, 11, 0.24);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.card-btn-secondary {
  height: 34px;
  padding: 0 15px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover:not(:disabled) {
    border-color: #a1a1aa;
    background: var(--xyf-surface-soft);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.pagination-wrap {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}
</style>
