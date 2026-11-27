<template>
  <div class="group-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Editorial Header + Integrated Workspace Switcher -->
      <header class="group-header">
        <div class="group-header__left">
          <span class="xyf-kicker">CO-LEARNING &amp; TIERED REBATE STUDIO</span>
          <h1>校园拼班团课大厅</h1>
          <p>认证创作者发起期末串讲、算法集训与实操小班，参团人数达标后系统自动为全体早期学员原路退回差价。</p>
        </div>

        <div class="group-header__right">
          <div v-if="userStore.userInfo && !userStore.isAdmin" class="xyf-segmented" role="tablist">
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'market' }"
              @click="switchWorkspace('market')"
            >
              <span>正在招募课程</span>
              <span class="xyf-segmented__count">{{ sessions.length }}</span>
            </button>
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'joined' }"
              @click="switchWorkspace('joined')"
            >
              <span>我报名参团</span>
              <span v-if="myJoined.length" class="xyf-segmented__count">{{ myJoined.length }}</span>
            </button>
            <button
              v-if="userStore.hasPublishPermission"
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'created' }"
              @click="switchWorkspace('created')"
            >
              <span>我开办的</span>
              <span v-if="myCreated.length" class="xyf-segmented__count">{{ myCreated.length }}</span>
            </button>
          </div>

          <el-button
            v-if="userStore.hasPublishPermission"
            type="primary"
            size="large"
            @click="openCreateDialog"
          >
            <el-icon><Plus /></el-icon>
            <span>发起拼班团课</span>
          </el-button>
        </div>
      </header>

      <!-- Tiered Rebate Mechanism Explainer Strip -->
      <section class="mechanism-strip">
        <div class="mech-step">
          <span class="mech-num xyf-tabular">01</span>
          <div>
            <strong>独立担保锁座</strong>
            <span>每位参团同学生成独立担保订单</span>
          </div>
        </div>
        <span class="mech-arrow">→</span>
        <div class="mech-step">
          <span class="mech-num xyf-tabular">02</span>
          <div>
            <strong>满阶梯自动退差价</strong>
            <span>达到优惠人数线即刻原路退还差额</span>
          </div>
        </div>
        <span class="mech-arrow">→</span>
        <div class="mech-step">
          <span class="mech-num xyf-tabular">03</span>
          <div>
            <strong>围栏签到核销</strong>
            <span>按约定课表与地图围栏逐单核销结算</span>
          </div>
        </div>
      </section>

      <!-- VIEW 1: Public Group Sessions -->
      <template v-if="activeWorkspace === 'market'">
        <section class="filter-studio">
          <div class="filter-studio__top">
            <div class="xyf-tabs">
              <button
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': !filters.tagId }"
                @click="selectTag(undefined)"
              >
                全部课程分类
              </button>
              <button
                v-for="t in dictStore.serviceTags || []"
                :key="t.tagId"
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': filters.tagId === t.tagId }"
                @click="selectTag(t.tagId)"
              >
                {{ t.tagName }}
              </button>
            </div>

            <div class="campus-pills">
              <button
                type="button"
                class="campus-pill"
                :class="{ 'is-active': !filters.campusId }"
                @click="selectCampus(undefined)"
              >
                全部校区
              </button>
              <button
                v-for="c in campuses"
                :key="c.id"
                type="button"
                class="campus-pill"
                :class="{ 'is-active': filters.campusId === c.id }"
                @click="selectCampus(c.id)"
              >
                {{ c.campusName }}
              </button>
            </div>
          </div>

          <div class="filter-studio__bottom">
            <div class="xyf-segmented">
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': !filters.tradeType }"
                @click="selectTradeType(undefined)"
              >
                全部授课形式
              </button>
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': filters.tradeType === 1 }"
                @click="selectTradeType(1)"
              >
                校内线下小班
              </button>
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': filters.tradeType === 2 }"
                @click="selectTradeType(2)"
              >
                线上共享课堂
              </button>
            </div>

            <div class="search-inline">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索团课主题（如 线代冲刺 / 算法 / 建模）..."
                clearable
                :prefix-icon="Search"
                @keyup.enter="reload"
                @clear="reload"
              />
              <button v-if="hasActiveFilters" type="button" class="reset-link" @click="resetFilters">
                重置
              </button>
            </div>
          </div>
        </section>

        <div v-if="loading" class="session-grid">
          <div v-for="n in 4" :key="n" class="xyf-surface-card" style="padding: 24px">
            <el-skeleton :rows="5" animated />
          </div>
        </div>

        <div v-else-if="sessions.length === 0" class="xyf-surface-card empty-wrap">
          <XyfEmptyState
            title="当前暂无招募中的拼班团课"
            description="认证创作者可发起期末总复习或实战小班课，学员平摊课时费、讲师单场收益翻倍。"
          >
            <template #action>
              <el-button v-if="hasActiveFilters" plain @click="resetFilters">清空筛选</el-button>
              <el-button v-if="userStore.hasPublishPermission" type="primary" @click="openCreateDialog">
                发起首节团课
              </el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="session-grid">
          <article
            v-for="item in sessions"
            :key="item.id"
            class="course-card"
            @click="router.push(`/groups/${item.id}`)"
          >
            <!-- Top Row: Calendar Block + Title & Status -->
            <div class="course-card__head">
              <div class="cal-badge xyf-tabular">
                <span class="cal-badge__month">{{ formatMonth(item.sessionStart) }}</span>
                <strong class="cal-badge__day">{{ formatDay(item.sessionStart) }}</strong>
              </div>

              <div class="course-head-main">
                <div class="course-tags">
                  <span class="c-pill">{{ item.tradeType === 1 ? '线下小班' : '线上课堂' }}</span>
                  <span v-if="item.tagName" class="c-pill">{{ item.tagName }}</span>
                  <span class="c-pill">{{ item.campusName || '主校区' }}</span>
                </div>
                <h3 class="course-title ellipsis-2">{{ item.title }}</h3>
              </div>

              <span class="session-state" :class="`is-st-${item.sessionStatus}`">
                {{ statusText(item.sessionStatus) }}
              </span>
            </div>

            <p class="course-desc ellipsis-2">{{ item.description }}</p>

            <!-- Time & Geofence Strip -->
            <div class="schedule-bar xyf-tabular">
              <span>时段：{{ formatTimeRange(item.sessionStart, item.sessionEnd) }}</span>
              <span v-if="item.tradeType === 1 && item.meetupPoiName" class="ellipsis">
                📍 {{ item.meetupPoiName }}（{{ item.meetupRadiusMeters || 80 }}m）
              </span>
            </div>

            <!-- Visual Seat Matrix & Tiered Rebate Ladder -->
            <div class="seat-module">
              <div class="seat-module__meta xyf-tabular">
                <span>
                  席位进度 <strong>{{ item.joinedCount }} / {{ item.maxParticipants }}</strong> 人
                  <small>（{{ item.minParticipants }}人成班）</small>
                </span>
                <span v-if="item.tierRebateApplied === 1" class="tier-unlocked">
                  ✦ 已解锁全员阶梯价
                </span>
                <span v-else class="tier-target">
                  满 {{ item.tierTargetCount }} 人降至 ¥{{ Number(item.tierDiscountPrice).toFixed(0) }}/人
                </span>
              </div>

              <!-- Visual Seat Capsules (up to 12 dots, fallback bar if > 12) -->
              <div v-if="item.maxParticipants <= 14" class="seat-dots">
                <span
                  v-for="seatIdx in item.maxParticipants"
                  :key="seatIdx"
                  class="seat-dot"
                  :class="{
                    'is-filled': seatIdx <= item.joinedCount,
                    'is-tier-mark': seatIdx === item.tierTargetCount
                  }"
                  :title="seatIdx === item.tierTargetCount ? `第 ${seatIdx} 人触发阶梯降价` : `席位 #${seatIdx}`"
                />
              </div>
              <div v-else class="seat-progress-track">
                <div
                  class="seat-progress-fill"
                  :style="{ width: `${Math.min(100, Math.round((item.joinedCount / item.maxParticipants) * 100))}%` }"
                />
              </div>
            </div>

            <!-- Footer: Instructor + Tiered Price Callout -->
            <div class="course-card__foot">
              <div class="instructor-mini">
                <XyfAvatar :src="item.creatorAvatar" :name="item.creatorName || '讲师'" :size="28" />
                <div class="instructor-mini__info">
                  <strong>{{ item.creatorName || '认证讲师' }}</strong>
                  <span>{{ item.creatorEduVerified === 1 ? 'edu.cn 已核验' : '认证创作者' }}</span>
                </div>
              </div>

              <div class="price-ladder xyf-tabular">
                <span
                  v-if="Number(item.basePrice) > Number(item.tierDiscountPrice)"
                  class="save-pill"
                >
                  成团立省 ¥{{ (Number(item.basePrice) - Number(item.tierDiscountPrice)).toFixed(0) }}
                </span>
                <div class="cur-price">
                  <span class="yen">¥</span>{{ Number(item.currentUnitPrice || item.basePrice).toFixed(0) }}
                  <small>/人</small>
                </div>
              </div>
            </div>
          </article>
        </div>
      </template>

      <!-- VIEW 2 & 3: My Joined / My Created Workspace -->
      <section v-else class="workspace-panel">
        <div
          v-if="(activeWorkspace === 'joined' ? myJoined : myCreated).length === 0"
          class="xyf-surface-card empty-wrap"
        >
          <XyfEmptyState
            :title="activeWorkspace === 'joined' ? '暂无报名参团记录' : '暂无开办团课记录'"
            :description="
              activeWorkspace === 'joined'
                ? '挑选感兴趣的期末串讲或专题小班课，人数达标自动享受阶梯退差价。'
                : '发起多人共享档期课程，一次授课服务多位同学，放大专业技能价值。'
            "
          >
            <template #action>
              <el-button
                v-if="activeWorkspace === 'joined'"
                type="primary"
                @click="switchWorkspace('market')"
              >
                浏览招募中团课
              </el-button>
              <el-button v-else type="primary" @click="openCreateDialog">发起拼班团课</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="my-session-grid">
          <div
            v-for="s in activeWorkspace === 'joined' ? myJoined : myCreated"
            :key="s.id"
            class="xyf-surface-card my-session-card"
            @click="router.push(`/groups/${s.id}`)"
          >
            <div class="my-session-card__top">
              <span class="xyf-mono">{{ s.sessionNo }}</span>
              <el-tag size="small" :type="s.sessionStatus === 1 ? 'success' : 'info'">
                {{ statusText(s.sessionStatus) }}
              </el-tag>
            </div>
            <h3>{{ s.title }}</h3>
            <div class="my-session-card__meta xyf-tabular">
              <span>开课：{{ formatDateTime(s.sessionStart).slice(0, 16) }}</span>
              <span>已报 {{ s.joinedCount }}/{{ s.maxParticipants }} 人</span>
            </div>
            <div class="my-session-card__foot xyf-tabular">
              <span>当前执行单价 <strong>¥{{ Number(s.currentUnitPrice || s.basePrice).toFixed(2) }}</strong></span>
              <span>查看教室详情 →</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Course Creation Studio Dialog with Live Tiered Revenue Simulator -->
    <el-dialog
      v-model="createVisible"
      title="发起校内多人拼班团课"
      width="720px"
      :close-on-click-modal="false"
    >
      <!-- Live Tiered Economics Simulator Banner -->
      <div class="econ-simulator xyf-tabular">
        <div class="econ-cell">
          <span>成班保底总营收 ({{ form.minParticipants }}人)</span>
          <strong>¥{{ minGrossRevenue }}</strong>
        </div>
        <div class="econ-cell">
          <span>满班预计总营收 ({{ form.maxParticipants }}人)</span>
          <strong>¥{{ maxGrossRevenue }}</strong>
        </div>
        <div class="econ-cell is-gold">
          <span>达 {{ form.tierTargetCount }} 人每位学员自动省</span>
          <strong>¥{{ perStudentRebate }}</strong>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="拼班团课标题（4~120字）" required>
          <el-input
            v-model="form.title"
            maxlength="120"
            show-word-limit
            placeholder="例如：高数下册期末核心题型 6 人冲刺小班（含自编历年真题讲义）"
          />
        </el-form-item>

        <div class="row-3">
          <el-form-item label="所属校区" required>
            <el-select v-model="form.campusId" style="width: 100%">
              <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程分类">
            <el-select v-model="form.tagId" style="width: 100%">
              <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
            </el-select>
          </el-form-item>
          <el-form-item label="授课方式">
            <el-select v-model="form.tradeType" style="width: 100%">
              <el-option label="校园线下当面小班" :value="1" />
              <el-option label="线上共享课堂" :value="2" />
            </el-select>
          </el-form-item>
        </div>

        <div class="row-3">
          <el-form-item label="最低成班人数" required>
            <el-input-number v-model="form.minParticipants" :min="2" :max="20" style="width: 100%" />
          </el-form-item>
          <el-form-item label="触发阶梯优惠人数" required>
            <el-input-number v-model="form.tierTargetCount" :min="2" :max="50" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高满班人数" required>
            <el-input-number v-model="form.maxParticipants" :min="2" :max="50" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="基础拼班单价（元/人）" required>
            <el-input-number v-model="form.basePrice" :min="1" :precision="2" :step="5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="满阶梯优惠单价（元/人，达标自动退早期学员差价）" required>
            <el-input-number v-model="form.tierDiscountPrice" :min="1" :precision="2" :step="5" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="开课开始时间" required>
            <el-date-picker
              v-model="form.sessionStart"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开课时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开课结束时间" required>
            <el-date-picker
              v-model="form.sessionEnd"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item
          v-if="form.tradeType === 1"
          label="在真实校园地图标定线下授课地点与签到核销半径"
          required
        >
          <CampusMapPicker
            :campus-id="form.campusId || 1"
            v-model="mapSpot"
            style="width: 100%"
            @fence-change="ok => (fenceValid = ok)"
          />
        </el-form-item>

        <el-form-item label="课程大纲与配套讲义说明" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="800"
            show-word-limit
            placeholder="详细介绍本节团课的核心模块、配套讲义资料及适合哪些专业年级的同学参加..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="creating"
          :disabled="form.tradeType === 1 && !fenceValid"
          @click="submitCreate"
        >
          确认发布拼班团课
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { listCampuses } from '@/api/campus'
import {
  listGroupSessions,
  createGroupSession,
  listMyCreatedGroupSessions,
  listMyJoinedGroupSessions,
  type GroupSessionItem
} from '@/api/groupSession'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { CampusInfo } from '@/types/dict'
import CampusMapPicker, { type MapLocationValue } from '@/components/business/CampusMapPicker.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dictStore = useDictStore()

