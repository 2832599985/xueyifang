<template>
  <div class="group-detail-page xyf-page">
    <div class="xyf-container">
      <!-- Slim Unboxed Top Breadcrumb -->
      <nav class="detail-topbar">
        <div class="detail-topbar__left">
          <button type="button" class="back-link" @click="router.push('/groups')">
            <el-icon><ArrowLeft /></el-icon>
            <span>拼班团课</span>
          </button>
          <template v-if="session">
            <span class="sep" aria-hidden="true">/</span>
            <span class="serial-text xyf-mono">{{ session.sessionNo }}</span>
          </template>
        </div>
      </nav>

      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!session" class="empty-wrap">
        <XyfEmptyState title="未找到该拼班团课" description="课程可能已被取消或下架">
          <template #action>
            <el-button type="primary" @click="router.push('/groups')">返回团课大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <div class="detail-grid">
          <!-- Left Editorial Column: Leads immediately with Title & Course Essence -->
          <main class="detail-main">
            <header class="doc-header">
              <div class="doc-kicker">
                <XyfStatusBadge
                  :tone="statusTone(session.sessionStatus)"
                  :icon="statusIcon(session.sessionStatus)"
                  :tooltip="statusLabel(session.sessionStatus)"
                  :icon-only="true"
                  size="sm"
                />
                <XyfDeliveryIcon :trade-type="session.tradeType" size="sm" />
                <span v-if="session.tagName">{{ session.tagName }}</span>
                <span v-if="session.tagName" class="sep" aria-hidden="true">·</span>
                <span>{{ session.campusName || '主校区' }}</span>
              </div>

              <h1 class="doc-title">{{ session.title }}</h1>

              <!-- Compact Inline Instructor Row -->
              <div class="author-row">
                <button
                  type="button"
                  class="author-row__btn"
                  @click="router.push(`/creators/${session.creatorId}`)"
                >
                  <XyfAvatar :src="session.creatorAvatar" :name="session.creatorName || '讲师'" :size="38" />
                  <div class="author-row__info">
                    <div class="author-row__name">
                      <strong>{{ session.creatorName || '认证讲师' }}</strong>
                      <XyfVerifiedMark
                        :verified="session.creatorEduVerified === 1"
                        :credit-score="session.creatorCreditScore ?? 100"
                      />
                    </div>
                    <div class="author-row__sub">
                      {{ session.creatorProfessional || '校内认证技能讲师' }} · 查看主页
                    </div>
                  </div>
                </button>

                <el-button
                  v-if="userStore.userInfo && !isCreator && !userStore.isAdmin"
                  size="small"
                  plain
                  @click="router.push(`/chat/${session.creatorId}`)"
                >
                  咨询讲师
                </el-button>
              </div>
            </header>

            <!-- Clean Hairline Key Logistics Strip -->
            <section class="key-specs xyf-tabular" aria-label="课程安排">
              <div class="key-spec">
                <span>上课时间</span>
                <strong class="spec-val">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatSessionTimeRange(session.sessionStart, session.sessionEnd) }}</span>
                </strong>
              </div>
              <div class="key-spec">
                <span>{{ session.tradeType === 1 ? '授课地点' : '授课形式' }}</span>
                <strong class="spec-val">
                  <XyfDeliveryIcon
                    :trade-type="session.tradeType"
                    size="sm"
                    :text="
                      session.tradeType === 1
                        ? `${session.meetupPoiName || '校内指定教室'}（${session.meetupRadiusMeters || 80}m）`
                        : '线上实时共享课堂'
                    "
                  />
                </strong>
              </div>
              <div class="key-spec">
                <span>班额进度</span>
                <strong class="spec-val">
                  <el-icon><User /></el-icon>
                  <span>{{ session.joinedCount }} / {{ session.maxParticipants }} 人（{{ session.minParticipants }}人成班）</span>
                </strong>
              </div>
            </section>

            <!-- Syllabus Prose -->
            <section class="doc-section">
              <h2 class="section-label">课程大纲与配套资料</h2>
              <p class="prose-text">{{ session.description }}</p>
            </section>

            <!-- Campus Geofence Map (when offline) -->
            <section v-if="session.tradeType === 1 && session.meetupLat && session.meetupLng" class="doc-section">
              <div class="map-header">
                <div>
                  <h2 class="section-label section-label--icon">
                    <el-icon><Location /></el-icon>
                    <span>授课地点 · {{ session.meetupPoiName }}</span>
                  </h2>
                  <p v-if="session.meetupAddress" class="map-sub">{{ session.meetupAddress }}</p>
                </div>
                <span class="map-radius xyf-tabular">签到围栏 {{ session.meetupRadiusMeters || 80 }}m</span>
              </div>

              <div class="map-frame">
                <RealCampusMap
                  :height="220"
                  :interactive="false"
                  :latitude="Number(session.meetupLat)"
                  :longitude="Number(session.meetupLng)"
                  :radius-meters="session.meetupRadiusMeters || 80"
                  :center-lat="Number(session.meetupLat)"
                  :center-lng="Number(session.meetupLng)"
                  :fence-valid="true"
                />
              </div>
            </section>

            <!-- Clean Human Student Roster (replaces noisy SEAT #01 dashed box matrix) -->
            <section class="doc-section doc-section--roster">
              <div class="roster-head">
                <h2 class="section-label" style="margin: 0">
                  已报名同学（{{ session.joinedCount }} / {{ session.maxParticipants }}）
                </h2>
                <span class="roster-remain xyf-tabular">
                  剩余 {{ remainingSeats }} 席
                </span>
              </div>

              <div v-if="session.participants?.length" class="roster-list">
                <div
                  v-for="p in session.participants"
                  :key="p.id"
                  class="roster-row"
                >
                  <div class="roster-row__left">
                    <XyfAvatar :src="p.buyerAvatar" :name="p.buyerName" :size="32" />
                    <span class="roster-row__name">{{ p.buyerName }}</span>
                    <span v-if="Number(p.rebateAmount) > 0" class="rebate-note xyf-tabular">
                      已退差价 ¥{{ Number(p.rebateAmount).toFixed(0) }}
                    </span>
                  </div>

                  <div class="roster-row__right xyf-tabular">
                    <span class="roster-price">¥{{ Number(p.effectivePrice).toFixed(0) }}</span>
                    <button
                      v-if="userStore.userInfo?.id === p.buyerId"
                      type="button"
                      class="inline-order-link"
                      @click="router.push(`/orders/${p.orderId}`)"
                    >
                      我的订单 →
                    </button>
                  </div>
                </div>
              </div>

              <p v-else class="roster-empty">
                当前尚无同学报名，成为首位报名学员即可开启拼班。
              </p>

              <div v-if="remainingSeats > 0" class="roster-footer xyf-tabular">
                <span>
                  {{
                    session.tierRebateApplied === 1
                      ? `还有 ${remainingSeats} 个开放席位，参团直接享阶梯价 ¥${Number(session.tierDiscountPrice).toFixed(0)}/人`
                      : `还有 ${remainingSeats} 个开放席位，再邀 ${tierGap} 人即可触发全员退差价至 ¥${Number(session.tierDiscountPrice).toFixed(0)}/人`
                  }}
                </span>
                <button
                  v-if="canEnroll"
                  type="button"
                  class="inline-join-link"
                  @click="handleJoin"
                >
                  立即占位 →
                </button>
              </div>
            </section>
          </main>

          <!-- Right Single Sticky Enrollment Card (Price + Compact Tier Progress + CTA) -->
          <aside class="detail-aside">
            <section class="action-card">
              <div class="action-card__price xyf-tabular">
                <span class="action-card__label">当前参团单价</span>
                <div class="action-card__amount">
                  <span class="yen">¥</span>{{ Number(session.currentUnitPrice || session.basePrice).toFixed(0) }}<small>/人</small>
                </div>
                <div
                  v-if="Number(session.basePrice) > Number(session.tierDiscountPrice)"
                  class="action-card__tier"
                >
                  基础价 ¥{{ Number(session.basePrice).toFixed(0) }} ·
                  <strong>满 {{ session.tierTargetCount }} 人全员 ¥{{ Number(session.tierDiscountPrice).toFixed(0) }}</strong>
                </div>
              </div>

              <!-- Integrated Compact Tiered Progress (directly adjacent to buy CTA) -->
              <div class="tier-progress xyf-tabular">
                <div class="tier-progress__top">
                  <span>已报名 {{ session.joinedCount }} / {{ session.maxParticipants }} 人</span>
                  <span v-if="session.tierRebateApplied === 1" class="tier-flag is-hit">
                    <XyfStatusBadge tone="success" icon="spark" tooltip="已解锁全员阶梯价" :icon-only="true" size="xs" />
                    <span>已解锁阶梯价</span>
                  </span>
                  <span v-else class="tier-flag is-next">
                    <XyfStatusBadge tone="warning" icon="spark" :tooltip="`再邀 ${tierGap} 人即享阶梯价`" :icon-only="true" size="xs" />
                    <span>还差 {{ tierGap }} 人降价</span>
                  </span>
                </div>
                <div class="tier-track">
                  <div
                    class="tier-track__fill"
                    :class="{ 'is-hit': session.tierRebateApplied === 1 }"
                    :style="{ width: `${enrollmentPercent}%` }"
                  />
                </div>
                <div class="tier-milestones">
                  <span :class="{ 'is-reached': session.joinedCount >= session.minParticipants }">
                    {{ session.minParticipants }}人成班
                  </span>
                  <span :class="{ 'is-reached': session.joinedCount >= session.tierTargetCount }">
                    {{ session.tierTargetCount }}人降至 ¥{{ Number(session.tierDiscountPrice).toFixed(0) }}
                  </span>
                  <span :class="{ 'is-reached': session.joinedCount >= session.maxParticipants }">
                    {{ session.maxParticipants }}人满班
                  </span>
                </div>
              </div>

              <template v-if="myJoinedRecord">
                <div class="status-note">
                  <strong>已锁定本节团课席位</strong>
                  <p>前往订单完成托管支付，开课时出示动态码核销签到。</p>
                </div>
                <el-button
                  type="primary"
                  size="large"
                  style="width: 100%"
                  @click="router.push(`/orders/${myJoinedRecord.orderId}`)"
                >
                  查看我的参团订单
                </el-button>
              </template>

              <template v-else-if="isCreator">
                <div class="status-note">
                  <strong>您是本节团课主讲导师</strong>
                  <p>学员报名后将自动生成独立担保订单，开课时逐一核销。</p>
                </div>
                <el-button
                  v-if="session.joinedCount === 0 && session.sessionStatus === 0"
                  type="danger"
                  plain
                  style="width: 100%"
                  @click="handleCancelSession"
                >
                  取消未报名团课
                </el-button>
              </template>

              <template v-else>
                <el-button
                  type="primary"
                  size="large"
                  style="width: 100%"
                  :disabled="!canEnroll"
                  :loading="joining"
                  @click="handleJoin"
                >
                  {{
                    session.joinedCount >= session.maxParticipants
                      ? '本班席位已满'
                      : session.sessionStatus >= 2
                        ? '已停止报名'
                        : '立即锁定席位'
                  }}
                </el-button>
              </template>

              <p class="escrow-footnote">
                达 {{ session.tierTargetCount }} 人阶梯线后，差价将自动原路退回钱包余额；未满 {{ session.minParticipants }} 人不开班自动全额退款。
              </p>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Calendar, Location, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getGroupSessionDetail,
  joinGroupSession,
  cancelGroupSession,
  type GroupSessionItem
} from '@/api/groupSession'
import { notifyFallbackError } from '@/api/request'
import { formatSessionTimeRange } from '@/utils/format'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfStatusBadge, { type StatusBadgeTone, type StatusBadgeIcon } from '@/components/ui/XyfStatusBadge.vue'
import XyfVerifiedMark from '@/components/ui/XyfVerifiedMark.vue'
import XyfDeliveryIcon from '@/components/ui/XyfDeliveryIcon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sessionId = computed(() => Number(route.params.id))
const loading = ref(true)
const joining = ref(false)
const session = ref<GroupSessionItem | null>(null)

