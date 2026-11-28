<template>
  <div class="creator-profile-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="创作者主页"
        :show-back="true"
        back-text="返回上一页"
      />

      <div v-if="loading" class="xyf-surface-card" style="padding: 32px">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!profile" class="xyf-surface-card" style="padding: 48px">
        <XyfEmptyState title="未找到该创作者档案" description="用户可能不存在或暂未公开档案" />
      </div>

      <template v-else>
        <!-- Architectural Studio Cover + Trust Passport Card -->
        <section class="studio-passport xyf-surface-card">
          <!-- Bespoke Architectural Topographic Ribbon -->
          <div class="studio-cover" aria-hidden="true">
            <svg viewBox="0 0 1200 160" preserveAspectRatio="none">
              <path d="M0 110 C 320 40, 620 150, 940 70 C 1060 40, 1140 75, 1200 55" stroke="rgba(255,255,255,0.12)" stroke-width="1.2" fill="none" />
              <path d="M0 135 C 280 75, 680 165, 980 95 C 1090 70, 1150 95, 1200 82" stroke="rgba(229,199,158,0.22)" stroke-width="1.2" fill="none" />
            </svg>
            <div class="studio-cover__badges">
              <span v-if="Number(profile.sellerDeposit || 0) > 0" class="cover-pill is-gold">
                <svg class="cover-svg" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M8 1.6L13 3.6v4c0 3.2-2.1 5.7-5 6.8-2.9-1.1-5-3.6-5-6.8v-4L8 1.6Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
                </svg>
                <span>履约保证金 ¥{{ Number(profile.sellerDeposit).toFixed(0) }}</span>
              </span>
              <span v-if="profile.publishPermission === 1" class="cover-pill">
                <svg class="cover-svg" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M3.5 8.5L6.5 11.5L12.5 4.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span>认证创作者</span>
              </span>
            </div>
          </div>

          <div class="studio-body">
            <div class="studio-identity">
              <div class="studio-avatar-frame">
                <XyfAvatar :src="profile.avatar" :name="profile.displayName" :size="88" />
              </div>

              <div class="studio-meta">
                <div class="studio-name-row">
                  <h1>{{ profile.displayName }}</h1>
                  <XyfVerifiedMark :verified="profile.eduVerified === 1" />
                  <span class="tier-pill">{{ profile.creditTier }}</span>
                </div>

                <div class="studio-affiliation">
                  <span v-if="profile.campusName">{{ profile.campusName }}</span>
                  <span v-if="profile.professionalName">· {{ profile.professionalName }}</span>
                  <span v-if="profile.grade">· {{ profile.grade }}</span>
                </div>

                <p class="studio-bio">
                  {{ profile.bio || '专注于提供高质量校园技能服务，支持平台资金担保冻结与真实地图围栏当面核销验收。' }}
                </p>
              </div>
            </div>

            <!-- Right: SVG Credit Score Gauge + Contact CTA -->
            <div class="studio-trust-dock">
              <div class="credit-ring-widget xyf-tabular">
                <svg viewBox="0 0 84 84" class="credit-ring-svg">
                  <circle cx="42" cy="42" r="35" fill="none" stroke="#f1f1f4" stroke-width="6" />
                  <circle
                    cx="42"
                    cy="42"
                    r="35"
                    fill="none"
                    stroke="#18181b"
                    stroke-width="6"
                    stroke-linecap="round"
                    :stroke-dasharray="ringCircumference"
                    :stroke-dashoffset="creditDashOffset"
                    transform="rotate(-90 42 42)"
                  />
                </svg>
                <div class="credit-ring-center">
                  <strong>{{ profile.creditScore }}</strong>
                  <span>信用分</span>
                </div>
              </div>

              <div class="studio-cta-col">
                <el-button
                  v-if="userStore.userInfo?.id !== profile.userId && !userStore.isAdmin"
                  type="primary"
                  size="large"
                  @click="router.push(`/chat/${profile.userId}`)"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  私信咨询创作者
                </el-button>
                <el-button
                  v-else-if="userStore.userInfo?.id === profile.userId"
                  plain
                  @click="router.push('/user/center')"
                >
                  编辑个人档案
                </el-button>
              </div>
            </div>
          </div>
        </section>

        <!-- 4 KPI Tiles -->
        <section class="studio-kpis">
          <XyfStatTile
            label="累计完结担保单"
            :value="`${profile.completedOrderCount} 笔`"
            hint="平台全流程担保结算"
          />
          <XyfStatTile
            label="公开在架技能"
            :value="`${profile.activeServiceCount} 项`"
            hint="标准化校园服务橱窗"
          />
          <XyfStatTile
            label="已核验真实评价"
            :value="`${profile.reviewCount} 条`"
            hint="仅真实完单买方可评"
          />
          <XyfStatTile
            label="历史综合口碑"
            :value="profile.averageRating ? `${Number(profile.averageRating).toFixed(1)} / 5.0` : '5.0 / 5.0'"
            hint="履约准时与交付满意度"
          />
        </section>

        <!-- Tabbed Portfolio & Verified Reviews -->
        <section class="xyf-surface-card portfolio-panel">
          <div class="portfolio-tabs-bar">
            <div class="xyf-tabs">
              <button
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': activeTab === 'services' }"
                @click="activeTab = 'services'"
              >
                在架技能作品橱窗 ({{ profile.services.length }})
              </button>
              <button
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': activeTab === 'reviews' }"
                @click="activeTab = 'reviews'"
              >
                已核验订单评价 ({{ profile.recentReviews.length }})
              </button>
            </div>
          </div>

          <!-- Tab 1: Active Services Showcase (Reusing Editorial ServiceCard) -->
          <div v-if="activeTab === 'services'" class="portfolio-body">
            <div v-if="profile.services.length === 0" class="empty-box">
              <XyfEmptyState
                title="该创作者当前暂无公开在架服务"
                description="您仍可通过右上角「私信咨询创作者」沟通定制需求或邀请其参与悬赏竞标。"
              />
            </div>

            <div v-else class="portfolio-service-grid">
              <ServiceCard
                v-for="svc in mappedServices"
                :key="svc.id"
                :service="svc"
                :seller-name-fallback="profile.displayName"
                @click="(id) => router.push(`/services/${id}`)"
              />
            </div>
          </div>

          <!-- Tab 2: Verified Buyer Reviews + Rating Distribution -->
          <div v-else class="portfolio-body">
            <div v-if="profile.recentReviews.length === 0" class="empty-box">
              <XyfEmptyState
                title="暂无历史订单评价"
                description="完成订单验收后，买方的评价将在此公开展示。"
              />
            </div>

            <template v-else>
              <!-- Rating Breakdown Bar -->
              <div class="rating-summary xyf-tabular">
                <div class="rating-summary__score">
                  <strong>{{ profile.averageRating ? Number(profile.averageRating).toFixed(1) : '5.0' }}</strong>
                  <el-rate
                    :model-value="Number(profile.averageRating || 5)"
                    disabled
                    size="small"
                  />
                  <span>基于 {{ profile.reviewCount }} 笔核验订单</span>
                </div>

                <div class="rating-bars">
                  <div v-for="star in [5, 4, 3, 2, 1]" :key="star" class="r-bar-row">
                    <span class="r-star">{{ star }} 星</span>
                    <div class="r-track">
                      <div class="r-fill" :style="{ width: `${starDistPercent(star)}%` }" />
                    </div>
                    <span class="r-pct">{{ starDistPercent(star) }}%</span>
                  </div>
                </div>
              </div>

              <div class="review-stream">
                <article v-for="rev in profile.recentReviews" :key="rev.id" class="review-card">
                  <div class="review-card__head">
                    <div class="rev-buyer">
                      <XyfAvatar :name="rev.buyerDisplayName" :size="34" />
                      <div>
                        <strong>{{ rev.buyerDisplayName }}</strong>
                        <span class="rev-svc">已购服务：{{ rev.serviceTitle }}</span>
                      </div>
                    </div>
                    <div class="rev-right xyf-tabular">
                      <el-rate :model-value="rev.rating" disabled size="small" />
                      <span class="rev-time">{{ formatDateTime(rev.createTime).slice(0, 10) }}</span>
                    </div>
                  </div>
                  <p class="rev-text">{{ rev.content || '按时高质量完成交付，当面核销体验顺畅！' }}</p>
                </article>
              </div>
            </template>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCreatorProfile } from '@/api/creator'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { CreatorProfileData } from '@/types/demand'