const activeWorkspace = ref<'market' | 'joined' | 'created'>('market')
const loading = ref(false)
const sessions = ref<GroupSessionItem[]>([])
const campuses = ref<CampusInfo[]>([])

const filters = reactive({
  campusId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  tradeType: undefined as number | undefined,
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : ''
})

const createVisible = ref(false)
const creating = ref(false)
const fenceValid = ref(true)
const form = reactive({
  campusId: 1,
  tagId: undefined as number | undefined,
  title: '',
  description: '',
  basePrice: 60,
  tierTargetCount: 4,
  tierDiscountPrice: 45,
  minParticipants: 2,
  maxParticipants: 6,
  tradeType: 1,
  sessionStart: '2026-12-05T19:00:00',
  sessionEnd: '2026-12-05T21:00:00'
})

const mapSpot = ref<MapLocationValue>({
  latitude: undefined,
  longitude: undefined,
  poiName: '',
  detailAddress: '',
  radiusMeters: 80
})

const myJoined = ref<GroupSessionItem[]>([])
const myCreated = ref<GroupSessionItem[]>([])

const hasActiveFilters = computed(() =>
  Boolean(filters.campusId || filters.tagId || filters.tradeType || filters.keyword.trim())
)

const minGrossRevenue = computed(() => {
  const unit = form.minParticipants >= form.tierTargetCount ? form.tierDiscountPrice : form.basePrice
  return (Number(unit || 0) * Number(form.minParticipants || 0)).toFixed(0)
})

