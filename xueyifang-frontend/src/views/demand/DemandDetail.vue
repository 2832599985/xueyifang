<template>
  <div class="demand-detail-page xyf-page">
    <div class="xyf-container">
      <!-- Slim Top Bar: Back + Unboxed Serial + Inline Lifecycle -->
      <nav class="detail-topbar">
        <div class="detail-topbar__left">
          <button type="button" class="back-link" @click="router.push('/demands')">
            <el-icon><ArrowLeft /></el-icon>
            <span>悬赏大厅</span>
          </button>
          <template v-if="demand">
            <span class="sep" aria-hidden="true">/</span>
            <span class="serial-text xyf-mono">{{ demand.demandNo }}</span>
          </template>
        </div>

        <ol v-if="demand" class="inline-stepper" aria-label="悬赏进度">
          <li
            v-for="(step, idx) in lifecycleSteps"
            :key="step.title"
            class="inline-stepper__item"
            :class="{
              'is-done': lifecycleIndex > idx,
              'is-current': lifecycleIndex === idx
            }"
          >
            <span class="inline-stepper__num xyf-tabular">0{{ idx + 1 }}</span>
            <span>{{ step.title }}</span>
            <span v-if="idx < lifecycleSteps.length - 1" class="inline-stepper__arrow" aria-hidden="true">→</span>
          </li>
        </ol>
      </nav>

      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="!demand" class="empty-wrap">
        <XyfEmptyState title="未找到该悬赏需求" description="该悬赏可能已被发布者撤回或关闭">
          <template #action>
            <el-button type="primary" @click="router.push('/demands')">返回悬赏大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Matched Escrow Order Banner -->
        <section v-if="demand.demandStatus === 1 && demand.matchedOrderId" class="matched-bar">
          <div class="matched-bar__info">
            <strong>已选标并生成担保订单</strong>
            <span>双方可在订单详情中完成支付与验收。</span>
          </div>
          <el-button
            v-if="isPublisher || isWinningBidder"
            type="primary"
            @click="router.push(`/orders/${demand.matchedOrderId}`)"
          >
            查看订单
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </section>

        <div class="detail-grid">
          <!-- Left Editorial Column (open document flow, hairline dividers, zero nested boxes) -->
          <main class="detail-main">
            <header class="doc-header">
              <div class="doc-kicker">
                <XyfUrgencyTag :level="demand.urgencyLevel" size="md" />
                <XyfStatusBadge
                  :tone="demand.demandStatus === 0 ? 'success' : demand.demandStatus === 1 ? 'dark' : 'neutral'"
                  :icon="demand.demandStatus === 0 ? 'live' : demand.demandStatus === 1 ? 'check' : 'lock'"
                  :tooltip="statusLabel(demand.demandStatus)"
                  :icon-only="true"
                  size="sm"
                />
                <span>{{ demand.campusName || '主校区' }}</span>
                <template v-if="demand.tagName">
                  <span class="sep" aria-hidden="true">·</span>
                  <span>{{ demand.tagName }}</span>
                </template>
              </div>

              <h1 class="doc-title">{{ demand.title }}</h1>

              <!-- Compact Inline Publisher Line -->
              <div class="author-row">
                <div class="author-row__left">
                  <XyfAvatar :src="demand.publisherAvatar" :name="demand.publisherName || '校内同学'" :size="36" />
                  <div class="author-row__info">
                    <div class="author-row__name">
                      <strong>{{ demand.publisherName || '校内实名同学' }}</strong>
                      <XyfVerifiedMark
                        :verified="demand.publisherEduVerified === 1"
                        :credit-score="demand.publisherCreditScore ?? 100"
                      />
                    </div>
                    <div class="author-row__time xyf-tabular">
                      发布于 {{ formatDateTimeMinute(demand.createTime) }}
                    </div>
                  </div>
                </div>

                <el-button
                  v-if="userStore.userInfo && !isPublisher && !userStore.isAdmin"
                  size="small"
                  plain
                  @click="router.push(`/chat/${demand.publisherId}`)"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  私信
                </el-button>
              </div>
            </header>

            <!-- Clean Hairline Key Specs Strip (animated SVG anchored values) -->
            <section class="key-specs xyf-tabular" aria-label="关键信息">
              <div class="key-spec">
                <span>交付方式</span>
                <strong class="spec-val">
                  <XyfDeliveryIcon
                    :trade-type="demand.tradeType"
                    size="sm"
                    :text="demand.tradeType === 1 ? '校内当面核销' : '线上凭证担保'"
                  />
                </strong>
              </div>
              <div class="key-spec">
                <span>期望截止</span>
                <strong class="spec-val">
                  <el-icon><Clock /></el-icon>
                  <span>{{ demand.deadlineTime ? formatDateTimeMinute(demand.deadlineTime) : '协商确定' }}</span>
                </strong>
              </div>
              <div class="key-spec">
                <span>已收报价</span>
                <strong class="spec-val">
                  <el-icon><Document /></el-icon>
                  <span>{{ demand.bids?.length || 0 }} 份方案</span>
                </strong>
              </div>
            </section>

            <!-- Acceptance Criteria -->
            <section class="doc-section">
              <h2 class="section-label">需求详情与验收标准</h2>
              <p class="prose-text">{{ demand.description }}</p>
            </section>

            <!-- Campus Geofence Map (when offline) -->
            <section v-if="demand.tradeType === 1 && demand.meetupLat && demand.meetupLng" class="doc-section">
              <div class="map-header">
                <div>
                  <h2 class="section-label section-label--icon">
                    <el-icon><Location /></el-icon>
                    <span>交接地点 · {{ demand.meetupPoiName }}</span>
                  </h2>
                  <p v-if="demand.meetupAddress" class="map-sub">{{ demand.meetupAddress }}</p>
                </div>
                <span class="map-radius xyf-tabular">核销半径 {{ demand.meetupRadiusMeters || 80 }}m</span>
              </div>

              <div class="map-frame">
                <RealCampusMap
                  :height="220"
                  :interactive="false"
                  :latitude="Number(demand.meetupLat)"
                  :longitude="Number(demand.meetupLng)"
                  :radius-meters="demand.meetupRadiusMeters || 80"
                  :center-lat="Number(demand.meetupLat)"
                  :center-lng="Number(demand.meetupLng)"
                  :fence-valid="true"
                />
              </div>
            </section>

            <!-- Creator Proposals (clean hairline-separated list) -->
            <section class="doc-section doc-section--proposals">
              <div class="proposals-head">
                <h2 class="section-label" style="margin: 0">
                  创作者报价方案（{{ demand.bids?.length || 0 }}）
                </h2>

                <div v-if="(demand.bids?.length || 0) > 1" class="xyf-segmented">
                  <button
                    type="button"
                    class="xyf-segmented__btn"
                    :class="{ 'is-active': bidSort === 'default' }"
                    @click="bidSort = 'default'"
                  >
                    综合
                  </button>
                  <button
                    type="button"
                    class="xyf-segmented__btn"
                    :class="{ 'is-active': bidSort === 'price_asc' }"
                    @click="bidSort = 'price_asc'"
                  >
                    报价最低
                  </button>
                  <button
                    type="button"
                    class="xyf-segmented__btn"
                    :class="{ 'is-active': bidSort === 'hours_asc' }"
                    @click="bidSort = 'hours_asc'"
                  >
                    交付最快
                  </button>
                </div>
              </div>

              <div v-if="!sortedBids.length" class="empty-proposals">
                <XyfEmptyState
                  title="暂未收到创作者报价"
                  description="认证创作者可在右侧填写报价金额与履约方案参与竞标。"
                />
              </div>

              <div v-else class="proposal-list">
                <article
                  v-for="bid in sortedBids"
                  :key="bid.id"
                  class="proposal-item"
                  :class="{ 'is-winner': bid.bidStatus === 1 }"
                >
                  <div v-if="bid.bidStatus === 1" class="winner-note">
                    <XyfStatusBadge tone="success" icon="check" label="已采纳中标 · 已转担保订单" size="xs" />
                  </div>

                  <div class="proposal-item__head">
                    <button
                      type="button"
                      class="bidder-link"
                      @click="router.push(`/creators/${bid.bidderId}`)"
                    >
                      <XyfAvatar :src="bid.bidderAvatar" :name="bid.bidderName || '创作者'" :size="38" />
                      <div class="bidder-link__info">
                        <div class="bidder-link__name">
                          <strong>{{ bid.bidderName || `认证创作者 #${bid.bidderId}` }}</strong>
                          <XyfVerifiedMark
                            :verified="bid.bidderEduVerified === 1"
                            :credit-score="bid.bidderCreditScore ?? 100"
                          />
                        </div>
                        <span class="xyf-tabular">
                          {{ bid.bidderProfessional || '校内认证创作者' }}
                        </span>
                      </div>
                    </button>

                    <div class="quote-summary xyf-tabular">
                      <strong>¥{{ Number(bid.bidPrice).toFixed(2) }}</strong>
                      <div class="quote-summary__sub">
                        <span :class="budgetDeltaClass(bid.bidPrice)">{{ budgetDeltaText(bid.bidPrice) }}</span>
                        <span class="sep" aria-hidden="true">·</span>
                        <span class="eta-inline">
                          <el-icon><Clock /></el-icon>
                          <span>{{ bid.estimatedHours }}h</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  <p class="proposal-item__plan">{{ bid.bidPlan }}</p>

                  <div class="proposal-item__foot">
                    <span class="proposal-time xyf-tabular">{{ formatDateTimeMinute(bid.createTime) }}</span>

                    <div class="proposal-actions">
                      <el-button
                        v-if="userStore.userInfo && userStore.userInfo.id !== bid.bidderId && !userStore.isAdmin"
                        size="small"
                        plain
                        @click="router.push(`/chat/${bid.bidderId}`)"
                      >
                        私信沟通
                      </el-button>

                      <el-button
                        v-if="isPublisher && demand.demandStatus === 0 && bid.bidStatus === 0"
                        type="primary"
                        size="small"
                        :loading="acceptingId === bid.id"
                        @click="handleAcceptBid(bid)"
                      >
                        采纳此方案
                      </el-button>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </main>

          <!-- Right Single Sticky Action Card -->
          <aside class="detail-aside">
            <section class="action-card">
              <div class="action-card__price xyf-tabular">
                <span class="action-card__label">参考预算</span>
                <div class="action-card__range">
                  ¥{{ Number(demand.budgetMin).toFixed(0) }}<span>–</span>¥{{ Number(demand.budgetMax).toFixed(0) }}
                </div>
              </div>

              <!-- Visual Budget Thermometer when Creator is Bidding -->
              <div
                v-if="!isPublisher && demand.demandStatus === 0 && userStore.hasPublishPermission && !hasAlreadyBid"
                class="budget-meter xyf-tabular"
              >
                <div class="budget-meter__track">
                  <div class="budget-meter__pin" :style="{ left: `${bidPinPercent}%` }" />
                </div>
                <div class="budget-meter__labels">
                  <span>¥{{ demand.budgetMin }}</span>
                  <span class="is-mid">当前报价 ¥{{ bidForm.bidPrice }}</span>
                  <span>¥{{ demand.budgetMax }}</span>
                </div>
              </div>

              <!-- Publisher Self Controls -->
              <template v-if="isPublisher">
                <div class="console-block">
                  <p>
                    您是本条悬赏的发布方，可在左侧对比报价方案并选标，确认后款项进入平台担保托管。
                  </p>
                  <el-button
                    v-if="demand.demandStatus === 0"
                    type="danger"
                    plain
                    style="width: 100%"
                    @click="handleCancelDemand"
                  >
                    撤回并关闭悬赏
                  </el-button>
                </div>
              </template>

              <!-- Creator Bid Submission Form -->
              <template v-else-if="demand.demandStatus === 0">
                <div v-if="!userStore.userInfo" class="console-block">
                  <p>登录后，认证创作者可在线提交报价与履约方案。</p>
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%"
                    @click="router.push(`/login?redirect=/demands/${demand.id}`)"
                  >
                    登录参与竞标
                  </el-button>
                </div>

                <div v-else-if="userStore.isAdmin" class="console-block">
                  <p>当前为管理员巡检视角，如需处理请前往后台悬赏管理。</p>
                  <el-button plain style="width: 100%" @click="router.push('/admin/demands')">
                    前往后台管理
                  </el-button>
                </div>

                <div v-else-if="!userStore.hasPublishPermission" class="console-block">
                  <p>参与悬赏竞标需先完成创作者发布权限认证。</p>
                  <el-button
                    plain
                    size="large"
                    style="width: 100%"
                    @click="router.push('/user/center?tab=permission')"
                  >
                    申请创作者认证
                  </el-button>
                </div>

                <div v-else-if="hasAlreadyBid" class="submitted-note">
                  <strong>已提交竞标报价</strong>
                  <p>需求方选标后将自动生成担保订单并通知您。</p>
                </div>

                <el-form v-else label-position="top" class="bid-form">
                  <el-form-item label="我的报价（元）" required>
                    <el-input-number
                      v-model="bidForm.bidPrice"
                      :min="1"
                      :max="9999"
                      :precision="2"
                      :step="5"
                      style="width: 100%"
                    />
                  </el-form-item>

                  <el-form-item label="承诺耗时（小时）" required>
                    <div class="eta-presets">
                      <button
                        v-for="h in [2, 6, 12, 24, 48]"
                        :key="h"
                        type="button"
                        class="eta-chip xyf-tabular"
                        :class="{ 'is-active': bidForm.estimatedHours === h }"
                        @click="bidForm.estimatedHours = h"
                      >
                        {{ h }}h
                      </button>
                    </div>
                    <el-input-number
                      v-model="bidForm.estimatedHours"
                      :min="1"
                      :max="720"
                      style="width: 100%; margin-top: 8px"
                    />
                  </el-form-item>

                  <el-form-item label="履约方案说明" required>
                    <el-input
                      v-model="bidForm.bidPlan"
                      type="textarea"
                      :rows="4"
                      maxlength="500"
                      show-word-limit
                      placeholder="简述相关经验、可配合时间与交付保障（至少6字）"
                    />
                  </el-form-item>

                  <el-button
                    type="primary"
                    size="large"
                    :loading="submittingBid"
                    style="width: 100%"
                    @click="handleSubmitBid"
                  >
                    提交竞标方案
                  </el-button>
                </el-form>
              </template>

              <p class="escrow-footnote">
                选标后款项由平台担保托管，支持里程碑分期或现场动态码核销，验收无误后放款。
              </p>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, ChatDotRound, Clock, Location, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getDemandDetail,
  submitDemandBid,
  acceptDemandBid,
  cancelDemand
} from '@/api/demand'
import { notifyFallbackError } from '@/api/request'
import { formatDateTimeMinute } from '@/utils/format'
import type { DemandItem, DemandBidItem } from '@/types/demand'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfUrgencyTag from '@/components/ui/XyfUrgencyTag.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfVerifiedMark from '@/components/ui/XyfVerifiedMark.vue'
import XyfDeliveryIcon from '@/components/ui/XyfDeliveryIcon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const demandId = computed(() => Number(route.params.id))
const loading = ref(true)
const demand = ref<DemandItem | null>(null)
const submittingBid = ref(false)
const acceptingId = ref<number | null>(null)
const bidSort = ref<'default' | 'price_asc' | 'hours_asc'>('default')

