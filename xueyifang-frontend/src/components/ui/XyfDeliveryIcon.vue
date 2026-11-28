<template>
  <el-tooltip
    :content="tooltipContent"
    placement="top"
    :show-after="120"
    :disabled="Boolean(text)"
  >
    <span class="xyf-delivery" :class="[`size-${size}`, { 'has-text': Boolean(text) }]" :aria-label="tooltipContent">
      <!-- Offline Campus Meetup: Animated Floating Map Pin + Expanding Geofence Ring -->
      <svg
        v-if="tradeType === 1"
        class="xyf-delivery__svg is-offline"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <ellipse class="geo-ring" cx="11" cy="17.6" rx="5.2" ry="2" stroke="#18181b" stroke-width="1.2" />
        <g class="geo-pin">
          <path
            d="M11 16.2s5-4.3 5-8.2a5 5 0 1 0-10 0c0 3.9 5 8.2 5 8.2Z"
            fill="#18181b"
          />
          <circle cx="11" cy="8" r="1.9" fill="#e5c79e" />
        </g>
      </svg>

      <!-- Online Remote Delivery: Animated Monitor/Node + Radiating Wireless Signal Waves -->
      <svg
        v-else
        class="xyf-delivery__svg is-online"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <rect x="3" y="6.5" width="13.2" height="9.2" rx="2" stroke="#27272a" stroke-width="1.5" fill="#fafafa" />
        <path d="M6.8 18.2h5.6M9.6 15.7v2.5" stroke="#27272a" stroke-width="1.5" stroke-linecap="round" />
        <circle class="sig-dot" cx="15.8" cy="6.8" r="1.5" fill="#10b981" />
        <path class="sig-wave-1" d="M15.8 3.8a3.2 3.2 0 0 1 3.1 3.1" stroke="#10b981" stroke-width="1.35" stroke-linecap="round" />
        <path class="sig-wave-2" d="M15.8 1.6a5.4 5.4 0 0 1 5.3 5.3" stroke="#10b981" stroke-width="1.25" stroke-linecap="round" />
      </svg>

      <span v-if="text" class="xyf-delivery__text ellipsis">{{ text }}</span>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  tradeType?: number
  size?: 'xs' | 'sm' | 'md'
  text?: string
  tooltip?: string
}

const props = withDefaults(defineProps<Props>(), {
  tradeType: 1,
  size: 'sm',
  text: '',
  tooltip: ''
})

const tooltipContent = computed(() => {
  if (props.tooltip) return props.tooltip
  return props.tradeType === 1 ? '校内当面履约 · 电子围栏核销' : '线上远程交付 · 平台担保验收'
})
</script>

<style scoped lang="scss">
.xyf-delivery {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  vertical-align: middle;
  cursor: help;
  color: var(--xyf-ink-2);

  &.has-text {
    cursor: inherit;
  }

  &__svg {
    display: block;
    overflow: visible;
    flex-shrink: 0;
  }

  &.size-xs .xyf-delivery__svg {
    width: 16px;
    height: 16px;
  }

  &.size-sm .xyf-delivery__svg {
    width: 18px;
    height: 18px;
  }

  &.size-md .xyf-delivery__svg {
    width: 21px;
    height: 21px;
  }

  &__text {
    min-width: 0;
  }
}

/* Offline Pin Subtle Hover Bob & Geofence Pulse */
.geo-pin {
  transform-origin: 11px 16px;
  animation: xyf-pin-bob 2.8s ease-in-out infinite;
}

.geo-ring {
  transform-origin: 11px 17.6px;
  animation: xyf-geo-pulse 2.8s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}

/* Online Live Signal Waves */
.sig-dot {
  animation: xyf-sig-dot 2.2s ease-in-out infinite;
}

.sig-wave-1 {
  animation: xyf-sig-wave 2.2s ease-out infinite;
}

.sig-wave-2 {
  animation: xyf-sig-wave 2.2s ease-out infinite 0.35s;
}

@keyframes xyf-pin-bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-1.4px);
  }
}

@keyframes xyf-geo-pulse {
  0% {
    transform: scale(0.7);
    opacity: 0.55;
  }
  75% {
    transform: scale(1.35);
    opacity: 0;
  }
  100% {
    transform: scale(1.35);
    opacity: 0;
  }
}

@keyframes xyf-sig-dot {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes xyf-sig-wave {
  0% {
    opacity: 0;
  }
  30% {
    opacity: 0.9;
  }
  85%,
  100% {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .geo-pin,
  .geo-ring,
  .sig-dot,
  .sig-wave-1,
  .sig-wave-2 {
    animation: none;
  }
}
</style>