const maxGrossRevenue = computed(() => {
  const unit = form.maxParticipants >= form.tierTargetCount ? form.tierDiscountPrice : form.basePrice
  return (Number(unit || 0) * Number(form.maxParticipants || 0)).toFixed(0)
})

const perStudentRebate = computed(() =>
  Math.max(0, Number(form.basePrice || 0) - Number(form.tierDiscountPrice || 0)).toFixed(0)
)

function statusText(st: number) {
  if (st === 0) return '招募拼班中'
  if (st === 1) return '已成班待开课'
  if (st === 2) return '已满员锁定'
  if (st === 3) return '已完结'
  return '已取消'
}

function formatMonth(dt?: string) {
  if (!dt) return '近期'
  const d = new Date(dt)
  return Number.isNaN(d.getTime()) ? '近期' : `${d.getMonth() + 1}月`
}

function formatDay(dt?: string) {
  if (!dt) return '--'
  const d = new Date(dt)
  return Number.isNaN(d.getTime()) ? '--' : String(d.getDate()).padStart(2, '0')
}

function formatTimeRange(start?: string, end?: string) {
  if (!start) return '待定'
  const s = formatDateTime(start).slice(5, 16)
  const e = end ? formatDateTime(end).slice(11, 16) : ''
  return e ? `${s} – ${e}` : s
}

