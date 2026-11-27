<template>
  <div class="service-detail-page xyf-page">
    <div class="xyf-container">
      <nav class="detail-breadcrumb" aria-label="面包屑导航">
        <router-link to="/" class="crumb-link">发现</router-link>
        <span class="crumb-sep">/</span>
        <router-link to="/services" class="crumb-link">全部服务</router-link>
        <template v-if="service?.tagName">
          <span class="crumb-sep">/</span>
          <span class="crumb-current">{{ service.tagName }}</span>
        </template>
      </nav>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="!service" class="xyf-surface-card">
        <XyfEmptyState
          title="服务不存在或暂不可查看"
          description="该服务可能已被卖家下架或删除，返回服务大厅探索其他技能吧。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/services')">浏览服务大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Admin / Owner Context Strip (replaces bulky el-alert) -->
        <div v-if="userStore.isAdmin" class="context-bar">
          <span class="context-bar__text">当前为管理员巡检视图，可直接前往后台审核详情。</span>
          <div class="context-bar__actions">
            <el-button size="small" @click="router.push('/admin/dashboard')">返回后台</el-button>
            <el-button
              size="small"
              type="primary"
              @click="router.push(`/admin/services/${service.id}`)"
            >
              进入审核详情
            </el-button>
          </div>
        </div>

        <div v-else-if="isOwner" class="context-bar">
          <span class="context-bar__text">这是你发布的技能服务，可随时管理上下架或编辑内容。</span>
          <div class="context-bar__actions">
            <el-button size="small" @click="router.push('/my-services')">管理我的服务</el-button>
            <el-button
              v-if="canEditService(service.status)"
              size="small"
              type="primary"
              @click="router.push(`/service/edit/${service.id}`)"
            >
              编辑此服务
            </el-button>
          </div>
        </div>

        <div class="detail-grid">
          <!-- Left: Adaptive Gallery + Continuous Editorial Flow -->
          <div class="detail-main">
            <!-- Adaptive Media Stage: 1=single, 2=split, >=3=Bento -->
            <section class="media-stage" :class="galleryLayoutClass">
              <template v-if="galleryUrls.length === 0">
                <XyfImage src="" height="400px" width="100%" :radius="16" :show-empty-text="true" />
              </template>

              <template v-else-if="galleryUrls.length === 1">
                <el-image
                  :src="galleryUrls[0]"
                  :preview-src-list="galleryUrls"
                  :initial-index="0"
                  fit="cover"
                  class="media-tile media-tile--single"
                />
              </template>

              <template v-else-if="galleryUrls.length === 2">
                <el-image
                  :src="galleryUrls[0]"
                  :preview-src-list="galleryUrls"
                  :initial-index="0"
                  fit="cover"
                  class="media-tile"
                />
                <el-image
                  :src="galleryUrls[1]"
                  :preview-src-list="galleryUrls"
                  :initial-index="1"
                  fit="cover"
                  class="media-tile"
                />
              </template>

              <template v-else>
                <el-image
                  :src="galleryUrls[0]"
                  :preview-src-list="galleryUrls"
                  :initial-index="0"
                  fit="cover"
                  class="media-tile media-tile--hero"
                />
                <div class="media-bento-side">
                  <el-image
                    :src="galleryUrls[1]"
                    :preview-src-list="galleryUrls"
                    :initial-index="1"
                    fit="cover"
                    class="media-tile"
                  />
                  <div class="media-tile-wrap">
                    <el-image
                      :src="galleryUrls[2]"
                      :preview-src-list="galleryUrls"
                      :initial-index="2"
                      fit="cover"
                      class="media-tile"
                    />
                    <span v-if="galleryUrls.length > 3" class="media-more-badge xyf-tabular">
                      +{{ galleryUrls.length - 3 }} 张
                    </span>
                  </div>
                </div>
              </template>
            </section>

            <!-- Editorial Title & Creator Trust Strip -->
            <section class="editorial-head">
              <div class="head-badges">
                <XyfStatusBadge tone="info" :label="getTradeTypeText(service.tradeType)" />
                <XyfStatusBadge v-if="service.tagName" tone="neutral" :label="service.tagName" />
                <XyfStatusBadge
                  :tone="service.status === ServiceStatus.ONLINE ? 'success' : 'warning'"
                  :label="getServiceStatusText(service.status)"
                  :dot="service.status === ServiceStatus.ONLINE"
                />
              </div>

              <h1 class="service-h1">{{ service.serviceTitle }}</h1>

              <div class="creator-trust-strip" @click="router.push(`/creators/${service.sellerId}`)">
                <div class="creator-trust-strip__profile">
                  <XyfAvatar
                    :src="service.sellerAvatar"
                    :name="service.sellerName"
                    :size="42"
                  />
                  <div class="creator-trust-strip__info">
                    <div class="creator-name-line">
                      <strong>{{ service.sellerName || `校园创作者 #${service.sellerId}` }}</strong>
                      <span
                        class="verify-chip"
                        :class="{ 'is-verified': service.sellerEduVerified === 1 }"
                      >
                        {{ service.sellerEduVerified === 1 ? 'edu.cn 已核验' : '学籍已登记' }}
                      </span>
                      <span class="credit-chip xyf-tabular">信用 {{ service.sellerCreditScore ?? 100 }}</span>
                    </div>
                    <div class="creator-sub">
                      <span v-if="service.sellerGrade">{{ service.sellerGrade }}</span>
                      <span v-if="service.sellerGrade && service.sellerProfessional">·</span>
                      <span v-if="service.sellerProfessional">{{ service.sellerProfessional }}</span>
                      <span v-if="!service.sellerGrade && !service.sellerProfessional">校内认证服务发布者</span>
                    </div>
                  </div>
                </div>

                <div class="creator-trust-strip__metrics xyf-tabular">
                  <div class="metric-item">
                    <strong>
                      {{
                        service.reviewCount && (service.averageRating || service.sellerRating)
                          ? Number(service.averageRating || service.sellerRating).toFixed(1)
                          : '暂无'
                      }}
                    </strong>
                    <span>评分</span>
                  </div>
                  <span class="metric-sep" />
                  <div class="metric-item">
                    <strong>{{ service.sellerCompletedOrders ?? service.salesCount ?? 0 }}</strong>
                    <span>完结单</span>
                  </div>
                  <span class="metric-sep" />
                  <div class="metric-item">
                    <strong>{{ service.reviewCount ?? 0 }}</strong>
                    <span>评价</span>
                  </div>
                </div>
              </div>
            </section>

            <!-- Continuous Section 1: About Service -->
            <section class="editorial-section">
              <div class="xyf-section-heading">
                <h2>关于这项服务</h2>
              </div>
              <div class="service-description">
                {{ service.serviceDescription || '卖家未填写详细说明，下单前可通过站内消息沟通交付细节。' }}
              </div>
            </section>

            <!-- Continuous Section 2: Delivery & Terms (Verified Real Fields Only) -->
            <section class="editorial-section">
              <div class="xyf-section-heading">
                <h2>交付与约定</h2>
              </div>
              <dl class="terms-grid xyf-tabular">
                <div class="terms-cell">
                  <dt>交付形态</dt>
                  <dd>{{ service.isPhysical === 1 ? '实物作品 / 教材器具' : '虚拟技能 / 数字交付' }}</dd>
                </div>
                <div class="terms-cell">
                  <dt>交易方式</dt>
                  <dd>{{ getTradeTypeText(service.tradeType) }}</dd>
                </div>
                <div class="terms-cell">
                  <dt>可购名额</dt>
                  <dd>
                    <span v-if="isUnlimited">不限名额</span>
                    <span v-else-if="isSoldOut" class="text-danger">已售罄</span>
                    <span v-else>剩余 {{ remainingCount }} / {{ service.maxPurchases }} 份</span>
                  </dd>
                </div>
                <div class="terms-cell">
                  <dt>担保结算</dt>
                  <dd>钱包冻结托管 · 验收后结算</dd>
                </div>
                <div v-if="service.cancelFreeHours" class="terms-cell">
                  <dt>免费改期 / 取消</dt>
                  <dd>预约前 {{ service.cancelFreeHours }} 小时内免责</dd>
                </div>
                <div v-if="service.tradeType === TradeType.OFFLINE" class="terms-cell">
                  <dt>线下核销</dt>
                  <dd>{{ service.requireBleVerify ? '动态核销码 + 围栏核验' : '30s 动态核销码面交结单' }}</dd>
                </div>
              </dl>
            </section>

            <!-- Continuous Section 3: Price Reference (Moved from Aside so Sticky Dock fits viewport) -->
            <section class="editorial-section editorial-section--compact">
              <SmartPricingCompassCard
                :tag-id="service.tagId"
                :trade-type="service.tradeType"
                :proposed-price="Number(service.price || 0)"
                :interactive="false"
              />
            </section>

            <!-- Continuous Section 4: Offline Campus Location & Map -->
            <section v-if="service.tradeType === TradeType.OFFLINE" class="editorial-section">
              <div class="xyf-section-heading">
                <h2>校园面交地点</h2>
              </div>
              <TradeLocationBadge
                :location-id="service.tradeLocationId"
                :location-name="service.tradeLocationName"
                :location="service.tradeLocation"
                title="推荐校内面交地标"
              />
              <div
                v-if="service.tradeLocationLat && service.tradeLocationLng"
                class="location-map-wrap"
              >
                <RealCampusMap
                  :latitude="service.tradeLocationLat"
                  :longitude="service.tradeLocationLng"
                  :center-lat="service.tradeLocationLat"
                  :center-lng="service.tradeLocationLng"
                  :radius-meters="80"
                  :interactive="false"
                  :height="220"
                />
              </div>
            </section>

            <!-- Continuous Section 5: Authentic Reviews -->
            <section class="editorial-section">
              <div class="xyf-section-heading">
                <h2>真实评价（{{ service.reviewCount ?? 0 }}）</h2>
              </div>
              <ServiceReviewList :service-id="service.id" />
            </section>
          </div>

          <!-- Right: Compact Sticky Purchase Dock (<= 480px so sticky always works) -->
          <aside class="detail-aside">
            <section class="xyf-surface-card purchase-dock">
              <div class="dock-price-row">
                <div>
                  <span class="dock-price-label">单次服务价</span>
                  <div class="dock-price-val xyf-tabular">{{ formatPrice(service.price) }}</div>
                </div>
                <div class="dock-stats xyf-tabular">
                  <span>浏览 {{ formatCompactNumber(service.viewCount) }}</span>
                  <span>·</span>
                  <span>销量 {{ formatCompactNumber(service.salesCount ?? service.currentPurchaseCount) }}</span>
                </div>
              </div>

              <dl class="dock-specs">
                <div class="dock-spec-row">
                  <dt>交付方式</dt>
                  <dd>{{ getTradeTypeText(service.tradeType) }}</dd>
                </div>
                <div class="dock-spec-row">
                  <dt>剩余名额</dt>
                  <dd class="xyf-tabular">
                    <span v-if="isUnlimited">不限名额</span>
                    <span v-else-if="isSoldOut" class="text-danger">已售罄</span>
                    <span v-else>剩 {{ remainingCount }} 份</span>
                  </dd>
                </div>
                <div v-if="service.tradeType === TradeType.OFFLINE" class="dock-spec-row">
                  <dt>交接地标</dt>
                  <dd class="ellipsis">{{ service.tradeLocationName || '下单时任选校内地点' }}</dd>
                </div>
              </dl>

              <div v-if="!userStore.isAdmin && !isOwner" class="dock-actions">
                <el-button
                  type="primary"
                  size="large"
                  class="buy-btn"
                  :disabled="!canPurchase"
                  @click="handleBuyNow"
                >
                  {{ purchaseButtonText }}
                </el-button>

                <div class="secondary-actions">
                  <el-button
                    class="flex-btn"
                    :loading="collectLoading"
                    @click="handleToggleCollect"
                  >
                    <el-icon><component :is="service.isCollected ? StarFilled : Star" /></el-icon>
                    {{ service.isCollected ? '已收藏' : '收藏' }} ({{ service.collectionCount || 0 }})
                  </el-button>
                  <el-button class="flex-btn" @click="handleContactSeller">
                    <el-icon><ChatDotRound /></el-icon>
                    私信卖家
                  </el-button>
                </div>
              </div>

              <div class="dock-creator-foot" @click="router.push(`/creators/${service.sellerId}`)">
                <div class="dock-creator-foot__left">
                  <XyfAvatar :src="service.sellerAvatar" :name="service.sellerName" :size="32" />
                  <div class="dock-creator-foot__text">
                    <strong>{{ service.sellerName || `创作者 #${service.sellerId}` }}</strong>
                    <span v-if="service.sellerBio" class="ellipsis">{{ service.sellerBio }}</span>
                    <span v-else>查看创作者主页与在架服务</span>
                  </div>
                </div>
                <el-icon class="dock-creator-foot__arrow"><ArrowRight /></el-icon>
              </div>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, ChatDotRound, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getServiceDetail, buildImageUrl } from '@/api/service'
