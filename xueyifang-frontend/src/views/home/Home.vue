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
        <h1 class="hero-title">找到身边的校园行家</h1>
        <p class="hero-subtitle">实名同学 · 资金担保 · 满意再放款</p>

        <form class="capsule-search" @submit.prevent="handleSearch()">
          <el-icon class="capsule-search__icon"><Search /></el-icon>
          <input
            v-model="keyword"
            type="text"
            class="capsule-search__input"
            placeholder="搜索胶片约拍、毕设辅导、PPT 重塑、笔记本清灰..."
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
          <button type="submit" class="capsule-search__submit">搜索</button>
        </form>

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
            <h2 class="showcase-heading">精选技能</h2>
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
          <h3>学号实名认证</h3>
          <p>校内身份核验，履约评价透明</p>
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
          <h3>平台资金托管</h3>
          <p>付款先行冻结，满意再确认结算</p>
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
          <h3>灵活校内交付</h3>
          <p>线上交付或图书馆、体育馆当面完成</p>
        </div>
      </div>
    </section>

    <!-- 4. Minimal Editorial Creator CTA -->
    <section class="creator-section">
      <div class="xyf-container">
        <div class="creator-card">
          <div>
            <h2>也有一技之长？</h2>
            <p>两分钟发布你的校园服务，自由定价，按单结算。</p>
          </div>
          <button type="button" class="creator-cta-btn" @click="handlePublishEntry">
            {{ publishActionText }}
            <el-icon><ArrowRight /></el-icon>
          </button>
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
import type { ServiceListItem } from '@/types/service'
import CampusAmbientCanvas from '@/components/home/CampusAmbientCanvas.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const keyword = ref('')
const activeTagId = ref<number | undefined>(undefined)
const servicesLoading = ref(false)
const services = ref<ServiceListItem[]>([])
const totalServiceCount = ref(0)

const hotQueries = ['胶片约拍', '全栈辅导', '笔记本清灰', '路演PPT', '羽毛球陪练']
const tags = computed(() => dictStore.serviceTags || [])

const publishActionText = computed(() => {
  if (!userStore.isLogin) return '成为创作者'
  if (userStore.isAdmin) return '返回管理后台'
  return userStore.hasPublishPermission ? '立即发布服务' : '申请发布权限'
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
const scrollProgress = ref(0) // 0..1 vertical-scroll coupled progress
const manualBias = ref(0) // arrow/wheel manual offset (-1..1)
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
  // Center card gently lifts to 1.04x; neighbors recede smoothly
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
  // Align card #1 cleanly with the left container margin so 3 cards greet the viewer immediately
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
  // Active while scrolling naturally past the section ("边滚边往下走" — zero sticky lock!)
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

// ==================== Actions & Data ====================
function formatCleanPrice(val: number | string) {
  const num = Number(val)
  if (Number.isNaN(num)) return '¥0'
  return Number.isInteger(num) ? `¥${num}` : `¥${num.toFixed(2)}`
}

function handleSearch() {
  const q = keyword.value.trim()
  router.push({ path: '/services', query: q ? { keyword: q } : {} })
}

function quickSearch(q: string) {
  router.push({ path: '/services', query: { keyword: q } })
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
    fetchShowcase()
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
  max-width: 720px;
  text-align: center;
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
  font-weight: 750;
  line-height: 1.12;
  letter-spacing: -0.035em;
  color: var(--xyf-ink);
}

.hero-subtitle {
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.5;
  color: var(--xyf-ink-2);
  letter-spacing: 0.02em;
}

.capsule-search {
  margin: 40px auto 0;
  max-width: 620px;
  height: 58px;
  padding: 6px 6px 6px 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
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
      background: var(--xyf-primary);
    }

    &:active {
      transform: scale(0.97);
    }
  }
}

.hero-hot-queries {
  margin-top: 22px;
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

/* ==================== 3. Ultra-Minimal Trust Strip ==================== */
.trust-section {
  padding: 88px 0;
  background: var(--xyf-surface-soft);
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