function selectTag(id?: number) {
  filters.tagId = id
  reload()
}

function selectCampus(id?: number) {
  filters.campusId = id
  reload()
}

function selectTradeType(type?: number) {
  filters.tradeType = type
  reload()
}

function resetFilters() {
  filters.campusId = undefined
  filters.tagId = undefined
  filters.tradeType = undefined
  filters.keyword = ''
  reload()
}

function reload() {
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listGroupSessions({
      pageNum: 1,
      pageSize: 24,
      campusId: filters.campusId,
      tagId: filters.tagId,
      tradeType: filters.tradeType,
      keyword: filters.keyword.trim() || undefined
    })
    sessions.value = res.records || []
  } catch (err) {
    notifyFallbackError(err, '加载拼班大厅失败')
  } finally {
    loading.value = false
  }
}

async function loadMySessions() {
  if (!userStore.userInfo || userStore.isAdmin) return
  const [j, c] = await Promise.all([
    listMyJoinedGroupSessions().catch(() => []),
    listMyCreatedGroupSessions().catch(() => [])
  ])
  myJoined.value = j
  myCreated.value = c
}

async function switchWorkspace(tab: 'market' | 'joined' | 'created') {
  activeWorkspace.value = tab
  if (tab !== 'market') {
    await loadMySessions()
  }
}

