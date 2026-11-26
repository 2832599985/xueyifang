<template>
  <div v-if="resolvedName" class="trade-location-badge">
    <div class="trade-location-badge__icon">
      <el-icon><Location /></el-icon>
    </div>
    <div class="trade-location-badge__content">
      <div class="trade-location-badge__title">
        <span>{{ title }}：</span>
        <strong>{{ resolvedName }}</strong>
      </div>
      <div v-if="resolvedAddress" class="trade-location-badge__addr">
        {{ resolvedAddress }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Location } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import type { TradeLocation } from '@/types/dict'

interface Props {
  locationId?: number
  locationName?: string
  location?: TradeLocation | null
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  locationId: undefined,
  locationName: '',
  location: null,
  title: '约定线下交易地点'
})

const dictStore = useDictStore()

onMounted(() => {
  if (props.locationId && !dictStore.hasTradeLocations) {
    dictStore.fetchTradeLocations().catch(() => {})
  }
})

const matchedLocation = computed(() => {
  if (props.location) return props.location
  if (props.locationId) return dictStore.getLocationById(props.locationId)
  return undefined
})

const resolvedName = computed(() => {
  return props.locationName || matchedLocation.value?.locationName || ''
})

const resolvedAddress = computed(() => {
  return matchedLocation.value?.locationAddress || matchedLocation.value?.locationDescription || ''
})
</script>

<style scoped lang="scss">
.trade-location-badge {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-primary-soft);
  border: 1px solid var(--xyf-primary-border);

  &__icon {
    width: 34px;
    height: 34px;
    border-radius: var(--xyf-radius-xs);
    background: var(--xyf-surface);
    color: var(--xyf-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 17px;
    flex-shrink: 0;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 14px;
    color: var(--xyf-ink-2);
    line-height: 1.4;

    strong {
      color: var(--xyf-ink);
      font-weight: 600;
    }
  }

  &__addr {
    margin-top: 3px;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }
}
</style>
