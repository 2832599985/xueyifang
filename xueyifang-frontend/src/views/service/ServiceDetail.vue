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
        <!-- Main 2-column showcase -->
        <div class="detail-grid">
          <!-- Left: Gallery + Description + Reviews -->
          <div class="detail-main">
            <section class="xyf-surface-card gallery-card">
              <div class="gallery-stage">
                <el-image
                  v-if="activeImageUrl"
                  :src="activeImageUrl"
                  :preview-src-list="galleryUrls"
                  :initial-index="activeImageIndex"
                  fit="contain"
                  class="gallery-stage__el-img"
                />
                <XyfImage
                  v-else
                  src=""
                  height="380px"
                  width="100%"
                  :show-empty-text="true"
                />
              </div>

              <div v-if="galleryUrls.length > 1" class="gallery-thumbs">
                <button
                  v-for="(url, idx) in galleryUrls"
                  :key="url + idx"
                  type="button"
                  class="thumb-btn"
                  :class="{ 'is-active': idx === activeImageIndex }"
                  @click="activeImageIndex = idx"
                >
                  <XyfImage :src="url" width="76px" height="58px" :radius="6" fit="cover" />
                </button>
              </div>
            </section>

            <!-- Description & Reviews Tabs -->
            <section class="xyf-surface-card detail-tabs-card">
              <el-tabs v-model="activeTab">
                <el-tab-pane label="服务详情介绍" name="intro">
                  <div class="service-description">
                    {{ service.serviceDescription || '卖家未填写详细说明，下单前可通过站内消息沟通交付细节。' }}
                  </div>

                  <div v-if="service.tradeType === TradeType.OFFLINE" class="offline-location-wrap">
                    <TradeLocationBadge
                      :location-id="service.tradeLocationId"
                      :location-name="service.tradeLocationName"
                      :location="service.tradeLocation"
                      title="推荐校内线下交易地点"
                    />
                  </div>
                </el-tab-pane>

                <el-tab-pane
                  :label="`真实评价 (${service.reviewCount ?? 0})`"
                  name="reviews"
                >
                  <ServiceReviewList :service-id="service.id" />
                </el-tab-pane>
              </el-tabs>
            </section>
          </div>

          <!-- Right: Purchase Dock & Seller Profile -->
          <aside class="detail-aside">
            <section class="xyf-surface-card purchase-card">
              <div class="purchase-badges">
                <XyfStatusBadge
                  :tone="service.tradeType === TradeType.OFFLINE ? 'accent' : 'teal'"
                  :label="getTradeTypeText(service.tradeType)"
                  :dot="false"
                />
                <XyfStatusBadge
                  v-if="service.tagName"
                  tone="neutral"
                  :label="service.tagName"
                  :dot="false"
                />
                <XyfStatusBadge
                  :tone="service.status === ServiceStatus.ONLINE ? 'success' : 'warning'"
                  :label="getServiceStatusText(service.status)"
                />
              </div>

              <h2 class="purchase-title">{{ service.serviceTitle }}</h2>

              <div class="purchase-price-box">
                <div>
                  <span class="price-label">单次服务价格</span>
                  <div class="price-value xyf-tabular">{{ formatPrice(service.price) }}</div>
                </div>
                <div class="price-metrics xyf-tabular">
                  <span>浏览 {{ formatCompactNumber(service.viewCount) }}</span>
                  <span>·</span>
                  <span>销量 {{ formatCompactNumber(service.salesCount ?? service.currentPurchaseCount) }}</span>
                </div>
              </div>

              <dl class="spec-list">
                <div class="spec-row">
                  <dt>交付形态</dt>
                  <dd>{{ service.isPhysical === 1 ? '实物作品 / 教材器具' : '虚拟技能 / 数字交付' }}</dd>
                </div>
                <div class="spec-row">
                  <dt>剩余可购名额</dt>
                  <dd class="xyf-tabular">
                    <span v-if="isUnlimited">不限名额</span>
                    <span v-else-if="isSoldOut" class="text-danger">已售罄</span>
                    <span v-else>{{ remainingCount }} 份（上限 {{ service.maxPurchases }} 份）</span>
                  </dd>
                </div>
                <div class="spec-row" v-if="service.tradeType === TradeType.OFFLINE">
                  <dt>线下地标</dt>
                  <dd>{{ service.tradeLocationName || '支持下单时任选校内地点' }}</dd>
                </div>
                <div class="spec-row">
                  <dt>担保机制</dt>
                  <dd>平台钱包冻结担保 · 验收后结算</dd>
                </div>
              </dl>

              <!-- Admin Inspection vs Owner Actions vs Buyer Actions -->
              <div v-if="userStore.isAdmin" class="owner-box">
                <el-alert
                  title="管理员巡检模式 · 可直接前往后台审核此服务"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <div class="owner-actions">
                  <el-button @click="router.push('/admin/dashboard')">返回管理后台</el-button>
                  <el-button
                    type="primary"
                    @click="router.push(`/admin/services/${service.id}`)"
                  >
                    进入后台审核详情
                  </el-button>
                </div>
              </div>

              <div v-else-if="isOwner" class="owner-box">
                <el-alert
                  title="这是您自己发布的技能服务"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <div class="owner-actions">
                  <el-button @click="router.push('/my-services')">管理我的服务</el-button>
                  <el-button
                    v-if="canEditService(service.status)"
                    type="primary"
                    @click="router.push(`/service/edit/${service.id}`)"
                  >
                    编辑此服务
                  </el-button>
                </div>
              </div>

              <div v-else class="buyer-actions">
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
                    size="large"
                    class="flex-btn"
                    :loading="collectLoading"
                    @click="handleToggleCollect"
                  >
                    <el-icon><component :is="service.isCollected ? StarFilled : Star" /></el-icon>
                    {{ service.isCollected ? '已收藏' : '收藏' }} ({{ service.collectionCount || 0 }})
                  </el-button>
                  <el-button size="large" class="flex-btn" @click="handleContactSeller">
                    <el-icon><ChatDotRound /></el-icon>
                    私信卖家
                  </el-button>
                </div>
              </div>
            </section>

            <!-- Seller Alumni Trust Card -->
            <section class="xyf-surface-card seller-card">
              <div class="seller-card__head">
                <XyfAvatar
                  :src="service.sellerAvatar"
                  :name="service.sellerName"
                  :size="50"
                />
                <div class="seller-card__info">
                  <div class="seller-name-row">
                    <span class="seller-name">{{ service.sellerName || `校园创作者 #${service.sellerId}` }}</span>
                    <span class="alumni-verify-tag" :style="service.sellerEduVerified === 1 ? 'background:#ecfdf5;color:#047857;border-color:#a7f3d0' : ''">{{ service.sellerEduVerified === 1 ? '教育邮箱已核验' : '学籍待核验' }} · 信用 {{ service.sellerCreditScore ?? 100 }}</span>
                  </div>
                  <div class="seller-meta">
                    <span v-if="service.sellerGrade">{{ service.sellerGrade }}</span>
                    <span v-if="service.sellerGrade && service.sellerProfessional">·</span>
                    <span v-if="service.sellerProfessional">{{ service.sellerProfessional }}</span>
                    <span v-if="!service.sellerGrade && !service.sellerProfessional">校内认证服务发布者</span>
                  </div>
                </div>
              </div>

              <p v-if="service.sellerBio" class="seller-bio">
                {{ service.sellerBio }}
              </p>

              <div class="seller-trust-grid xyf-tabular">
                <div class="trust-cell">
                  <strong>{{ (service.reviewCount && (service.averageRating || service.sellerRating)) ? Number(service.averageRating || service.sellerRating).toFixed(1) : '暂无' }}</strong>
                  <span>综合评分</span>
                </div>
                <div class="trust-cell">
                  <strong>{{ service.sellerCompletedOrders ?? service.salesCount ?? 0 }}</strong>
                  <span>累计完结单</span>
                </div>
                <div class="trust-cell">
                  <strong>{{ service.reviewCount ?? 0 }}</strong>
                  <span>真实评价</span>
                </div>
              </div>

              <el-button
                plain
                class="seller-works-btn"
                @click="router.push({ path: '/services', query: { sellerId: String(service.sellerId) } })"
              >
                查看 TA 的全部在架服务
              </el-button>
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
import { Star, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
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
import ServiceReviewList from './components/ServiceReviewList.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const collectLoading = ref(false)
const service = ref<ServiceDetail | null>(null)
const activeImageIndex = ref(0)
const activeTab = ref('intro')

const serviceId = computed(() => Number(route.params.id))

const galleryUrls = computed(() => {
  if (!service.value?.images?.length) return []
  return service.value.images
    .map((img) => buildImageUrl(img))
    .filter((url) => Boolean(url))
})

const activeImageUrl = computed(() => galleryUrls.value[activeImageIndex.value] || '')

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
    activeImageIndex.value = 0
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

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.gallery-card {
  padding: 16px;
}

.gallery-stage {
  height: 380px;
  background: var(--xyf-bg);
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  &__el-img {
    width: 100%;
    height: 100%;
  }
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  font-size: 13.5px;
  color: var(--xyf-ink-3);

  .crumb-link {
    color: var(--xyf-ink-2);
    transition: color var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
    }
  }

  .crumb-sep {
    color: #d4d4d8;
  }

  .crumb-current {
    color: var(--xyf-ink);
    font-weight: 500;
  }
}