const lifecycleSteps = [
  { title: '发布悬赏' },
  { title: '行家报价' },
  { title: '选标下单' },
  { title: '验收结算' }
]

const bidForm = reactive({
  bidPrice: 80,
  estimatedHours: 12,
  bidPlan: ''
})

const isPublisher = computed(
  () => Boolean(userStore.userInfo?.id && demand.value?.publisherId === userStore.userInfo.id)
)

const hasAlreadyBid = computed(() =>
  Boolean(
    userStore.userInfo?.id &&
      demand.value?.bids?.some(b => b.bidderId === userStore.userInfo?.id)
  )
)

const isWinningBidder = computed(() =>
  Boolean(
    userStore.userInfo?.id &&
      demand.value?.bids?.some(b => b.bidderId === userStore.userInfo?.id && b.bidStatus === 1)
  )
)

const lifecycleIndex = computed(() => {
  if (!demand.value) return 0
  if (demand.value.demandStatus === 1) return 2
  if ((demand.value.bids?.length || 0) > 0) return 1
  return 0
})

const sortedBids = computed(() => {
  const list = [...(demand.value?.bids || [])]
  return list.sort((a, b) => {
    if (a.bidStatus === 1 && b.bidStatus !== 1) return -1
    if (b.bidStatus === 1 && a.bidStatus !== 1) return 1
    if (bidSort.value === 'price_asc') return Number(a.bidPrice) - Number(b.bidPrice)
    if (bidSort.value === 'hours_asc') return Number(a.estimatedHours) - Number(b.estimatedHours)
    return (b.bidderCreditScore ?? 100) - (a.bidderCreditScore ?? 100)
  })
})

