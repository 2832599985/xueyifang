<template>
  <article class="service-card" @click="emit('click', service.id)">
    <div class="service-card__media">
      <XyfImage
        :src="buildImageUrl(service.coverImage)"
        :alt="service.serviceTitle"
        :lazy="lazy"
        height="100%"
        width="100%"
        fit="cover"
        class="service-card__img"
      />
      <slot name="overlay" />
    </div>

    <div class="service-card__body">
      <div class="service-card__meta-top">
        <div class="service-card__seller" v-if="sellerDisplayName">
          <XyfAvatar
            :src="service.sellerAvatar"
            :name="sellerDisplayName"
            :size="20"
          />
          <span class="seller-name ellipsis">{{ sellerDisplayName }}</span>
        </div>
        <span v-else class="seller-name">{{ service.tagName || '校园技能' }}</span>

        <span v-if="service.averageRating && service.averageRating > 0" class="seller-rating xyf-tabular">
          <el-icon><StarFilled /></el-icon>
          {{ Number(service.averageRating).toFixed(1) }}
        </span>
      </div>

      <h3 class="service-card__title ellipsis-2" :title="service.serviceTitle">
        {{ service.serviceTitle }}
      </h3>

      <div class="service-card__footer">
        <div class="service-card__price-wrap xyf-tabular">
          <span class="service-card__price">{{ formattedPrice }}</span>
          <span class="service-card__unit">/ 次</span>
        </div>

        <XyfDeliveryIcon :trade-type="service.tradeType" size="xs" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { StarFilled } from '@element-plus/icons-vue'
import { buildImageUrl } from '@/api/service'
import type { ServiceListItem } from '@/types/service'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfDeliveryIcon from '@/components/ui/XyfDeliveryIcon.vue'

interface Props {
  service: ServiceListItem
  sellerNameFallback?: string
  lazy?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sellerNameFallback: '',
  lazy: true
})

const emit = defineEmits<{ (e: 'click', id: number): void }>()

const sellerDisplayName = computed(() => props.service.sellerName || props.sellerNameFallback || '')

const formattedPrice = computed(() => {
  const num = Number(props.service.price)
  if (Number.isNaN(num)) return '¥0'
  return Number.isInteger(num) ? `¥${num}` : `¥${num.toFixed(2)}`
})
</script>

<style scoped lang="scss">
.service-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
  transition: transform var(--xyf-transition);

  &:hover {
    .service-card__img :deep(.xyf-image__img) {
      transform: scale(1.04);
    }

    .service-card__title {
      color: var(--xyf-primary);
    }
  }

  &__media {
    position: relative;
    width: 100%;
    aspect-ratio: 4 / 3;
    border-radius: 14px;
    overflow: hidden;
    background: var(--xyf-surface-muted);
    flex-shrink: 0;
  }

  &__img {
    width: 100%;
    height: 100%;

    :deep(.xyf-image__img) {
      transition: opacity 0.25s ease, transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
    }
  }

  &__body {
    padding: 12px 2px 2px;
    display: flex;
    flex-direction: column;
    flex: 1;
    gap: 6px;
  }

  &__meta-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 22px;
  }

  &__seller {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    min-width: 0;
    flex: 1;
  }

  .seller-name {
    font-size: 12.5px;
    color: var(--xyf-ink-2);
    line-height: 1.2;
  }

  .seller-rating {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--xyf-ink);
    flex-shrink: 0;

    .el-icon {
      color: var(--xyf-ink);
      font-size: 12px;
    }
  }

  &__title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--xyf-ink);
    line-height: 1.45;
    letter-spacing: -0.01em;
    min-height: 43px;
    transition: color var(--xyf-transition);
  }

  &__footer {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 8px;
    margin-top: auto;
    padding-top: 2px;
  }

  &__price-wrap {
    display: inline-flex;
    align-items: baseline;
    gap: 3px;
  }

  &__price {
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
    letter-spacing: -0.02em;
  }

  &__unit {
    font-size: 12px;
    color: var(--xyf-ink-3);
    font-weight: 400;
  }

  &__mode {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 12px;
    color: var(--xyf-ink-3);

    .el-icon {
      font-size: 12px;
      flex-shrink: 0;
    }
  }
}
</style>
