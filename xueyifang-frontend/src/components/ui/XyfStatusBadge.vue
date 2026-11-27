<template>
  <span class="xyf-badge" :class="[`tone-${tone}`, `size-${size}`]">
    <span v-if="dot" class="xyf-badge__dot" aria-hidden="true" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
export type StatusBadgeTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

interface Props {
  label?: string
  tone?: StatusBadgeTone
  size?: 'sm' | 'md'
  dot?: boolean
}

withDefaults(defineProps<Props>(), {
  label: '',
  tone: 'neutral',
  size: 'sm',
  dot: false
})
</script>

<style scoped lang="scss">
.xyf-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 500;
  border-radius: var(--xyf-radius-xs);
  line-height: 1.25;
  letter-spacing: -0.005em;
  white-space: nowrap;
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

  &.size-sm {
    font-size: 12px;
    padding: 2px 8px;
  }

  &.size-md {
    font-size: 12px;
    padding: 4px 10px;
  }

  &.tone-neutral {
    background: #fafafa;
    border-color: #ebebef;
    color: #71717a;

    .xyf-badge__dot {
      background: #a1a1aa;
    }
  }

  &.tone-info {
    background: #f4f4f5;
    border-color: #e4e4e7;
    color: #3f3f46;

    .xyf-badge__dot {
      background: #52525b;
    }
  }

  &.tone-success {
    background: var(--xyf-success-soft);
    border-color: var(--xyf-success-border);
    color: var(--xyf-success);

    .xyf-badge__dot {
      background: var(--xyf-success);
    }
  }

  &.tone-warning {
    background: var(--xyf-champagne-soft);
    border-color: var(--xyf-champagne-border);
    color: #9a5b13;

    .xyf-badge__dot {
      background: var(--xyf-champagne);
    }
  }

  &.tone-danger {
    background: var(--xyf-danger-soft);
    border-color: #f5caca;
    color: #b91c1c;

    .xyf-badge__dot {
      background: var(--xyf-danger);
    }
  }
}
</style>