function openCreateDialog() {
  if (!userStore.userInfo) {
    router.push({ path: '/login', query: { redirect: '/groups?compose=1' } })
    return
  }
  if (!userStore.hasPublishPermission) {
    ElMessage.info('发起拼班团课需先完成创作者发布权限认证')
    router.push('/user/center?tab=permission')
    return
  }
  form.campusId = userStore.userInfo?.campusId || campuses.value[0]?.id || 1
  form.tagId = dictStore.serviceTags?.[0]?.tagId
  createVisible.value = true
}

async function submitCreate() {
  if (!form.title.trim() || form.title.trim().length < 4) {
    ElMessage.warning('团课标题至少填写 4 个字')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请填写课程大纲说明')
    return
  }
  if (form.tierDiscountPrice > form.basePrice) {
    ElMessage.warning('阶梯优惠单价不能高于基础单价')
    return
  }
  if (form.maxParticipants < form.minParticipants) {
    ElMessage.warning('最高满班人数不能小于最低成班人数')
    return
  }
  if (form.tradeType === 1 && (!mapSpot.value.latitude || !mapSpot.value.poiName.trim())) {
    ElMessage.warning('请在校园地图上点击标记授课地点')
    return
  }

  creating.value = true
  try {
    const id = await createGroupSession({
      campusId: form.campusId,
      tagId: form.tagId,
      title: form.title.trim(),
      description: form.description.trim(),
      basePrice: form.basePrice,
      tierTargetCount: form.tierTargetCount,
      tierDiscountPrice: form.tierDiscountPrice,
      minParticipants: form.minParticipants,
      maxParticipants: form.maxParticipants,
      tradeType: form.tradeType,
      sessionStart: form.sessionStart,
      sessionEnd: form.sessionEnd,
      meetupLat: form.tradeType === 1 ? mapSpot.value.latitude : undefined,
      meetupLng: form.tradeType === 1 ? mapSpot.value.longitude : undefined,
      meetupPoiName: form.tradeType === 1 ? mapSpot.value.poiName.trim() : undefined,
      meetupAddress: form.tradeType === 1 ? mapSpot.value.detailAddress?.trim() : undefined,
      meetupRadiusMeters: form.tradeType === 1 ? mapSpot.value.radiusMeters || 80 : undefined
    })
    ElMessage.success('多人拼班团课已发布！')
    createVisible.value = false
    router.push(`/groups/${id}`)
  } catch (err) {
    notifyFallbackError(err, '发布团课失败')
  } finally {
    creating.value = false
  }
}

