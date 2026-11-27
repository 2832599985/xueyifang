<template>
  <div class="order-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="订单详情"
        :show-back="true"
        back-text="返回订单列表"
        :back-to="isSeller ? '/orders/sales' : '/orders'"
      >
        <template #actions v-if="orderDetail">
          <button type="button" class="order-no-pill xyf-mono" @click="copyOrderNo">
            <span>{{ orderDetail.orderNumber }}</span>
            <el-icon><DocumentCopy /></el-icon>
          </button>
        </template>
      </XyfPageHeader>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!orderDetail" class="xyf-surface-card loading-card">
        <XyfEmptyState
          title="无法获取订单详情"
          description="订单可能不存在或您暂无权查看此订单。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/orders')">返回我的订单</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Status Hero Banner (with folded primary actions) -->
        <section class="xyf-surface-card status-banner">
          <div class="status-banner__top">
            <div class="status-banner__left">
              <div class="status-badges">
                <XyfStatusBadge
                  size="md"
                  :tone="statusTone"
                  :label="getOrderStatusText(orderDetail.orderStatus)"
                  :dot="isOrderActive"
                />
                <XyfStatusBadge
                  size="md"
                  :tone="paymentTone"
                  :label="getPaymentStatusText(orderDetail.paymentStatus)"
                />
                <XyfStatusBadge
                  v-if="orderDetail.refundStatus && getRefundStatusText(orderDetail.refundStatus)"
                  size="md"
                  tone="warning"
                  :label="getRefundStatusText(orderDetail.refundStatus)"
                  :dot="orderDetail.refundStatus === RefundStatus.PENDING"
                />
              </div>

              <p class="status-explain">{{ statusExplanation }}</p>

              <div
                v-if="orderDetail.refundStatus && getRefundStatusText(orderDetail.refundStatus) && orderDetail.refundReason"
                class="refund-note"
              >
                退款说明：{{ orderDetail.refundReason }}
              </div>
            </div>

            <div class="status-banner__right">
              <div class="amount-box">
                <span class="amount-label">订单金额</span>
                <strong class="xyf-tabular">{{ formatPrice(orderDetail.totalAmount) }}</strong>
                <span v-if="orderDetail.frozenAmount && orderDetail.frozenAmount > 0" class="frozen-pill xyf-tabular">
                  担保冻结中 {{ formatPrice(orderDetail.frozenAmount) }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="hasAnyAction" class="status-banner__actions">
            <el-button
              v-if="isBuyer && canPayOrder(orderDetail.orderStatus)"
              type="primary"
              :loading="actionLoading"
              @click="handlePayOrder"
            >
              立即支付
            </el-button>

            <el-button
              v-if="isBuyer && canCancelOrder(orderDetail.orderStatus)"
              :loading="actionLoading"
              @click="handleCancelOrder"
            >
              取消订单
            </el-button>

            <el-button
              v-if="isBuyer && canBuyerConfirm(orderDetail.orderStatus, orderDetail.refundStatus)"
              type="primary"
              :loading="actionLoading"
              @click="handleConfirmReceipt"
            >
              确认收货并结算
            </el-button>

            <el-button
              v-if="isBuyer && canApplyRefund(orderDetail.orderStatus, orderDetail.refundStatus)"
              @click="refundApplyVisible = true"
            >
              申请退款
            </el-button>

            <el-button
              v-if="isBuyer && orderDetail.orderStatus === OrderStatus.COMPLETED && !orderDetail.isReviewed"
              type="primary"
              @click="reviewDialogVisible = true"
            >
              发表服务评价
            </el-button>

            <el-button
              v-if="isSeller && canOfflineVerify"
              type="primary"
              @click="openVerifyCodeDialog"
            >
              扫码 / 动态码核销结单
            </el-button>

            <el-button
              v-if="isSeller && canSellerShip(orderDetail.orderStatus)"
              :type="canOfflineVerify ? 'default' : 'primary'"
              :loading="actionLoading"
              @click="openShipDialog"
            >
              {{ orderDetail.tradeType === TradeType.OFFLINE ? '标记已出发 / 交接中' : '提交交付并发货' }}
            </el-button>

            <el-button
              v-if="isSeller && canSellerHandleRefund(orderDetail.orderStatus, orderDetail.refundStatus)"
              type="primary"
              @click="sellerRefundVisible = true"
            >
              处理退款申请
            </el-button>

            <el-button @click="handleContactCounterparty">
              <el-icon><ChatDotRound /></el-icon>
              联系{{ isBuyer ? '卖家' : '买家' }}
            </el-button>

            <el-button
              v-if="canInitiateDispute(orderDetail.orderStatus)"
              type="danger"
              plain
              @click="openDisputeDialog"
            >
              发起平台纠纷
            </el-button>
          </div>
        </section>

        <!-- Main 2-column detail -->
        <div class="detail-grid">
          <div class="detail-main">
            <!-- Buyer Exclusive: Dynamic 10s Anti-Screenshot QR + 30s Rotating TOTP Code -->
            <OfflineMeetupPassCard
              v-if="isBuyer && canOfflineVerify"
              :order-id="orderDetail.id"
              :initial-code="orderDetail.verifyCode"
              @verified="fetchDetail"
            />

            <!-- Bidirectional Campus Map Meetup Negotiation Card -->
            <MeetupProposalCard
              v-if="orderDetail.tradeType === TradeType.OFFLINE"
              :order="orderDetail"
              :current-user-id="userStore.userInfo?.id"
              @updated="fetchDetail"
            />

            <!-- Escrow Progress & Timeline -->
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>担保流程与时间节点</h2>
              </div>
              <OrderTimeline :order="orderDetail" />
            </section>

            <!-- Seller Delivery Voucher Card -->
            <section
              v-if="orderDetail.deliveryNote || orderDetail.deliveryImages?.length"
              class="xyf-surface-card panel-card"
            >
              <div class="xyf-section-heading">
                <h2>卖方履约交付凭证</h2>
              </div>

              <div v-if="orderDetail.deliveryNote" class="voucher-note-box">
                <span class="voucher-label">交付说明</span>
                <p>{{ orderDetail.deliveryNote }}</p>
              </div>

              <div v-if="orderDetail.deliveryImages?.length" class="voucher-images">
                <el-image
                  v-for="(img, idx) in orderDetail.deliveryImages"
                  :key="img + idx"
                  :src="img"
                  :preview-src-list="orderDetail.deliveryImages"
                  :initial-index="idx"
                  fit="cover"
                  class="voucher-img-item"
                />
              </div>
            </section>

            <!-- Staged Milestone Escrow Card -->
            <OrderMilestoneCard
              :order="orderDetail"
              :is-buyer="isBuyer"
              :is-seller="isSeller"
              @updated="fetchDetail"
            />

            <!-- Appointment No-Show Automatic Compensation Card -->
            <OrderNoShowCompensationCard
              :order="orderDetail"
              :is-buyer="isBuyer"
              :is-seller="isSeller"
              @updated="fetchDetail"
            />

            <!-- SHA-256 Digital Fulfillment & Campus Skill Practice Certificate Card -->
            <OrderDeliveryCertificateCard
              :order="orderDetail"
            />

            <!-- Service & Trade Details -->
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>订单商品与交付约定</h2>
              </div>

              <div class="service-box" @click="router.push(`/services/${orderDetail.serviceId}`)">
                <XyfImage
                  :src="buildImageUrl(orderDetail.serviceImage)"
                  :alt="orderDetail.serviceTitle"
                  width="108px"
                  height="78px"
                  :radius="10"
                  fit="cover"
                />
                <div class="service-box__info">
                  <h3>{{ orderDetail.serviceTitle }}</h3>
                  <div class="service-pricing xyf-tabular">
                    <span>单价 {{ formatPrice(orderDetail.unitPrice) }}</span>
                    <span>·</span>
                    <span>数量 × {{ orderDetail.quantity }}</span>
                    <span>·</span>
                    <span>{{ getTradeTypeText(orderDetail.tradeType) }}</span>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- Sticky Counterparties & Fee Breakdown Sidebar -->
          <aside class="detail-aside">
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>交易双方</h2>
              </div>

              <div class="party-block">
                <span class="party-role">买方</span>
                <div class="party-row">
                  <XyfAvatar
                    :src="orderDetail.buyerAvatar"
                    :name="orderDetail.buyerName"
                    :size="38"
                  />
                  <div>
                    <div class="party-name">{{ orderDetail.buyerName }}</div>
                    <div class="party-sub" v-if="orderDetail.buyerProfessional">
                      {{ orderDetail.buyerProfessional }}
                    </div>
                  </div>
                </div>
              </div>

              <div class="party-divider" />

              <div class="party-block">
                <span class="party-role">卖方（服务提供者）</span>
                <div class="party-row">
                  <XyfAvatar
                    :src="orderDetail.sellerAvatar"
                    :name="orderDetail.sellerName"
                    :size="38"
                  />
                  <div>
                    <div class="party-name">{{ orderDetail.sellerName }}</div>
                    <div class="party-sub" v-if="orderDetail.sellerProfessional">
                      {{ orderDetail.sellerProfessional }}
                    </div>
                  </div>
                </div>
              </div>

              <dl class="aside-fee-list xyf-tabular">
                <div class="aside-fee-row">
                  <dt>单价 × 数量</dt>
                  <dd>{{ formatPrice(orderDetail.unitPrice) }} × {{ orderDetail.quantity }}</dd>
                </div>
                <div v-if="orderDetail.platformFee && orderDetail.platformFee > 0" class="aside-fee-row">
                  <dt>平台服务费</dt>
                  <dd>{{ formatPrice(orderDetail.platformFee) }}</dd>
                </div>
                <div v-if="orderDetail.sellerSettleAmount !== undefined && orderDetail.sellerSettleAmount !== null && orderDetail.orderStatus === OrderStatus.COMPLETED" class="aside-fee-row">
                  <dt>卖方实收</dt>
                  <dd>{{ formatPrice(orderDetail.sellerSettleAmount) }}</dd>
                </div>
                <div class="aside-fee-row is-total">
                  <dt>订单总额</dt>
                  <dd>{{ formatPrice(orderDetail.totalAmount) }}</dd>
                </div>
              </dl>

              <div v-if="orderDetail.remark" class="remark-card">
                <span class="remark-label">买家下单备注</span>
                <p>{{ orderDetail.remark }}</p>
              </div>

              <el-button
                plain
                style="width: 100%; margin-top: 16px"
                @click="handleContactCounterparty"
              >
                <el-icon><ChatDotRound /></el-icon>
                发送站内消息
              </el-button>
            </section>
          </aside>
        </div>
      </template>
    </div>

    <!-- Seller Delivery / Ship Dialog -->
    <el-dialog v-model="shipDialogVisible" title="提交履约交付并发货" width="480px">
      <el-form label-position="top">
        <el-form-item label="快捷填入交付说明（选填）">
          <div class="quick-note-chips">
            <button
              v-for="tpl in shipQuickTemplates"
              :key="tpl"
              type="button"
              class="note-chip"
              @click="shipForm.deliveryNote = tpl"
            >
              {{ tpl }}
            </button>
          </div>
        </el-form-item>

        <el-form-item label="履约交付说明（建议填写成果摘要、仓库/网盘说明或现场交接记录）">
          <el-input
            v-model="shipForm.deliveryNote"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="填写交付详情，将同步存档至订单详情作为履约凭证..."
          />
        </el-form-item>

        <el-form-item label="上传交付成果或现场凭证截图（选填，最多 4 张）">
          <div class="upload-strip">
            <div v-for="(img, idx) in shipForm.deliveryImages" :key="img + idx" class="upload-thumb">
              <img :src="img" alt="交付凭证" />
              <button type="button" class="thumb-remove" @click="shipForm.deliveryImages.splice(idx, 1)">×</button>
            </div>
            <label v-if="shipForm.deliveryImages.length < 4" class="upload-trigger">
              <input type="file" accept="image/*" hidden :disabled="uploadingMedia" @change="handleDeliveryImageSelect" />
              <span>{{ uploadingMedia ? '上传中...' : '+ 添加图片' }}</span>
            </label>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading || uploadingMedia" @click="submitSellerShip">
          确认提交并发货
        </el-button>
      </template>
    </el-dialog>

    <!-- Seller Offline Meetup Live Camera QR Scanner & 30s Code Dialog -->
    <OfflineVerifyDialog
      v-if="orderDetail"
      v-model:visible="verifyDialogVisible"
      :order-id="orderDetail.id"
      :order-title="orderDetail.serviceTitle"
      initial-mode="qr"
      @verified="fetchDetail"
    />

    <!-- Buyer Apply Refund Dialog -->
    <el-dialog v-model="refundApplyVisible" title="申请订单退款" width="440px">
      <el-form label-position="top">
        <el-form-item label="退款原因说明">
          <el-input
            v-model="refundReasonInput"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请清晰说明退款原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundApplyVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitApplyRefund">
          确认提交
        </el-button>
      </template>
    </el-dialog>

    <!-- Seller Handle Refund Dialog -->
    <el-dialog v-model="sellerRefundVisible" title="处理买家退款申请" width="450px">
      <div v-if="orderDetail" class="dialog-stack">
        <p v-if="orderDetail.refundReason" class="refund-note">
          买家申请说明：{{ orderDetail.refundReason }}
        </p>
        <el-form label-position="top">
          <el-form-item label="若拒绝退款，请填写拒绝理由（同意退款可直接点击同意）">
            <el-input
              v-model="rejectReasonInput"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请填写拒绝说明..."
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="sellerRefundVisible = false">取消</el-button>
        <el-button type="danger" plain :loading="actionLoading" @click="handleRefundDecision(false)">
          拒绝退款
        </el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleRefundDecision(true)">
          同意全额退款
        </el-button>
      </template>
    </el-dialog>

    <!-- Initiate Dispute Dialog (with Evidence Upload) -->
    <el-dialog v-model="disputeDialogVisible" title="发起平台仲裁纠纷" width="480px">
      <el-form label-position="top">
        <el-form-item label="纠纷类型">
          <el-select v-model="disputeForm.disputeType" style="width: 100%">
            <el-option
              v-for="opt in disputeTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="问题描述与诉求">
          <el-input
            v-model="disputeForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请客观描述遇到的问题、沟通情况及期望处理方案（发起后系统将自动冻结超时确认收货）..."
          />
        </el-form-item>

        <el-form-item label="上传举证截图（选填，最多 4 张，支持聊天/实物/现场截图）">
          <div class="upload-strip">
            <div v-for="(img, idx) in disputeForm.evidenceImages" :key="img + idx" class="upload-thumb">
              <img :src="img" alt="举证图片" />
              <button type="button" class="thumb-remove" @click="disputeForm.evidenceImages.splice(idx, 1)">×</button>
            </div>
            <label v-if="disputeForm.evidenceImages.length < 4" class="upload-trigger">
              <input type="file" accept="image/*" hidden :disabled="uploadingMedia" @change="handleDisputeEvidenceSelect" />
              <span>{{ uploadingMedia ? '上传中...' : '+ 上传证据' }}</span>
            </label>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading || uploadingMedia" @click="submitDispute">
          提交仲裁申请
        </el-button>
      </template>
    </el-dialog>

    <!-- Review Dialog -->
    <ServiceReviewDialog
      v-if="orderDetail"
      :visible="reviewDialogVisible"
      :order-id="orderDetail.id"
      @update:visible="reviewDialogVisible = $event"
      @success="handleReviewSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { buildImageUrl } from '@/api/service'
import { uploadFile } from '@/api/file'
import {
  getOrderDetail,
  payOrder,
  createEscrowPrepay,
  triggerSandboxEscrowWebhook,
  cancelOrder,
  buyerConfirm,
  sellerShip,
  applyRefund,
  sellerHandleRefund
} from '@/api/order'
import { createDispute } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import {
  OrderStatus,
  PaymentStatus,
  PaymentMethod,
  RefundStatus,
  TradeType,
  getOrderStatusText,
  getPaymentStatusText,
  getRefundStatusText,
  getTradeTypeText,
  canPayOrder,
  canCancelOrder,
  canBuyerConfirm,
  canSellerShip,
  canApplyRefund,
  canSellerHandleRefund,
  canInitiateDispute,
  type OrderDetail
} from '@/types/order'
import { DISPUTE_TYPE, disputeTypeOptions } from '@/types/dispute'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import OrderMilestoneCard from '@/components/business/OrderMilestoneCard.vue'
import OrderNoShowCompensationCard from '@/components/order/OrderNoShowCompensationCard.vue'
import OrderDeliveryCertificateCard from '@/components/order/OrderDeliveryCertificateCard.vue'
import OfflineMeetupPassCard from '@/components/business/OfflineMeetupPassCard.vue'
import MeetupProposalCard from '@/components/business/MeetupProposalCard.vue'
import OfflineVerifyDialog from '@/components/business/OfflineVerifyDialog.vue'
import ServiceReviewDialog from '@/components/review/ServiceReviewDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const orderId = computed(() => Number(route.params.id))

const loading = ref(true)
const actionLoading = ref(false)
const uploadingMedia = ref(false)
const orderDetail = ref<OrderDetail | null>(null)

const shipDialogVisible = ref(false)
const shipForm = ref({
  deliveryNote: '',
  deliveryImages: [] as string[]
})

const verifyDialogVisible = ref(false)

const refundApplyVisible = ref(false)
const refundReasonInput = ref('')
const sellerRefundVisible = ref(false)
const rejectReasonInput = ref('')
const disputeDialogVisible = ref(false)
const reviewDialogVisible = ref(false)

const disputeForm = ref({
  orderId: 0,
  disputeType: DISPUTE_TYPE.SELLER_NOT_FULFILL as number,
  description: '',
  evidenceImages: [] as string[]
})

const isBuyer = computed(() =>
  Boolean(userStore.userInfo?.id && orderDetail.value?.buyerId === userStore.userInfo.id)
)

const isSeller = computed(() =>
  Boolean(userStore.userInfo?.id && orderDetail.value?.sellerId === userStore.userInfo.id)
)

const canOfflineVerify = computed(() => {
  const o = orderDetail.value
  if (!o || o.tradeType !== TradeType.OFFLINE) return false
  if (o.refundStatus === RefundStatus.PENDING) return false
  return o.orderStatus === OrderStatus.PENDING_SHIP || o.orderStatus === OrderStatus.PENDING_RECEIPT
})

const shipQuickTemplates = computed(() =>
  orderDetail.value?.tradeType === TradeType.OFFLINE
    ? ['已到达校内约定地点完成交接', '现场服务已完成，请核对验收', '已按约定时间完成线下陪练/辅导']
    : ['已完成全套定制成果并发送站内消息', '代码排障与注释说明已全部交付', '文书批改稿与批注清单已提交，请查收']
)

const isOrderActive = computed(() => {
  const s = orderDetail.value?.orderStatus
  return (
    s === OrderStatus.UNPAID ||
    s === OrderStatus.PENDING_SHIP ||
    s === OrderStatus.PENDING_RECEIPT
  )
})

const statusTone = computed<'neutral' | 'info' | 'success' | 'warning' | 'danger'>(() => {
  const s = orderDetail.value?.orderStatus
  if (s === OrderStatus.COMPLETED) return 'success'
  if (s === OrderStatus.UNPAID) return 'warning'
  if (s === OrderStatus.FAILED) return 'danger'
  if (s === OrderStatus.CANCELLED) return 'neutral'
  return 'info'
})

const paymentTone = computed(() => {
  const p = orderDetail.value?.paymentStatus
  if (p === PaymentStatus.PAID) return 'success'
  if (p === PaymentStatus.REFUNDED) return 'danger'
  return 'warning'
})

const statusExplanation = computed(() => {
  const s = orderDetail.value?.orderStatus
  if (s === OrderStatus.UNPAID) return '订单已生成，请在付款后开始服务交付。'
  if (s === OrderStatus.PENDING_SHIP) {
    return orderDetail.value?.tradeType === TradeType.OFFLINE
      ? '买方资金已由平台托管冻结，可前往校内约定地点当面实时扫码（10 秒防截图）或输入 30 秒动态核销码直接完结订单。'
      : '买方资金已由平台托管冻结，等待卖方提交交付凭证并确认发货。'
  }
  if (s === OrderStatus.PENDING_RECEIPT) return '卖方已提交交付记录，请买方验收后确认收货结算。'
  if (s === OrderStatus.COMPLETED) return '双方已顺利完成交付，担保资金已结算入卖家账户。'
  if (s === OrderStatus.CANCELLED) return '订单已取消，未发生资金扣款或资金已退回原账户。'
  if (s === OrderStatus.FAILED) return '订单已退款关闭，托管资金已原路退还买方。'
  return ''
})

const hasAnyAction = computed(() => Boolean(orderDetail.value))

async function copyOrderNo() {
  if (!orderDetail.value) return
  try {
    await navigator.clipboard.writeText(orderDetail.value.orderNumber)
    ElMessage.success('订单号已复制')
  } catch {
    ElMessage.info(orderDetail.value.orderNumber)
  }
}

async function fetchDetail() {
  loading.value = true
  try {
    orderDetail.value = await getOrderDetail(orderId.value)
  } catch (err) {
    notifyFallbackError(err, '获取订单详情失败')
    orderDetail.value = null
  } finally {
    loading.value = false
  }
}

async function handlePayOrder() {
  if (!orderDetail.value) return
  try {
    await ElMessageBox.confirm(
      `确认支付 ${formatPrice(orderDetail.value.totalAmount)} 至平台托管吗？`,
      '订单支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'info' }
    )
    actionLoading.value = true
    const prepay = await createEscrowPrepay(orderDetail.value.id, 'WECHAT_MINI')
    if (prepay.sandboxCallbackSign) {
      await triggerSandboxEscrowWebhook({
        paymentNo: prepay.paymentNo,
        paidAmount: prepay.amount,
        signature: prepay.sandboxCallbackSign
      })
    } else {
      await payOrder(orderDetail.value.id, {
        orderId: orderDetail.value.id,
        paymentMethod: PaymentMethod.WALLET
      })
    }
    ElMessage.success('支付成功，款项已进入平台担保')
    await fetchDetail()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '支付失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleCancelOrder() {
  if (!orderDetail.value) return
  try {
    await ElMessageBox.confirm('确认取消这笔未支付订单吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '保留',
      type: 'warning'
    })
    actionLoading.value = true
    await cancelOrder(orderDetail.value.id)
    ElMessage.success('订单已取消')
    await fetchDetail()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '取消订单失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirmReceipt() {
  if (!orderDetail.value) return
  try {
    await ElMessageBox.confirm(
      '确认验收通过并立即结算资金给卖家吗？',
      '确认收货',
      { confirmButtonText: '确认结算', cancelButtonText: '取消', type: 'success' }
    )
    actionLoading.value = true
    await buyerConfirm(orderDetail.value.id)
    ElMessage.success('交易已完成，资金已结算给卖家')
    await fetchDetail()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '确认收货失败')
  } finally {
    actionLoading.value = false
  }
}

function openShipDialog() {
  shipForm.value = {
    deliveryNote: '',
    deliveryImages: []
  }
  shipDialogVisible.value = true
}

async function handleDeliveryImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploadingMedia.value = true
  try {
    const url = await uploadFile(file, 'delivery_image')
    shipForm.value.deliveryImages.push(url)
  } catch (err) {
    notifyFallbackError(err, '上传交付凭证图片失败')
  } finally {
    uploadingMedia.value = false
  }
}

