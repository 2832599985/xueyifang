<template>
  <div class="order-confirm-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="确认担保订单"
        subtitle="核对服务项目、交易方式与校内地点，下单后由平台冻结担保款项"
        :show-back="true"
        back-text="返回服务详情"
        :back-to="serviceId ? `/services/${serviceId}` : '/services'"
      />

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!service" class="xyf-surface-card loading-card">
        <XyfEmptyState
          title="未找到待购买的服务"
          description="请从服务详情页重新发起下单流程。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/services')">浏览服务大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <el-alert
          v-if="isOwnService"
          title="您无法购买自己发布的服务"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 18px"
        />

        <div class="checkout-grid">
          <!-- Left: Order Config -->
          <div class="checkout-main">
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>1. 服务信息核对</h2>
              </div>

              <div class="service-summary-row">
                <XyfImage
                  :src="buildImageUrl(service.images?.[0] || service.coverImage)"
                  :alt="service.serviceTitle"
                  width="116px"
                  height="84px"
                  :radius="10"
                  fit="cover"
                />
                <div class="service-summary-info">
                  <div class="tag-line">
                    <XyfStatusBadge
                      v-if="service.tagName"
                      tone="teal"
                      :label="service.tagName"
                      :dot="false"
                    />
                    <span class="seller-note">卖家：{{ service.sellerName || `#${service.sellerId}` }}</span>
                  </div>
                  <h3>{{ service.serviceTitle }}</h3>
                  <div class="price-unit xyf-tabular">
                    单价 <strong>{{ formatPrice(service.price) }}</strong>
                    <span class="stock-hint" v-if="maxBuyCount !== null">
                      （剩余可购 {{ maxBuyCount }} 份）
                    </span>
                  </div>
                </div>
              </div>
            </section>

            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>2. 购买数量与交易方式</h2>
              </div>

              <el-form label-position="top">
                <div class="form-row-2">
                  <el-form-item label="购买数量">
                    <el-input-number
                      v-model="form.quantity"
                      :min="1"
                      :max="maxBuyCount ?? 99"
                      :disabled="isSoldOut"
                    />
                  </el-form-item>

                  <el-form-item label="交易方式（由卖家设定）">
                    <div class="locked-trade-mode">
                      <XyfStatusBadge
                        size="md"
                        :tone="form.tradeType === TradeType.ONLINE ? 'teal' : 'accent'"
                        :label="form.tradeType === TradeType.ONLINE ? '线上担保交付' : '校园线下会面'"
                      />
                      <span class="locked-trade-hint">
                        {{
                          form.tradeType === TradeType.ONLINE
                            ? '线上交付成果，平台全程托管担保资金'
                            : '按约定校内地点会面交付，平台同样提供资金担保'
                        }}
                      </span>
                    </div>
                  </el-form-item>
                </div>

                <el-form-item
                  v-if="form.tradeType === TradeType.OFFLINE"
                  label="约定校内交易地点"
                  required
                >
                  <el-select
                    v-model="form.tradeLocationId"
                    placeholder="请选择校内会面地点"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="loc in dictStore.tradeLocations"
                      :key="loc.id"
                      :label="`${loc.locationName}（${loc.locationAddress || '校内地点'}）`"
                      :value="loc.id"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="期望履约或会面时段（选填，点击快捷填入）">
                  <div class="slot-chips">
                    <button
                      v-for="slot in campusTimeSlots"
                      :key="slot"
                      type="button"
                      class="slot-chip"
                      :class="{ 'is-active': selectedSlot === slot }"
                      @click="toggleSlot(slot)"
                    >
                      {{ slot }}
                    </button>
                  </div>
                </el-form-item>

                <el-form-item label="给卖家的需求备注（选填）">
                  <el-input
                    v-model="form.remark"
                    type="textarea"
                    :rows="3"
                    maxlength="300"
                    show-word-limit
                    placeholder="填写具体需求说明、课余空闲时段或交接细节（支持线下当面 6 位核销码验收）..."
                  />
                </el-form-item>
              </el-form>
            </section>
          </div>

          <!-- Right: Payment & Balance Summary -->
          <aside class="checkout-aside">
            <section class="xyf-surface-card summary-card">
              <h3>费用与担保明细</h3>

              <div class="fee-rows">
                <div class="fee-row">
                  <span>服务单价</span>
                  <span class="xyf-tabular">{{ formatPrice(service.price) }}</span>
                </div>
                <div class="fee-row">
                  <span>购买份数</span>
                  <span class="xyf-tabular">× {{ form.quantity }}</span>
                </div>
                <div class="fee-row">
                  <span>平台托管服务费</span>
                  <span class="free-tag">免手续费</span>
                </div>
              </div>

              <div class="total-bar">
                <span>应付总额</span>
                <strong class="xyf-tabular">{{ formatPrice(totalAmount) }}</strong>
              </div>

              <!-- Wallet Status Box -->
              <div class="wallet-box" :class="{ 'is-insufficient': isInsufficientBalance }">
                <div class="wallet-box__head">
                  <span>钱包可用余额</span>
                  <strong class="xyf-tabular">{{ formatPrice(wallet?.walletBalance || 0) }}</strong>
                </div>
                <p v-if="isInsufficientBalance" class="wallet-box__warn">
                  还差 {{ formatPrice(balanceShortfall) }}，可立即快捷充值后完成付款。
                </p>
                <div v-if="isInsufficientBalance" class="wallet-box__cta">
                  <el-button size="small" @click="openQuickRecharge">
                    一键快捷充值
                  </el-button>
                </div>
              </div>

              <div class="checkout-actions">
                <el-button
                  type="primary"
                  size="large"
                  class="pay-btn"
                  :disabled="isOwnService || isSoldOut || isInsufficientBalance"
                  :loading="submittingPay"
                  @click="handleSubmitOrder(true)"
                >
                  提交订单并立即支付
                </el-button>

                <el-button
                  size="large"
                  class="defer-btn"
                  :disabled="isOwnService || isSoldOut"
                  :loading="submittingCreate"
                  @click="handleSubmitOrder(false)"
                >
                  先下单，稍后支付
                </el-button>
              </div>

              <p class="escrow-guarantee">
                点击支付即表示资金进入平台托管账户，仅在您点击「确认收货」后划转至卖家。
              </p>
            </section>
          </aside>
        </div>
      </template>
    </div>

    <!-- Inline Quick Recharge Dialog -->
    <el-dialog v-model="rechargeVisible" title="钱包快捷充值" width="400px">
      <el-form label-position="top">
        <el-form-item label="充值金额（元）">
          <el-input-number
            v-model="rechargeAmount"
            :min="1"
            :max="5000"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <div class="quick-chips">
          <button
            v-for="preset in [50, 100, 200, Math.ceil(balanceShortfall)]"
            :key="preset"
            type="button"
            class="xyf-chip"
            @click="rechargeAmount = Math.max(1, preset)"
          >
            +{{ preset }} 元
          </button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="confirmQuickRecharge">
          确认充值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { getServiceDetail, buildImageUrl } from '@/api/service'
