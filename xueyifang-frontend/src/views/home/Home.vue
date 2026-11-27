<template>
  <div class="home-page">
    <!-- 1. Full-Viewport Interactive Hero (Campus Ambient Canvas + Parallax Floating Works) -->
    <section class="hero-stage" @mousemove="handleHeroPointer" @mouseleave="resetHeroPointer">
      <CampusAmbientCanvas />

      <!-- Peripheral Mouse-Parallax Floating Campus Works (Desktop >= 1280px) -->
      <div v-if="heroFloatingWorks.length > 0" class="hero-float-layer" aria-hidden="false">
        <button
          v-for="(work, i) in heroFloatingWorks"
          :key="work.id"
          type="button"
          class="float-work-card"
          :class="`is-slot-${i}`"
          :style="getFloatCardStyle(i)"
          @click="router.push(`/services/${work.id}`)"
        >
          <img
            :src="buildImageUrl(work.coverImage)"
            :alt="work.serviceTitle"
            class="float-work-card__img"
          />
          <span class="float-work-card__caption">
            <span class="float-work-card__tag">{{ work.tagName }}</span>
            <span class="float-work-card__price xyf-tabular">{{ formatCleanPrice(work.price) }}</span>
          </span>
        </button>
      </div>

      <div class="xyf-container hero-content">
        <div class="hero-eyebrow">
          <span class="hero-eyebrow__dot" />
          <span>高校 .edu.cn 实名认证 · 电子围栏当面核销 · 平台全额担保</span>
        </div>

        <h1 class="hero-title">找到身边的校园行家</h1>
        <p class="hero-subtitle">标准化技能橱窗 · 反向悬赏急单 · 多人阶梯拼班</p>

        <!-- Multi-Scope Commercial Search Capsule -->
        <div class="search-dock">
          <div class="search-scopes" role="tablist" aria-label="搜索模式">
            <button
              type="button"
              class="scope-pill"
              :class="{ 'is-active': searchScope === 'services' }"
              @click="searchScope = 'services'"
            >
              找技能服务
            </button>
            <button
              type="button"
              class="scope-pill"
              :class="{ 'is-active': searchScope === 'demands' }"
              @click="searchScope = 'demands'"
            >
              接悬赏急单
            </button>
            <button
              type="button"
              class="scope-pill"
              :class="{ 'is-active': searchScope === 'groups' }"
              @click="searchScope = 'groups'"
            >
              报拼班小班
            </button>
          </div>

          <form class="capsule-search" @submit.prevent="handleSearch()">
            <el-icon class="capsule-search__icon"><Search /></el-icon>
            <input
              v-model="keyword"
              type="text"
              class="capsule-search__input"
              :placeholder="scopePlaceholder"
            />
            <button
              v-if="keyword"
              type="button"
              class="capsule-search__clear"
              aria-label="清空搜索"
              @click="keyword = ''"
            >
              <el-icon><Close /></el-icon>
            </button>
            <button type="submit" class="capsule-search__submit">探索</button>
          </form>
        </div>

        <div class="hero-hot-queries">
          <button
            v-for="q in hotQueries"
            :key="q"
            type="button"
            class="hero-hot-pill"
            @click="quickSearch(q)"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <button type="button" class="scroll-cue" @click="scrollToShowcase">
        <span>向下滑动浏览精选</span>
        <span class="scroll-cue__line" />
      </button>
    </section>

    <!-- 2. Non-Blocking Scroll-Linked Horizontal Showcase ("边滚边往下走" with Live Center Focus) -->
    <section ref="runwayRef" class="horizontal-runway">
      <div class="horizontal-sticky">
        <div class="xyf-container showcase-header">
          <div class="showcase-header__left">
            <h2 class="showcase-heading">精选技能橱窗</h2>
            <div class="category-tabs" role="tablist" aria-label="服务分类筛选">
              <button
                type="button"
                role="tab"
                class="category-tab"
                :class="{ 'is-active': activeTagId === undefined }"
                @click="selectCategory(undefined)"
              >
                全部
              </button>
              <button
                v-for="tag in tags"
                :key="tag.tagId"
                type="button"
                role="tab"
                class="category-tab"
                :class="{ 'is-active': activeTagId === tag.tagId }"
                @click="selectCategory(tag.tagId)"
              >
                {{ tag.tagName }}
              </button>
            </div>
          </div>

          <div class="showcase-header__right">
            <div class="track-nav-btns" v-if="services.length > 2">
              <button
                type="button"
                class="nav-circle-btn"
                aria-label="向左浏览"
                @click="nudgeTrack(-1)"
              >
                <el-icon><ArrowLeft /></el-icon>
              </button>
              <button
                type="button"
                class="nav-circle-btn"
                aria-label="向右浏览"
                @click="nudgeTrack(1)"
              >
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <router-link to="/services" class="view-all-link">
              全部 {{ totalServiceCount }} 项
              <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
        </div>

        <!-- Horizontal Viewport Stage -->
        <div ref="viewportRef" class="horizontal-viewport">
          <div v-if="servicesLoading" class="horizontal-skeleton">
            <div v-for="n in 4" :key="n" class="skeleton-slide">
              <el-skeleton animated>
                <template #template>
                  <el-skeleton-item
                    variant="image"
                    style="width: 100%; aspect-ratio: 4 / 3; height: auto; border-radius: 18px"
                  />
                  <div style="padding: 16px 4px 0">
                    <el-skeleton-item variant="text" style="width: 38%" />
                    <el-skeleton-item variant="h3" style="width: 82%; margin-top: 10px" />
                  </div>
                </template>
              </el-skeleton>
            </div>
          </div>

          <div v-else-if="services.length === 0" class="xyf-container">
            <XyfEmptyState title="该分类下暂无服务" description="切换其他分类看看吧。">
              <template #action>
                <el-button @click="selectCategory(undefined)">查看全部</el-button>
              </template>
            </XyfEmptyState>
          </div>

          <div
            v-else
            ref="trackRef"
            class="horizontal-track"
            :style="trackTransformStyle"
          >
            <article
              v-for="(item, idx) in services"
              :key="item.id"
              class="cinema-card"
              :style="getCardDynamicStyle(idx)"
              @click="router.push(`/services/${item.id}`)"
            >
              <div class="cinema-card__media">
                <XyfImage
                  :src="buildImageUrl(item.coverImage)"
                  :alt="item.serviceTitle"
                  :lazy="false"
                  width="100%"
                  height="100%"
                  fit="cover"
                  class="cinema-card__img"
                />
              </div>

              <div class="cinema-card__body">
                <div class="cinema-card__meta">
                  <div class="cinema-card__author">
                    <XyfAvatar
                      :src="item.sellerAvatar"
                      :name="item.sellerName"
                      :size="22"
                    />
                    <span class="ellipsis">{{ item.sellerName }}</span>
                  </div>
                  <span v-if="item.averageRating" class="cinema-card__rating xyf-tabular">
                    <el-icon><StarFilled /></el-icon>
                    {{ Number(item.averageRating).toFixed(1) }}
                  </span>
                </div>

                <h3 class="cinema-card__title ellipsis">{{ item.serviceTitle }}</h3>

                <div class="cinema-card__foot">
                  <span class="cinema-card__price xyf-tabular">{{ formatCleanPrice(item.price) }}</span>
                  <span class="cinema-card__tag">{{ item.tagName }}</span>
                </div>
              </div>
            </article>
          </div>
        </div>

        <!-- Minimal Scroll Progress Bar -->
        <div v-if="isDesktopMotion && services.length > 1" class="xyf-container progress-wrap">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: `${Math.max(10, effectiveProgress * 100)}%` }" />
          </div>
          <span class="progress-hint xyf-tabular">
            {{ String(activeFocusIndex + 1).padStart(2, '0') }} / {{ String(services.length).padStart(2, '0') }}
          </span>
        </div>
      </div>
    </section>

    <!-- 2.5 Live Campus Commercial Ecosystem Bento Matrix (Bounties + Co-Learning + Top Creators) -->
    <section class="ecosystem-section">
      <div class="xyf-container">
        <div class="eco-head">
          <div>
            <span class="xyf-kicker">LIVE CAMPUS MARKETPLACE</span>
            <h2 class="eco-title">实时校园协作矩阵</h2>
          </div>
          <p class="eco-sub">找不到现成服务？发悬赏让全校行家主动报价，或加入多人阶梯小班平摊课时费。</p>
        </div>

        <div class="eco-bento">
          <!-- Left Tall Card: C2B Reverse Bounties -->
          <div class="xyf-surface-card bento-panel bento-panel--bounty">
            <div class="bento-panel__head">
              <div>
                <span class="bento-tag">C2B 反向定制</span>
                <h3>校内悬赏急单榜</h3>
              </div>
              <div class="bento-head-actions">
                <button
                  type="button"
                  class="bento-ghost-btn"
                  @click="router.push('/demands?compose=1')"
                >
                  + 发悬赏
                </button>
                <router-link to="/demands" class="bento-more">
                  全部 <el-icon><ArrowRight /></el-icon>
                </router-link>
              </div>
            </div>

            <div v-if="liveDemands.length === 0" class="bento-empty">
              <p>当前暂无招募中的校内悬赏，立即发布第一条定制需求。</p>
              <el-button size="small" type="primary" @click="router.push('/demands?compose=1')">
                发布悬赏需求
              </el-button>
            </div>

            <div v-else class="bounty-feed">
              <div
                v-for="d in liveDemands"
                :key="d.id"
                class="bounty-mini-ticket"
                @click="router.push(`/demands/${d.id}`)"
              >
                <div class="bounty-mini-ticket__left">
                  <div class="bounty-mini-meta">
                    <span class="u-dot" :class="`is-u${d.urgencyLevel}`" />
                    <span class="u-txt">
                      {{ d.urgencyLevel === 3 ? '十万火急' : d.urgencyLevel === 2 ? '加急' : '常规' }}
                    </span>
                    <span class="sep">·</span>
                    <span>{{ d.campusName || '主校区' }}</span>
                    <span v-if="d.meetupPoiName" class="sep">·</span>
                    <span v-if="d.meetupPoiName" class="ellipsis">{{ d.meetupPoiName }}</span>
                  </div>
                  <h4 class="ellipsis">{{ d.title }}</h4>
                </div>

                <div class="bounty-mini-ticket__right xyf-tabular">
                  <strong>¥{{ Number(d.budgetMin).toFixed(0) }}-{{ Number(d.budgetMax).toFixed(0) }}</strong>
                  <span>{{ d.bidCount || 0 }} 人竞标</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Right Stack: Group Sessions + Top Creators -->
          <div class="bento-stack">
            <!-- Top Card: Tiered Group Sessions -->
            <div class="xyf-surface-card bento-panel">
              <div class="bento-panel__head">
                <div>
                  <span class="bento-tag is-amber">阶梯自动退差价</span>
                  <h3>近期拼班小班课</h3>
                </div>
                <router-link to="/groups" class="bento-more">
                  团课大厅 <el-icon><ArrowRight /></el-icon>
                </router-link>
              </div>

              <div v-if="liveSessions.length === 0" class="bento-empty is-compact">
                <p>暂无招募中的拼班课程，认证创作者可发起期末冲刺或实战小班。</p>
              </div>

              <div v-else class="session-mini-list">
                <div
                  v-for="s in liveSessions"
                  :key="s.id"
                  class="session-mini-row"
                  @click="router.push(`/groups/${s.id}`)"
                >
                  <div class="date-cube xyf-tabular">
                    <span class="date-cube__m">{{ formatMonthLabel(s.sessionStart) }}</span>
                    <strong class="date-cube__d">{{ formatDayLabel(s.sessionStart) }}</strong>
                  </div>
                  <div class="session-mini-body">
                    <h4 class="ellipsis">{{ s.title }}</h4>
                    <div class="session-mini-sub xyf-tabular">
                      <span>已报 {{ s.joinedCount }}/{{ s.maxParticipants }} 人</span>
                      <span>·</span>
                      <span class="tier-highlight">满{{ s.tierTargetCount }}人降至 ¥{{ Number(s.tierDiscountPrice).toFixed(0) }}</span>
                    </div>
                  </div>
                  <div class="session-mini-price xyf-tabular">
                    ¥{{ Number(s.currentUnitPrice || s.basePrice).toFixed(0) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Bottom Card: Hall of Fame Creators -->
            <div class="xyf-surface-card bento-panel">
              <div class="bento-panel__head">
                <div>
                  <span class="bento-tag">数字信用背书</span>
                  <h3>标杆创作者席位</h3>
                </div>
                <router-link to="/leaderboard" class="bento-more">
                  完整榜单 <el-icon><ArrowRight /></el-icon>
                </router-link>
              </div>

              <div v-if="topCreators.length === 0" class="bento-empty is-compact">
                <p>实名创作者榜单加载中，点击查看全部校内行家。</p>
              </div>

              <div v-else class="creator-mini-strip">
                <div
                  v-for="c in topCreators"
                  :key="c.userId"
                  class="creator-mini-chip"
                  @click="router.push(`/creators/${c.userId}`)"
                >
                  <div class="creator-mini-avatar">
                    <XyfAvatar :src="c.avatar" :name="c.displayName" :size="40" />
                    <span class="rank-corner xyf-tabular">{{ c.rank }}</span>
                  </div>
                  <div class="creator-mini-info">
                    <strong class="ellipsis">{{ c.displayName }}</strong>
                    <span class="ellipsis">{{ c.professionalName || c.creatorLevelTitle }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. Ultra-Minimal Trust Strip with Bespoke Animated SVG Icons -->
    <section class="trust-section">
      <div class="xyf-container trust-grid">
        <div class="trust-col">
          <div class="trust-svg-box" aria-hidden="true">
            <svg viewBox="0 0 44 44" fill="none">
              <circle cx="22" cy="22" r="19" stroke="#E4E4E7" stroke-width="1.2" stroke-dasharray="3 5" class="svg-rot-slow" />
              <path d="M22 9L32 13.2V21.5C32 27.6 27.7 32.8 22 34.8C16.3 32.8 12 27.6 12 21.5V13.2L22 9Z" stroke="#18181B" stroke-width="1.8" stroke-linejoin="round" />
              <path d="M17.5 22L20.5 25L26.8 18.6" stroke="#18181B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-check-pulse" />
            </svg>
          </div>
          <h3>高校双核实名</h3>
          <p>学号与 .edu.cn 教育邮箱双重核验，履约信用分全程公开</p>
        </div>

        <div class="trust-col">
          <div class="trust-svg-box" aria-hidden="true">
            <svg viewBox="0 0 44 44" fill="none">
              <rect x="12" y="20" width="20" height="14" rx="4" stroke="#18181B" stroke-width="1.8" />
              <path d="M16 20V15.5C16 12.2 18.7 9.5 22 9.5C25.3 9.5 28 12.2 28 15.5V20" stroke="#18181B" stroke-width="1.8" stroke-linecap="round" />
              <circle cx="22" cy="27" r="2.2" fill="#D97706" class="svg-core-glow" />
              <circle cx="22" cy="27" r="5" stroke="rgba(217,119,6,0.35)" stroke-width="1.2" class="svg-ripple" />
            </svg>
          </div>
          <h3>里程碑担保托管</h3>
          <p>支持整单或分阶段托管冻结，验收通过再释放资金</p>
        </div>

        <div class="trust-col">
          <div class="trust-svg-box" aria-hidden="true">
            <svg viewBox="0 0 44 44" fill="none">
              <circle cx="22" cy="22" r="16" stroke="rgba(24,24,27,0.16)" stroke-width="1.2" class="svg-radar-2" />
              <circle cx="22" cy="22" r="10.5" stroke="rgba(24,24,27,0.34)" stroke-width="1.4" class="svg-radar-1" />
              <circle cx="22" cy="22" r="4" fill="#18181B" />
              <circle cx="31.5" cy="14.5" r="2.4" fill="#D97706" class="svg-core-glow" />
            </svg>
          </div>
          <h3>电子围栏当面核销</h3>
          <p>真实校园地图米级选点，10s 动态码与蓝牙近场防伪交接</p>
        </div>
      </div>
    </section>

    <!-- 4. Minimal Editorial Creator CTA -->
    <section class="creator-section">
      <div class="xyf-container">
        <div class="creator-card">
          <div>
            <h2>也有一技之长？</h2>
            <p>开通创作者工作室，上架标准服务、参与高额悬赏竞标或发起多人拼班团课。</p>
          </div>
          <div class="creator-cta-group">
            <button type="button" class="creator-cta-ghost" @click="router.push('/demands')">
              浏览悬赏大厅
            </button>
            <button type="button" class="creator-cta-btn" @click="handlePublishEntry">
              {{ publishActionText }}
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  Close,
  ArrowLeft,
  ArrowRight,
  StarFilled
} from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { getServiceList, buildImageUrl } from '@/api/service'
import { listDemands } from '@/api/demand'
import { listGroupSessions, type GroupSessionItem } from '@/api/groupSession'
import { getCreatorLeaderboard, type CreatorLeaderboardItem } from '@/api/leaderboard'
import type { ServiceListItem } from '@/types/service'
import type { DemandItem } from '@/types/demand'
import CampusAmbientCanvas from '@/components/home/CampusAmbientCanvas.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const searchScope = ref<'services' | 'demands' | 'groups'>('services')
const keyword = ref('')
const activeTagId = ref<number | undefined>(undefined)
const servicesLoading = ref(false)
const services = ref<ServiceListItem[]>([])
const totalServiceCount = ref(0)

const liveDemands = ref<DemandItem[]>([])
const liveSessions = ref<GroupSessionItem[]>([])
const topCreators = ref<CreatorLeaderboardItem[]>([])

const hotQueries = ['胶片约拍', '全栈辅导', '笔记本清灰', '路演PPT', '高数突击']
const tags = computed(() => dictStore.serviceTags || [])

const scopePlaceholder = computed(() => {
  if (searchScope.value === 'demands') {
    return '搜索正在招募的校内悬赏急单（如 STM32 联调、考前突击、跟拍）...'
  }
  if (searchScope.value === 'groups') {
    return '搜索近期开班的多人阶梯团课（如 线代冲刺、算法集训、建模）...'
  }
  return '搜索胶片约拍、毕设辅导、PPT 重塑、笔记本清灰...'
})

const publishActionText = computed(() => {
  if (!userStore.isLogin) return '成为创作者'
  if (userStore.isAdmin) return '进入治理后台'
  return userStore.hasPublishPermission ? '立即发布服务' : '申请创作者认证'
})

// ==================== Hero Peripheral Parallax Works ====================
const heroFloatingWorks = computed(() => {
  if (allInitialServices.value.length === 0) return []
  return allInitialServices.value.slice(0, 4)
})
const allInitialServices = ref<ServiceListItem[]>([])
const heroPointer = ref({ x: 0, y: 0 })

function handleHeroPointer(e: MouseEvent) {
  const nx = e.clientX / window.innerWidth - 0.5
  const ny = e.clientY / window.innerHeight - 0.5
  heroPointer.value = { x: nx, y: ny }
}

function resetHeroPointer() {
  heroPointer.value = { x: 0, y: 0 }
}

const FLOAT_DEPTHS = [
  { dx: -26, dy: -18, rot: -4 },
  { dx: 24, dy: -22, rot: 4 },
  { dx: -20, dy: 20, rot: 3 },
  { dx: 28, dy: 16, rot: -3.5 }
]

function getFloatCardStyle(index: number) {
  const cfg = FLOAT_DEPTHS[index % FLOAT_DEPTHS.length]
  const tx = (heroPointer.value.x * cfg.dx).toFixed(1)
  const ty = (heroPointer.value.y * cfg.dy).toFixed(1)
  return {
    transform: `translate3d(${tx}px, ${ty}px, 0) rotate(${cfg.rot}deg)`
  }
}

// ==================== Continuous Scroll-Linked Horizontal Gallery Engine ====================
const runwayRef = ref<HTMLElement | null>(null)
const viewportRef = ref<HTMLElement | null>(null)
const trackRef = ref<HTMLElement | null>(null)

const isDesktopMotion = ref(true)
const scrollProgress = ref(0)
const manualBias = ref(0)
let targetProgress = 0
let targetBias = 0
let rafId = 0

const CARD_WIDTH = 396
const CARD_GAP = 32
const viewportWidth = ref(1440)
const startInset = ref(108)
const maxTranslateX = ref(0)

const effectiveProgress = computed(() =>
  Math.min(1, Math.max(0, scrollProgress.value + manualBias.value))
)

const currentTranslateX = computed(() =>
  startInset.value - effectiveProgress.value * maxTranslateX.value
)

const trackTransformStyle = computed(() => {
  if (!isDesktopMotion.value) return {}
  return {
    transform: `translate3d(${currentTranslateX.value.toFixed(2)}px, 0, 0)`
  }
})

const activeFocusIndex = computed(() => {
  if (services.value.length <= 1) return 0
  if (!isDesktopMotion.value) return 0
  const centerX = viewportWidth.value / 2
  const stride = CARD_WIDTH + CARD_GAP
  const rawIndex = (centerX - currentTranslateX.value - CARD_WIDTH / 2) / stride
  return Math.min(services.value.length - 1, Math.max(0, Math.round(rawIndex)))
})

function getCardDynamicStyle(index: number) {
  if (!isDesktopMotion.value || services.value.length <= 1) return {}
  const stride = CARD_WIDTH + CARD_GAP
  const cardCenterX = currentTranslateX.value + index * stride + CARD_WIDTH / 2
  const viewportCenterX = viewportWidth.value / 2
  const distance = Math.abs(cardCenterX - viewportCenterX) / stride
  const clamped = Math.min(distance, 2.2)
  const scale = 1.045 - clamped * 0.052
  const opacity = 1 - Math.min(distance, 2.6) * 0.1
  const translateY = clamped * 6
  return {
    transform: `translate3d(0, ${translateY.toFixed(1)}px, 0) scale(${scale.toFixed(4)})`,
    opacity: opacity.toFixed(3),
    zIndex: String(Math.round(20 - clamped * 5))
  }
}

function measureTrack() {
  viewportWidth.value = window.innerWidth
  isDesktopMotion.value = window.innerWidth >= 960
  if (!isDesktopMotion.value) return
  const vw = viewportRef.value?.clientWidth || window.innerWidth
  const containerLeft = Math.max(32, Math.round((vw - 1280) / 2 + 28))
  startInset.value = containerLeft
  const totalTrackW =
    services.value.length * CARD_WIDTH + Math.max(0, services.value.length - 1) * CARD_GAP
  maxTranslateX.value = Math.max(0, totalTrackW - (vw - containerLeft * 2))
  updateScrollTarget()
}

function updateScrollTarget() {
  if (!isDesktopMotion.value || !runwayRef.value) return
  const rect = runwayRef.value.getBoundingClientRect()
  const vh = window.innerHeight
  const enterY = vh * 0.9
  const leaveY = -rect.height * 0.72
  const span = enterY - leaveY
  if (span <= 0) {
    targetProgress = 0
    return
  }
  const raw = (enterY - rect.top) / span
  targetProgress = Math.min(1, Math.max(0, raw))
}

function tickSmoothScroll() {
  if (isDesktopMotion.value) {
    const diff = targetProgress - scrollProgress.value
    if (Math.abs(diff) > 0.0004) {
      scrollProgress.value += diff * 0.16
    } else if (diff !== 0) {
      scrollProgress.value = targetProgress
    }

    const biasDiff = targetBias - manualBias.value
    if (Math.abs(biasDiff) > 0.0004) {
      manualBias.value += biasDiff * 0.18
    } else if (biasDiff !== 0) {
      manualBias.value = targetBias
    }
  }
  rafId = window.requestAnimationFrame(tickSmoothScroll)
}

function nudgeTrack(direction: -1 | 1) {
  if (!isDesktopMotion.value || services.value.length <= 1) {
    viewportRef.value?.scrollBy({ left: direction * 360, behavior: 'smooth' })
    return
  }
  const step = 1 / Math.max(1, services.value.length - 1)
  const nextEffective = Math.min(1, Math.max(0, effectiveProgress.value + direction * step))
  targetBias = nextEffective - targetProgress
}

function scrollToShowcase() {
  if (!runwayRef.value) return
  window.scrollTo({
    top: runwayRef.value.offsetTop - 92,
    behavior: 'smooth'
  })
}

// ==================== Helpers & Data ====================
function formatCleanPrice(val: number | string) {
  const num = Number(val)
  if (Number.isNaN(num)) return '¥0'
  return Number.isInteger(num) ? `¥${num}` : `¥${num.toFixed(2)}`
}

function formatMonthLabel(dt?: string) {
  if (!dt) return '近期'
  const d = new Date(dt)
  return Number.isNaN(d.getTime()) ? '近期' : `${d.getMonth() + 1}月`
}

function formatDayLabel(dt?: string) {
  if (!dt) return '--'
  const d = new Date(dt)
  return Number.isNaN(d.getTime()) ? '--' : String(d.getDate()).padStart(2, '0')
}

function handleSearch() {
  const q = keyword.value.trim()
  const target =
    searchScope.value === 'demands'
      ? '/demands'
      : searchScope.value === 'groups'
        ? '/groups'
        : '/services'
  router.push({ path: target, query: q ? { keyword: q } : {} })
}

function quickSearch(q: string) {
  const target =
    searchScope.value === 'demands'
      ? '/demands'
      : searchScope.value === 'groups'
        ? '/groups'
        : '/services'
  router.push({ path: target, query: { keyword: q } })
}

function handlePublishEntry() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: '/service/publish' } })
    return
  }
  if (userStore.isAdmin) {
    router.push('/admin/dashboard')
    return
  }
  if (userStore.hasPublishPermission) {
    router.push('/service/publish')
  } else {
    router.push({ path: '/user/center', query: { tab: 'permission' } })
  }
}

