<template>
  <div
    class="xyf-stat-tile"
    :class="[
      `tone-${tone}`,
      `emphasis-${emphasis}`,
      { 'is-clickable': clickable }
    ]"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="clickable && emit('click')"
    @keydown.enter.prevent="clickable && emit('click')"
    @keydown.space.prevent="clickable && emit('click')"
  >
    <div class="xyf-stat-tile__head">
      <span class="xyf-stat-tile__label-wrap">
        <span v-if="$slots.icon" class="xyf-stat-tile__icon" aria-hidden="true">
          <slot name="icon" />
        </span>
        <span class="xyf-stat-tile__label">{{ label }}</span>
      </span>
      <span v-if="badge" class="xyf-stat-tile__badge">{{ badge }}</span>
    </div>

    <div class="xyf-stat-tile__value xyf-tabular">
      <span v-if="prefix" class="xyf-stat-tile__prefix">{{ prefix }}</span>
      <span>{{ value }}</span>
      <span v-if="unit" class="xyf-stat-tile__unit">{{ unit }}</span>
    </div>

    <div v-if="hint || delta" class="xyf-stat-tile__foot">
      <span v-if="hint" class="xyf-stat-tile__hint">{{ hint }}</span>
      <span
        v-if="delta"
        class="xyf-stat-tile__delta xyf-tabular"
        :class="`is-${delta.direction}`"
      >
        {{ delta.value }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
export interface StatTileDelta {
  value: string
  direction: 'up' | 'down' | 'flat'
}

interface Props {
  label: string
  value: string | number
  prefix?: string
  unit?: string
  hint?: string
  badge?: string
  delta?: StatTileDelta
  tone?: 'default' | 'danger' | 'gold' | 'info' | 'neutral'
  emphasis?: 'primary' | 'default'
  clickable?: boolean
}

withDefaults(defineProps<Props>(), {
  prefix: '',
  unit: '',
  hint: '',
  badge: '',
  delta: undefined,
  tone: 'default',
  emphasis: 'default',
  clickable: false
})

const emit = defineEmits<{ (e: 'click'): void }>()
</script>

<style scoped lang="scss">
.xyf-stat-tile {
  position: relative;
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  padding: 16px 20px;
  min-height: 104px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  box-shadow: var(--xyf-shadow-xs);
  transition: transform var(--xyf-transition), box-shadow var(--xyf-transition),
    border-color var(--xyf-transition);

  &.emphasis-primary {
    padding: 20px 22px;
    min-height: 120px;

    .xyf-stat-tile__value {
      font-size: 32px;
      font-weight: 700;
    }
  }

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--xyf-shadow-sm);
    }

    &:focus-visible {
      outline: none;
      box-shadow: var(--xyf-shadow-focus);
    }
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  &__label-wrap {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: var(--xyf-ink-4);
    flex-shrink: 0;
  }

  &__label {
    font-size: 13px;
    font-weight: 500;
    color: var(--xyf-ink-2);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__value {
    display: flex;
    align-items: baseline;
    gap: 3px;
    font-size: 24px;
    font-weight: 600;
    color: var(--xyf-ink);
    line-height: 1.15;
    letter-spacing: -0.02em;
  }

  &__prefix {
    font-size: 0.72em;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }

  &__unit {
    font-size: 13px;
    font-weight: 500;
    color: var(--xyf-ink-3);
    margin-left: 2px;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 18px;
  }

  &__hint {
    font-size: 12px;
    color: var(--xyf-ink-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__delta {
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;

    &.is-up {
      color: var(--xyf-success);
    }

    &.is-down {
      color: #b91c1c;
    }

    &.is-flat {
      color: var(--xyf-ink-3);
    }
  }

  &__badge {
    font-size: 12px;
    font-weight: 500;
    padding: 1px 7px;
    border-radius: var(--xyf-radius-xs);
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border);
    color: var(--xyf-ink-2);
    flex-shrink: 0;
  }

  &.tone-danger {
    border-left: 2px solid var(--xyf-danger);

    .xyf-stat-tile__value {
      color: #b91c1c;
    }

    .xyf-stat-tile__badge {
      background: var(--xyf-danger-soft);
      border-color: #f5caca;
      color: #b91c1c;
    }
  }

  &.tone-gold {
    .xyf-stat-tile__value {
      color: var(--xyf-champagne);
    }
  }
}
</style>
