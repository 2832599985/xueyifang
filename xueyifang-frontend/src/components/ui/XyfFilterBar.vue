<template>
  <section class="xyf-filter-bar">
    <div v-if="$slots.tabs" class="xyf-filter-bar__tabs">
      <slot name="tabs" />
    </div>

    <div v-if="$slots.fields || $slots.default" class="xyf-filter-bar__fields">
      <slot name="fields" />
      <slot />
    </div>

    <div v-if="$slots.actions" class="xyf-filter-bar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
// Unified single-row filter container across admin queues & data lists
</script>

<style scoped lang="scss">
.xyf-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 52px;
  padding: 10px 16px;
  margin-bottom: 16px;
  border-radius: var(--xyf-radius-md);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  box-shadow: var(--xyf-shadow-xs);

  &__tabs {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
    max-width: 100%;
  }

  &__fields {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    flex: 1;
    min-width: 0;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    margin-left: auto;
  }

  :deep(.xyf-filter-search) {
    width: 240px;
  }

  :deep(.xyf-filter-select) {
    width: 148px;
  }
}

@media (max-width: 768px) {
  .xyf-filter-bar {
    align-items: stretch;

    &__fields,
    &__actions {
      width: 100%;
      margin-left: 0;
    }

    :deep(.xyf-filter-search),
    :deep(.xyf-filter-select) {
      width: 100%;
    }
  }
}
</style>
