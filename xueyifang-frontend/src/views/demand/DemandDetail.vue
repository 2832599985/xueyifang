<template>
  <div class="demand-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="悬赏详情"
        :show-back="true"
        back-text="返回悬赏大厅"
        back-to="/demands"
      >
        <template #actions v-if="demand">
          <span class="demand-serial xyf-mono">{{ demand.demandNo }}</span>
        </template>
      </XyfPageHeader>

      <div v-if="loading" class="xyf-surface-card" style="padding: 32px">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="!demand" class="xyf-surface-card" style="padding: 48px">
        <XyfEmptyState title="未找到该悬赏需求" description="该悬赏可能已被发布者撤回或关闭">
          <template #action>
            <el-button type="primary" @click="router.push('/demands')">返回悬赏大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- 4-Step Bounty Escrow Lifecycle Stepper -->
        <section class="xyf-surface-card lifecycle-bar">
          <div
            v-for="(step, idx) in lifecycleSteps"
            :key="step.title"
            class="lc-step"
            :class="{
              'is-done': lifecycleIndex > idx,
              'is-current': lifecycleIndex === idx
            }"
          >
            <span class="lc-step__num xyf-tabular">0{{ idx + 1 }}</span>
            <div class="lc-step__text">
              <strong>{{ step.title }}</strong>
              <span>{{ step.sub }}</span>
            </div>
          </div>
        </section>

        <!-- Matched Escrow Order Banner -->
        <section v-if="demand.demandStatus === 1 && demand.matchedOrderId" class="matched-escrow-bar">
          <div class="matched-escrow-bar__info">
            <span class="matched-badge">已成单</span>
            <div>
              <strong>已选标并生成担保订单</strong>
              <p>双方可在订单详情中完成支付与验收。</p>
            </div>
          </div>
          <el-button
            v-if="isPublisher || isWinningBidder"
            type="primary"
            size="large"
            @click="router.push(`/orders/${demand.matchedOrderId}`)"
          >
            查看订单
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </section>

        <div class="detail-grid">
          <!-- Left Main Column: Spec Sheet + Proposal Comparison Arena -->
          <div class="detail-main">
            <section class="xyf-surface-card spec-sheet">
              <div class="spec-sheet__top">
                <div class="tag-cluster">
                  <span class="urgency-pill" :class="`is-u${demand.urgencyLevel}`">
                    {{ demand.urgencyLevel === 3 ? '十万火急' : demand.urgencyLevel === 2 ? '加急优先' : '常规悬赏' }}
                  </span>
                  <span class="neutral-pill">{{ demand.campusName || '主校区' }}</span>
                  <span v-if="demand.tagName" class="neutral-pill">{{ demand.tagName }}</span>
                </div>

                <span class="status-seal" :class="{ 'is-open': demand.demandStatus === 0 }">
                  {{ statusLabel(demand.demandStatus) }}
                </span>
              </div>

              <h1 class="spec-title">{{ demand.title }}</h1>

              <!-- Publisher Trust Bar -->
              <div class="publisher-strip">
                <div class="publisher-strip__left">
                  <XyfAvatar :src="demand.publisherAvatar" :name="demand.publisherName || '校内同学'" :size="40" />
                  <div>
                    <div class="pub-name-line">
                      <strong>{{ demand.publisherName || '校内实名同学' }}</strong>
                      <span class="verify-pill">
                        {{ demand.publisherEduVerified === 1 ? '高校 .edu.cn 已核验' : '实名学籍' }}
                      </span>
                      <span class="verify-pill xyf-tabular">履约信用 {{ demand.publisherCreditScore ?? 100 }}</span>
                    </div>
                    <div class="pub-time xyf-tabular">
                      发布于 {{ formatDateTime(demand.createTime) }}
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
                  私信需求方
                </el-button>
              </div>

              <!-- 4-Tile Key Spec Bento -->
              <div class="spec-bento xyf-tabular">
                <div class="spec-tile">
                  <span>悬赏预算区间</span>
                  <strong>¥{{ Number(demand.budgetMin).toFixed(0) }} – ¥{{ Number(demand.budgetMax).toFixed(0) }}</strong>
                </div>
                <div class="spec-tile">
                  <span>交付与验收模式</span>
                  <strong>{{ demand.tradeType === 1 ? '校内当面核销' : '线上凭证担保' }}</strong>
                </div>
                <div class="spec-tile">
                  <span>当前收到报价</span>
                  <strong>{{ demand.bids?.length || 0 }} 份方案</strong>
                </div>
                <div class="spec-tile">
                  <span>期望截止时间</span>
                  <strong>{{ demand.deadlineTime ? formatDateTime(demand.deadlineTime).slice(0, 16) : '协商确定' }}</strong>
                </div>
              </div>

              <!-- Acceptance Criteria -->
              <div class="criteria-box">
                <div class="xyf-section-heading">
                  <h3>需求详情与验收标准</h3>
                </div>
                <p class="criteria-text">{{ demand.description }}</p>
              </div>

              <!-- Split Campus Map Card when Offline -->
              <div v-if="demand.tradeType === 1 && demand.meetupLat && demand.meetupLng" class="geofence-box">
                <div class="geofence-box__head">
                  <div>
                    <h4>交接地点：{{ demand.meetupPoiName }}</h4>
                    <p v-if="demand.meetupAddress">{{ demand.meetupAddress }}</p>
                  </div>
                  <span class="radius-chip xyf-tabular">
                    核销有效半径 {{ demand.meetupRadiusMeters || 80 }}m
                  </span>
                </div>

                <RealCampusMap
                  :height="230"
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

            <!-- Creator Proposals & Comparison Arena -->
            <section class="xyf-surface-card arena-card">
              <div class="arena-head">
                <div class="xyf-section-heading" style="margin: 0">
                  <h2>创作者竞标方案对比台（{{ demand.bids?.length || 0 }}）</h2>
                </div>

                <div v-if="(demand.bids?.length || 0) > 1" class="xyf-segmented">
                  <button
                    type="button"
                    class="xyf-segmented__btn"
                    :class="{ 'is-active': bidSort === 'default' }"
                    @click="bidSort = 'default'"
                  >
                    综合排序
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

              <div v-if="!sortedBids.length" class="empty-arena">
                <XyfEmptyState
                  title="暂未收到创作者报价方案"
                  description="认证创作者可在右侧竞标工作台填写报价金额、交付耗时与履约方案抢先接单。"
                />
              </div>

              <div v-else class="proposal-list">
                <article
                  v-for="bid in sortedBids"
                  :key="bid.id"
                  class="proposal-card"
                  :class="{ 'is-winner': bid.bidStatus === 1 }"
                >
                  <div v-if="bid.bidStatus === 1" class="winner-ribbon">
                    ✦ 需求方已采纳中标方案 · 已转为平台担保订单
                  </div>

                  <div class="proposal-card__top">
                    <div class="bidder-profile" @click="router.push(`/creators/${bid.bidderId}`)">
                      <XyfAvatar :src="bid.bidderAvatar" :name="bid.bidderName || '创作者'" :size="44" />
                      <div>
                        <div class="bidder-name-row">
                          <strong>{{ bid.bidderName || `认证创作者 #${bid.bidderId}` }}</strong>
                          <span class="trust-chip">
                            {{ bid.bidderEduVerified === 1 ? 'edu.cn 已核验' : '认证创作者' }}
                          </span>
                          <span class="trust-chip xyf-tabular">信用 {{ bid.bidderCreditScore ?? 100 }}</span>
                        </div>
                        <div class="bidder-sub">
                          {{ bid.bidderProfessional || '校内认证创作者' }} · 点击调阅公开数字信用档案
                        </div>
                      </div>
                    </div>

                    <div class="quote-block xyf-tabular">
                      <div class="quote-block__price-row">
                        <span class="budget-delta" :class="budgetDeltaClass(bid.bidPrice)">
                          {{ budgetDeltaText(bid.bidPrice) }}
                        </span>
                        <strong>¥{{ Number(bid.bidPrice).toFixed(2) }}</strong>
                      </div>
                      <span class="quote-block__eta">承诺 {{ bid.estimatedHours }} 小时内完成交付</span>
                    </div>
                  </div>

                  <div class="proposal-plan">
                    {{ bid.bidPlan }}
                  </div>

                  <div class="proposal-card__foot">
                    <span class="proposal-time xyf-tabular">报价于 {{ formatDateTime(bid.createTime) }}</span>

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
                        采纳此方案并生成担保单
                      </el-button>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <!-- Right Sticky Bidding & Escrow Studio -->
          <aside class="detail-aside">
            <section class="xyf-surface-card bid-studio">
              <div class="studio-price-head xyf-tabular">
                <span class="studio-kicker">需求方参考预算</span>
                <div class="studio-range">
                  ¥{{ Number(demand.budgetMin).toFixed(0) }}
                  <span>–</span>
                  ¥{{ Number(demand.budgetMax).toFixed(0) }}
                </div>
              </div>

              <!-- Visual Budget Thermometer when Creator is Bidding -->
              <div
                v-if="!isPublisher && demand.demandStatus === 0 && userStore.hasPublishPermission && !hasAlreadyBid"
                class="budget-meter xyf-tabular"
              >
                <div class="budget-meter__track">
                  <div class="budget-meter__zone" />
                  <div class="budget-meter__pin" :style="{ left: `${bidPinPercent}%` }" />
                </div>
                <div class="budget-meter__labels">
                  <span>下限 ¥{{ demand.budgetMin }}</span>
                  <span class="is-mid">当前报价 ¥{{ bidForm.bidPrice }}</span>
                  <span>上限 ¥{{ demand.budgetMax }}</span>
                </div>
              </div>

              <!-- Publisher Self Controls -->
              <template v-if="isPublisher">
                <div class="publisher-console">
                  <p>
                    您是本条悬赏的发布方。建议对比各创作者的「历史完单信用、承诺耗时与具体履约方案」，选标后款项将进入平台担保冻结池。
                  </p>
                  <el-button
                    v-if="demand.demandStatus === 0"
                    type="danger"
                    plain
                    style="width: 100%"
                    @click="handleCancelDemand"
                  >
                    撤回并关闭本条悬赏
                  </el-button>
                </div>
              </template>

              <!-- Creator Bid Submission Form -->
              <template v-else-if="demand.demandStatus === 0">
                <div v-if="!userStore.userInfo" class="gate-box">
                  <p>登录校园账号后，认证创作者可在线提交定制报价方案。</p>
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%"
                    @click="router.push(`/login?redirect=/demands/${demand.id}`)"
                  >
                    登录参与竞标
                  </el-button>
                </div>

                <div v-else-if="userStore.isAdmin" class="gate-box">
                  <p>当前为管理员巡检视角，如需违规下架请前往后台「反向悬赏治理」。</p>
                  <el-button plain style="width: 100%" @click="router.push('/admin/demands')">
                    前往后台悬赏巡检
                  </el-button>
                </div>

                <div v-else-if="!userStore.hasPublishPermission" class="gate-box">
                  <p>参与悬赏竞标接单需先完成「创作者发布权限认证」。</p>
                  <el-button
                    plain
                    size="large"
                    style="width: 100%"
                    @click="router.push('/user/center?tab=permission')"
                  >
                    前往申请创作者认证
                  </el-button>
                </div>

                <div v-else-if="hasAlreadyBid" class="submitted-banner">
                  <strong>✓ 您已提交竞标报价</strong>
                  <p>需求方采纳方案后将立即通过站内通知提醒您，并自动在「收到的销售单」生成担保订单。</p>
                </div>

                <el-form v-else label-position="top" class="bid-form">
                  <h3 class="bid-form__title">提交创作者竞标方案</h3>

                  <el-form-item label="我的报价金额（元）" required>
                    <el-input-number
                      v-model="bidForm.bidPrice"
                      :min="1"
                      :max="9999"
                      :precision="2"
                      :step="5"
                      style="width: 100%"
                    />
                  </el-form-item>

                  <el-form-item label="承诺完成耗时（小时）" required>
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

                  <el-form-item label="履约计划与个人优势说明" required>
                    <el-input
                      v-model="bidForm.bidPlan"
                      type="textarea"
                      :rows="4"
                      maxlength="500"
                      show-word-limit
                      placeholder="简述您的相关课程成绩/项目经验、可碰面时段及具体交付保障（至少6字）..."
                    />
                  </el-form-item>

                  <el-button
                    type="primary"
                    size="large"
                    :loading="submittingBid"
                    style="width: 100%"
                    @click="handleSubmitBid"
                  >
                    立即提交竞标方案
                  </el-button>
                </el-form>
              </template>

              <ul class="escrow-guarantees">
                <li>选标后买方支付款项进入平台担保冻结池</li>
                <li>支持拆解多阶段里程碑或现场 10s 动态码核销</li>
                <li>平台争议仲裁与预约爽约自动退赔双重护航</li>
              </ul>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getDemandDetail,
  submitDemandBid,
  acceptDemandBid,
  cancelDemand
} from '@/api/demand'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { DemandItem, DemandBidItem } from '@/types/demand'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

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
  { title: '发布悬赏', sub: '填写需求与预算' },
  { title: '行家报价', sub: '查看方案与工期' },
  { title: '选标下单', sub: '确认人选并支付担保' },
  { title: '验收结算', sub: '确认交付后放款' }
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
  // Winning bid always stays on top
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
  if (p <= max) return '契合预算区间'
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
  if (st === 1) return '已选标转担保单'
  if (st === 2) return '发布者已撤回'
  return '管理员已关闭'
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

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.demand-serial {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 12px;
  color: var(--xyf-ink-2);
}

