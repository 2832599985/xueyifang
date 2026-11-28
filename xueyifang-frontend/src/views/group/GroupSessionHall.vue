<template>
  <div class="group-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Minimalist Page Header -->
      <header class="page-head">
        <div class="page-head__title-wrap">
          <h1>拼班团课</h1>
          <span v-if="activeWorkspace === 'market' && !loading" class="page-head__count xyf-tabular">
            {{ sessions.length }} 门课程
          </span>
        </div>

        <div class="page-head__actions">
          <div v-if="userStore.userInfo && !userStore.isAdmin" class="xyf-segmented" role="tablist">
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'market' }"
              @click="switchWorkspace('market')"
            >
              <span>全部课程</span>
            </button>
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'joined' }"
              @click="switchWorkspace('joined')"
            >
              <span>我报名的</span>
              <span v-if="myJoined.length" class="xyf-segmented__count">{{ myJoined.length }}</span>
            </button>
            <button
              v-if="userStore.hasPublishPermission"
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'created' }"
              @click="switchWorkspace('created')"
            >
              <span>我发起的</span>
              <span v-if="myCreated.length" class="xyf-segmented__count">{{ myCreated.length }}</span>
            </button>
          </div>

          <el-button
            v-if="userStore.hasPublishPermission"
            type="primary"
            @click="openCreateDialog"
          >
            <el-icon><Plus /></el-icon>
            <span>发起拼班</span>
          </el-button>
        </div>
      </header>

      <!-- VIEW 1: Public Group Sessions -->
      <template v-if="activeWorkspace === 'market'">
        <section class="filter-bar">
          <!-- Row 1: Full-width Category Tabs (never clipped) -->
          <div class="xyf-tabs filter-bar__tabs">
            <button
              type="button"
              class="xyf-tab"
              :class="{ 'is-active': !filters.tagId }"
              @click="selectTag(undefined)"
            >
              全部分类
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

          <!-- Row 2: Compact Secondary Filters + Search -->
          <div class="filter-bar__controls">
            <div class="filter-bar__left">
              <div class="xyf-segmented" role="tablist" aria-label="授课形式">
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': !filters.tradeType }"
                  @click="selectTradeType(undefined)"
                >
                  全部形式
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.tradeType === 1 }"
                  @click="selectTradeType(1)"
                >
                  <el-icon><Location /></el-icon>
                  <span>线下小班</span>
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.tradeType === 2 }"
                  @click="selectTradeType(2)"
                >
                  <el-icon><Monitor /></el-icon>
                  <span>线上课堂</span>
                </button>
              </div>

              <el-select
                v-model="filters.campusId"
                placeholder="全部校区"
                clearable
                class="campus-select"
                @change="reload"
              >
                <el-option
                  v-for="c in campuses"
                  :key="c.id"
                  :label="c.campusName"
                  :value="c.id"
                />
              </el-select>
            </div>

            <div class="filter-bar__right">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索团课主题"
                clearable
                :prefix-icon="Search"
                class="search-input"
                @keyup.enter="reload"
                @clear="reload"
              />
              <button v-if="hasActiveFilters" type="button" class="reset-btn" @click="resetFilters">
                重置
              </button>
            </div>
          </div>
        </section>

        <!-- Loading Skeleton -->
        <div v-if="loading" class="session-grid">
          <div v-for="n in 6" :key="n" class="course-card course-card--skeleton">
            <el-skeleton :rows="4" animated />
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="sessions.length === 0" class="empty-panel">
          <XyfEmptyState
            title="当前暂无符合条件的拼班团课"
            description="尝试调整筛选条件，或等待创作者发布新的校内专题小班课。"
          >
            <template #action>
              <el-button v-if="hasActiveFilters" plain @click="resetFilters">清空筛选</el-button>
              <el-button v-if="userStore.hasPublishPermission" type="primary" @click="openCreateDialog">
                发起拼班
              </el-button>
            </template>
          </XyfEmptyState>
        </div>

        <!-- Clean Editorial 3-Column Course Grid -->
        <div v-else class="session-grid">
          <article
            v-for="item in sessions"
            :key="item.id"
            class="course-card"
            tabindex="0"
            role="link"
            :aria-label="item.title"
            @click="router.push(`/groups/${item.id}`)"
            @keydown.enter="router.push(`/groups/${item.id}`)"
          >
            <!-- 1. Pure Animated Delivery SVG + Category/Campus + Animated Status Emblem -->
            <div class="course-card__meta">
              <div class="meta-line ellipsis">
                <XyfDeliveryIcon :trade-type="item.tradeType" size="xs" />
                <span v-if="item.tagName">{{ item.tagName }}</span>
                <span v-if="item.tagName" class="sep" aria-hidden="true">·</span>
                <span class="ellipsis">{{ item.campusName || '主校区' }}</span>
              </div>
              <XyfStatusBadge
                :tone="statusTone(item.sessionStatus)"
                :icon="statusIcon(item.sessionStatus)"
                :tooltip="shortStatusText(item.sessionStatus)"
                :icon-only="true"
                size="sm"
              />
            </div>

            <!-- 2. Primary Focal Anchor: Title -->
            <h2 class="course-card__title ellipsis-2">{{ item.title }}</h2>

            <!-- 3. Key Logistics: Clean Time & Venue -->
            <div class="course-card__logistics xyf-tabular">
              <span class="meta-icon-txt">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatSessionTimeRange(item.sessionStart, item.sessionEnd) }}</span>
              </span>
              <template v-if="item.tradeType === 1 && item.meetupPoiName">
                <span class="sep" aria-hidden="true">·</span>
                <span class="ellipsis">{{ item.meetupPoiName }}</span>
              </template>
            </div>

            <!-- 4. Minimal Seat Progress & Animated Twinkling Tier Spark -->
            <div class="course-card__progress">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :class="{ 'is-tier-hit': item.tierRebateApplied === 1 }"
                  :style="{ width: `${seatPercent(item)}%` }"
                />
              </div>
              <div class="progress-caption xyf-tabular">
                <span class="meta-icon-txt">
                  <el-icon><User /></el-icon>
                  <span>{{ item.joinedCount }}/{{ item.maxParticipants }}</span>
                </span>
                <span v-if="item.tierRebateApplied === 1" class="tier-note is-hit">
                  <XyfStatusBadge
                    tone="success"
                    icon="spark"
                    :tooltip="`已解锁全员阶梯价 ¥${Number(item.tierDiscountPrice).toFixed(0)}`"
                    :icon-only="true"
                    size="xs"
                  />
                  <span>¥{{ Number(item.tierDiscountPrice).toFixed(0) }}</span>
                </span>
                <span
                  v-else-if="Number(item.basePrice) > Number(item.tierDiscountPrice)"
                  class="tier-note"
                >
                  <XyfStatusBadge
                    tone="warning"
                    icon="spark"
                    :tooltip="`满 ${item.tierTargetCount} 人自动享阶梯价 ¥${Number(item.tierDiscountPrice).toFixed(0)}`"
                    :icon-only="true"
                    size="xs"
                  />
                  <span>{{ item.tierTargetCount }}人→¥{{ Number(item.tierDiscountPrice).toFixed(0) }}</span>
                </span>
              </div>
            </div>

            <!-- 5. Clean Hairline Footer: Instructor + Verified Shield + Price Anchor -->
            <div class="course-card__foot">
              <div class="instructor-inline">
                <XyfAvatar :src="item.creatorAvatar" :name="item.creatorName || '讲师'" :size="24" />
                <span class="instructor-inline__name ellipsis">{{ item.creatorName || '认证讲师' }}</span>
                <XyfVerifiedMark :verified="item.creatorEduVerified === 1" />
              </div>

              <div class="price-anchor xyf-tabular">
                <span
                  v-if="item.tierRebateApplied === 1 && Number(item.basePrice) > Number(item.currentUnitPrice || item.basePrice)"
                  class="price-anchor__orig"
                >
                  ¥{{ Number(item.basePrice).toFixed(0) }}
                </span>
                <strong class="price-anchor__val">
                  <span class="yen">¥</span>{{ Number(item.currentUnitPrice || item.basePrice).toFixed(0) }}
                </strong>
                <span class="price-anchor__unit">/人</span>
              </div>
            </div>
          </article>
        </div>
      </template>

      <!-- VIEW 2 & 3: My Joined / My Created Workspace -->
      <section v-else class="workspace-panel">
        <div
          v-if="(activeWorkspace === 'joined' ? myJoined : myCreated).length === 0"
          class="empty-panel"
        >
          <XyfEmptyState
            :title="activeWorkspace === 'joined' ? '暂无报名参团记录' : '暂无开办团课记录'"
            :description="
              activeWorkspace === 'joined'
                ? '挑选感兴趣的专题小班课，人数达标自动享受阶梯退差价。'
                : '发起多人共享档期课程，一次授课服务多位同学。'
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

        <div v-else class="my-session-list">
          <article
            v-for="s in activeWorkspace === 'joined' ? myJoined : myCreated"
            :key="s.id"
            class="my-session-row"
            tabindex="0"
            role="link"
            @click="router.push(`/groups/${s.id}`)"
            @keydown.enter="router.push(`/groups/${s.id}`)"
          >
            <div class="my-session-row__main">
              <div class="meta-line xyf-tabular">
                <XyfStatusBadge
                  :tone="statusTone(s.sessionStatus)"
                  :icon="statusIcon(s.sessionStatus)"
                  :tooltip="shortStatusText(s.sessionStatus)"
                  :icon-only="true"
                  size="xs"
                />
                <span class="xyf-mono">{{ s.sessionNo }}</span>
              </div>
              <h3>{{ s.title }}</h3>
              <div class="my-session-row__sub xyf-tabular">
                <span>{{ formatSessionTimeRange(s.sessionStart, s.sessionEnd) }}</span>
                <span class="sep" aria-hidden="true">·</span>
                <span>已报 {{ s.joinedCount }}/{{ s.maxParticipants }} 人</span>
              </div>
            </div>
            <div class="my-session-row__right xyf-tabular">
              <strong>¥{{ Number(s.currentUnitPrice || s.basePrice).toFixed(0) }}<span>/人</span></strong>
              <span class="row-link">查看详情 →</span>
            </div>
          </article>
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
import { Plus, Search, Location, Monitor, Calendar, User } from '@element-plus/icons-vue'
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
import { formatSessionTimeRange } from '@/utils/format'
import type { CampusInfo } from '@/types/dict'
import CampusMapPicker, { type MapLocationValue } from '@/components/business/CampusMapPicker.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge, { type StatusBadgeTone, type StatusBadgeIcon } from '@/components/ui/XyfStatusBadge.vue'
import XyfVerifiedMark from '@/components/ui/XyfVerifiedMark.vue'
import XyfDeliveryIcon from '@/components/ui/XyfDeliveryIcon.vue'

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

