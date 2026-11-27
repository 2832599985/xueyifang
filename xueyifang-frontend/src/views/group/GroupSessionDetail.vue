<template>
  <div class="group-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        kicker="CO-LEARNING CLASSROOM"
        title="拼班团课与阶梯成团教室"
        subtitle="每位参团学员均生成独立米级围栏担保订单，满阶梯人数系统自动向全体早期学员退回差价"
        :show-back="true"
        back-text="返回团课大厅"
        back-to="/groups"
      >
        <template #actions v-if="session">
          <span class="session-serial xyf-mono">{{ session.sessionNo }}</span>
        </template>
      </XyfPageHeader>

      <div v-if="loading" class="xyf-surface-card" style="padding: 32px">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!session" class="xyf-surface-card" style="padding: 48px">
        <XyfEmptyState title="未找到该拼班团课" description="课程可能已被取消或下架">
          <template #action>
            <el-button type="primary" @click="router.push('/groups')">返回团课大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Visual 3-Stage Tiered Pricing & Formation Milestone Ladder -->
        <section class="xyf-surface-card ladder-banner xyf-tabular">
          <div class="ladder-banner__head">
            <div>
              <span class="xyf-kicker">TIERED REBATE PROGRESS</span>
              <h2>阶梯成班与全员退差价进度轴</h2>
            </div>
            <span class="rebate-pill" :class="{ 'is-hit': session.tierRebateApplied === 1 }">
              {{
                session.tierRebateApplied === 1
                  ? `✦ 已达成 ${session.tierTargetCount} 人阶梯线 · 全员执行 ¥${Number(session.tierDiscountPrice).toFixed(2)}`
                  : `再邀 ${Math.max(0, session.tierTargetCount - session.joinedCount)} 人即可触发全员退差价至 ¥${Number(session.tierDiscountPrice).toFixed(2)}`
              }}
            </span>
          </div>

          <div class="ladder-track-wrap">
            <div class="ladder-track">
              <div class="ladder-track__fill" :style="{ width: `${enrollmentPercent}%` }" />
            </div>

            <div class="ladder-nodes">
              <div class="l-node" :class="{ 'is-reached': session.joinedCount >= 1 }">
                <span class="l-dot" />
                <strong>开放报名 (1人)</strong>
                <span>基础拼班价 ¥{{ Number(session.basePrice).toFixed(0) }}/人</span>
              </div>

              <div class="l-node" :class="{ 'is-reached': session.joinedCount >= session.minParticipants }">
                <span class="l-dot" />
                <strong>正式成班 ({{ session.minParticipants }}人)</strong>
                <span>锁定授课档期与教室</span>
              </div>

              <div class="l-node is-gold" :class="{ 'is-reached': session.joinedCount >= session.tierTargetCount }">
                <span class="l-dot" />
                <strong>阶梯降价 ({{ session.tierTargetCount }}人)</strong>
                <span>全员 ¥{{ Number(session.tierDiscountPrice).toFixed(0) }} · 自动退差价</span>
              </div>

              <div class="l-node" :class="{ 'is-reached': session.joinedCount >= session.maxParticipants }">
                <span class="l-dot" />
                <strong>满班封顶 ({{ session.maxParticipants }}人)</strong>
                <span>小班精品容量上限</span>
              </div>
            </div>
          </div>
        </section>

        <div class="detail-grid">
          <!-- Left Column: Syllabus + Classroom Seat Matrix -->
          <div class="detail-main">
            <section class="xyf-surface-card syllabus-card">
              <div class="top-badges">
                <span class="s-pill">{{ session.campusName || '主校区' }}</span>
                <span class="s-pill">{{ session.tradeType === 1 ? '校园线下小班' : '线上共享课堂' }}</span>
                <span v-if="session.tagName" class="s-pill">{{ session.tagName }}</span>
                <span class="s-pill is-dark">{{ statusLabel(session.sessionStatus) }}</span>
              </div>

              <h1 class="course-h1">{{ session.title }}</h1>

              <!-- Instructor Studio Bar -->
              <div class="instructor-bar" @click="router.push(`/creators/${session.creatorId}`)">
                <div class="instructor-bar__left">
                  <XyfAvatar :src="session.creatorAvatar" :name="session.creatorName || '讲师'" :size="48" />
                  <div>
                    <div class="inst-name">
                      <strong>{{ session.creatorName || '认证讲师' }}</strong>
                      <span class="inst-chip">
                        {{ session.creatorEduVerified === 1 ? '高校 .edu.cn 已核验' : '认证创作者' }}
                      </span>
                      <span class="inst-chip xyf-tabular">信用 {{ session.creatorCreditScore ?? 100 }}</span>
                    </div>
                    <div class="inst-sub">
                      {{ session.creatorProfessional || '校内认证技能讲师' }} · 点击访问讲师作品集主页
                    </div>
                  </div>
                </div>

                <el-button
                  v-if="userStore.userInfo && !isCreator && !userStore.isAdmin"
                  size="small"
                  plain
                  @click.stop="router.push(`/chat/${session.creatorId}`)"
                >
                  咨询讲师
                </el-button>
              </div>

              <!-- Schedule & Landmark Bento -->
              <div class="schedule-bento xyf-tabular">
                <div class="sch-box">
                  <span>开课开始时间</span>
                  <strong>{{ formatDateTime(session.sessionStart) }}</strong>
                </div>
                <div class="sch-box">
                  <span>预计结束时间</span>
                  <strong>{{ formatDateTime(session.sessionEnd) }}</strong>
                </div>
                <div class="sch-box" v-if="session.tradeType === 1">
                  <span>线下签到围栏</span>
                  <strong>{{ session.meetupPoiName || '校内指定教室' }}（{{ session.meetupRadiusMeters || 80 }}m）</strong>
                </div>
                <div class="sch-box" v-else>
                  <span>授课交付形式</span>
                  <strong>线上实时共享课堂 · 独立凭证核销</strong>
                </div>
              </div>

              <div class="desc-section">
                <div class="xyf-section-heading">
                  <h3>课程大纲与配套资料</h3>
                </div>
                <p>{{ session.description }}</p>
              </div>

              <div v-if="session.tradeType === 1 && session.meetupLat && session.meetupLng" class="map-box">
                <div class="xyf-section-heading">
                  <h3>线下小班授课位置与电子围栏</h3>
                </div>
                <p v-if="session.meetupAddress" class="map-addr">{{ session.meetupAddress }}</p>
                <RealCampusMap
                  :height="230"
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

            <!-- Interactive Classroom Seat Matrix -->
            <section class="xyf-surface-card roster-card">
              <div class="roster-head">
                <div class="xyf-section-heading" style="margin: 0">
                  <h2>班级席位矩阵（已入座 {{ session.joinedCount }} / {{ session.maxParticipants }}）</h2>
                </div>
                <span class="seat-remain xyf-tabular">
                  剩余 {{ Math.max(0, session.maxParticipants - session.joinedCount) }} 个开放席位
                </span>
              </div>

              <div class="classroom-seats">
                <!-- Enrolled Student Seats -->
                <div
                  v-for="(p, idx) in session.participants || []"
                  :key="p.id"
                  class="seat-card is-occupied"
                >
                  <div class="seat-card__top">
                    <span class="seat-no xyf-mono">SEAT #{{ String(idx + 1).padStart(2, '0') }}</span>
                    <span v-if="Number(p.rebateAmount) > 0" class="rebate-chip xyf-tabular">
                      已退差价 ¥{{ Number(p.rebateAmount).toFixed(0) }}
                    </span>
                  </div>

                  <div class="seat-student">
                    <XyfAvatar :src="p.buyerAvatar" :name="p.buyerName" :size="36" />
                    <div class="seat-student__info">
                      <strong>{{ p.buyerName }}</strong>
                      <span class="xyf-tabular">执行价 ¥{{ Number(p.effectivePrice).toFixed(2) }}</span>
                    </div>
                  </div>

                  <el-button
                    v-if="userStore.userInfo?.id === p.buyerId"
                    size="small"
                    plain
                    style="width: 100%; margin-top: 4px"
                    @click="router.push(`/orders/${p.orderId}`)"
                  >
                    查看我的担保单 →
                  </el-button>
                </div>

                <!-- Open Seats (Visual Scarcity Slots, capped at 12 visual empty slots) -->
                <button
                  v-for="openIdx in openSeatSlots"
                  :key="`open-${openIdx}`"
                  type="button"
                  class="seat-card is-open"
                  :disabled="!canEnroll"
                  @click="canEnroll && handleJoin()"
                >
                  <span class="seat-no xyf-mono">
                    SEAT #{{ String((session.participants?.length || 0) + openIdx).padStart(2, '0') }}
                  </span>
                  <div class="open-placeholder">
                    <span class="plus-circle">+</span>
                    <span>
                      {{
                        (session.participants?.length || 0) + openIdx === session.tierTargetCount
                          ? '阶梯降价解锁席位'
                          : '开放席位 · 虚位以待'
                      }}
                    </span>
                  </div>
                </button>
              </div>
            </section>
          </div>

          <!-- Right Sticky Tiered Enrollment Checkout Dock -->
          <aside class="detail-aside">
            <section class="xyf-surface-card enroll-dock">
              <div class="price-stage xyf-tabular">
                <span class="price-stage__lbl">当前参团执行单价</span>
                <div class="price-stage__val">
                  <span class="yen">¥</span>{{ Number(session.currentUnitPrice || session.basePrice).toFixed(2) }}
                  <small>/ 人</small>
                </div>
                <div
                  v-if="Number(session.basePrice) > Number(session.tierDiscountPrice)"
                  class="price-stage__compare"
                >
                  <span>基础单价 ¥{{ Number(session.basePrice).toFixed(2) }}</span>
                  <span>·</span>
                  <strong>满 {{ session.tierTargetCount }} 人全员 ¥{{ Number(session.tierDiscountPrice).toFixed(2) }}</strong>
                </div>
              </div>

              <div class="dock-metrics xyf-tabular">
                <div class="d-row">
                  <span>最低成班人数</span>
                  <strong>{{ session.minParticipants }} 人</strong>
                </div>
                <div class="d-row">
                  <span>当前剩余名额</span>
                  <strong>{{ Math.max(0, session.maxParticipants - session.joinedCount) }} 席</strong>
                </div>
                <div class="d-row">
                  <span>差价退还机制</span>
                  <strong>原路自动退回钱包余额</strong>
                </div>
              </div>

              <template v-if="myJoinedRecord">
                <div class="enrolled-notice">
                  <strong>✓ 您已锁定本节团课席位</strong>
                  <p>请前往关联担保订单完成托管支付，开课时向讲师出示 10s 动态核销码签到。</p>
                </div>
                <el-button
                  type="primary"
                  size="large"
                  style="width: 100%"
                  @click="router.push(`/orders/${myJoinedRecord.orderId}`)"
                >
                  查看我的参团担保单
                </el-button>
              </template>

              <template v-else-if="isCreator">
                <div class="enrolled-notice">
                  <strong>您是本节拼班团课的主讲导师</strong>
                  <p>每位学员参团后均会在您的「收到的销售单」生成独立担保订单，开课时逐一扫码或动态码核销。</p>
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
                        : '立即锁定席位并生成担保单'
                  }}
                </el-button>
              </template>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getGroupSessionDetail,
  joinGroupSession,
  cancelGroupSession,
  type GroupSessionItem
} from '@/api/groupSession'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

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