const bidPinPercent = computed(() => {
  if (!demand.value) return 50
  const min = Number(demand.value.budgetMin || 0)
  const max = Math.max(min + 1, Number(demand.value.budgetMax || min + 1))
  const pct = ((Number(bidForm.bidPrice) - min) / (max - min)) * 100
  return Math.min(96, Math.max(4, Math.round(pct)))
})

function budgetDeltaText(price: number) {
  if (!demand.value) return '标准报价'
  const p = Number(price)
  const min = Number(demand.value.budgetMin)
  const max = Number(demand.value.budgetMax)
  if (p < min) return `低于下限 ¥${(min - p).toFixed(0)}`
  if (p <= max) return '契合预算'
  return `超出上限 ¥${(p - max).toFixed(0)}`
}

function budgetDeltaClass(price: number) {
  if (!demand.value) return ''
  const p = Number(price)
  if (p <= Number(demand.value.budgetMax)) return 'is-in-budget'
  return 'is-over-budget'
}

function statusLabel(st: number) {
  if (st === 0) return '招募竞标中'
  if (st === 1) return '已选标成单'
  if (st === 2) return '已撤回'
  return '已关闭'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getDemandDetail(demandId.value)
    demand.value = res
    const mid = Math.round((Number(res.budgetMin || 50) + Number(res.budgetMax || 100)) / 2)
    bidForm.bidPrice = mid
  } catch (err) {
    notifyFallbackError(err, '加载悬赏详情失败')
    demand.value = null
  } finally {
    loading.value = false
  }
}

