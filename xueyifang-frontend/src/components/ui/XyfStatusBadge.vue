<template>
  <span class="xyf-badge" :class="[`tone-${tone}`, `size-${size}`]">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
interface Props {
  label?: string
  tone?: 'teal' | 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'accent'
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
  font-weight: 550;
  border-radius: 6px;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
  color: #18181b;
  line-height: 1.25;
  letter-spacing: -0.005em;
  white-space: nowrap;

  &.size-sm {
    font-size: 12px;
    padding: 3px 9px;
  }

  &.size-md {
    font-size: 12.5px;
    padding: 4px 11px;
  }

  &.tone-teal,
  &.tone-accent {
    background: #f4f4f5;
    border-color: #e4e4e7;
    color: #18181b;
  }

  &.tone-info {
    background: #ffffff;
    border-color: #d4d4d8;
    color: #18181b;
  }

  &.tone-success {
    background: #fafafa;
    border-color: #e4e4e7;
    color: #3f3f46;
  }

  &.tone-warning {
    background: #f4f4f5;
    border-color: #d4d4d8;
    color: #18181b;
  }

  &.tone-danger {
    background: #faf5f5;
    border-color: #ead5d5;
    color: #8c2626;
  }

  &.tone-neutral {
    background: #fafafa;
    border-color: #ebebef;
    color: #71717a;
  }
}
</style>
