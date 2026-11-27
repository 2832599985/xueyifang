<template>
  <div class="demand-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="悬赏需求与创作者竞标"
        subtitle="发布者选标后自动锁定真实地图围栏并转为平台担保订单"
        :show-back="true"
        back-text="返回悬赏大厅"
        back-to="/demands"
      />

      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!demand" class="xyf-surface-card" style="padding: 40px">
        <XyfEmptyState title="未找到该悬赏需求" description="可能已被撤回或删除" />
      </div>

      <div v-else class="detail-grid">
        <!-- Left Main Column -->
        <div class="detail-main">
          <section class="xyf-surface-card spec-card">
            <div class="top-bar">
              <div class="pill-group">
                <span class="urgency-badge" :class="`is-u${demand.urgencyLevel}`">
                  {{ demand.urgencyLevel === 3 ? '十万火急' : demand.urgencyLevel === 2 ? '加急悬赏' : '常规悬赏' }}
                </span>
                <span class="campus-badge">{{ demand.campusName || '主校区' }}</span>
                <span v-if="demand.tagName" class="campus-badge">{{ demand.tagName }}</span>
              </div>
              <span class="xyf-mono demand-no">{{ demand.demandNo }}</span>
            </div>

            <h1>{{ demand.title }}</h1>

            <div class="publisher-row">
              <XyfAvatar :src="demand.publisherAvatar" :name="demand.publisherName" :size="36" />
              <div>
                <div class="pub-name">
                  {{ demand.publisherName || '校内同学' }}
                  <span class="verify-chip">
                    {{ demand.publisherEduVerified === 1 ? 'edu.cn 已核验' : '校内注册' }} · 信用 {{ demand.publisherCreditScore ?? 100 }}
                  </span>
                </div>
                <div class="pub-time">
                  发布于 {{ formatDateTime(demand.createTime) }}
                  <span v-if="demand.deadlineTime"> · 期望最晚交付：{{ formatDateTime(demand.deadlineTime) }}</span>
                </div>
              </div>
            </div>

            <div class="desc-block">
              <h3>需求详情与验收标准</h3>
              <p>{{ demand.description }}</p>
            </div>

            <!-- Real Campus Map Preview when Offline -->
            <div v-if="demand.tradeType === 1 && demand.meetupLat && demand.meetupLng" class="map-section">
              <div class="map-head">
                <strong>线下约定交接点：{{ demand.meetupPoiName }}</strong>
                <span class="xyf-mono">
                  有效当面核销范围：{{ demand.meetupRadiusMeters || 80 }} 米内
                </span>
              </div>
              <p v-if="demand.meetupAddress" class="map-sub">{{ demand.meetupAddress }}</p>
              <RealCampusMap
                :height="240"
                :interactive="false"
                :latitude="Number(demand.meetupLat)"
                :longitude="Number(demand.meetupLng)"
                :radius-meters="demand.meetupRadiusMeters || 80"
                :center-lat="Number(demand.meetupLat)"
                :center-lng="Number(demand.meetupLng)"
                :fence-valid="true"
              />
            </div>

            <!-- Matched Order Banner -->
            <div v-if="demand.demandStatus === 1 && demand.matchedOrderId" class="matched-banner">
              <div>
                <strong>该悬赏已完成选标并生成平台担保订单</strong>
                <p>买卖双方可在订单详情页完成担保付款、动态核销码/蓝牙当面交接与分账结算。</p>
              </div>
              <el-button
                v-if="isPublisher || isWinningBidder"
                type="primary"
                @click="router.push(`/orders/${demand.matchedOrderId}`)"
              >
                前往担保订单详情
              </el-button>
            </div>
          </section>

          <!-- Bids List -->
          <section class="xyf-surface-card bids-card">
            <div class="xyf-section-heading">
              <h2>创作者竞标方案（{{ demand.bids?.length || 0 }}）</h2>
            </div>

            <div v-if="!demand.bids || demand.bids.length === 0" class="empty-bids">
              暂未收到报价方案，认证创作者可在右侧填写报价与履约计划抢先接单。
            </div>

            <div v-else class="bid-list">
              <div
                v-for="bid in demand.bids"
                :key="bid.id"
                class="bid-item"
                :class="{ 'is-winner': bid.bidStatus === 1 }"
              >
                <div class="bid-item__top">
                  <div class="bidder-info" @click="router.push(`/creators/${bid.bidderId}`)">
                    <XyfAvatar :src="bid.bidderAvatar" :name="bid.bidderName" :size="40" />
                    <div>
                      <div class="b-name">
                        <span>{{ bid.bidderName || `创作者 #${bid.bidderId}` }}</span>
                        <span class="b-credit">
                          {{ bid.bidderEduVerified === 1 ? '教育邮箱已核验' : '认证创作者' }} · 信用 {{ bid.bidderCreditScore ?? 100 }}
                        </span>
                      </div>
                      <div class="b-prof">
                        {{ bid.bidderProfessional || '校内认证创作者' }} · 点击查看数字信用主页
                      </div>
                    </div>
                  </div>

                  <div class="bid-quote xyf-tabular">
                    <strong>¥{{ Number(bid.bidPrice).toFixed(2) }}</strong>
                    <span>承诺 {{ bid.estimatedHours }} 小时完成</span>
                  </div>
                </div>

                <p class="bid-plan">{{ bid.bidPlan }}</p>

                <div class="bid-item__foot">
                  <span class="bid-time">{{ formatDateTime(bid.createTime) }}</span>
                  <div class="bid-actions">
                    <el-tag v-if="bid.bidStatus === 1" type="success" effect="dark">已中标采纳</el-tag>
                    <el-button
                      v-else-if="isPublisher && demand.demandStatus === 0"
                      type="primary"
                      size="small"
                      :loading="acceptingId === bid.id"
                      @click="handleAcceptBid(bid)"
                    >
                      采纳此方案并生成担保订单
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- Right Sticky Action Column -->
        <aside class="detail-aside">
          <section class="xyf-surface-card action-box">
            <div class="budget-hero xyf-tabular">
              <span>需求方预算区间</span>
              <strong>¥{{ Number(demand.budgetMin).toFixed(0) }} - ¥{{ Number(demand.budgetMax).toFixed(0) }}</strong>
            </div>

            <div class="meta-rows">
              <div class="m-row">
                <span>交付形式</span>
                <strong>{{ demand.tradeType === 1 ? '校园线下当面' : '线上远程交付' }}</strong>
              </div>
              <div class="m-row">
                <span>当前状态</span>
                <strong>{{ statusLabel(demand.demandStatus) }}</strong>
              </div>
            </div>

            <!-- Publisher Controls -->
            <template v-if="isPublisher">
              <p class="hint-box">
                这是您发布的悬赏需求。收到满意报价后点击左侧「采纳此方案」，系统将自动创建担保订单。
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
            </template>

            <!-- Creator Bid Form -->
            <template v-else-if="demand.demandStatus === 0">
              <div v-if="!userStore.userInfo" class="login-cta">
                <p>登录后可使用认证创作者身份提交竞标报价</p>
                <el-button type="primary" style="width: 100%" @click="router.push(`/login?redirect=/demands/${demand.id}`)">
                  登录参与竞标
                </el-button>
              </div>

              <div v-else-if="!userStore.hasPublishPermission" class="login-cta">
                <p>参与悬赏竞标需先开通「技能发布权限认证」</p>
                <el-button plain style="width: 100%" @click="router.push('/user/center?tab=permission')">
                  前往申请创作者认证
                </el-button>
              </div>

              <div v-else-if="hasAlreadyBid" class="hint-box is-ok">
                您已提交竞标方案，请耐心等待需求方选标，中标后将收到站内实时通知。
              </div>

              <el-form v-else label-position="top" class="bid-form">
                <h3>提交创作者竞标报价</h3>
                <el-form-item label="我的报价金额（元）" required>
                  <el-input-number v-model="bidForm.bidPrice" :min="1" :max="9999" :precision="2" style="width: 100%" />
                </el-form-item>
                <el-form-item label="预计完成耗时（小时）" required>
                  <el-input-number v-model="bidForm.estimatedHours" :min="1" :max="720" style="width: 100%" />
                </el-form-item>
                <el-form-item label="履约方案与优势说明" required>
                  <el-input
                    v-model="bidForm.bidPlan"
                    type="textarea"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    placeholder="简述您的相关课程成绩/实战经验、可会面时段及交付保障..."
                  />
                </el-form-item>
                <el-button type="primary" size="large" :loading="submittingBid" style="width: 100%" @click="handleSubmitBid">
                  立即提交竞标方案
                </el-button>
              </el-form>
            </template>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
    bidForm.bidPrice = Number(res.budgetMin || 60)
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
    ElMessage.success('竞标报价已提交，需求方选标后将第一时间通知您')
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
      `确认采纳「${bid.bidderName || '该创作者'}」的报价 ¥${Number(bid.bidPrice).toFixed(2)} 吗？确认后将自动为您生成担保订单并跳转至订单页完成托管支付。`,
      '确认选标并生成担保订单',
      { confirmButtonText: '确认选标', cancelButtonText: '再看看', type: 'success' }
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
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.85fr);
  gap: 22px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.spec-card,
