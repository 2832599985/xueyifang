<template>
  <div class="creator-profile-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="创作者数字信用档案"
        subtitle="基于高校教育邮箱实名、历史担保结算与真实买方评价构建的履约作品集"
        :show-back="true"
        back-text="返回上一页"
      />

      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!profile" class="xyf-surface-card" style="padding: 40px">
        <XyfEmptyState title="未找到该创作者档案" />
      </div>

      <template v-else>
        <!-- Hero Identity & Trust Card -->
        <section class="xyf-surface-card identity-hero">
          <div class="identity-left">
            <XyfAvatar :src="profile.avatar" :name="profile.displayName" :size="82" />
            <div class="identity-meta">
              <div class="name-line">
                <h1>{{ profile.displayName }}</h1>
                <span class="edu-pill" :class="{ 'is-verified': profile.eduVerified === 1 }">
                  {{ profile.eduVerified === 1 ? '高校 .edu.cn 实名已核验' : '校内注册成员' }}
                </span>
                <span v-if="profile.publishPermission === 1" class="creator-pill">认证技能创作者</span>
                <span v-if="Number(profile.sellerDeposit || 0) > 0" class="edu-pill is-verified">已缴履约保证金 ¥{{ Number(profile.sellerDeposit).toFixed(0) }}</span>
              </div>

              <div class="campus-line">
                <span v-if="profile.campusName">{{ profile.campusName }}</span>
                <span v-if="profile.professionalName">· {{ profile.professionalName }}</span>
                <span v-if="profile.grade">· {{ profile.grade }}</span>
              </div>

              <p class="bio-text">
                {{ profile.bio || '这位创作者专注于提供高质量校园技能服务，支持平台担保与当面核销验收。' }}
              </p>
            </div>
          </div>

          <div class="identity-right">
            <div class="credit-badge-box xyf-tabular">
              <span class="c-label">校园履约信用分</span>
              <strong class="c-score">{{ profile.creditScore }}</strong>
              <span class="c-tier">{{ profile.creditTier }}</span>
            </div>

            <el-button
              v-if="userStore.userInfo?.id !== profile.userId && !userStore.isAdmin"
              type="primary"
              @click="router.push(`/chat/${profile.userId}`)"
            >
              私信咨询创作者
            </el-button>
          </div>
        </section>

        <!-- Stats Strip -->
        <div class="stats-grid xyf-tabular">
          <div class="xyf-surface-card stat-box">
            <span>已完结担保订单</span>
            <strong>{{ profile.completedOrderCount }} 笔</strong>
          </div>
          <div class="xyf-surface-card stat-box">
            <span>在架技能服务</span>
            <strong>{{ profile.activeServiceCount }} 项</strong>
          </div>
          <div class="xyf-surface-card stat-box">
            <span>买方真实评价数</span>
            <strong>{{ profile.reviewCount }} 条</strong>
          </div>
          <div class="xyf-surface-card stat-box">
            <span>历史综合好评度</span>
            <strong>{{ profile.averageRating ? `${Number(profile.averageRating).toFixed(1)} / 5.0` : '暂无评分' }}</strong>
          </div>
        </div>

        <!-- Active Services Portfolio -->
        <section class="xyf-surface-card section-block">
          <div class="xyf-section-heading">
            <h2>在架技能与作品橱窗</h2>
          </div>

          <div v-if="profile.services.length === 0" class="empty-tip">
            该创作者当前暂无公开在架技能服务
          </div>

          <div v-else class="svc-grid">
            <article
              v-for="svc in profile.services"
              :key="svc.id"
              class="svc-item"
              @click="router.push(`/services/${svc.id}`)"
            >
              <XyfImage
                :src="buildImageUrl(svc.coverImage)"
                :alt="svc.serviceTitle"
                width="100%"
                height="156px"
                :radius="10"
                fit="cover"
              />
              <div class="svc-body">
                <div class="svc-tags">
                  <span v-if="svc.tagName" class="s-tag">{{ svc.tagName }}</span>
                  <span class="s-mode">{{ svc.tradeType === 1 ? '线下当面' : '线上担保' }}</span>
                </div>
                <h3>{{ svc.serviceTitle }}</h3>
                <div class="svc-foot xyf-tabular">
                  <strong>¥{{ Number(svc.price).toFixed(2) }}</strong>
                  <span>已履约 {{ svc.currentPurchaseCount || 0 }} 次</span>
                </div>
              </div>
            </article>
          </div>
        </section>

        <!-- Verified Buyer Reviews -->
        <section class="xyf-surface-card section-block">
          <div class="xyf-section-heading">
            <h2>已核验订单真实评价</h2>
          </div>

          <div v-if="profile.recentReviews.length === 0" class="empty-tip">
            暂无历史订单评价记录
          </div>

          <div v-else class="review-list">
            <div v-for="rev in profile.recentReviews" :key="rev.id" class="review-row">
              <div class="review-head">
                <div>
                  <strong>{{ rev.buyerDisplayName }}</strong>
                  <span class="rev-svc">购买了「{{ rev.serviceTitle }}」</span>
                </div>
                <el-rate :model-value="rev.rating" disabled />
              </div>
              <p class="rev-content">{{ rev.content || '默认好评，当面核销验收顺利！' }}</p>
              <span class="rev-time">{{ formatDateTime(rev.createTime) }}</span>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCreatorProfile } from '@/api/creator'
