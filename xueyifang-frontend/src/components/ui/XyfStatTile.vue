<template>
  <div
    class="xyf-stat-tile"
    :class="[`tone-${tone}`, { 'is-clickable': clickable }]"
    @click="clickable && emit('click')"
  >
    <div class="xyf-stat-tile__head">
      <span class="xyf-stat-tile__label">{{ label }}</span>
      <span v-if="$slots.icon" class="xyf-stat-tile__icon">
        <slot name="icon" />
      </span>
    </div>

    <div class="xyf-stat-tile__value xyf-tabular">{{ value }}</div>

    <div v-if="hint || badge" class="xyf-stat-tile__foot">
      <span v-if="hint" class="xyf-stat-tile__hint">{{ hint }}</span>
      <span v-if="badge" class="xyf-stat-tile__badge">{{ badge }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  label: string
  value: string | number
  hint?: string
  badge?: string
  tone?: 'teal' | 'amber' | 'sage' | 'info' | 'danger' | 'neutral'
  clickable?: boolean
}

withDefaults(defineProps<Props>(), {
  hint: '',
  badge: '',
  tone: 'teal',
  clickable: false
})

const emit = defineEmits<{ (e: 'click'): void }>()
</script>

<style scoped lang="scss">
.xyf-stat-tile {
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: transform var(--xyf-transition), box-shadow var(--xyf-transition),
    border-color var(--xyf-transition);

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-2px);
      border-color: var(--xyf-primary-border);
      box-shadow: var(--xyf-shadow-sm);
    }
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  &__label {
    font-size: 13px;
    font-weight: 500;
    color: var(--xyf-ink-2);
  }

  &__icon {
    width: 34px;
    height: 34px;
    border-radius: var(--xyf-radius-sm);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 17px;
  }

  &__value {
    font-size: 26px;
    font-weight: 700;
    color: var(--xyf-ink);
    line-height: 1.15;
    letter-spacing: -0.01em;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 20px;
  }

  &__hint {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  &__badge {
    font-size: 11.5px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: var(--xyf-radius-pill);
  }

  &.tone-teal .xyf-stat-tile__icon,
  &.tone-amber .xyf-stat-tile__icon,
  &.tone-sage .xyf-stat-tile__icon,
  &.tone-info .xyf-stat-tile__icon,
  &.tone-neutral .xyf-stat-tile__icon {
    background: #f4f4f5;
    border: 1px solid #e4e4e7;
    color: #18181b;
  }

  &.tone-danger {
    .xyf-stat-tile__icon {
      background: #faf5f5;
      border: 1px solid #eed6d6;
      color: #b91c1c;
    }

    .xyf-stat-tile__badge {
      background: #faf5f5;
      color: #b91c1c;
    }
  }
}
</style>