async function fetchShowcase() {
  servicesLoading.value = true
  try {
    const res = await getServiceList({
      tagId: activeTagId.value,
      sortBy: 'view_desc',
      pageNum: 1,
      pageSize: 10
    })
    services.value = res.records || []
    if (activeTagId.value === undefined) {
      totalServiceCount.value = res.total || 0
      if (allInitialServices.value.length === 0 && services.value.length > 0) {
        allInitialServices.value = [...services.value]
      }
    }
    await nextTick()
    scrollProgress.value = 0
    targetProgress = 0
    manualBias.value = 0
    targetBias = 0
    measureTrack()
  } catch {
    services.value = []
  } finally {
    servicesLoading.value = false
  }
}

async function fetchEcosystemPulse() {
  const [demRes, grpRes, rankRes] = await Promise.all([
    listDemands({ pageNum: 1, pageSize: 4 }).catch(() => null),
    listGroupSessions({ pageNum: 1, pageSize: 2 }).catch(() => null),
    getCreatorLeaderboard({ rankBy: 'COMPREHENSIVE', limit: 3 }).catch(() => [])
  ])
  liveDemands.value = (demRes?.records || []).slice(0, 4)
  liveSessions.value = (grpRes?.records || []).slice(0, 2)
  topCreators.value = (rankRes || []).slice(0, 3)
}