async function handleSubmitBid() {
  if (!demand.value) return
  if (!bidForm.bidPlan.trim() || bidForm.bidPlan.trim().length < 6) {
    ElMessage.warning('请填写至少 6 个字的履约方案说明')
    return
  }
  submittingBid.value = true
  try {
    await submitDemandBid(demand.value.id, {
      bidPrice: bidForm.bidPrice,
      estimatedHours: bidForm.estimatedHours,
      bidPlan: bidForm.bidPlan.trim()
    })
    ElMessage.success('竞标方案已提交，需求方选标后将第一时间通知您')
    bidForm.bidPlan = ''
    await loadDetail()
  } catch (err) {
    notifyFallbackError(err, '提交报价失败')
  } finally {
    submittingBid.value = false
  }
}

async function handleAcceptBid(bid: DemandBidItem) {
  if (!demand.value) return
  try {
    await ElMessageBox.confirm(
      `确认采纳「${bid.bidderName || '该创作者'}」的报价 ¥${Number(bid.bidPrice).toFixed(2)} 吗？确认后将自动为您生成平台担保订单并跳转至订单页完成托管支付。`,
      '确认选标并生成担保订单',
      { confirmButtonText: '确认选标锁单', cancelButtonText: '再对比一下', type: 'success' }
    )
  } catch {
    return
  }

  acceptingId.value = bid.id
  try {
    const orderId = await acceptDemandBid(demand.value.id, bid.id)
    ElMessage.success('选标成功！已自动生成校内担保订单，请完成托管支付')
    router.push(`/orders/${orderId}`)
  } catch (err) {
    notifyFallbackError(err, '选标生成订单失败')
  } finally {
    acceptingId.value = null
  }
}

