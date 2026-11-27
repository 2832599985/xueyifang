<template>
  <div class="orders-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="销售订单">
        <template #meta>
          <span class="head-count xyf-tabular">共 {{ total }} 笔</span>
        </template>
        <template #actions>
          <el-button type="primary" @click="openGlobalScanner">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px" aria-hidden="true">
              <path d="M3 7V5a2 2 0 0 1 2-2h2" />
              <path d="M17 3h2a2 2 0 0 1 2 2v2" />
              <path d="M21 17v2a2 2 0 0 1-2 2h-2" />
              <path d="M7 21H5a2 2 0 0 1-2-2v-2" />
              <line x1="7" y1="12" x2="17" y2="12" />
            </svg>
            扫码核销
          </el-button>
        </template>
      </XyfPageHeader>

      <SellerSuiteTabs />

      <div class="toolbar-row">
        <div class="xyf-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.label"
            type="button"
            class="xyf-tabs__btn"
            :class="{ 'is-active': activeStatus === tab.value }"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="orders.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="暂无匹配的销售订单"
          description="优化服务封面与描述能吸引更多校内同学下单预约。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/my-services')">管理我的服务</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <div v-else class="orders-stack">
        <OrderCard v-for="order in orders" :key="order.id" :order="order" role="SELLER">
          <template #actions>
            <el-button
              v-if="canOrderOfflineVerify(order)"
              type="primary"
              size="small"
              @click="openVerifyModal(order)"
            >
              扫码 / 核销
            </el-button>

            <el-button
              v-if="canSellerShip(order.orderStatus)"
              :type="canOrderOfflineVerify(order) ? 'default' : 'primary'"
              size="small"
              :loading="operatingId === order.id"
              @click="openShipModal(order)"
            >
              {{ order.tradeType === TradeType.OFFLINE ? '标记交接中' : '提交交付发货' }}
            </el-button>

            <el-button
              v-if="canSellerHandleRefund(order.orderStatus, order.refundStatus)"
              type="primary"
              size="small"
              @click="openRefundDecision(order)"
            >
              处理退款
            </el-button>

            <el-button
              v-if="order.buyerId"
              size="small"
              @click="contactBuyer(order)"
            >
              联系买家
            </el-button>
          </template>
        </OrderCard>
      </div>

      <div v-if="total > pageSize" class="xyf-pagination">
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

    <!-- Seller Delivery / Ship Dialog -->
    <el-dialog v-model="shipDialogVisible" title="提交履约交付并发货" width="470px">
      <div v-if="shipTargetOrder" class="refund-review-body">
        <div class="refund-summary">
          <div>服务：<strong>{{ shipTargetOrder.serviceTitle }}</strong></div>
          <div class="xyf-mono">{{ shipTargetOrder.orderNumber }}</div>
        </div>

        <el-form label-position="top">
          <el-form-item label="履约交付说明（选填，将作为交付凭证展示在订单详情）">
            <el-input
              v-model="shipNote"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="例如：已完成定制交付物发送 / 已到达校内地点交接..."
            />
          </el-form-item>

          <el-form-item label="交付成果或现场凭证图（选填，最多 4 张）">
            <div class="upload-strip">
              <div v-for="(img, idx) in shipImages" :key="img + idx" class="upload-thumb">
                <img :src="img" alt="凭证" />
                <button type="button" class="thumb-remove" @click="shipImages.splice(idx, 1)">×</button>
              </div>
              <label v-if="shipImages.length < 4" class="upload-trigger">
                <input type="file" accept="image/*" hidden :disabled="uploadingImg" @change="handleShipImgUpload" />
                <span>{{ uploadingImg ? '上传中...' : '+ 添加图片' }}</span>
              </label>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting || uploadingImg" @click="submitShip">
          确认发货
        </el-button>
      </template>
    </el-dialog>

    <!-- Offline Meetup Live Camera QR Scanner & 30s Code Dialog -->
    <OfflineVerifyDialog
      v-model:visible="verifyDialogVisible"
      :order-id="verifyTargetOrder?.id ?? null"
      :order-title="verifyTargetOrder?.serviceTitle ?? ''"
      :initial-mode="verifyInitialMode"
      :pending-offline-orders="pendingOfflineOrders"
      @verified="handleOfflineVerified"
    />

    <!-- Seller Refund Decision Dialog -->
    <el-dialog v-model="refundDialogVisible" title="处理买家退款申请" width="460px">
      <div v-if="refundTargetOrder" class="refund-review-body">
        <div class="refund-summary">
          <div>订单号：<span class="xyf-mono">{{ refundTargetOrder.orderNumber }}</span></div>
          <div>订单金额：<strong>{{ formatPrice(refundTargetOrder.totalAmount) }}</strong></div>
        </div>
        <p v-if="refundTargetOrder.refundReason" class="refund-reason-box">
          买家说明：{{ refundTargetOrder.refundReason }}
        </p>

        <el-form label-position="top">
          <el-form-item label="若拒绝退款，请填写拒绝理由（同意退款则留空）：">
            <el-input
              v-model="rejectReason"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="例如：已完成定制交付物发送，如需调整可继续沟通修改..."
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="refundDialogVisible = false">关闭</el-button>
        <el-button
          type="danger"
          plain
          :loading="refundSubmitting"
          @click="submitRefundDecision(false)"
        >
          拒绝退款
        </el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefundDecision(true)">
          同意并全额退款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSellerOrders, sellerShip, sellerHandleRefund } from '@/api/order'