function selectCategory(tagId: number | undefined) {
  if (activeTagId.value === tagId) return
  activeTagId.value = tagId
  fetchShowcase()
}

watch(
  () => services.value.length,
  async () => {
    await nextTick()
    measureTrack()
  }
)

onMounted(async () => {
  window.addEventListener('scroll', updateScrollTarget, { passive: true })
  window.addEventListener('resize', measureTrack, { passive: true })
  rafId = window.requestAnimationFrame(tickSmoothScroll)

  await Promise.all([
    dictStore.fetchServiceTags().catch(() => {}),
    fetchShowcase(),
    fetchEcosystemPulse()
  ])
})

onUnmounted(() => {
  window.cancelAnimationFrame(rafId)
  window.removeEventListener('scroll', updateScrollTarget)
  window.removeEventListener('resize', measureTrack)
})
</script>

<style scoped lang="scss">
.home-page {
  background: var(--xyf-bg);
}

/* ==================== 1. Full-Viewport Interactive Hero ==================== */
.hero-stage {
  position: relative;
  min-height: calc(100vh - var(--xyf-header-height));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0 88px;
  overflow: hidden;
  background: linear-gradient(180deg, #fbfcfc 0%, #ffffff 100%);
}

.hero-content {
  position: relative;
  z-index: 3;
  max-width: 740px;
  text-align: center;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--xyf-border);
  font-size: 12.5px;
  font-weight: 550;
  color: var(--xyf-ink-2);
  margin-bottom: 22px;
  backdrop-filter: blur(10px);

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--xyf-gold);
  }
}