const openSeatSlots = computed(() => {
  if (!session.value) return 0
  const remain = Math.max(0, session.value.maxParticipants - (session.value.participants?.length || 0))
  return Math.min(12, remain)
})

function statusLabel(st: number) {
  if (st === 0) return '招募拼班中'
  if (st === 1) return '已成班待开课'
  if (st === 2) return '已满员锁定'
  if (st === 3) return '已完结'
  return '已取消'
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

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.session-serial {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 12px;
  color: var(--xyf-ink-2);
}

/* Tiered Pricing Milestone Ladder */
.ladder-banner {
  padding: 22px 26px;
  margin-bottom: 22px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 18px;

    h2 {
      margin: 4px 0 0;
      font-size: 18px;
      font-weight: 750;
      color: var(--xyf-ink);
    }
  }

  .rebate-pill {
    padding: 5px 12px;
    border-radius: 999px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-2);

    &.is-hit {
      background: #18181b;
      border-color: #18181b;
      color: #e5c79e;
    }
  }
}

.ladder-track-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ladder-track {
  height: 8px;
  border-radius: 999px;
  background: #f1f1f4;
  overflow: hidden;

  &__fill {
    height: 100%;
    border-radius: 999px;
    background: linear-gradient(90deg, #18181b 0%, #c8a97e 100%);
    transition: width 0.35s ease;
  }
}

.ladder-nodes {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.l-node {
  display: flex;
  flex-direction: column;
  gap: 2px;
  opacity: 0.55;

  .l-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    border: 2px solid var(--xyf-border-strong);
    background: #ffffff;
    margin-bottom: 4px;
  }

  strong {
    font-size: 13px;
    color: var(--xyf-ink);
  }

  span {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }

  &.is-reached {
    opacity: 1;

    .l-dot {
      background: #18181b;
      border-color: #18181b;
    }
  }

  &.is-gold.is-reached .l-dot {
    background: #d97706;
    border-color: #d97706;
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

.syllabus-card,
.roster-card,
.enroll-dock {
  padding: 26px 28px;
}

.top-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  .s-pill {
    padding: 3px 10px;
    border-radius: 999px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-2);

    &.is-dark {
      background: #18181b;
      border-color: #18181b;
      color: #ffffff;
    }
  }
}

.course-h1 {
  margin: 0 0 18px;
  font-size: 24px;
  font-weight: 760;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
}

.instructor-bar {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
  margin-bottom: 20px;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .inst-name {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;

    strong {
      font-size: 14.5px;
      color: var(--xyf-ink);
    }
  }

  .inst-chip {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 999px;
    background: #ffffff;
    border: 1px solid var(--xyf-border);
    color: var(--xyf-ink-2);
  }

  .inst-sub {
    margin-top: 3px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.schedule-bento {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 22px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.sch-box {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }

  strong {
    font-size: 13.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.desc-section p {
  margin: 0;
  white-space: pre-wrap;
  font-size: 14.5px;
  line-height: 1.75;
  color: var(--xyf-ink-2);
}

.map-box {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--xyf-border-subtle);

  .map-addr {
    margin: -6px 0 12px;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }
}

/* Classroom Seat Matrix */
.roster-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  .seat-remain {
    font-size: 12.5px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }
}

.classroom-seats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
}

.seat-card {
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
  font-family: inherit;

  .seat-no {
    font-size: 11px;
    font-weight: 700;
    color: var(--xyf-ink-3);
  }

  &.is-occupied {
    background: #ffffff;
    border: 1px solid var(--xyf-border);

    .seat-card__top {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .rebate-chip {
      padding: 1px 7px;
      border-radius: 999px;
      background: var(--xyf-champagne-soft);
      border: 1px solid var(--xyf-champagne-border);
      color: #9a5b13;
      font-size: 10.5px;
      font-weight: 650;
    }

    .seat-student {
      display: flex;
      align-items: center;
      gap: 10px;

      &__info {
        display: flex;
        flex-direction: column;

        strong {
          font-size: 13.5px;
          color: var(--xyf-ink);
        }

        span {
          font-size: 12px;
          color: var(--xyf-ink-3);
        }
      }
    }
  }

  &.is-open {
    background: #fafafa;
    border: 1px dashed var(--xyf-border-strong);
    cursor: pointer;
    transition: all var(--xyf-transition);

    &:hover:not(:disabled) {
      border-color: #18181b;
      background: #ffffff;
    }

    &:disabled {
      cursor: default;
      opacity: 0.7;
    }

    .open-placeholder {
      display: flex;
      align-items: center;
      gap: 9px;
      padding: 6px 0;
      font-size: 12.5px;
      color: var(--xyf-ink-3);

      .plus-circle {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: 1px solid var(--xyf-border-strong);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 15px;
        color: var(--xyf-ink-2);
      }
    }
  }
}

/* Sticky Enrollment Dock */
.detail-aside {
  position: sticky;
  top: 86px;

  @media (max-width: 960px) {
    position: static;
  }
}

.price-stage {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border);

  &__lbl {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  &__val {
    margin-top: 4px;
    font-size: 32px;
    font-weight: 800;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);

    .yen {
      font-size: 18px;
      margin-right: 2px;
    }

    small {
      font-size: 13px;
      font-weight: 500;
      color: var(--xyf-ink-3);
    }
  }

  &__compare {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--xyf-ink-2);

    strong {
      color: #9a5b13;
    }
  }
}

.dock-metrics {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;

  .d-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--xyf-ink-2);

    strong {
      color: var(--xyf-ink);
    }
  }
}

.enrolled-notice {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-ink);
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
</style>