import { createOrder, payOrder } from '@/api/order'
import { getWalletInfo, rechargeWallet } from '@/api/wallet'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import { TradeType, PaymentMethod } from '@/types/order'
import type { ServiceDetail } from '@/types/service'
import type { WalletInfo } from '@/types/wallet'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dictStore = useDictStore()

const serviceId = computed(() => Number(route.params.serviceId || route.query.serviceId))

const loading = ref(true)
const submittingPay = ref(false)
const submittingCreate = ref(false)
const service = ref<ServiceDetail | null>(null)
const wallet = ref<WalletInfo | null>(null)

const rechargeVisible = ref(false)
const rechargeAmount = ref(50)
const recharging = ref(false)
const selectedSlot = ref('')

const campusTimeSlots = computed(() =>
  form.tradeType === TradeType.OFFLINE
    ? ['今日课后 18:30-21:00', '明日午休 12:15-13:45', '明日下午 16:00-18:00', '本周末双方协商']
    : ['付款后尽快开始', '今日晚间 22:00 前初稿', '48 小时内完整交付', '按站内沟通节点推进']
)

function toggleSlot(slot: string) {
  if (selectedSlot.value === slot) {
    selectedSlot.value = ''
    form.remark = form.remark.replace(`【期望时段：${slot}】`, '').trim()
    return
  }
  if (selectedSlot.value) {
    form.remark = form.remark.replace(`【期望时段：${selectedSlot.value}】`, '').trim()
  }
  selectedSlot.value = slot
  const prefix = `【期望时段：${slot}】`
  form.remark = form.remark ? `${prefix} ${form.remark}` : prefix
}

const form = reactive({
  quantity: 1,
  tradeType: TradeType.ONLINE,
  tradeLocationId: undefined as number | undefined,
  remark: ''
})

const isOwnService = computed(() =>
  Boolean(userStore.userInfo?.id && service.value?.sellerId === userStore.userInfo.id)
)

const maxBuyCount = computed(() => {
  if (!service.value || service.value.maxPurchases === -1) return null
  return Math.max(0, service.value.maxPurchases - service.value.currentPurchaseCount)
})

const isSoldOut = computed(() => maxBuyCount.value !== null && maxBuyCount.value <= 0)

const totalAmount = computed(() => {
  if (!service.value) return 0
  return Number((service.value.price * form.quantity).toFixed(2))
})

const isInsufficientBalance = computed(() => {
  const avail = wallet.value?.walletBalance || 0
  return avail < totalAmount.value
})

const balanceShortfall = computed(() =>
  Math.max(0, Number((totalAmount.value - (wallet.value?.walletBalance || 0)).toFixed(2)))
)