.hero-float-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  max-width: 1560px;
  margin: 0 auto;

  @media (max-width: 1279px) {
    display: none;
  }
}

.float-work-card {
  position: absolute;
  pointer-events: auto;
  width: 214px;
  padding: 6px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
  box-shadow: 0 20px 44px -16px rgba(17, 24, 39, 0.16), 0 4px 12px rgba(17, 24, 39, 0.05);
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s ease;
  font-family: inherit;
  text-align: left;

  &:hover {
    box-shadow: 0 26px 56px -14px rgba(9, 9, 11, 0.22);
    z-index: 4;
  }

  &.is-slot-0 {
    top: 14%;
    left: 4.5%;
  }

  &.is-slot-1 {
    top: 13%;
    right: 4.5%;
  }

  &.is-slot-2 {
    bottom: 16%;
    left: 6.5%;
  }

  &.is-slot-3 {
    bottom: 15%;
    right: 6.5%;
  }

  &__img {
    width: 100%;
    aspect-ratio: 4 / 3;
    object-fit: cover;
    border-radius: 11px;
    display: block;
  }

  &__caption {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 7px 6px 3px;
  }

  &__tag {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  &__price {
    font-size: 12.5px;
    font-weight: 700;
    color: var(--xyf-primary);
  }
}

.hero-title {
  margin: 0;
  font-size: clamp(38px, 5vw, 60px);
  font-weight: 760;
  line-height: 1.12;
  letter-spacing: -0.035em;
  color: var(--xyf-ink);
}

.hero-subtitle {
  margin: 16px 0 0;
  font-size: 16.5px;
  line-height: 1.5;
  color: var(--xyf-ink-2);
  letter-spacing: 0.015em;
}

.search-dock {
  margin: 34px auto 0;
  max-width: 636px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.search-scopes {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: rgba(244, 244, 245, 0.85);
  border: 1px solid rgba(228, 228, 231, 0.8);
  backdrop-filter: blur(10px);
  gap: 2px;
}

.scope-pill {
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--xyf-ink-2);
  font-size: 12.5px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &.is-active {
    background: #18181b;
    color: #ffffff;
    font-weight: 600;
  }
}

.capsule-search {
  width: 100%;
  height: 58px;
  padding: 6px 6px 6px 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(16px);
  border: 1px solid #e4e4e7;
  box-shadow: 0 16px 44px -12px rgba(17, 24, 39, 0.11), 0 2px 6px rgba(17, 24, 39, 0.04);
  display: flex;
  align-items: center;
  gap: 10px;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:focus-within {
    border-color: var(--xyf-ink);
    box-shadow: 0 20px 50px -14px rgba(9, 9, 11, 0.14), 0 0 0 3px rgba(24, 24, 27, 0.06);
  }

  &__icon {
    font-size: 18px;
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }

  &__input {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    font-size: 15px;
    color: var(--xyf-ink);
    font-family: inherit;

    &::placeholder {
      color: #9ca3af;
    }
  }

  &__clear {
    width: 28px;
    height: 28px;
    border: none;
    border-radius: 50%;
    background: var(--xyf-surface-muted);
    color: var(--xyf-ink-2);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex-shrink: 0;
  }

  &__submit {
    height: 46px;
    padding: 0 28px;
    border: none;
    border-radius: 999px;
    background: var(--xyf-ink);
    color: #ffffff;
    font-size: 14.5px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    flex-shrink: 0;
    transition: background var(--xyf-transition), transform 0.15s ease;

    &:hover {
      background: var(--xyf-primary-hover);
    }

    &:active {
      transform: scale(0.97);
    }
  }
}

.hero-hot-queries {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-hot-pill {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  font-size: 13px;
  color: var(--xyf-ink-2);
  cursor: pointer;
  font-family: inherit;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-primary);
    color: var(--xyf-primary);
    background: #ffffff;
  }
}