function seatPercent(item: GroupSessionItem) {
  if (!item.maxParticipants) return 0
  return Math.min(100, Math.round((item.joinedCount / item.maxParticipants) * 100))
}

function shortStatusText(st: number) {
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

function selectTag(id?: number) {
  filters.tagId = id
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
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 24px;

  &__title-wrap {
    display: flex;
    align-items: baseline;
    gap: 12px;

    h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: -0.025em;
      color: var(--xyf-ink);
    }
  }

  &__count {
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

/* Crisp Two-Row Filter Bar (zero clipped tabs) */
.filter-bar {
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__tabs {
    width: 100%;
  }

  &__controls {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.campus-select {
  width: 168px;
}

.search-input {
  width: 240px;

  @media (max-width: 640px) {
    width: 100%;
  }
}

.reset-btn {
  border: none;
  background: transparent;
  padding: 6px 4px;
  font-size: 13px;
  color: var(--xyf-ink-3);
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    color: var(--xyf-ink);
  }
}

/* Clean 3-Column Editorial Grid */
.session-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;

  @media (max-width: 1080px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 680px) {
    grid-template-columns: 1fr;
  }
}

.empty-panel {
  padding: 64px 24px;
  border-top: 1px solid var(--xyf-border);
}

.course-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 14px;
  padding: 22px 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  cursor: pointer;
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1),
    border-color 0.18s ease,
    box-shadow 0.18s ease;

  &:hover,
  &:focus-visible {
    outline: none;
    transform: translateY(-2px);
    border-color: var(--xyf-ink);
    box-shadow: var(--xyf-shadow-sm);
  }

  &--skeleton {
    cursor: default;
    &:hover {
      transform: none;
      border-color: var(--xyf-border);
      box-shadow: none;
    }
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  &__title {
    margin: 0;
    font-size: 17.5px;
    font-weight: 700;
    line-height: 1.42;
    letter-spacing: -0.015em;
    color: var(--xyf-ink);
    text-wrap: balance;
    min-height: 50px;
  }

  &__logistics {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--xyf-ink-2);
    min-width: 0;

    > span:first-child {
      white-space: nowrap;
      flex-shrink: 0;
    }
  }

  &__progress {
    margin-top: 2px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__foot {
    margin-top: auto;
    padding-top: 16px;
    border-top: 1px solid var(--xyf-border-subtle);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
}

.meta-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.meta-icon-txt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;

  .el-icon {
    font-size: 13px;
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }
}

.sep {
  color: var(--xyf-ink-4);
  flex-shrink: 0;
}

.progress-track {
  height: 3px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--xyf-ink);
  transition: width 0.24s ease;

  &.is-tier-hit {
    background: var(--xyf-success);
  }
}