async function handleCancelDemand() {
  if (!demand.value) return
  try {
    const res = await ElMessageBox.prompt('可选填撤回原因', '撤回悬赏需求', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：已自行解决或时间计划调整'
    })
    await cancelDemand(demand.value.id, res.value)
    ElMessage.success('已撤回该悬赏需求')
    await loadDetail()
  } catch {
    // cancelled
  }
}

watch(demandId, (newId, oldId) => {
  if (newId && !Number.isNaN(newId) && newId !== oldId) {
    loadDetail()
  }
})

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 18px;
  margin-bottom: 28px;
  border-bottom: 1px solid var(--xyf-border);

  &__left {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  padding: 0;
  color: var(--xyf-ink-2);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    color: var(--xyf-ink);
  }
}

.serial-text {
  font-size: 12.5px;
  color: var(--xyf-ink-3);
}

.sep {
  color: var(--xyf-ink-4);
}

/* Slim Unboxed Inline Stepper */
.inline-stepper {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  &__item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12.5px;
    color: var(--xyf-ink-4);

    &.is-done {
      color: var(--xyf-ink-2);
    }

    &.is-current {
      color: var(--xyf-ink);
      font-weight: 600;
    }
  }

  &__num {
    font-size: 11.5px;
  }

  &__arrow {
    margin-left: 3px;
    color: var(--xyf-border-strong);
  }
}