.scroll-cue {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--xyf-ink-3);
  cursor: pointer;
  font-family: inherit;
  transition: color var(--xyf-transition);

  &:hover {
    color: var(--xyf-ink);
  }

  &__line {
    width: 1px;
    height: 28px;
    background: linear-gradient(180deg, var(--xyf-ink-3) 0%, transparent 100%);
    animation: cue-pulse 2s ease-in-out infinite;
  }
}

@keyframes cue-pulse {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  50% {
    transform: translateY(6px);
    opacity: 1;
  }
}

/* ==================== 2. Scroll-Driven Horizontal Gallery ==================== */
.horizontal-runway {
  position: relative;
  background: #ffffff;
  border-top: 1px solid var(--xyf-border-subtle);
  overflow: hidden;
}

.horizontal-sticky {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 72px 0 64px;
}

.showcase-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 34px;

  &__left {
    display: flex;
    align-items: center;
    gap: 32px;
    min-width: 0;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 18px;
    flex-shrink: 0;
  }
}

.showcase-heading {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
  white-space: nowrap;
}

.category-tabs {
  display: flex;
  align-items: center;
  gap: 22px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.category-tab {
  position: relative;
  border: none;
  background: transparent;
  padding: 8px 2px;
  font-size: 14px;
  font-weight: 500;
  color: var(--xyf-ink-3);
  white-space: nowrap;
  cursor: pointer;
  font-family: inherit;
  transition: color var(--xyf-transition);

  &:hover {
    color: var(--xyf-ink);
  }

  &.is-active {
    color: var(--xyf-ink);
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 2px;
      border-radius: 2px;
      background: var(--xyf-ink);
    }
  }
}

