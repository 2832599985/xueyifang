<template>
  <!-- Pure Animated SVG Emblem Mode (zero bloated text badges!) -->
  <el-tooltip
    v-if="iconOnly"
    :content="tooltip || label"
    placement="top"
    :show-after="120"
  >
    <span
      class="xyf-emblem"
      :class="[`tone-${tone}`, `size-${size}`]"
      :aria-label="tooltip || label"
    >
      <!-- Live / Recruiting Animated Radar Beacon -->
      <svg
        v-if="icon === 'live' || icon === 'users' || icon === 'clock'"
        class="xyf-emblem__svg"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <circle class="emblem-radar" cx="11" cy="11" r="8.5" stroke="currentColor" stroke-width="1.3" />
        <circle cx="11" cy="11" r="8.5" fill="currentColor" fill-opacity="0.1" />
        <circle class="emblem-core" cx="11" cy="11" r="3.6" fill="currentColor" />
      </svg>

      <!-- Formed / Matched / Verified Check Seal -->
      <svg
        v-else-if="icon === 'check'"
        class="xyf-emblem__svg"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <circle cx="11" cy="11" r="9" fill="currentColor" fill-opacity="0.12" stroke="currentColor" stroke-width="1.3" />
        <path
          class="emblem-check"
          d="M7.2 11.4l2.5 2.5 5.1-5.4"
          stroke="currentColor"
          stroke-width="1.9"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>

      <!-- Full / Locked Seal -->
      <svg
        v-else-if="icon === 'lock'"
        class="xyf-emblem__svg"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <circle cx="11" cy="11" r="9" fill="currentColor" fill-opacity="0.12" stroke="currentColor" stroke-width="1.2" />
        <rect x="7" y="10.2" width="8" height="5.6" rx="1.5" stroke="currentColor" stroke-width="1.5" />
        <path d="M8.6 10.2V8.4a2.4 2.4 0 0 1 4.8 0v1.8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      </svg>

      <!-- Animated Twinkling Tier Spark -->
      <svg
        v-else-if="icon === 'spark'"
        class="xyf-emblem__svg"
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
      >
        <path
          class="emblem-spark-main"
          d="M11 3.2L12.9 9.1L18.8 11L12.9 12.9L11 18.8L9.1 12.9L3.2 11L9.1 9.1L11 3.2Z"
          fill="currentColor"
        />
        <path
          class="emblem-spark-sub"
          d="M17.2 3.8L17.8 5.6L19.6 6.2L17.8 6.8L17.2 8.6L16.6 6.8L14.8 6.2L16.6 5.6L17.2 3.8Z"
          fill="currentColor"
        />
      </svg>

      <!-- Fallback Dot Emblem -->
      <svg v-else class="xyf-emblem__svg" viewBox="0 0 22 22" fill="none" aria-hidden="true">
        <circle cx="11" cy="11" r="8.5" fill="currentColor" fill-opacity="0.12" />
        <circle cx="11" cy="11" r="3.5" fill="currentColor" />
      </svg>
    </span>
  </el-tooltip>

  <!-- Standard Compact Badge Mode (for admin tables / explicit labels) -->
  <span v-else class="xyf-badge" :class="[`tone-${tone}`, `size-${size}`]">
    <span v-if="dot && !icon" class="xyf-badge__dot" aria-hidden="true" />
    <svg
      v-if="icon === 'check'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M3.5 8.5L6.5 11.5L12.5 4.8"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'clock'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="8" cy="8" r="5.8" stroke="currentColor" stroke-width="1.5" />
      <path d="M8 4.8V8.2L10.4 9.6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>
    <svg
      v-else-if="icon === 'live'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <circle class="emblem-radar-sm" cx="8" cy="8" r="5.5" stroke="currentColor" stroke-width="1.4" />
      <circle cx="8" cy="8" r="2.6" fill="currentColor" />
    </svg>
    <svg
      v-else-if="icon === 'lock'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <rect x="3.5" y="7" width="9" height="6.5" rx="1.6" stroke="currentColor" stroke-width="1.5" />
      <path d="M5.5 7V5.2a2.5 2.5 0 0 1 5 0V7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>
    <svg
      v-else-if="icon === 'pin'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M8 14s4.5-3.9 4.5-7.5a4.5 4.5 0 1 0-9 0C3.5 10.1 8 14 8 14Z"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linejoin="round"
      />
      <circle cx="8" cy="6.5" r="1.4" fill="currentColor" />
    </svg>
    <svg
      v-else-if="icon === 'online'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <rect x="2.2" y="3" width="11.6" height="8" rx="1.5" stroke="currentColor" stroke-width="1.5" />
      <path d="M5.5 13.2h5M8 11v2.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>
    <svg
      v-else-if="icon === 'spark'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <path
        class="emblem-spark-main"
        d="M8 2.2L9.4 6.6L13.8 8L9.4 9.4L8 13.8L6.6 9.4L2.2 8L6.6 6.6L8 2.2Z"
        fill="currentColor"
      />
    </svg>
    <svg
      v-else-if="icon === 'users'"
      class="xyf-badge__icon"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="6.2" cy="5.5" r="2.2" stroke="currentColor" stroke-width="1.4" />
      <path d="M2.2 12.8c.5-2 2-3 4-3s3.5 1 4 3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
      <circle cx="11.2" cy="6" r="1.6" stroke="currentColor" stroke-width="1.3" />
      <path d="M11.6 10c1.2.3 2 1.1 2.3 2.4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
    </svg>
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
export type StatusBadgeTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'dark'
export type StatusBadgeIcon = 'live' | 'check' | 'clock' | 'lock' | 'pin' | 'online' | 'spark' | 'users'