.progress-caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  color: var(--xyf-ink-3);

  .tier-note {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-weight: 600;
    color: var(--xyf-warning);

    &.is-hit {
      color: var(--xyf-success);
    }

    .spark-svg {
      width: 12px;
      height: 12px;
      flex-shrink: 0;
    }
  }
}

.instructor-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  &__name {
    font-size: 13px;
    color: var(--xyf-ink-2);
    font-weight: 500;
  }
}

.price-anchor {
  display: flex;
  align-items: baseline;
  gap: 3px;
  flex-shrink: 0;

  &__orig {
    font-size: 12.5px;
    color: var(--xyf-ink-4);
    text-decoration: line-through;
    margin-right: 4px;
  }

  &__val {
    font-size: 21px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--xyf-ink);
    line-height: 1;

    .yen {
      font-size: 13px;
      font-weight: 600;
      margin-right: 1px;
    }
  }

  &__unit {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

/* My Sessions Workspace (Clean Editorial List) */
.my-session-list {
  border-top: 1px solid var(--xyf-border);
}

.my-session-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 12px;
  border-bottom: 1px solid var(--xyf-border);
  cursor: pointer;
  transition: background 0.16s ease;

  &:hover,
  &:focus-visible {
    outline: none;
    background: var(--xyf-bg-subtle);
  }

  &__main {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  &__sub {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
    flex-shrink: 0;

    strong {
      font-size: 19px;
      font-weight: 700;
      color: var(--xyf-ink);

      span {
        font-size: 12px;
        font-weight: 400;
        color: var(--xyf-ink-3);
      }
    }

    .row-link {
      font-size: 12.5px;
      color: var(--xyf-ink-3);
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
      font-weight: 700;
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