const isCreator = computed(
  () => Boolean(userStore.userInfo?.id && session.value?.creatorId === userStore.userInfo.id)
)

const myJoinedRecord = computed(
  () => session.value?.participants?.find(p => p.buyerId === userStore.userInfo?.id) || null
)

const canEnroll = computed(() => {
  if (!session.value || isCreator.value || myJoinedRecord.value || userStore.isAdmin) return false
  return session.value.joinedCount < session.value.maxParticipants && session.value.sessionStatus < 2
})

const enrollmentPercent = computed(() => {
  if (!session.value || !session.value.maxParticipants) return 0
  return Math.min(100, Math.round((session.value.joinedCount / session.value.maxParticipants) * 100))
})

const remainingSeats = computed(() => {
  if (!session.value) return 0
  return Math.max(0, session.value.maxParticipants - session.value.joinedCount)
})

const tierGap = computed(() => {
  if (!session.value) return 0
  return Math.max(0, session.value.tierTargetCount - session.value.joinedCount)
})

function statusLabel(st: number) {
  if (st === 0) return '招募拼班中'
  if (st === 1) return '已成班待开课'
  if (st === 2) return '已满员锁定'
  if (st === 3) return '已完结'
  return '已取消'
}

function statusTone(st: number): StatusBadgeTone {
  if (st === 0) return 'dark'
  if (st === 1) return 'success'
  if (st === 2) return 'warning'
  return 'neutral'
}