.track-nav-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-circle-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    background: var(--xyf-ink);
    color: #ffffff;
  }
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--xyf-ink-2);
  white-space: nowrap;
  transition: color var(--xyf-transition);

  &:hover {
    color: var(--xyf-primary);
  }
}

.horizontal-viewport {
  width: 100%;
  overflow: visible;
}

.horizontal-skeleton {
  max-width: var(--xyf-container-width);
  margin: 0 auto;
  padding: 0 32px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.horizontal-track {
  display: flex;
  align-items: center;
  gap: 32px;
  will-change: transform;
  padding: 18px 0 26px;
}

.cinema-card {
  width: 396px;
  flex-shrink: 0;
  cursor: pointer;
  will-change: transform, opacity;
  transform-origin: center center;
  transition: filter 0.25s ease;

  &:hover {
    .cinema-card__img :deep(.xyf-image__img) {
      transform: scale(1.045);
    }

    .cinema-card__title {
      color: var(--xyf-primary);
    }
  }

  &__media {
    width: 100%;
    aspect-ratio: 4 / 3;
    border-radius: 18px;
    overflow: hidden;
    background: var(--xyf-surface-muted);
    box-shadow: 0 22px 46px -18px rgba(17, 24, 39, 0.2);
  }

  &__img {
    width: 100%;
    height: 100%;

    :deep(.xyf-image__img) {
      transition: opacity 0.25s ease, transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
    }
  }

  &__body {
    padding: 16px 4px 0;
    display: flex;
    flex-direction: column;
    gap: 7px;
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  &__author {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--xyf-ink-2);
    min-width: 0;
  }

  &__rating {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
    flex-shrink: 0;
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 650;
    letter-spacing: -0.015em;
    color: var(--xyf-ink);
    transition: color var(--xyf-transition);
  }

  &__foot {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 2px;
  }

  &__price {
    font-size: 18px;
    font-weight: 700;
    color: var(--xyf-ink);
    letter-spacing: -0.02em;
  }

  &__tag {
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }
}

.progress-wrap {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-track {
  flex: 1;
  height: 2px;
  background: var(--xyf-border);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--xyf-ink);
  border-radius: 2px;
  transition: width 0.08s linear;
}

.progress-hint {
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink-3);
  letter-spacing: 0.06em;
}

