<template>
  <div class="xyf-brand" :class="[`is-${theme}`, { 'is-compact': compact }]">
    <span class="xyf-brand__mark" aria-hidden="true">
      <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
        <!-- Timeless Architectural Monochrome Squircle -->
        <rect
          x="4"
          y="4"
          width="56"
          height="56"
          rx="15"
          :fill="theme === 'light' ? '#FFFFFF' : '#111113'"
        />
        <!-- Precision Folded Studio Ribbon Monogram (Abstract Craft Arch + Spark) -->
        <path
          class="mark-stroke-a"
          d="M20 42L32 20L44 42"
          :stroke="theme === 'light' ? '#111113' : '#FFFFFF'"
          stroke-width="3.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <path
          class="mark-stroke-b"
          d="M24.5 34.5H39.5"
          :stroke="theme === 'light' ? '#111113' : '#FFFFFF'"
          stroke-width="3.2"
          stroke-linecap="round"
        />
        <circle
          class="mark-dot"
          cx="32"
          cy="44.5"
          r="2.4"
          :fill="theme === 'light' ? '#111113' : '#FFFFFF'"
        />
      </svg>
    </span>

    <span v-if="!iconOnly" class="xyf-brand__text">
      <span class="xyf-brand__title">{{ title }}</span>
      <span v-if="subtitle && !compact" class="xyf-brand__subtitle">{{ subtitle }}</span>
    </span>
  </div>
</template>

<script setup lang="ts">
interface Props {
  theme?: 'default' | 'light'
  compact?: boolean
  iconOnly?: boolean
  title?: string
  subtitle?: string
}

withDefaults(defineProps<Props>(), {
  theme: 'default',
  compact: false,
  iconOnly: false,
  title: '学艺坊',
  subtitle: ''
})
</script>

<style scoped lang="scss">
.xyf-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  user-select: none;
  flex-shrink: 0;

  &:hover {
    .xyf-brand__mark {
      transform: scale(1.04);
    }

    .mark-stroke-a {
      stroke-dashoffset: 0;
    }

    .mark-dot {
      transform: scale(1.25);
    }
  }

  &__mark {
    width: 34px;
    height: 34px;
    display: inline-flex;
    flex-shrink: 0;
    transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);

    svg {
      width: 100%;
      height: 100%;
      display: block;
    }
  }

  .mark-dot {
    transform-origin: 32px 44.5px;
    transition: transform 0.25s ease;
  }

  &__text {
    display: flex;
    flex-direction: column;
    line-height: 1.15;
  }

  &__title {
    font-size: 17.5px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--xyf-ink);
    white-space: nowrap;
  }

  &__subtitle {
    font-size: 11px;
    font-weight: 500;
    color: var(--xyf-ink-3);
    margin-top: 2px;
    letter-spacing: 0.02em;
  }

  &.is-light {
    .xyf-brand__title {
      color: var(--xyf-ink-invert);
    }

    .xyf-brand__subtitle {
      color: rgba(255, 255, 255, 0.6);
    }
  }

  &.is-compact {
    gap: 8px;

    .xyf-brand__mark {
      width: 30px;
      height: 30px;
    }

    .xyf-brand__title {
      font-size: 16.5px;
    }
  }
}
</style>
