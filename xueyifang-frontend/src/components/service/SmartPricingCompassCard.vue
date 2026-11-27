<template>
  <div
    v-if="benchmark"
    class="pricing-compass"
    :class="{ 'is-studio': interactive, 'is-compact': !interactive }"
  >
    <div class="compass-head">
      <div class="compass-head__left">
        <span class="compass-eyebrow">MARKET PRICING BENCHMARK</span>
        <h4 class="compass-title">
          {{ benchmark.tagName || '同类校园技能' }} · 成交分位参考
        </h4>
      </div>
      <span class="pos-pill xyf-tabular" :class="positionClass">
        {{ benchmark.pricePositionLabel }} · P{{ benchmark.percentileRank }}
      </span>
    </div>

    <!-- Interactive Studio Mode (ServicePublish): 4 Clickable Strategy Cards -->
    <div v-if="interactive" class="strategy-grid xyf-tabular">
      <button
        type="button"
        class="strategy-card"
        :class="{ 'is-selected': isNearPrice(benchmark.p25Price) }"
        @click="emit('select-price', Number(benchmark.p25Price))"
      >
        <span class="s-tag">P25 引流破冰</span>
        <strong class="s-price">¥{{ formatMoney(benchmark.p25Price) }}</strong>
        <span class="s-cta">点击填入</span>
      </button>

      <button
        type="button"
        class="strategy-card is-recommended"
        :class="{ 'is-selected': isNearPrice(benchmark.p50Price) }"
        @click="emit('select-price', Number(benchmark.p50Price))"
      >
        <span class="s-tag">P50 主流中位 ✦</span>
        <strong class="s-price">¥{{ formatMoney(benchmark.p50Price) }}</strong>
        <span class="s-cta">黄金成交价</span>
      </button>

      <button
        type="button"
        class="strategy-card"
        :class="{ 'is-selected': isNearPrice(benchmark.p75Price) }"
        @click="emit('select-price', Number(benchmark.p75Price))"
      >
        <span class="s-tag">P75 资深溢价</span>
        <strong class="s-price">¥{{ formatMoney(benchmark.p75Price) }}</strong>
        <span class="s-cta">点击填入</span>
      </button>

      <button
        type="button"
        class="strategy-card"
        :class="{ 'is-selected': isNearPrice(benchmark.p90Price) }"
        @click="emit('select-price', Number(benchmark.p90Price))"
      >
        <span class="s-tag">P90 深度定制</span>
        <strong class="s-price">¥{{ formatMoney(benchmark.p90Price) }}</strong>
        <span class="s-cta">均价 ¥{{ formatMoney(benchmark.avgPrice) }}</span>
      </button>
    </div>

    <!-- Spectrum Distribution Gauge (Works cleanly in both narrow sidebar & wide studio) -->
    <div class="gauge-box xyf-tabular">
      <div class="gauge-track">
        <div class="gauge-sweet-band" title="P25–P75 校内主流成交区间" />
        <div
          class="gauge-pin"
          :style="{ left: `${Math.min(94, Math.max(6, benchmark.percentileRank || 50))}%` }"
        >
          <span class="gauge-pin__bubble">¥{{ formatMoney(proposedPrice || benchmark.p50Price) }}</span>
        </div>
      </div>

      <!-- Compact 3-Tick Quartile Strip for Narrow Sidebars -->
      <div class="gauge-ticks">
        <span>P25 ¥{{ formatShortMoney(benchmark.p25Price) }}</span>
        <span class="is-mid">中位 ¥{{ formatShortMoney(benchmark.p50Price) }}</span>
        <span>P75 ¥{{ formatShortMoney(benchmark.p75Price) }}</span>
      </div>
    </div>

    <div class="compass-foot">
      <p v-if="interactive && benchmark.conversionAdvice" class="advice-line">
        {{ benchmark.conversionAdvice }}
      </p>
      <span class="sample-line xyf-tabular">
        样本基数：{{ benchmark.completedOrderSampleCount }} 笔真实完单 · {{ benchmark.activeListingSampleCount }} 项校内在售
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

const formatShortMoney = (val?: number) => {
  if (val === undefined || val === null) return '0'
  const n = Number(val)
  return Number.isInteger(n) ? String(n) : n.toFixed(0)
}

const isNearPrice = (target?: number) => {
  if (target === undefined || props.proposedPrice === undefined) return false
  return Math.abs(Number(target) - Number(props.proposedPrice)) < 0.01
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
    // silent fallback
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
.pricing-compass {
  margin: 14px 0 18px;
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &.is-compact {
    padding: 14px 16px;
  }
}

.compass-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  .compass-eyebrow {
    display: block;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: var(--xyf-ink-3);
  }

  .compass-title {
    margin: 2px 0 0;
    font-size: 13.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.pos-pill {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 650;
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  color: var(--xyf-ink-2);

  &.is-sweet {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }

  &.is-value {
    background: #f2fbf5;
    border-color: #ccebd7;
    color: #15803d;
  }

  &.is-premium {
    background: var(--xyf-champagne-soft);
    border-color: var(--xyf-champagne-border);
    color: #9a5b13;
  }

  &.is-outlier {
    background: #fef2f2;
    border-color: #f7cece;
    color: #b91c1c;
  }
}

/* Interactive Studio 4-Card Grid */
.strategy-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;

  @media (max-width: 680px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.strategy-card {
  padding: 11px 12px;
  border-radius: 11px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 3px;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #18181b;
    transform: translateY(-1px);
  }

  &.is-recommended {
    background: #fffdf9;
    border-color: var(--xyf-champagne-border);
  }

  &.is-selected {
    border-color: #18181b;
    box-shadow: 0 0 0 1px #18181b inset;
  }

  .s-tag {
    font-size: 11px;
    color: var(--xyf-ink-3);
    font-weight: 600;
  }

  .s-price {
    font-size: 16px;
    font-weight: 800;
    color: var(--xyf-ink);
  }

  .s-cta {
    font-size: 11px;
    color: var(--xyf-ink-2);
  }
}

/* Distribution Spectrum Bar */
.gauge-box {
  padding-top: 18px;
}

.gauge-track {
  position: relative;
  height: 7px;
  border-radius: 999px;
  background: #e4e4e7;

  .gauge-sweet-band {
    position: absolute;
    left: 25%;
    width: 50%;
    top: 0;
    bottom: 0;
    border-radius: 999px;
    background: linear-gradient(90deg, #52525b 0%, #18181b 50%, #b89058 100%);
    opacity: 0.42;
  }

  .gauge-pin {
    position: absolute;
    top: 50%;
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: #18181b;
    border: 2.5px solid #ffffff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
    transform: translate(-50%, -50%);
    transition: left 0.22s ease;

    &__bubble {
      position: absolute;
      bottom: 16px;
      left: 50%;
      transform: translateX(-50%);
      padding: 1px 7px;
      border-radius: 6px;
      background: #18181b;
      color: #ffffff;
      font-size: 10.5px;
      font-weight: 700;
      white-space: nowrap;
    }
  }
}

.gauge-ticks {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--xyf-ink-3);

  .is-mid {
    font-weight: 650;
    color: var(--xyf-ink);
  }
}

.compass-foot {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 3px;

  .advice-line {
    margin: 0;
    font-size: 12px;
    color: var(--xyf-ink-2);
    line-height: 1.5;
  }

  .sample-line {
    font-size: 11px;
    color: var(--xyf-ink-3);
  }
}
</style>