/* ==================== 2.5 Live Campus Ecosystem Bento ==================== */
.ecosystem-section {
  padding: 68px 0 84px;
  border-top: 1px solid var(--xyf-border-subtle);
  background: #fafafa;
}

.eco-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}

.eco-title {
  margin: 4px 0 0;
  font-size: 26px;
  font-weight: 750;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
}

.eco-sub {
  margin: 0;
  font-size: 14px;
  color: var(--xyf-ink-2);
  max-width: 480px;
}

.eco-bento {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
}

.bento-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bento-panel {
  padding: 24px 26px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: #ffffff;

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;

    h3 {
      margin: 4px 0 0;
      font-size: 18px;
      font-weight: 700;
      letter-spacing: -0.015em;
      color: var(--xyf-ink);
    }
  }
}

.bento-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 650;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);

  &.is-amber {
    background: var(--xyf-champagne-soft);
    color: #9a5b13;
    border: 1px solid var(--xyf-champagne-border);
  }
}

.bento-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bento-ghost-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-ink);
  background: var(--xyf-ink);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: opacity var(--xyf-transition);

  &:hover {
    opacity: 0.88;
  }
}

.bento-more {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 550;
  color: var(--xyf-ink-2);

  &:hover {
    color: var(--xyf-ink);
  }
}

.bento-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--xyf-ink-3);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  &.is-compact {
    padding: 16px 8px;
  }
}