.gallery-thumbs {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumb-btn {
  padding: 2px;
  border: 2px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color var(--xyf-transition);

  &.is-active {
    border-color: var(--xyf-primary);
  }
}

.detail-tabs-card {
  padding: 18px 22px 24px;
}

.service-description {
  font-size: 14.5px;
  line-height: 1.8;
  color: var(--xyf-ink-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.offline-location-wrap {
  margin-top: 20px;
}

.detail-aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 84px;
}

.purchase-card,
.seller-card {
  padding: 22px;
}

.purchase-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.purchase-title {
  margin: 0 0 16px;
  font-size: 20px;
  line-height: 1.38;
  font-weight: 750;
  color: var(--xyf-ink);
}

.purchase-price-box {
  padding: 14px 16px;
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;

  .price-label {
    font-size: 12px;
    color: var(--xyf-ink-muted);
    display: block;
    margin-bottom: 2px;
  }

  .price-value {
    font-size: 28px;
    font-weight: 800;
    color: var(--xyf-accent);
    line-height: 1.1;
  }

  .price-metrics {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
    display: flex;
    gap: 6px;
  }
}

.spec-list {
  margin: 0 0 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13.5px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--xyf-border-light);

  dt {
    color: var(--xyf-ink-muted);
    flex-shrink: 0;
  }

  dd {
    margin: 0;
    color: var(--xyf-ink);
    font-weight: 500;
    text-align: right;
  }

  .text-danger {
    color: var(--xyf-danger);
    font-weight: 600;
  }
}

.owner-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.owner-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.buyer-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .buy-btn {
    width: 100%;
    height: 44px;
    font-size: 15px;
    font-weight: 650;
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

.seller-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  .seller-name-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .seller-name {
    font-size: 15px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .alumni-verify-tag {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 7px;
    border-radius: 5px;
    background: #f4f4f5;
    border: 1px solid #e4e4e7;
    color: var(--xyf-ink-2);
  }

  .seller-meta {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
    margin-top: 3px;
  }
}

.seller-bio {
  margin: 0 0 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-light);
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--xyf-ink-2);
}

.seller-trust-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 10px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  margin-bottom: 14px;

  .trust-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    text-align: center;

    strong {
      font-size: 16px;
      font-weight: 750;
      color: var(--xyf-ink);
      line-height: 1.15;
    }

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.seller-works-btn {
  width: 100%;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-aside {
    position: static;
  }

  .gallery-stage {
    height: 260px;
  }
}
</style>