watch(
  () => route.query,
  (q) => {
    if (typeof q.keyword === 'string' && q.keyword !== filters.keyword) {
      filters.keyword = q.keyword
      reload()
    }
    if (q.view === 'joined') {
      switchWorkspace('joined')
    }
    if (q.compose === '1') {
      openCreateDialog()
    }
  }
)

onMounted(async () => {
  await Promise.all([
    listCampuses().then(res => (campuses.value = res)).catch(() => {}),
    dictStore.fetchServiceTags().catch(() => {}),
    fetchList(),
    loadMySessions()
  ])
  if (route.query.view === 'joined') {
    activeWorkspace.value = 'joined'
  }
  if (route.query.compose === '1') {
    openCreateDialog()
  }
})
</script>

<style scoped lang="scss">
.group-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 22px;

  &__left {
    max-width: 640px;

    h1 {
      margin: 6px 0 8px;
      font-size: 32px;
      font-weight: 780;
      letter-spacing: -0.03em;
      color: var(--xyf-ink);
    }

    p {
      margin: 0;
      font-size: 14.5px;
      color: var(--xyf-ink-2);
      line-height: 1.55;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.mechanism-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 15px 24px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  margin-bottom: 24px;

  .mech-step {
    display: flex;
    align-items: center;
    gap: 12px;

    .mech-num {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #18181b;
      color: #ffffff;
      font-size: 11.5px;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    strong {
      display: block;
      font-size: 13.5px;
      color: var(--xyf-ink);
    }

    span {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }

  .mech-arrow {
    color: var(--xyf-border-strong);
    font-weight: 600;

    @media (max-width: 820px) {
      display: none;
    }
  }
}

.filter-studio {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    border-bottom: 1px solid var(--xyf-border);

    .xyf-tabs {
      border-bottom: none;
      flex: 1;
      min-width: 240px;
    }
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.campus-pills {
  display: flex;
  gap: 6px;
  padding-bottom: 8px;
  flex-wrap: wrap;
}

.campus-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &.is-active {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

.search-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 280px;
  flex: 1;
  max-width: 360px;

  .reset-link {
    border: none;
    background: transparent;
    color: var(--xyf-ink-2);
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;
  }
}

/* Schedule-Driven Course Cards */
.session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 18px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.empty-wrap {
  padding: 52px 24px;
}

.course-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 18px;
  padding: 22px 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  transition: transform var(--xyf-transition), box-shadow var(--xyf-transition), border-color var(--xyf-transition);

  &:hover {
    transform: translateY(-3px);
    border-color: #18181b;
    box-shadow: var(--xyf-shadow-md);
  }

  &__head {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }

  &__foot {
    margin-top: auto;
    padding-top: 14px;
    border-top: 1px dashed var(--xyf-border-strong);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
}

.cal-badge {
  width: 52px;
  height: 54px;
  border-radius: 12px;
  background: #18181b;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1.1;

  &__month {
    font-size: 10.5px;
    color: rgba(255, 255, 255, 0.65);
  }

  &__day {
    font-size: 18px;
    font-weight: 800;
    margin-top: 2px;
  }
}

.course-head-main {
  flex: 1;
  min-width: 0;

  .course-tags {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    margin-bottom: 5px;
  }

  .c-pill {
    font-size: 11px;
    font-weight: 550;
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border-subtle);
    color: var(--xyf-ink-2);
  }

  .course-title {
    margin: 0;
    font-size: 16.5px;
    font-weight: 700;
    line-height: 1.38;
    color: var(--xyf-ink);
  }
}

.session-state {
  font-size: 11px;
  font-weight: 650;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);
  white-space: nowrap;

  &.is-st-0,
  &.is-st-1 {
    background: #18181b;
    color: #ffffff;
  }
}

.course-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--xyf-ink-2);
}