.bounty-feed {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bounty-mini-ticket {
  padding: 13px 16px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    transform: translateX(2px);
    box-shadow: var(--xyf-shadow-xs);
  }

  &__left {
    min-width: 0;
    flex: 1;

    h4 {
      margin: 4px 0 0;
      font-size: 14.5px;
      font-weight: 650;
      color: var(--xyf-ink);
    }
  }

  &__right {
    text-align: right;
    flex-shrink: 0;

    strong {
      display: block;
      font-size: 15.5px;
      font-weight: 750;
      color: var(--xyf-ink);
    }

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.bounty-mini-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--xyf-ink-3);
  min-width: 0;

  .u-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
    background: #71717a;

    &.is-u3 {
      background: #dc2626;
    }
    &.is-u2 {
      background: #d97706;
    }
  }

  .u-txt {
    font-weight: 600;
    color: var(--xyf-ink-2);
  }
}

.session-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.session-mini-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 14px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    background: var(--xyf-surface-soft);
  }
}

.date-cube {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  background: #18181b;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1.1;

  &__m {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.7);
  }

  &__d {
    font-size: 15px;
    font-weight: 750;
  }
}

.session-mini-body {
  min-width: 0;
  flex: 1;

  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 650;
    color: var(--xyf-ink);
  }

  .session-mini-sub {
    margin-top: 3px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .tier-highlight {
    color: #9a5b13;
    font-weight: 600;
  }
}

.session-mini-price {
  font-size: 16px;
  font-weight: 750;
  color: var(--xyf-ink);
  flex-shrink: 0;
}

.creator-mini-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.creator-mini-chip {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface-soft);
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: #ffffff;
    border-color: var(--xyf-ink);
    box-shadow: var(--xyf-shadow-xs);
  }
}

.creator-mini-avatar {
  position: relative;
  flex-shrink: 0;

  .rank-corner {
    position: absolute;
    right: -3px;
    bottom: -3px;
    width: 17px;
    height: 17px;
    border-radius: 50%;
    background: #18181b;
    color: #ffffff;
    border: 1.5px solid #ffffff;
    font-size: 10px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

.creator-mini-info {
  min-width: 0;
  display: flex;
  flex-direction: column;

  strong {
    font-size: 13.5px;
    font-weight: 650;
    color: var(--xyf-ink);
  }

  span {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }
}

/* ==================== 3. Ultra-Minimal Trust Strip ==================== */
.trust-section {
  padding: 80px 0;
  background: #ffffff;
  border-top: 1px solid var(--xyf-border);
  border-bottom: 1px solid var(--xyf-border);
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 48px;
}

.trust-col {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .trust-svg-box {
    width: 44px;
    height: 44px;
    margin-bottom: 6px;

    svg {
      width: 100%;
      height: 100%;
      overflow: visible;
    }
  }

  .svg-rot-slow {
    transform-origin: 22px 22px;
    animation: trust-spin 24s linear infinite;
  }

  .svg-core-glow {
    animation: trust-pulse 2.6s ease-in-out infinite;
  }

  .svg-ripple {
    transform-origin: 22px 27px;
    animation: trust-ripple 2.6s ease-out infinite;
  }

  .svg-radar-1 {
    transform-origin: 22px 22px;
    animation: trust-radar 3.2s ease-out infinite;
  }

  .svg-radar-2 {
    transform-origin: 22px 22px;
    animation: trust-radar 3.2s ease-out infinite 1.2s;
  }

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 14px;
    color: var(--xyf-ink-2);
  }
}

@keyframes trust-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes trust-pulse {
  0%,
  100% {
    opacity: 0.65;
  }
  50% {
    opacity: 1;
  }
}

@keyframes trust-ripple {
  0% {
    transform: scale(0.75);
    opacity: 0.7;
  }
  100% {
    transform: scale(1.45);
    opacity: 0;
  }
}

@keyframes trust-radar {
  0% {
    transform: scale(0.86);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.18);
    opacity: 0;
  }
}

/* ==================== 4. Minimal Creator CTA ==================== */
.creator-section {
  padding: 84px 0 96px;
}

.creator-card {
  background: var(--xyf-surface-ink);
  color: #ffffff;
  border-radius: 22px;
  padding: 52px 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;

  h2 {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #ffffff;
  }

  p {
    margin: 10px 0 0;
    font-size: 15px;
    color: rgba(255, 255, 255, 0.68);
  }
}

.creator-cta-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.creator-cta-ghost {
  height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: transparent;
  color: #ffffff;
  font-size: 14px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #ffffff;
    background: rgba(255, 255, 255, 0.08);
  }
}

.creator-cta-btn {
  height: 48px;
  padding: 0 30px;
  border-radius: 999px;
  border: none;
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 14.5px;
  font-weight: 600;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.15s ease, opacity 0.2s ease;

  &:hover {
    opacity: 0.92;
    transform: translateY(-1px);
  }
}

/* ==================== Responsive Fallback (< 960px) ==================== */
@media (max-width: 959px) {
  .hero-stage {
    min-height: 72vh;
    padding: 40px 0 72px;
  }

  .showcase-header {
    flex-direction: column;
    align-items: flex-start;

    &__left {
      width: 100%;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    &__right {
      width: 100%;
      justify-content: space-between;
    }
  }

  .horizontal-viewport {
    overflow-x: auto;
    padding: 0 18px;
    scroll-snap-type: x mandatory;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .horizontal-track {
    gap: 16px;
    padding: 8px 0 16px;
  }

  .cinema-card {
    width: 300px;
    scroll-snap-align: center;
  }

  .eco-bento {
    grid-template-columns: 1fr;
  }

  .creator-mini-strip {
    grid-template-columns: 1fr;
  }

  .trust-grid {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .creator-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 36px 28px;
  }
}
</style>