function statusIcon(st: number): StatusBadgeIcon {
  if (st === 0) return 'live'
  if (st === 1) return 'check'
  if (st === 2) return 'lock'
  return 'clock'
}

async function loadDetail() {
  loading.value = true
  try {
    session.value = await getGroupSessionDetail(sessionId.value)
  } catch (err) {
    notifyFallbackError(err, '加载团课详情失败')
    session.value = null
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!userStore.userInfo) {
    router.push(`/login?redirect=/groups/${sessionId.value}`)
    return
  }
  if (!session.value) return
  try {
    await ElMessageBox.confirm(
      `确认报名「${session.value.title}」（当前单价 ¥${Number(session.value.currentUnitPrice || session.value.basePrice).toFixed(2)}）吗？参团满 ${session.value.tierTargetCount} 人后系统将自动按阶梯价 ¥${Number(session.value.tierDiscountPrice).toFixed(2)} 原路退回差价。`,
      '锁定拼班席位',
      { confirmButtonText: '确认参团锁座', cancelButtonText: '取消', type: 'success' }
    )
  } catch {
    return
  }

  joining.value = true
  try {
    const orderId = await joinGroupSession(session.value.id)
    ElMessage.success('参团成功！已生成专属担保订单，请完成托管支付')
    router.push(`/orders/${orderId}`)
  } catch (err) {
    notifyFallbackError(err, '报名参团失败')
  } finally {
    joining.value = false
  }
}