/* 4-Step Lifecycle Bar */
.lifecycle-bar {
  padding: 18px 24px;
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 820px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.lc-step {
  display: flex;
  align-items: center;
  gap: 12px;
  opacity: 0.52;

  &__num {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    border: 1px solid var(--xyf-border-strong);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;
  }

  &__text {
    display: flex;
    flex-direction: column;

    strong {
      font-size: 13.5px;
      color: var(--xyf-ink);
    }

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }
  }

  &.is-done,
  &.is-current {
    opacity: 1;
  }

  &.is-done .lc-step__num {
    background: var(--xyf-surface-muted);
    border-color: var(--xyf-ink);
    color: var(--xyf-ink);
  }

  &.is-current .lc-step__num {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

/* Matched Escrow Banner */
.matched-escrow-bar {
  margin-bottom: 20px;
  padding: 20px 26px;
  border-radius: 16px;
  background: #18181b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;

  &__info {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    strong {
      display: block;
      font-size: 16px;
    }

    p {
      margin: 4px 0 0;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.7);
    }
  }

  .matched-badge {
    padding: 4px 10px;
    border-radius: 999px;
    background: rgba(200, 169, 126, 0.2);
    border: 1px solid rgba(200, 169, 126, 0.45);
    color: #e5c79e;
    font-size: 12px;
    font-weight: 650;
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.85fr);
  gap: 24px;
  align-items: start;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.spec-sheet,
.arena-card,
.bid-studio {
  padding: 26px 28px;
}

.spec-sheet__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.tag-cluster {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.urgency-pill,
.neutral-pill,
.status-seal {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.urgency-pill {
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);

  &.is-u3 {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #f7cece;
  }
  &.is-u2 {
    background: var(--xyf-champagne-soft);
    color: #9a5b13;
    border: 1px solid var(--xyf-champagne-border);
  }
}

.neutral-pill {
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  color: var(--xyf-ink-2);
}

.status-seal {
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);

  &.is-open {
    background: #18181b;
    color: #ffffff;
  }
}

.spec-title {
  margin: 0 0 18px;
  font-size: 24px;
  font-weight: 760;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
}

.publisher-strip {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .pub-name-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    strong {
      font-size: 14px;
      color: var(--xyf-ink);
    }
  }

  .verify-pill {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 999px;
    background: #ffffff;
    border: 1px solid var(--xyf-border);
    color: var(--xyf-ink-2);
  }

  .pub-time {
    margin-top: 3px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.spec-bento {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 22px;

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.spec-tile {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }

  strong {
    font-size: 14.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.criteria-box {
  .criteria-text {
    margin: 0;
    white-space: pre-wrap;
    font-size: 14.5px;
    line-height: 1.75;
    color: var(--xyf-ink-2);
  }
}

.geofence-box {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--xyf-border-subtle);

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 12px;

    h4 {
      margin: 4px 0 2px;
      font-size: 15px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    p {
      margin: 0;
      font-size: 12.5px;
      color: var(--xyf-ink-3);
    }
  }

  .radius-chip {
    padding: 4px 10px;
    border-radius: 999px;
    background: #18181b;
    color: #ffffff;
    font-size: 12px;
    font-weight: 600;
  }
}

/* Proposal Arena */
.arena-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.proposal-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.proposal-card {
  border-radius: 14px;
  border: 1px solid var(--xyf-border);
  padding: 18px 20px;
  background: #ffffff;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    box-shadow: var(--xyf-shadow-xs);
  }

  &.is-winner {
    border-color: #18181b;
    background: linear-gradient(180deg, #faf6ef 0%, #ffffff 42%);
  }

  .winner-ribbon {
    display: inline-block;
    margin-bottom: 12px;
    padding: 3px 10px;
    border-radius: 999px;
    background: #18181b;
    color: #e5c79e;
    font-size: 11.5px;
    font-weight: 650;
  }

  &__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 14px;
    flex-wrap: wrap;
  }

  &__foot {
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid var(--xyf-border-subtle);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.bidder-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;

  .bidder-name-row {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;

    strong {
      font-size: 15px;
      color: var(--xyf-ink);
    }
  }

  .trust-chip {
    padding: 1px 7px;
    border-radius: 999px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    font-size: 11px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }

  .bidder-sub {
    margin-top: 3px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.quote-block {
  text-align: right;

  &__price-row {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;

    strong {
      font-size: 22px;
      font-weight: 800;
      color: var(--xyf-ink);
    }
  }

  .budget-delta {
    font-size: 11px;
    font-weight: 650;
    padding: 2px 8px;
    border-radius: 999px;

    &.is-in-budget {
      background: #f2fbf5;
      color: #15803d;
      border: 1px solid #ccebd7;
    }
    &.is-over-budget {
      background: var(--xyf-champagne-soft);
      color: #9a5b13;
      border: 1px solid var(--xyf-champagne-border);
    }
  }

  &__eta {
    display: block;
    margin-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.proposal-plan {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--xyf-ink-2);
  white-space: pre-wrap;
}

.proposal-time {
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.proposal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Right Sticky Bid Studio */
.detail-aside {
  position: sticky;
  top: 86px;

  @media (max-width: 960px) {
    position: static;
  }
}

.studio-price-head {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border);

  .studio-kicker {
    display: block;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .studio-range {
    margin-top: 4px;
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);

    span {
      color: var(--xyf-ink-3);
      font-weight: 400;
      margin: 0 2px;
    }
  }
}

.budget-meter {
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);

  &__track {
    position: relative;
    height: 6px;
    border-radius: 999px;
    background: #e4e4e7;
    margin: 6px 0 10px;
  }

  &__zone {
    position: absolute;
    inset: 0;
    border-radius: 999px;
    background: linear-gradient(90deg, #d4d4d8 0%, #18181b 50%, #c8a97e 100%);
    opacity: 0.45;
  }

  &__pin {
    position: absolute;
    top: 50%;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #18181b;
    border: 2.5px solid #ffffff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
    transform: translate(-50%, -50%);
    transition: left 0.18s ease;
  }

  &__labels {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: var(--xyf-ink-3);

    .is-mid {
      font-weight: 700;
      color: var(--xyf-ink);
    }
  }
}

.bid-form__title {
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.eta-presets {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  width: 100%;
}

.eta-chip {
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &.is-active {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

.publisher-console,
.gate-box {
  display: flex;
  flex-direction: column;
  gap: 12px;

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: var(--xyf-ink-2);
  }
}

.submitted-banner {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-ink);

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

.escrow-guarantees {
  margin: 20px 0 0;
  padding: 16px 0 0 16px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--xyf-ink-3);
}
</style>