.loading-wrap,
.empty-wrap {
  padding: 40px 0;
}

.matched-bar {
  margin-bottom: 28px;
  padding: 16px 22px;
  border-radius: 12px;
  background: #18181b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  &__info {
    display: flex;
    align-items: baseline;
    gap: 12px;
    flex-wrap: wrap;

    strong {
      font-size: 15px;
    }

    span {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.72);
    }
  }
}

/* Open Editorial 2-Column Layout */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) 360px;
  gap: 48px;
  align-items: start;

  @media (max-width: 980px) {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

.detail-main {
  min-width: 0;
}

.doc-header {
  padding-bottom: 24px;
}

.doc-kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xyf-ink-3);
  margin-bottom: 10px;
}

.doc-title {
  margin: 0 0 20px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
  text-wrap: balance;
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    strong {
      font-size: 14px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  &__time {
    margin-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-4);
  }
}

/* Hairline Key Specs (Zero Cards) */
.key-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
  border-top: 1px solid var(--xyf-border);
  border-bottom: 1px solid var(--xyf-border);

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

.key-spec {
  display: flex;
  flex-direction: column;
  gap: 4px;

  > span {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .spec-val {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 14.5px;
    font-weight: 600;
    color: var(--xyf-ink);

    .el-icon {
      font-size: 14px;
      color: var(--xyf-ink-3);
      flex-shrink: 0;
    }
  }
}

.doc-section {
  padding: 28px 0;
  border-bottom: 1px solid var(--xyf-border);

  &--proposals {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.section-label {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--xyf-ink);

  &--icon {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .el-icon {
      font-size: 15px;
      color: var(--xyf-ink-2);
    }
  }
}

.prose-text {
  margin: 0;
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.8;
  color: var(--xyf-ink-2);
  max-width: 72ch;
}

.map-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  .section-label {
    margin-bottom: 4px;
  }
}

.map-sub {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-3);
}