.schedule-bar {
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--xyf-ink-2);
}

.seat-module {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border-subtle);
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
    color: var(--xyf-ink-2);

    small {
      color: var(--xyf-ink-3);
    }
  }

  .tier-unlocked {
    color: #9a5b13;
    font-weight: 700;
  }

  .tier-target {
    color: var(--xyf-ink);
    font-weight: 600;
  }
}

.seat-dots {
  display: flex;
  align-items: center;
  gap: 6px;
}

.seat-dot {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: #e4e4e7;
  transition: background var(--xyf-transition);

  &.is-filled {
    background: #18181b;
  }

  &.is-tier-mark {
    box-shadow: 0 0 0 2px #c8a97e;
  }
}

.seat-progress-track {
  height: 7px;
  border-radius: 999px;
  background: #e4e4e7;
  overflow: hidden;

  .seat-progress-fill {
    height: 100%;
    background: #18181b;
    border-radius: 999px;
  }
}

.instructor-mini {
  display: flex;
  align-items: center;
  gap: 9px;

  &__info {
    display: flex;
    flex-direction: column;

    strong {
      font-size: 13px;
      color: var(--xyf-ink);
    }

    span {
      font-size: 11px;
      color: var(--xyf-ink-3);
    }
  }
}

.price-ladder {
  display: flex;
  align-items: center;
  gap: 10px;

  .save-pill {
    padding: 3px 8px;
    border-radius: 999px;
    background: var(--xyf-champagne-soft);
    border: 1px solid var(--xyf-champagne-border);
    color: #9a5b13;
    font-size: 11px;
    font-weight: 650;
  }

  .cur-price {
    font-size: 21px;
    font-weight: 800;
    color: var(--xyf-ink);

    .yen {
      font-size: 13px;
      margin-right: 1px;
    }

    small {
      font-size: 12px;
      font-weight: 500;
      color: var(--xyf-ink-3);
    }
  }
}

/* My Sessions Workspace */
.my-session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.my-session-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #18181b;
    transform: translateY(-2px);
  }

  &__top,
  &__meta,
  &__foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  &__foot {
    margin-top: auto;
    padding-top: 10px;
    border-top: 1px solid var(--xyf-border-subtle);
    color: var(--xyf-ink-2);
    strong {
      color: var(--xyf-ink);
    }
  }
}

/* Dialog Economics Simulator */
.econ-simulator {
  margin-bottom: 18px;
  padding: 14px 18px;
  border-radius: 12px;
  background: #18181b;
  color: #ffffff;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .econ-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;

    span {
      font-size: 11.5px;
      color: rgba(255, 255, 255, 0.65);
    }

    strong {
      font-size: 18px;
      font-weight: 750;
    }

    &.is-gold strong {
      color: #e5c79e;
    }
  }
}

.row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.row-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .row-3,
  .row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