import type { ServiceListItem } from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfVerifiedMark from '@/components/ui/XyfVerifiedMark.vue'
import ServiceCard from '@/components/business/ServiceCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const activeTab = ref<'services' | 'reviews'>('services')
const profile = ref<CreatorProfileData | null>(null)

const ringCircumference = 2 * Math.PI * 35

const creditDashOffset = computed(() => {
  const score = Number(profile.value?.creditScore || 100)
  const ratio = Math.min(1, Math.max(0.1, score / 130))
  return Number((ringCircumference * (1 - ratio)).toFixed(2))
})

const mappedServices = computed<ServiceListItem[]>(() => {
  if (!profile.value) return []
  return profile.value.services.map(s => ({
    ...s,
    sellerName: profile.value?.displayName,
    sellerAvatar: profile.value?.avatar,
    averageRating: profile.value?.averageRating
  })) as unknown as ServiceListItem[]
})

function starDistPercent(star: number) {
  const list = profile.value?.recentReviews || []
  if (!list.length) return star === 5 ? 100 : 0
  const count = list.filter(r => Math.round(Number(r.rating || 5)) === star).length
  return Math.round((count / list.length) * 100)
}

async function fetchProfile() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    profile.value = await getCreatorProfile(id)
  } catch (err) {
    notifyFallbackError(err, '加载创作者主页失败')
    profile.value = null
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, fetchProfile)
onMounted(fetchProfile)
</script>