import { toggleCollection } from '@/api/favorite'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatCompactNumber } from '@/utils/format'
import {
  ServiceStatus,
  TradeType,
  getTradeTypeText,
  getServiceStatusText,
  canEditService,
  type ServiceDetail
} from '@/types/service'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import TradeLocationBadge from '@/components/business/TradeLocationBadge.vue'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import ServiceReviewList from './components/ServiceReviewList.vue'
import SmartPricingCompassCard from '@/components/service/SmartPricingCompassCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const collectLoading = ref(false)
const service = ref<ServiceDetail | null>(null)

const serviceId = computed(() => Number(route.params.id))

const galleryUrls = computed(() => {
  if (!service.value?.images?.length) return []
  return service.value.images
    .map((img) => buildImageUrl(img))
    .filter((url) => Boolean(url))
})

const galleryLayoutClass = computed(() => {
  const len = galleryUrls.value.length
  if (len <= 1) return 'is-single'
  if (len === 2) return 'is-split'
  return 'is-bento'
})

const isOwner = computed(() =>
  Boolean(userStore.userInfo?.id && service.value?.sellerId === userStore.userInfo.id)
)

const isUnlimited = computed(() => service.value?.maxPurchases === -1)

const remainingCount = computed(() => {
  if (!service.value || isUnlimited.value) return Infinity
  return Math.max(0, service.value.maxPurchases - service.value.currentPurchaseCount)
})

