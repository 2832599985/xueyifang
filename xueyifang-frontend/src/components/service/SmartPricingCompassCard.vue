<template>
  <div class="pricing-compass-card" v-if="benchmark">
    <div class="compass-header">
      <div class="compass-header__left">
        <span class="compass-kicker">SMART PRICING BENCHMARK · 历史成交分位数参考</span>
        <h4 class="compass-title">
          {{ benchmark.tagName || '校内同类技能' }} · 定价分位罗盘
        </h4>
      </div>
      <div class="compass-badge" :class="positionClass">
        {{ benchmark.pricePositionLabel }}（P{{ benchmark.percentileRank }}）
      </div>
    </div>

    <div class="percentile-grid">
      <div
        class="percentile-box"
        :class="{ 'is-clickable': interactive }"
        @click="interactive && emit('select-price', benchmark.p25Price)"
      >
        <span class="p-label">P25 引流线</span>
        <strong class="p-val">¥{{ formatMoney(benchmark.p25Price) }}</strong>
        <span v-if="interactive" class="p-action">点击采用</span>
      </div>

      <div
        class="percentile-box is-highlight"
        :class="{ 'is-clickable': interactive }"
        @click="interactive && emit('select-price', benchmark.p50Price)"
      >
        <span class="p-label">P50 成交中位数</span>
        <strong class="p-val">¥{{ formatMoney(benchmark.p50Price) }}</strong>
        <span v-if="interactive" class="p-action">主流推荐</span>
      </div>

      <div
        class="percentile-box"
        :class="{ 'is-clickable': interactive }"
        @click="interactive && emit('select-price', benchmark.p75Price)"
      >
        <span class="p-label">P75 资深溢价线</span>
        <strong class="p-val">¥{{ formatMoney(benchmark.p75Price) }}</strong>
        <span v-if="interactive" class="p-action">点击采用</span>
      </div>

      <div class="percentile-box">
        <span class="p-label">P90 定制顶配线</span>
        <strong class="p-val">¥{{ formatMoney(benchmark.p90Price) }}</strong>
        <span class="p-sub">均价 ¥{{ formatMoney(benchmark.avgPrice) }}</span>
      </div>
    </div>

    <div class="spectrum-track-wrap">
      <div class="spectrum-track">
        <div class="spectrum-sweet-zone" />
        <div
          class="spectrum-pointer"
          :style="{ left: `${Math.min(96, Math.max(4, benchmark.percentileRank || 50))}%` }"
        >
          <span class="pointer-bubble">¥{{ formatMoney(proposedPrice || benchmark.p50Price) }}</span>
        </div>
      </div>
      <div class="spectrum-legend">
        <span>高性价比区 (&lt;P25)</span>
        <span class="sweet-text">校内黄金成交带 [¥{{ formatMoney(benchmark.recommendedMinPrice) }} ~ ¥{{ formatMoney(benchmark.recommendedMaxPrice) }}]</span>
        <span>高客单区 (&gt;P75)</span>
      </div>
    </div>

    <div class="compass-footer">
      <p class="advice-text">{{ benchmark.conversionAdvice }}</p>
      <span class="sample-meta">
        统计样本：{{ benchmark.completedOrderSampleCount }} 笔真实完单 + {{ benchmark.activeListingSampleCount }} 项校内在售服务
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { getPricingBenchmark, type PricingBenchmarkData } from '@/api/riskRadar'

const props = withDefaults(
  defineProps<{
    tagId?: number
    campusId?: number
    tradeType?: number
    proposedPrice?: number
    interactive?: boolean
  }>(),
  {
    interactive: false
  }
)

const emit = defineEmits<{
  (e: 'select-price', price: number): void
}>()

const benchmark = ref<PricingBenchmarkData | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const positionClass = computed(() => {
  const code = benchmark.value?.pricePositionCode
  if (code === 'BELOW_P25_VALUE') return 'is-value'
  if (code === 'SWEET_SPOT_P25_P75') return 'is-sweet'
  if (code === 'PREMIUM_P75_P90') return 'is-premium'
  return 'is-outlier'
})

const formatMoney = (val?: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toFixed(2)
}

const fetchBenchmark = async () => {
  try {
    const res = await getPricingBenchmark({
      tagId: props.tagId,
      campusId: props.campusId,
      tradeType: props.tradeType,
      proposedPrice: props.proposedPrice
    })
    benchmark.value = res
  } catch {
    // 静默容错
  }
}

const scheduleFetch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchBenchmark()
  }, 240)
}

watch(
  () => [props.tagId, props.campusId, props.tradeType, props.proposedPrice],
  () => {
    scheduleFetch()
  }
)

onMounted(() => {
  fetchBenchmark()
})
</script>

<style scoped lang="scss">
.pricing-compass-card {
  margin: 12px 0 18px;
  padding: 16px 18px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
}

.compass-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  .compass-kicker {
    display: block;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #64748b;
  }

  .compass-title {
    margin: 2px 0 0;
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
  }
}

.compass-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;

  &.is-sweet {
    background: #dcfce7;
    color: #15803d;
  }
  &.is-value {
    background: #dbeafe;
    color: #1d4ed8;
  }
  &.is-premium {
    background: #fef3c7;
    color: #b45309;
  }
  &.is-outlier {
    background: #fee2e2;
    color: #b91c1c;
  }
}

.percentile-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.percentile-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  transition: all 0.18s ease;

  &.is-highlight {
    border-color: #10b981;
    background: #f0fdf4;
  }

  &.is-clickable {
    cursor: pointer;
    &:hover {
      transform: translateY(-2px);
      border-color: #0f172a;
      box-shadow: 0 4px 10px rgba(15, 23, 42, 0.07);
    }
  }

  .p-label {
    font-size: 11.5px;
    color: #64748b;
  }

  .p-val {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }

  .p-action {
    font-size: 11px;
    font-weight: 600;
    color: #059669;
  }

  .p-sub {
    font-size: 11px;
    color: #94a3b8;
  }
}

.spectrum-track-wrap {
  margin: 18px 0 12px;
}

.spectrum-track {
  position: relative;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, #93c5fd 0%, #34d399 25%, #10b981 75%, #fbbf24 90%, #f87171 100%);
}

.spectrum-sweet-zone {
  position: absolute;
  left: 25%;
  width: 50%;
  top: -2px;
  bottom: -2px;
  border: 2px dashed rgba(15, 23, 42, 0.35);
  border-radius: 999px;
  pointer-events: none;
}

.spectrum-pointer {
  position: absolute;
  top: -5px;
  width: 20px;
  height: 20px;
  margin-left: -10px;
  border-radius: 50%;
  background: #0f172a;
  border: 3px solid #ffffff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.35);
  transition: left 0.25s ease;

  .pointer-bubble {
    position: absolute;
    bottom: 22px;
    left: 50%;
    transform: translateX(-50%);
    background: #0f172a;
    color: #ffffff;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 7px;
    border-radius: 6px;
    white-space: nowrap;
  }
}

.spectrum-legend {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #64748b;
  margin-top: 8px;

  .sweet-text {
    font-weight: 600;
    color: #047857;
  }
}

.compass-footer {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 10px;

  .advice-text {
    margin: 0;
    font-size: 12.5px;
    color: #334155;
    line-height: 1.5;
  }

  .sample-meta {
    font-size: 11.5px;
    color: #64748b;
  }
}
</style>
