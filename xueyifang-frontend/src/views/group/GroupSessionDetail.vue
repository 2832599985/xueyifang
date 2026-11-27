<template>
  <div class="group-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="拼班团课与阶梯成团详情"
        subtitle="每位参团同学均生成独立米级围栏担保订单，满阶梯人数自动退回早期学员差价"
        :show-back="true"
        back-text="返回团课大厅"
        back-to="/groups"
      />

      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!session" class="xyf-surface-card" style="padding: 40px">
        <XyfEmptyState title="未找到该拼班团课" />
      </div>

      <div v-else class="detail-grid">
        <div class="detail-main">
          <section class="xyf-surface-card main-card">
            <div class="top-row">
              <div class="badges">
                <span class="pill">{{ session.campusName || '主校区' }}</span>
                <span class="pill">{{ session.tradeType === 1 ? '校园线下小班' : '线上共享课堂' }}</span>
                <span v-if="session.tagName" class="pill">{{ session.tagName }}</span>
              </div>
              <span class="xyf-mono">{{ session.sessionNo }}</span>
            </div>

            <h1>{{ session.title }}</h1>

            <div class="creator-bar" @click="router.push(`/creators/${session.creatorId}`)">
              <XyfAvatar :src="session.creatorAvatar" :name="session.creatorName" :size="44" />
              <div>
                <div class="c-name">
                  {{ session.creatorName || '认证讲师' }}
                  <span class="c-verify">
                    {{ session.creatorEduVerified === 1 ? 'edu.cn 已核验' : '认证创作者' }} · 信用 {{ session.creatorCreditScore ?? 100 }}
                  </span>
                </div>
                <div class="c-sub">
                  {{ session.creatorProfessional || '校内认证技能讲师' }} · 点击查看创作者数字信用主页
                </div>
              </div>
            </div>

            <div class="schedule-strip xyf-mono">
              <div><strong>开课时间：</strong>{{ formatDateTime(session.sessionStart) }} 至 {{ formatDateTime(session.sessionEnd) }}</div>
              <div v-if="session.tradeType === 1 && session.meetupPoiName">
                <strong>线下地点：</strong>{{ session.meetupPoiName }} {{ session.meetupAddress || '' }}（{{ session.meetupRadiusMeters || 80 }}米内当面核销）
              </div>
            </div>

            <div class="desc-section">
              <h3>课程大纲与讲义说明</h3>
              <p>{{ session.description }}</p>
            </div>

            <div v-if="session.tradeType === 1 && session.meetupLat && session.meetupLng" class="map-box">
              <RealCampusMap
                :height="240"
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

          <!-- Participants Roster -->
          <section class="xyf-surface-card roster-card">
            <div class="xyf-section-heading">
              <h2>已参团同学（{{ session.joinedCount }} / {{ session.maxParticipants }}）</h2>
            </div>

            <div v-if="!session.participants || session.participants.length === 0" class="empty-tip">
              暂未有同学报名，立即成为第一位参团学员锁定席位！
            </div>

            <div v-else class="part-list">
              <div v-for="p in session.participants" :key="p.id" class="part-row">
                <div class="part-left">
                  <XyfAvatar :src="p.buyerAvatar" :name="p.buyerName" :size="36" />
                  <div>
                    <strong>{{ p.buyerName }}</strong>
                    <div class="p-time">{{ formatDateTime(p.createTime) }} 报名</div>
                  </div>
                </div>
                <div class="part-right xyf-tabular">
                  <span>执行单价 <strong>¥{{ Number(p.effectivePrice).toFixed(2) }}</strong></span>
                  <el-tag v-if="Number(p.rebateAmount) > 0" size="small" type="success">
                    已退阶梯差价 ¥{{ Number(p.rebateAmount).toFixed(2) }}
                  </el-tag>
                  <el-button
                    v-if="userStore.userInfo?.id === p.buyerId"
                    size="small"
                    type="primary"
                    link
                    @click="router.push(`/orders/${p.orderId}`)"
                  >
                    进入我的担保单
                  </el-button>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- Right Sticky Enrollment Box -->
        <aside class="detail-aside">
          <section class="xyf-surface-card enroll-box">
            <div class="price-banner xyf-tabular">
              <span>当前参团单价</span>
              <strong>¥{{ Number(session.currentUnitPrice || session.basePrice).toFixed(2) }} / 人</strong>
              <p class="tier-note">
                基础拼班价 ¥{{ Number(session.basePrice).toFixed(2) }} · 满 <strong>{{ session.tierTargetCount }}</strong> 人全员降至
                <strong>¥{{ Number(session.tierDiscountPrice).toFixed(2) }}</strong>（早期高价参团同学系统自动原路退差价）
              </p>
            </div>

            <div class="seat-stat xyf-tabular">
              <div class="s-row">
                <span>成班门槛</span>
                <strong>至少 {{ session.minParticipants }} 人</strong>
              </div>
              <div class="s-row">
                <span>剩余席位</span>
                <strong>{{ Math.max(0, session.maxParticipants - session.joinedCount) }} 个名额</strong>
              </div>
            </div>

            <template v-if="myJoinedRecord">
              <div class="joined-alert">
                您已成功报名该拼班团课！请前往关联担保订单完成托管支付或当面出示核销码。
              </div>
              <el-button type="primary" size="large" style="width: 100%" @click="router.push(`/orders/${myJoinedRecord.orderId}`)">
                查看我的参团担保订单
              </el-button>
            </template>

            <template v-else-if="isCreator">
              <div class="joined-alert">
                您是本节拼班团课的授课讲师。每位学员报名后都会在您的「收到的销售单」中生成一笔独立担保单。
              </div>
              <el-button
                v-if="session.joinedCount === 0 && session.sessionStatus === 0"
                type="danger"
                plain
                style="width: 100%"
                @click="handleCancelSession"
              >
                取消本节团课
              </el-button>
            </template>

            <template v-else>
              <el-button
                type="primary"
                size="large"
                style="width: 100%"
                :disabled="session.joinedCount >= session.maxParticipants || session.sessionStatus >= 2"
                :loading="joining"
                @click="handleJoin"
              >
                {{
                  session.joinedCount >= session.maxParticipants
                    ? '名额已满'
                    : '立即报名参团并生成担保单'
                }}
              </el-button>
            </template>
          </section>
        </aside>
      </div>
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