.bids-card,
.action-box {
  padding: 24px 26px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.pill-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.urgency-badge,
.campus-badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.urgency-badge {
  &.is-u3 {
    background: #fef2f2;
    color: #dc2626;
  }
  &.is-u2 {
    background: #fffbeb;
    color: #d97706;
  }
  &.is-u1 {
    background: #f4f4f5;
    color: #52525b;
  }
}

.campus-badge {
  background: #f4f4f5;
  color: #18181b;
}

.demand-no {
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

h1 {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 800;
  color: var(--xyf-ink);
}

.publisher-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border);

  .pub-name {
    font-size: 14px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .verify-chip {
    margin-left: 8px;
    font-size: 11.5px;
    font-weight: 600;
    color: #047857;
    background: #ecfdf5;
    padding: 2px 8px;
    border-radius: 999px;
  }

  .pub-time {
    margin-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.desc-block {
  margin-top: 18px;

  h3 {
    margin: 0 0 8px;
    font-size: 15px;
    font-weight: 700;
  }

  p {
    margin: 0;
    white-space: pre-wrap;
    font-size: 14px;
    line-height: 1.7;
    color: var(--xyf-ink-2);
  }
}

.map-section {
  margin-top: 20px;
  padding: 14px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  .map-head {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 6px;
    font-size: 13.5px;
  }

  .map-sub {
    margin: 0 0 10px;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.matched-banner {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 12px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;

  strong {
    color: #065f46;
  }

  p {
    margin: 4px 0 0;
    font-size: 12.5px;
    color: #047857;
  }
}

.bid-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bid-item {
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;

  &.is-winner {
    border-color: #10b981;
    background: #f0fdf4;
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
  }

  .bidder-info {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    .b-name {
      font-weight: 700;
      font-size: 14px;
    }

    .b-credit {
      margin-left: 6px;
      font-size: 11.5px;
      color: #047857;
      font-weight: 600;
    }

    .b-prof {
      font-size: 12px;
      color: var(--xyf-ink-muted);
    }
  }

  .bid-quote {
    text-align: right;

    strong {
      display: block;
      font-size: 19px;
      font-weight: 800;
      color: var(--xyf-ink);
    }

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-muted);
    }
  }

  .bid-plan {
    margin: 12px 0;
    font-size: 13.5px;
    line-height: 1.6;
    color: var(--xyf-ink-2);
    white-space: pre-wrap;
  }

  &__foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.empty-bids {
  padding: 24px 0;
  color: var(--xyf-ink-muted);
  font-size: 13.5px;
}

.detail-aside {
  position: sticky;
  top: 84px;
}

.budget-hero {
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--xyf-border);
  margin-bottom: 14px;

  span {
    display: block;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  strong {
    font-size: 28px;
    font-weight: 800;
    color: var(--xyf-ink);
  }
}

.meta-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;

  .m-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--xyf-ink-2);
  }
}

.hint-box {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--xyf-ink-2);
  margin-bottom: 14px;

  &.is-ok {
    background: #ecfdf5;
    color: #065f46;
  }
}

.bid-form h3 {
  margin: 0 0 12px;
  font-size: 15.5px;
  font-weight: 700;
}

.login-cta p {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--xyf-ink-2);
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .detail-aside {
    position: static;
  }
}
</style>