function openQuickRecharge() {
  rechargeAmount.value = Math.max(10, Math.ceil(balanceShortfall.value))
  rechargeVisible.value = true
}

async function confirmQuickRecharge() {
  if (recharging.value || rechargeAmount.value <= 0) return
  recharging.value = true
  try {
    await rechargeWallet({ amount: Number(rechargeAmount.value) })
    wallet.value = await getWalletInfo()
    rechargeVisible.value = false
    ElMessage.success('充值成功，可继续付款')
  } catch (err) {
    notifyFallbackError(err, '充值失败')
  } finally {
    recharging.value = false
  }
}

async function handleSubmitOrder(payImmediately: boolean) {
  if (!service.value) return
  if (form.tradeType === TradeType.OFFLINE && !form.tradeLocationId) {
    ElMessage.warning('请选择校内线下交易地点')
    return
  }

  if (payImmediately) submittingPay.value = true
  else submittingCreate.value = true

  try {
    const orderId = await createOrder({
      serviceId: service.value.id,
      quantity: form.quantity,
      tradeType: form.tradeType,
      tradeLocationId: form.tradeType === TradeType.OFFLINE ? form.tradeLocationId : undefined,
      remark: form.remark.trim() || undefined
    })

    if (payImmediately) {
      await payOrder(orderId, { orderId, paymentMethod: PaymentMethod.WALLET })
      ElMessage.success('下单并支付成功，款项已进入平台担保')
    } else {
      ElMessage.success('订单已创建，请在详情页尽快完成支付')
    }

    router.replace(`/orders/${orderId}`)
  } catch (err) {
    notifyFallbackError(err, payImmediately ? '下单或支付失败' : '创建订单失败')
  } finally {
    submittingPay.value = false
    submittingCreate.value = false
  }
}

onMounted(async () => {
  if (!serviceId.value || Number.isNaN(serviceId.value)) {
    loading.value = false
    return
  }

  loading.value = true
  try {
    const [svc, w] = await Promise.all([
      getServiceDetail(serviceId.value),
      getWalletInfo().catch(() => null),
      dictStore.fetchTradeLocations().catch(() => {})
    ])
    service.value = svc
    wallet.value = w
    form.tradeType = svc.tradeType === TradeType.OFFLINE ? TradeType.OFFLINE : TradeType.ONLINE
    form.tradeLocationId = svc.tradeLocationId || dictStore.tradeLocations?.[0]?.id
  } catch (err) {
    notifyFallbackError(err, '加载订单确认页失败')
    service.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 22px;
  align-items: start;
}

.checkout-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card,
.summary-card {
  padding: 22px 24px;
}

.service-summary-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.service-summary-info {
  flex: 1;
  min-width: 0;

  .tag-line {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;

    .seller-note {
      font-size: 12.5px;
      color: var(--xyf-ink-muted);
    }
  }

  h3 {
    margin: 0 0 6px;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .price-unit {
    font-size: 13.5px;
    color: var(--xyf-ink-secondary);

    strong {
      color: var(--xyf-accent);
      font-size: 16px;
    }

    .stock-hint {
      font-size: 12px;
      color: var(--xyf-ink-muted);
    }
  }
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.locked-trade-mode {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;

  .locked-trade-hint {
    font-size: 12px;
    color: var(--xyf-ink-muted);
    line-height: 1.4;
  }
}

.checkout-aside {
  position: sticky;
  top: 84px;
}

.summary-card h3 {
  margin: 0 0 16px;
  font-size: 16.5px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.fee-rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--xyf-border);
}

.fee-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: var(--xyf-ink-secondary);

  .free-tag {
    color: var(--xyf-ink);
    font-weight: 600;
  }
}

.total-bar {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 16px 0;

  span {
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  strong {
    font-size: 26px;
    font-weight: 800;
    color: var(--xyf-accent);
  }
}

.wallet-box {
  padding: 12px 14px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  margin-bottom: 18px;

  &.is-insufficient {
    background: var(--xyf-warning-soft);
    border-color: rgba(180, 83, 9, 0.25);
  }

  &__head {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--xyf-ink-secondary);

    strong {
      color: var(--xyf-ink);
    }
  }

  &__warn {
    margin: 6px 0 0;
    font-size: 12px;
    color: var(--xyf-warning);
  }

  &__cta {
    margin-top: 8px;
  }
}

.checkout-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .pay-btn,
  .defer-btn {
    width: 100%;
    margin-left: 0;
    font-weight: 600;
  }
}

.escrow-guarantee {
  margin: 14px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--xyf-ink-muted);
}

.quick-chips,
.slot-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.slot-chip {
  height: 32px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #a1a1aa;
    color: var(--xyf-ink);
  }

  &.is-active {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

@media (max-width: 900px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }

  .checkout-aside {
    position: static;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