<style scoped lang="scss">
.studio-passport {
  overflow: hidden;
  margin-bottom: 18px;
}

.studio-cover {
  position: relative;
  height: 118px;
  background: linear-gradient(135deg, #121215 0%, #232329 100%);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 18px 24px;

  svg {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
  }

  &__badges {
    position: relative;
    z-index: 2;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .cover-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 11.5px;
    font-weight: 600;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.16);
    color: #ffffff;

    .cover-svg {
      width: 12px;
      height: 12px;
      flex-shrink: 0;
    }

    &.is-gold {
      background: rgba(200, 169, 126, 0.18);
      border-color: rgba(200, 169, 126, 0.45);
      color: #f3e2c7;
    }
  }
}

.studio-body {
  padding: 0 28px 26px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.studio-identity {
  display: flex;
  align-items: flex-end;
  gap: 22px;
  flex: 1;
  min-width: 280px;

  @media (max-width: 640px) {
    flex-direction: column;
    align-items: flex-start;
  }
}

.studio-avatar-frame {
  margin-top: -38px;
  position: relative;
  z-index: 3;
  padding: 4px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: var(--xyf-shadow-sm);
  flex-shrink: 0;
}

.studio-meta {
  padding-top: 14px;

  .studio-name-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.02em;
      color: var(--xyf-ink);
    }

    .tier-pill {
      padding: 3px 10px;
      border-radius: 999px;
      background: #18181b;
      color: #e5c79e;
      font-size: 11.5px;
      font-weight: 600;
    }
  }

  .studio-affiliation {
    margin-top: 5px;
    font-size: 13px;
    color: var(--xyf-ink-3);
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .studio-bio {
    margin: 10px 0 0;
    font-size: 14px;
    line-height: 1.6;
    color: var(--xyf-ink-2);
    max-width: 620px;
  }
}

.studio-trust-dock {
  padding-top: 14px;
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.credit-ring-widget {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;

  .credit-ring-svg {
    width: 100%;
    height: 100%;
  }

  .credit-ring-center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    line-height: 1.1;

    strong {
      font-size: 19px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    span {
      font-size: 10px;
      color: var(--xyf-ink-3);
    }
  }
}

.studio-kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;

  @media (max-width: 820px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Tabbed Portfolio */
.portfolio-panel {
  overflow: hidden;
}

.portfolio-tabs-bar {
  padding: 6px 24px 0;
  background: var(--xyf-surface-soft);
  border-bottom: 1px solid var(--xyf-border);

  .xyf-tabs {
    border-bottom: none;
  }
}

.portfolio-body {
  padding: 26px 28px;
}

.portfolio-service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 22px;
}

/* Rating Distribution & Reviews */
.rating-summary {
  padding: 20px 24px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  gap: 36px;
  flex-wrap: wrap;
  margin-bottom: 20px;

  &__score {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;

    strong {
      font-size: 36px;
      font-weight: 700;
      line-height: 1;
      color: var(--xyf-ink);
    }

    span {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }

  .rating-bars {
    flex: 1;
    min-width: 240px;
    max-width: 420px;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .r-bar-row {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    color: var(--xyf-ink-2);

    .r-star {
      width: 32px;
    }

    .r-track {
      flex: 1;
      height: 6px;
      border-radius: 999px;
      background: #e4e4e7;
      overflow: hidden;

      .r-fill {
        height: 100%;
        background: #18181b;
        border-radius: 999px;
      }
    }

    .r-pct {
      width: 36px;
      text-align: right;
      color: var(--xyf-ink-3);
    }
  }
}

.review-stream {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .rev-buyer {
    display: flex;
    align-items: center;
    gap: 10px;

    strong {
      display: block;
      font-size: 13.5px;
      color: var(--xyf-ink);
    }

    .rev-svc {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }

  .rev-right {
    display: flex;
    align-items: center;
    gap: 10px;

    .rev-time {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }

  .rev-text {
    margin: 10px 0 0;
    font-size: 13.5px;
    line-height: 1.65;
    color: var(--xyf-ink-2);
  }
}
</style>