const isSoldOut = computed(() => !isUnlimited.value && remainingCount.value <= 0)

const canPurchase = computed(() => {
  if (!service.value) return false
  return service.value.status === ServiceStatus.ONLINE && !isSoldOut.value
})

const purchaseButtonText = computed(() => {
  if (!service.value) return '暂不可购'
  if (service.value.status !== ServiceStatus.ONLINE) {
    return `当前状态：${getServiceStatusText(service.value.status)}`
  }
  if (isSoldOut.value) return '名额已满 · 暂不可购'
  return '立即下单 · 平台担保'
})

async function fetchDetail() {
  if (!serviceId.value || Number.isNaN(serviceId.value)) {
    service.value = null
    return
  }
  loading.value = true
  try {
    service.value = await getServiceDetail(serviceId.value)
  } catch (err) {
    notifyFallbackError(err, '加载服务详情失败')
    service.value = null
  } finally {
    loading.value = false
  }
}

function handleBuyNow() {
  if (!service.value) return
  if (!userStore.isLogin) {
    ElMessage.info('请先登录后再提交担保订单')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push(`/order/confirm/${service.value.id}`)
}

async function handleToggleCollect() {
  if (!service.value || collectLoading.value) return
  if (!userStore.isLogin) {
    ElMessage.info('请先登录后收藏服务')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  collectLoading.value = true
  try {
    const prev = Boolean(service.value.isCollected)
    await toggleCollection(service.value.id, prev)
    service.value.isCollected = !prev
    service.value.collectionCount = Math.max(
      0,
      (service.value.collectionCount || 0) + (prev ? -1 : 1)
    )
    ElMessage.success(prev ? '已取消收藏' : '已加入我的收藏')
  } catch (err) {
    notifyFallbackError(err, '收藏操作失败')
  } finally {
    collectLoading.value = false
  }
}

function handleContactSeller() {
  if (!service.value) return
  if (!userStore.isLogin) {
    ElMessage.info('请先登录后联系卖家')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push({
    name: 'ChatWithUser',
    params: { userId: service.value.sellerId },
    query: { relatedServiceId: String(service.value.id) }
  })
}

watch(
  serviceId,
  () => {
    fetchDetail()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--xyf-ink-3);

  .crumb-link {
    color: var(--xyf-ink-2);
    transition: color var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
    }
  }

  .crumb-sep {
    color: var(--xyf-border-strong);
  }

  .crumb-current {
    color: var(--xyf-ink);
    font-weight: 500;
  }
}

.context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 16px;
  margin-bottom: 20px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &__text {
    font-size: 13px;
    color: var(--xyf-ink-2);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 348px;
  gap: 28px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 28px;
  min-width: 0;
}

/* Adaptive Media Gallery (consistent 400px stage height) */
.media-stage {
  height: 400px;
  border-radius: var(--xyf-radius-md);
  overflow: hidden;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &.is-single {
    display: block;
  }

  &.is-split {
    display: grid;
    grid-template-columns: 1.45fr 1fr;
    gap: 6px;
    background: #ffffff;
    border: none;
  }

  &.is-bento {
    display: grid;
    grid-template-columns: 1.65fr 1fr;
    gap: 6px;
    background: #ffffff;
    border: none;
  }
}

.media-bento-side {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 6px;
  height: 100%;
  min-height: 0;
}

.media-tile-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.media-tile {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  background: var(--xyf-surface-soft);

  &--single {
    border-radius: var(--xyf-radius-md);
  }
}

.media-more-badge {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(17, 17, 19, 0.78);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  pointer-events: none;
}

/* Editorial Title & Creator Trust Strip */
.editorial-head {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--xyf-border);
}

.head-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.service-h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
}