async function submitSellerShip() {
  if (!orderDetail.value) return
  actionLoading.value = true
  try {
    await sellerShip(orderDetail.value.id, {
      deliveryNote: shipForm.value.deliveryNote.trim() || undefined,
      deliveryImages: shipForm.value.deliveryImages.length ? shipForm.value.deliveryImages : undefined
    })
    ElMessage.success('交付凭证已提交，订单进入待收货')
    shipDialogVisible.value = false
    await fetchDetail()
  } catch (err) {
    notifyFallbackError(err, '提交发货失败')
  } finally {
    actionLoading.value = false
  }
}

function openVerifyCodeDialog() {
  verifyDialogVisible.value = true
}

async function submitApplyRefund() {
  if (!orderDetail.value) return
  if (!refundReasonInput.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  actionLoading.value = true
  try {
    await applyRefund(orderDetail.value.id, refundReasonInput.value.trim())
    ElMessage.success('已提交退款申请')
    refundApplyVisible.value = false
    refundReasonInput.value = ''
    await fetchDetail()
  } catch (err) {
    notifyFallbackError(err, '提交退款申请失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleRefundDecision(approve: boolean) {
  if (!orderDetail.value) return
  if (!approve && !rejectReasonInput.value.trim()) {
    ElMessage.warning('拒绝退款时请填写拒绝理由')
    return
  }
  actionLoading.value = true
  try {
    await sellerHandleRefund(orderDetail.value.id, {
      approve,
      rejectReason: approve ? undefined : rejectReasonInput.value.trim()
    })
    ElMessage.success(approve ? '已同意全额退款' : '已拒绝退款申请')
    sellerRefundVisible.value = false
    rejectReasonInput.value = ''
    await fetchDetail()
  } catch (err) {
    notifyFallbackError(err, '处理退款失败')
  } finally {
    actionLoading.value = false
  }
}

function handleContactCounterparty() {
  if (!orderDetail.value) return
  const targetUserId = isBuyer.value ? orderDetail.value.sellerId : orderDetail.value.buyerId
  router.push({
    name: 'ChatWithUser',
    params: { userId: targetUserId },
    query: { relatedOrderId: String(orderDetail.value.id) }
  })
}

function openDisputeDialog() {
  if (!orderDetail.value) return
  disputeForm.value = {
    orderId: orderDetail.value.id,
    disputeType: DISPUTE_TYPE.SELLER_NOT_FULFILL,
    description: '',
    evidenceImages: []
  }
  disputeDialogVisible.value = true
}

async function handleDisputeEvidenceSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploadingMedia.value = true
  try {
    const url = await uploadFile(file, 'dispute_evidence')
    disputeForm.value.evidenceImages.push(url)
  } catch (err) {
    notifyFallbackError(err, '上传举证图片失败')
  } finally {
    uploadingMedia.value = false
  }
}

async function submitDispute() {
  if (!disputeForm.value.description.trim()) {
    ElMessage.warning('请填写纠纷问题描述')
    return
  }
  actionLoading.value = true
  try {
    const disputeId = await createDispute({
      orderId: disputeForm.value.orderId,
      disputeType: disputeForm.value.disputeType,
      description: disputeForm.value.description.trim(),
      evidenceImages: disputeForm.value.evidenceImages.length ? disputeForm.value.evidenceImages : undefined
    })
    ElMessage.success('已发起纠纷，等待管理员介入仲裁')
    disputeDialogVisible.value = false
    router.push(`/disputes/${disputeId}`)
  } catch (err) {
    notifyFallbackError(err, '发起纠纷失败')
  } finally {
    actionLoading.value = false
  }
}

function handleReviewSuccess() {
  if (orderDetail.value) {
    orderDetail.value.isReviewed = true
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.order-no-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  font-size: 13px;
  color: var(--xyf-ink-secondary);
  cursor: pointer;

  &:hover {
    border-color: var(--xyf-primary);
    color: var(--xyf-primary);
  }
}

.status-banner {
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 20px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding-top: 16px;
    border-top: 1px solid var(--xyf-border-subtle);
  }
}

.status-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.status-explain {
  margin: 0;
  font-size: 14px;
  color: var(--xyf-ink-2);
}

.refund-note {
  margin-top: 10px;
  padding: 9px 13px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  color: var(--xyf-ink-2);
  font-size: 13px;
}

.amount-box {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;

  .amount-label {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  strong {
    font-size: 26px;
    font-weight: 700;
    color: var(--xyf-ink);
    line-height: 1.1;
  }

  .frozen-pill {
    font-size: 12px;
    font-weight: 500;
    color: #9a5b13;
    background: var(--xyf-champagne-soft);
    border: 1px solid var(--xyf-champagne-border);
    padding: 2px 9px;
    border-radius: var(--xyf-radius-xs);
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 348px;
  gap: 24px;
  align-items: start;
}

.detail-aside {
  position: sticky;
  top: 84px;
}

.aside-fee-list {
  margin: 16px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aside-fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;

  dt {
    color: var(--xyf-ink-3);
  }

  dd {
    margin: 0;
    font-weight: 500;
    color: var(--xyf-ink);
  }

  &.is-total {
    padding-top: 8px;
    border-top: 1px dashed var(--xyf-border);

    dt {
      color: var(--xyf-ink);
      font-weight: 600;
    }

    dd {
      font-size: 16px;
      font-weight: 700;
      color: var(--xyf-ink);
    }
  }
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card {
  padding: 22px 24px;
}

.voucher-note-box {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  .voucher-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.65;
    color: var(--xyf-ink);
    white-space: pre-wrap;
  }
}

.voucher-images {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 10px;
}

.voucher-img-item {
  width: 100%;
  height: 92px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  overflow: hidden;
  cursor: zoom-in;
}

.service-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px;
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  cursor: pointer;
  transition: border-color var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-primary);
  }

  &__info {
    flex: 1;
    min-width: 0;

    .service-tag {
      font-size: 12px;
      color: var(--xyf-primary-dark);
      font-weight: 600;
      margin-bottom: 4px;
    }

    h3 {
      margin: 0 0 6px;
      font-size: 15.5px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    .service-pricing {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 13px;
      color: var(--xyf-ink-muted);
    }
  }
}

.remark-card {
  margin-top: 16px;
  padding: 12px 14px;
  background: var(--xyf-bg);
  border-radius: var(--xyf-radius-sm);

  .remark-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-muted);
    display: block;
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    font-size: 13.5px;
    color: var(--xyf-ink-secondary);
  }
}

.party-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.party-role {
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink-muted);
}

.party-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.party-name {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.party-sub {
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

.party-divider {
  height: 1px;
  background: var(--xyf-border-light);
  margin: 16px 0;
}

/* Dialog Helpers */
.quick-note-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.note-chip {
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #18181b;
    color: var(--xyf-ink);
  }
}

.upload-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-thumb {
  position: relative;
  width: 82px;
  height: 82px;
  border-radius: 10px;
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
    line-height: 1;
    cursor: pointer;
  }
}

.upload-trigger {
  width: 82px;
  height: 82px;
  border-radius: 10px;
  border: 1px dashed #a1a1aa;
  background: var(--xyf-surface-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--xyf-ink-2);
  cursor: pointer;
  transition: border-color var(--xyf-transition);

  &:hover {
    border-color: #18181b;
    color: var(--xyf-ink);
  }
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-aside {
    position: static;
  }
}
</style>