const myJoinedRecord = computed(() =>
  session.value?.participants?.find(p => p.buyerId === userStore.userInfo?.id) || null
)

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
      `确认报名「${session.value.title}」（当前单价 ¥${Number(session.value.currentUnitPrice || session.value.basePrice).toFixed(2)}）吗？人数达 ${session.value.tierTargetCount} 人后将自动按阶梯价 ¥${Number(session.value.tierDiscountPrice).toFixed(2)} 退回差价。`,
      '报名拼班团课',
      { confirmButtonText: '确认参团', cancelButtonText: '取消', type: 'success' }
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

.main-card,
.roster-card,
.enroll-box {
  padding: 24px 26px;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.badges {
  display: flex;
  gap: 8px;
}

.pill {
  padding: 3px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  font-size: 12px;
  font-weight: 600;
}

h1 {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 800;
}

.creator-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border);
  cursor: pointer;

  .c-name {
    font-weight: 700;
    font-size: 14.5px;
  }

  .c-verify {
    margin-left: 8px;
    font-size: 11.5px;
    color: #047857;
    background: #ecfdf5;
    padding: 2px 8px;
    border-radius: 999px;
  }

  .c-sub {
    margin-top: 2px;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.schedule-strip {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.desc-section {
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

.map-box {
  margin-top: 18px;
}

.part-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.part-row {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .part-left {
    display: flex;
    align-items: center;
    gap: 10px;

    .p-time {
      font-size: 11.5px;
      color: var(--xyf-ink-muted);
    }
  }

  .part-right {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
  }
}

.detail-aside {
  position: sticky;
  top: 84px;
}

.price-banner {
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
    color: #047857;
  }

  .tier-note {
    margin: 8px 0 0;
    font-size: 12.5px;
    line-height: 1.55;
    color: var(--xyf-ink-2);
  }
}

.seat-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;

  .s-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
  }
}

.joined-alert {
  padding: 12px 14px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #065f46;
  font-size: 12.5px;
  line-height: 1.6;
  margin-bottom: 14px;
}

.empty-tip {
  padding: 18px 0;
  color: var(--xyf-ink-muted);
  font-size: 13.5px;
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