.creator-trust-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  cursor: pointer;
  transition: border-color var(--xyf-transition), background-color var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    background: #ffffff;
  }

  &__profile {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  &__info {
    min-width: 0;
  }

  .creator-name-line {
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

  .verify-chip,
  .credit-chip {
    font-size: 12px;
    font-weight: 500;
    padding: 1px 8px;
    border-radius: var(--xyf-radius-xs);
    background: #ffffff;
    border: 1px solid var(--xyf-border);
    color: var(--xyf-ink-2);
  }

  .verify-chip.is-verified {
    background: var(--xyf-champagne-soft);
    border-color: var(--xyf-champagne-border);
    color: #9a5b13;
  }

  .creator-sub {
    margin-top: 3px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  &__metrics {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-shrink: 0;

    .metric-item {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 2px;

      strong {
        font-size: 16px;
        font-weight: 700;
        color: var(--xyf-ink);
        line-height: 1.1;
      }

      span {
        font-size: 12px;
        color: var(--xyf-ink-3);
      }
    }

    .metric-sep {
      width: 1px;
      height: 22px;
      background: var(--xyf-border);
    }
  }
}

.editorial-section {
  padding-bottom: 24px;
  border-bottom: 1px solid var(--xyf-border-subtle);

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &--compact {
    padding-bottom: 20px;
  }
}

.service-description {
  font-size: 14px;
  line-height: 1.8;
  color: var(--xyf-ink-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.terms-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  padding: 18px 20px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
}

.terms-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;

  dt {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: var(--xyf-ink);
  }

  .text-danger {
    color: var(--xyf-danger);
    font-weight: 600;
  }
}

.location-map-wrap {
  margin-top: 14px;
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  border: 1px solid var(--xyf-border);
}

/* Compact Sticky Purchase Dock */
.detail-aside {
  position: sticky;
  top: 84px;
}

.purchase-dock {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dock-price-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border-subtle);

  .dock-price-label {
    display: block;
    font-size: 12px;
    color: var(--xyf-ink-3);
    margin-bottom: 4px;
  }

  .dock-price-val {
    font-size: 32px;
    font-weight: 700;
    color: var(--xyf-ink);
    line-height: 1;
    letter-spacing: -0.025em;
  }

  .dock-stats {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.dock-specs {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dock-spec-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;

  dt {
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }

  dd {
    margin: 0;
    color: var(--xyf-ink);
    font-weight: 500;
    text-align: right;
    min-width: 0;
  }

  .text-danger {
    color: var(--xyf-danger);
    font-weight: 600;
  }
}

.dock-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .buy-btn {
    width: 100%;
    height: 44px;
    font-size: 14px;
    font-weight: 600;
  }

  .secondary-actions {
    display: flex;
    gap: 10px;

    .flex-btn {
      flex: 1;
      margin-left: 0;
    }
  }
}

.dock-creator-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border-subtle);
  cursor: pointer;

  &:hover .dock-creator-foot__arrow {
    transform: translateX(2px);
    color: var(--xyf-ink);
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  &__text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    strong {
      font-size: 13px;
      font-weight: 600;
      color: var(--xyf-ink);
    }

    span {
      font-size: 12px;
      color: var(--xyf-ink-3);
      max-width: 220px;
    }
  }

  &__arrow {
    color: var(--xyf-ink-3);
    flex-shrink: 0;
    transition: transform var(--xyf-transition), color var(--xyf-transition);
  }
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-aside {
    position: static;
  }

  .media-stage {
    height: 260px;

    &.is-split,
    &.is-bento {
      grid-template-columns: 1fr;
      height: auto;

      .media-tile {
        height: 240px;
      }

      .media-bento-side {
        grid-template-columns: 1fr 1fr;
        grid-template-rows: auto;

        .media-tile {
          height: 120px;
        }
      }
    }
  }

  .terms-grid {
    grid-template-columns: 1fr;
  }
}
</style>