.map-radius {
  font-size: 12.5px;
  color: var(--xyf-ink-3);
}

.map-frame {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--xyf-border);
}

/* Proposals List */
.proposals-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.empty-proposals {
  padding: 24px 0;
}

.proposal-list {
  display: flex;
  flex-direction: column;
}

.proposal-item {
  padding: 20px 0;
  border-top: 1px solid var(--xyf-border);

  &.is-winner {
    padding: 20px;
    margin: 0 -20px;
    border-radius: 12px;
    background: var(--xyf-champagne-soft);
    border-top: none;
  }

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }

  &__plan {
    margin: 14px 0 0;
    white-space: pre-wrap;
    font-size: 14px;
    line-height: 1.7;
    color: var(--xyf-ink-2);
  }

  &__foot {
    margin-top: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.winner-note {
  margin-bottom: 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--xyf-warning);
}

.bidder-link {
  display: flex;
  align-items: center;
  gap: 11px;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  font-family: inherit;
  cursor: pointer;

  &:hover strong {
    text-decoration: underline;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 3px;

    > span {
      font-size: 12.5px;
      color: var(--xyf-ink-3);
    }
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 7px;
    flex-wrap: wrap;

    strong {
      font-size: 14.5px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }
}

.quote-summary {
  text-align: right;

  strong {
    display: block;
    font-size: 20px;
    font-weight: 700;
    color: var(--xyf-ink);
    line-height: 1.15;
  }

  &__sub {
    margin-top: 4px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--xyf-ink-3);

    .is-in-budget {
      color: var(--xyf-success);
      font-weight: 600;
    }

    .is-over-budget {
      color: var(--xyf-warning);
      font-weight: 600;
    }

    .eta-inline {
      display: inline-flex;
      align-items: center;
      gap: 3px;

      .el-icon {
        font-size: 12px;
      }
    }
  }
}

.proposal-time {
  font-size: 12px;
  color: var(--xyf-ink-4);
}

.proposal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Right Single Sticky Decision Card */
.detail-aside {
  position: sticky;
  top: 88px;

  @media (max-width: 980px) {
    position: static;
  }
}

.action-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 14px;
  padding: 24px;

  &__price {
    padding-bottom: 18px;
    margin-bottom: 18px;
    border-bottom: 1px solid var(--xyf-border-subtle);
  }

  &__label {
    display: block;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  &__range {
    margin-top: 6px;
    font-size: 30px;
    font-weight: 700;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
    line-height: 1.1;

    span {
      color: var(--xyf-ink-4);
      font-weight: 400;
      margin: 0 4px;
    }
  }
}

.budget-meter {
  margin-bottom: 18px;

  &__track {
    position: relative;
    height: 4px;
    border-radius: 999px;
    background: var(--xyf-surface-muted);
    margin: 8px 0 10px;
  }

  &__pin {
    position: absolute;
    top: 50%;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #18181b;
    border: 2px solid #ffffff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
    transform: translate(-50%, -50%);
    transition: left 0.16s ease;
  }

  &__labels {
    display: flex;
    justify-content: space-between;
    font-size: 11.5px;
    color: var(--xyf-ink-3);

    .is-mid {
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }
}

.eta-presets {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  width: 100%;
}

.eta-chip {
  height: 30px;
  border-radius: 6px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.16s ease;

  &.is-active {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

.console-block {
  display: flex;
  flex-direction: column;
  gap: 14px;

  p {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.6;
    color: var(--xyf-ink-2);
  }
}

.submitted-note {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);

  strong {
    display: block;
    font-size: 14px;
    color: var(--xyf-ink);
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    font-size: 12.5px;
    line-height: 1.55;
    color: var(--xyf-ink-2);
  }
}

.escrow-footnote {
  margin: 18px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--xyf-border-subtle);
  font-size: 12px;
  line-height: 1.6;
  color: var(--xyf-ink-3);
}
</style>