interface Props {
  label?: string
  tooltip?: string
  tone?: StatusBadgeTone
  size?: 'xs' | 'sm' | 'md'
  dot?: boolean
  icon?: StatusBadgeIcon
  iconOnly?: boolean
}

withDefaults(defineProps<Props>(), {
  label: '',
  tooltip: '',
  tone: 'neutral',
  size: 'sm',
  dot: false,
  icon: undefined,
  iconOnly: false
})
</script>

<style scoped lang="scss">
/* Pure Animated SVG Emblem Mode */
.xyf-emblem {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: help;
  vertical-align: middle;

  &__svg {
    display: block;
    overflow: visible;
  }

  &.size-xs .xyf-emblem__svg {
    width: 18px;
    height: 18px;
  }

  &.size-sm .xyf-emblem__svg {
    width: 21px;
    height: 21px;
  }

  &.size-md .xyf-emblem__svg {
    width: 24px;
    height: 24px;
  }

  &.tone-dark,
  &.tone-info {
    color: #18181b;
  }

  &.tone-success {
    color: #10b981;
  }

  &.tone-warning {
    color: #d97706;
  }

  &.tone-danger {
    color: #dc2626;
  }

  &.tone-neutral {
    color: #71717a;
  }
}

.emblem-radar {
  transform-origin: 11px 11px;
  animation: xyf-beacon-ring 2.4s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}

.emblem-radar-sm {
  transform-origin: 8px 8px;
  animation: xyf-beacon-ring 2.4s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}

.emblem-core {
  transform-origin: 11px 11px;
  animation: xyf-beacon-core 2.4s ease-in-out infinite;
}

.emblem-check {
  stroke-dasharray: 20;
  stroke-dashoffset: 0;
  animation: xyf-check-breathe 3.6s ease-in-out infinite;
}

.emblem-spark-main {
  transform-origin: center;
  animation: xyf-spark-twinkle 2.6s ease-in-out infinite;
}

.emblem-spark-sub {
  transform-origin: 17.2px 6.2px;
  animation: xyf-spark-twinkle 2.6s ease-in-out infinite 0.55s;
}

@keyframes xyf-beacon-ring {
  0% {
    transform: scale(0.75);
    opacity: 0.72;
  }
  75% {
    transform: scale(1.42);
    opacity: 0;
  }
  100% {
    transform: scale(1.42);
    opacity: 0;
  }
}

@keyframes xyf-beacon-core {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.16);
  }
}

@keyframes xyf-check-breathe {
  0%,
  80%,
  100% {
    opacity: 1;
  }
  90% {
    opacity: 0.65;
  }
}

@keyframes xyf-spark-twinkle {
  0%,
  100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
  50% {
    transform: scale(0.84) rotate(12deg);
    opacity: 0.68;
  }
}

/* Standard Compact Badge Mode */
.xyf-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  border-radius: 5px;
  line-height: 1.2;
  letter-spacing: -0.005em;
  white-space: nowrap;
  flex-shrink: 0;
  border: 1px solid #ebebef;
  background: #fafafa;
  color: #71717a;

  &__dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    flex-shrink: 0;
    background: currentColor;
  }

  &__icon {
    width: 12px;
    height: 12px;
    flex-shrink: 0;
    overflow: visible;
  }

  &.size-xs {
    font-size: 11px;
    padding: 2px 6px;

    .xyf-badge__icon {
      width: 11px;
      height: 11px;
    }
  }

  &.size-sm {
    font-size: 12px;
    padding: 3px 8px;
  }

  &.size-md {
    font-size: 12.5px;
    padding: 4px 10px;
  }

  &.tone-neutral {
    background: #f7f7f8;
    border-color: #e8e8ec;
    color: #52525b;
  }

  &.tone-info {
    background: #f4f4f5;
    border-color: #e4e4e7;
    color: #27272a;
  }

  &.tone-dark {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }

  &.tone-success {
    background: var(--xyf-success-soft);
    border-color: var(--xyf-success-border);
    color: var(--xyf-success);
  }

  &.tone-warning {
    background: var(--xyf-warning-soft);
    border-color: var(--xyf-warning-border);
    color: #b45309;
  }

  &.tone-danger {
    background: var(--xyf-danger-soft);
    border-color: #f5caca;
    color: #dc2626;
  }
}

@media (prefers-reduced-motion: reduce) {
  .emblem-radar,
  .emblem-radar-sm,
  .emblem-core,
  .emblem-check,
  .emblem-spark-main,
  .emblem-spark-sub {
    animation: none;
  }
}
</style>