import { buildImageUrl } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { CreatorProfileData } from '@/types/demand'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const profile = ref<CreatorProfileData | null>(null)

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
.identity-hero {
  padding: 26px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.identity-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  min-width: 280px;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 800;
  }
}

.edu-pill,
.creator-pill {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.edu-pill {
  background: #f4f4f5;
  color: #52525b;

  &.is-verified {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #a7f3d0;
  }
}

.creator-pill {
  background: #18181b;
  color: #ffffff;
}

.campus-line {
  margin-top: 6px;
  font-size: 13px;
  color: var(--xyf-ink-muted);
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.bio-text {
  margin: 10px 0 0;
  font-size: 13.5px;
  color: var(--xyf-ink-2);
  max-width: 620px;
  line-height: 1.6;
}

.identity-right {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  min-width: 170px;
}

.credit-badge-box {
  padding: 14px 18px;
  border-radius: 14px;
  background: #18181b;
  color: #ffffff;
  text-align: center;

  .c-label {
    display: block;
    font-size: 11.5px;
    color: rgba(255, 255, 255, 0.72);
  }

  .c-score {
    display: block;
    font-size: 30px;
    font-weight: 800;
    margin: 2px 0;
  }

  .c-tier {
    font-size: 11.5px;
    color: #34d399;
    font-weight: 600;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.stat-box {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  strong {
    font-size: 20px;
    font-weight: 800;
    color: var(--xyf-ink);
  }
}

.section-block {
  padding: 22px 26px;
  margin-bottom: 18px;
}

.svc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.svc-item {
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease;

  &:hover {
    transform: translateY(-2px);
  }

  .svc-body {
    padding: 12px 14px;
  }

  .svc-tags {
    display: flex;
    gap: 6px;
    margin-bottom: 6px;
    font-size: 11px;

    .s-tag {
      padding: 2px 7px;
      border-radius: 999px;
      background: #f4f4f5;
    }

    .s-mode {
      padding: 2px 7px;
      border-radius: 999px;
      background: #ecfdf5;
      color: #047857;
    }
  }

  h3 {
    margin: 0 0 10px;
    font-size: 14.5px;
    font-weight: 700;
    line-height: 1.4;
  }

  .svc-foot {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    font-size: 12px;
    color: var(--xyf-ink-muted);

    strong {
      font-size: 16px;
      color: var(--xyf-ink);
    }
  }
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-row {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  .review-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .rev-svc {
    margin-left: 8px;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  .rev-content {
    margin: 8px 0 6px;
    font-size: 13.5px;
    color: var(--xyf-ink-2);
  }

  .rev-time {
    font-size: 11.5px;
    color: var(--xyf-ink-muted);
  }
}

.empty-tip {
  padding: 20px 0;
  color: var(--xyf-ink-muted);
  font-size: 13.5px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