async function handleCancelSession() {
  if (!session.value) return
  try {
    await cancelGroupSession(session.value.id, '讲师主动取消未开班团课')
    ElMessage.success('已取消该团课')
    router.push('/groups')
  } catch (err) {
    notifyFallbackError(err, '取消团课失败')
  }
}

watch(sessionId, (newId, oldId) => {
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
  gap: 12px;
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

.loading-wrap,
.empty-wrap {
  padding: 40px 0;
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

.meta-icon-txt {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  .el-icon {
    font-size: 13px;
    color: var(--xyf-ink-3);
  }
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

  &__btn {
    display: flex;
    align-items: center;
    gap: 12px;
    border: none;
    background: transparent;
    padding: 0;
    text-align: left;
    font-family: inherit;
    cursor: pointer;

    &:hover strong {
      text-decoration: underline;
    }
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    strong {
      font-size: 14.5px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  &__sub {
    margin-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-4);
  }
}

/* Hairline Key Logistics Strip */
.key-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
  border-top: 1px solid var(--xyf-border);
  border-bottom: 1px solid var(--xyf-border);

  @media (max-width: 640px) {
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
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
    line-height: 1.4;

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

  &--roster {
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

/* Clean Human Roster */
.roster-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  .roster-remain {
    font-size: 13px;
    color: var(--xyf-ink-3);
  }
}

.roster-list {
  display: flex;
  flex-direction: column;
}

.roster-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--xyf-border-subtle);

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    color: var(--xyf-ink);
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
  }
}

.rebate-note {
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-success);
}

.roster-price {
  font-size: 13.5px;
  color: var(--xyf-ink-2);
}

.inline-order-link,
.inline-join-link {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  color: var(--xyf-ink);
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

.roster-empty {
  margin: 0;
  padding: 16px 0;
  font-size: 13.5px;
  color: var(--xyf-ink-3);
}

.roster-footer {
  margin-top: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xyf-ink-3);
}

/* Right Single Sticky Action Card */
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

  &__amount {
    margin-top: 6px;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
    line-height: 1.1;

    .yen {
      font-size: 18px;
      font-weight: 600;
      margin-right: 2px;
    }

    small {
      font-size: 13px;
      font-weight: 400;
      color: var(--xyf-ink-3);
    }
  }

  &__tier {
    margin-top: 8px;
    font-size: 13px;
    color: var(--xyf-ink-3);

    strong {
      color: var(--xyf-warning);
      font-weight: 600;
    }
  }
}

/* Compact Integrated Tier Progress */
.tier-progress {
  margin-bottom: 20px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
    margin-bottom: 8px;

    .tier-flag {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-weight: 600;

      .spark-svg {
        width: 12px;
        height: 12px;
        flex-shrink: 0;
      }

      &.is-hit {
        color: var(--xyf-success);
      }

      &.is-next {
        color: var(--xyf-warning);
      }
    }
  }
}

.tier-track {
  height: 4px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  overflow: hidden;

  &__fill {
    height: 100%;
    border-radius: 999px;
    background: var(--xyf-ink);
    transition: width 0.25s ease;

    &.is-hit {
      background: var(--xyf-success);
    }
  }
}

.tier-milestones {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--xyf-ink-4);

  .is-reached {
    color: var(--xyf-ink-2);
    font-weight: 600;
  }
}

.status-note {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  margin-bottom: 14px;

  strong {
    display: block;
    font-size: 13.5px;
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
