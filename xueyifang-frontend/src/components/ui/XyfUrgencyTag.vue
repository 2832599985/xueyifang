<template>
  <el-tooltip
    v-if="visible"
    :content="tooltipText"
    placement="top"
    :show-after="120"
    :disabled="showLabel"
  >
    <span
      class="xyf-urgency"
      :class="[`is-l${level}`, `size-${size}`, { 'has-label': showLabel }]"
      :aria-label="tooltipText"
    >
      <!-- Level 3: Animated High-Voltage Crimson Pulse + Lightning Bolt -->
      <svg
        v-if="level === 3"
        class="xyf-urgency__svg xyf-urgency__svg--bolt"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle class="u-pulse-ring" cx="12" cy="12" r="9.5" stroke="#ef4444" stroke-width="1.3" />
        <circle class="u-core-bg" cx="12" cy="12" r="9.5" fill="#fef2f2" stroke="#fecaca" stroke-width="1" />
        <path
          class="u-bolt-path"
          d="M13.3 5.2L7.5 12.6h4.2l-.9 6.2 5.7-7.4h-4.1l.9-6.2Z"
          fill="#dc2626"
        />
      </svg>

      <!-- Level 2: Animated Flickering Amber-Gold Dual-Layer Flame -->
      <svg
        v-else-if="level === 2"
        class="xyf-urgency__svg xyf-urgency__svg--flame"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9.5" fill="#fffbeb" stroke="#fde68a" stroke-width="1" />
        <path
          class="u-flame-outer"
          d="M12.9 5.6c.4 2.5-1.5 3.8-2.2 5.3-.5 1.1.1 2 .8 2.4.5-.7 1-1.3 1.6-1.7 1.6 1.2 2.7 2.8 2.7 4.6a4.5 4.5 0 0 1-9 0c0-3.1 2-5.4 3.7-7.3.9-1.1 1.8-2.1 2.4-3.3Z"
          fill="#d97706"
        />
        <path
          class="u-flame-inner"
          d="M12.2 11.4c.7.9 1.5 2 1.5 3.3a2.2 2.2 0 0 1-4.4 0c0-1.4 1-2.5 1.8-3.4.3.7.7 1 1.1.1Z"
          fill="#fbbf24"
        />
      </svg>

      <!-- Level 1: Calm Breathing Neutral Dot (only shown when showNormal=true) -->
      <svg
        v-else
        class="xyf-urgency__svg"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9.5" fill="#f4f4f5" stroke="#e4e4e7" stroke-width="1" />
        <circle cx="12" cy="12" r="3.4" fill="#71717a" />
      </svg>

      <span v-if="showLabel" class="xyf-urgency__label">{{ labelText }}</span>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  level?: number
  size?: 'xs' | 'sm' | 'md'
  showNormal?: boolean
  showLabel?: boolean
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  level: 1,
  size: 'sm',
  showNormal: false,
  showLabel: false,
  compact: false
})

const visible = computed(() => props.level >= 2 || props.showNormal)

const labelText = computed(() => {
  if (props.level === 3) return props.compact ? '急单' : '十万火急'
  if (props.level === 2) return '加急'
  return '常规'
})

const tooltipText = computed(() => {
  if (props.level === 3) return '十万火急 · 24h 内急需响应'
  if (props.level === 2) return '加急需求 · 优先推荐校内行家'
  return '常规需求'
})
</script>

<style scoped lang="scss">
.xyf-urgency {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  vertical-align: middle;
  flex-shrink: 0;
  cursor: help;
  user-select: none;

  &.has-label {
    cursor: inherit;
  }

  &__svg {
    display: block;
    overflow: visible;
    flex-shrink: 0;
  }

  &.size-xs .xyf-urgency__svg {
    width: 19px;
    height: 19px;
  }

  &.size-sm .xyf-urgency__svg {
    width: 22px;
    height: 22px;
  }

  &.size-md .xyf-urgency__svg {
    width: 26px;
    height: 26px;
  }

  &__label {
    font-size: 12.5px;
    font-weight: 600;
    line-height: 1.2;
    white-space: nowrap;
  }

  &.is-l3 .xyf-urgency__label {
    color: #dc2626;
  }

  &.is-l2 .xyf-urgency__label {
    color: #b45309;
  }

  &.is-l1 .xyf-urgency__label {
    color: #52525b;
  }
}

/* Level 3: Crimson Radar Ring + Crisp Bolt Zap */
.u-pulse-ring {
  transform-origin: 12px 12px;
  animation: xyf-urgent-radar 2s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}

.u-bolt-path {
  transform-origin: 12px 12px;
  animation: xyf-bolt-zap 2.2s ease-in-out infinite;
}

/* Level 2: Organic Dual-Layer Flame Flicker */
.u-flame-outer {
  transform-origin: 12px 17px;
  animation: xyf-flame-sway 1.9s ease-in-out infinite;
}

.u-flame-inner {
  transform-origin: 12px 16px;
  animation: xyf-flame-core 1.4s ease-in-out infinite;
}

@keyframes xyf-urgent-radar {
  0% {
    transform: scale(0.95);
    opacity: 0.72;
  }
  70% {
    transform: scale(1.48);
    opacity: 0;
  }
  100% {
    transform: scale(1.48);
    opacity: 0;
  }
}

@keyframes xyf-bolt-zap {
  0%,
  72%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  78% {
    transform: scale(1.12);
    opacity: 0.78;
  }
  84% {
    transform: scale(0.96);
    opacity: 1;
  }
  90% {
    transform: scale(1.08);
    opacity: 0.9;
  }
}

@keyframes xyf-flame-sway {
  0%,
  100% {
    transform: scaleY(1) skewX(0deg);
  }
  35% {
    transform: scaleY(1.06) skewX(-3deg);
  }
  70% {
    transform: scaleY(0.96) skewX(3deg);
  }
}

@keyframes xyf-flame-core {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.95;
  }
  50% {
    transform: scale(1.12, 1.18);
    opacity: 0.75;
  }
}

@media (prefers-reduced-motion: reduce) {
  .u-pulse-ring,
  .u-bolt-path,
  .u-flame-outer,
  .u-flame-inner {
    animation: none;
  }
}
</style>