import { uploadFile } from '@/api/file'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import {
  OrderStatus,
  RefundStatus,
  TradeType,
  canSellerShip,
  canSellerHandleRefund,
  type OrderListItem
} from '@/types/order'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import OrderCard from '@/components/business/OrderCard.vue'
import OfflineVerifyDialog from '@/components/business/OfflineVerifyDialog.vue'
import SellerSuiteTabs from '@/components/service/SellerSuiteTabs.vue'

const router = useRouter()

const statusTabs: Array<{ label: string; value: OrderStatus | undefined }> = [
  { label: '全部销售单', value: undefined },
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
const orders = ref<Array<OrderListItem & { refundReason?: string }>>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const shipDialogVisible = ref(false)
const shipTargetOrder = ref<OrderListItem | null>(null)
const shipNote = ref('')
const shipImages = ref<string[]>([])
const uploadingImg = ref(false)
const shipSubmitting = ref(false)

const verifyDialogVisible = ref(false)
const verifyTargetOrder = ref<OrderListItem | null>(null)
const verifyInitialMode = ref<'qr' | 'code'>('qr')

const pendingOfflineOrders = computed(() => orders.value.filter(canOrderOfflineVerify))

const refundDialogVisible = ref(false)
const refundTargetOrder = ref<(OrderListItem & { refundReason?: string }) | null>(null)
const rejectReason = ref('')
const refundSubmitting = ref(false)

function canOrderOfflineVerify(order: OrderListItem) {
  return (
    order.tradeType === TradeType.OFFLINE &&
    order.refundStatus !== RefundStatus.PENDING &&
    (order.orderStatus === OrderStatus.PENDING_SHIP || order.orderStatus === OrderStatus.PENDING_RECEIPT)
  )
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await listSellerOrders({
      pageNum: pageNum.value,
      pageSize,
      orderStatus: activeStatus.value
    })
    orders.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取销售订单失败')
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

function openShipModal(order: OrderListItem) {
  shipTargetOrder.value = order
  shipNote.value = ''
  shipImages.value = []
  shipDialogVisible.value = true
}

async function handleShipImgUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploadingImg.value = true
  try {
    const url = await uploadFile(file, 'delivery_image')
    shipImages.value.push(url)
  } catch (err) {
    notifyFallbackError(err, '上传凭证图片失败')
  } finally {
    uploadingImg.value = false
  }
}

async function submitShip() {
  if (!shipTargetOrder.value) return
  shipSubmitting.value = true
  operatingId.value = shipTargetOrder.value.id
  try {
    await sellerShip(shipTargetOrder.value.id, {
      deliveryNote: shipNote.value.trim() || undefined,
      deliveryImages: shipImages.value.length ? shipImages.value : undefined
    })
    ElMessage.success('已提交发货，等待买家确认收货')
    shipDialogVisible.value = false
    await fetchOrders()
  } catch (err) {
    notifyFallbackError(err, '发货失败')
  } finally {
    shipSubmitting.value = false
    operatingId.value = null
  }
}

function openGlobalScanner() {
  verifyTargetOrder.value = null
  verifyInitialMode.value = 'qr'
  verifyDialogVisible.value = true
}

function openVerifyModal(order: OrderListItem) {
  verifyTargetOrder.value = order
  verifyInitialMode.value = 'qr'
  verifyDialogVisible.value = true
}

async function handleOfflineVerified() {
  await fetchOrders()
}

function contactBuyer(order: OrderListItem) {
  router.push({
    name: 'ChatWithUser',
    params: { userId: order.buyerId },
    query: { relatedOrderId: String(order.id) }
  })
}

function openRefundDecision(order: OrderListItem & { refundReason?: string }) {
  refundTargetOrder.value = order
  rejectReason.value = ''
  refundDialogVisible.value = true
}

async function submitRefundDecision(approve: boolean) {
  if (!refundTargetOrder.value) return
  if (!approve && !rejectReason.value.trim()) {
    ElMessage.warning('拒绝退款时请填写拒绝原因')
    return
  }

  refundSubmitting.value = true
  try {
    await sellerHandleRefund(refundTargetOrder.value.id, {
      approve,
      rejectReason: approve ? undefined : rejectReason.value.trim()
    })
    ElMessage.success(approve ? '已同意退款，托管款项已退还买家' : '已拒绝退款申请')
    refundDialogVisible.value = false
    await fetchOrders()
  } catch (err) {
    notifyFallbackError(err, '处理退款申请失败')
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped lang="scss">
.head-count {
  font-size: 13px;
  color: var(--xyf-ink-3);
}

.toolbar-row {
  margin-bottom: 20px;
}

.loading-card {
  padding: 24px;
}

.orders-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.refund-review-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-summary {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xyf-ink-2);
}

.refund-reason-box {
  margin: 0;
  padding: 10px 12px;
  border-radius: var(--xyf-radius-xs);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  color: var(--xyf-ink);
  font-size: 13px;
}

.upload-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-thumb {
  position: relative;
  width: 78px;
  height: 78px;
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  border: 1px solid var(--xyf-border);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .thumb-remove {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    border: none;
    background: rgba(24, 24, 27, 0.78);
    color: #ffffff;
    font-size: 13px;
    cursor: pointer;
  }
}

.upload-trigger {
  width: 78px;
  height: 78px;
  border-radius: var(--xyf-radius-sm);
  border: 1px dashed var(--xyf-border-strong);
  background: var(--xyf-surface-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--xyf-ink-2);
  cursor: pointer;
}
</style>
