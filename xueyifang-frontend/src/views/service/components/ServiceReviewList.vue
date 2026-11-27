<template>
  <div class="service-review-list">
    <div class="review-list-head">
      <span class="review-count">
        共 <strong class="xyf-tabular">{{ total }}</strong> 条订单评价
      </span>
    </div>

    <div v-if="loading" class="review-skeleton">
      <el-skeleton :rows="3" animated />
    </div>

    <XyfEmptyState
      v-else-if="reviews.length === 0"
      title="暂未收到订单评价"
      description="完成交易并确认收货后，买家可在此留下客观评价。"
    />

    <div v-else class="reviews-stack">
      <article v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-card__top">
          <div class="review-card__author">
            <XyfAvatar
              :src="getReviewerAvatar(review, buildImageUrl)"
              :name="getReviewerDisplayName(review)"
              :size="36"
            />
            <div>
              <div class="author-name">{{ getReviewerDisplayName(review) }}</div>
              <div class="review-time xyf-tabular">{{ formatReviewTime(review.createTime) }}</div>
            </div>
          </div>

          <div class="review-card__rating">
            <el-rate
              :model-value="review.rating"
              disabled
              :colors="['#F59E0B', '#F59E0B', '#F59E0B']"
              void-color="#E3DFD5"
            />
            <span class="rating-label">{{ getRatingLabel(review.rating) }}</span>
          </div>
        </div>

        <p class="review-card__content">{{ review.content }}</p>
      </article>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="fetchReviews"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { listServiceReviews } from '@/api/review'
import { buildImageUrl } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import {
  getRatingLabel,
  formatReviewTime,
  getReviewerDisplayName,
  getReviewerAvatar,
  type ServiceReview
} from '@/types/review'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

interface Props {
  serviceId: number
}

const props = defineProps<Props>()

const loading = ref(false)
const reviews = ref<ServiceReview[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 8

async function fetchReviews() {
  if (!props.serviceId) return
  loading.value = true
  try {
    const res = await listServiceReviews(props.serviceId, {
      pageNum: pageNum.value,
      pageSize
    })
    reviews.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '加载服务评价失败')
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => props.serviceId,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      pageNum.value = 1
      fetchReviews()
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.review-list-head {
  margin-bottom: 14px;
  font-size: 13.5px;
  color: var(--xyf-ink-muted);

  strong {
    color: var(--xyf-ink);
  }
}

.reviews-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  padding: 16px 18px;
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-bg);

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 10px;
  }

  &__author {
    display: flex;
    align-items: center;
    gap: 10px;

    .author-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--xyf-ink);
    }

    .review-time {
      font-size: 12px;
      color: var(--xyf-ink-muted);
    }
  }

  &__rating {
    display: flex;
    align-items: center;
    gap: 8px;

    .rating-label {
      font-size: 12.5px;
      font-weight: 600;
      color: var(--xyf-primary-dark);
    }
  }

  &__content {
    margin: 0;
    font-size: 14px;
    line-height: 1.65;
    color: var(--xyf-ink-secondary);
    white-space: pre-wrap;
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
