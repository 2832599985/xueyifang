<template>
  <header class="xyf-page-header" :class="{ 'has-border': bordered, 'is-dense': dense }">
    <div class="xyf-page-header__left">
      <el-button
        v-if="showBack"
        text
        class="xyf-page-header__back"
        @click="handleBack"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span>{{ backText }}</span>
      </el-button>

      <div class="xyf-page-header__title-row">
        <h1 class="xyf-page-header__title">{{ title }}</h1>
        <div v-if="$slots.meta" class="xyf-page-header__meta">
          <slot name="meta" />
        </div>
      </div>
      <p v-if="subtitle" class="xyf-page-header__subtitle">{{ subtitle }}</p>
    </div>

    <div v-if="$slots.actions" class="xyf-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

interface Props {
  title: string
  subtitle?: string
  showBack?: boolean
  backText?: string
  backTo?: string
  bordered?: boolean
  dense?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  subtitle: '',
  showBack: false,
  backText: '返回',
  backTo: '',
  bordered: true,
  dense: false
})

const router = useRouter()

function handleBack() {
  if (props.backTo) {
    router.push(props.backTo)
  } else {
    router.back()
  }
}
</script>

<style scoped lang="scss">
.xyf-page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 18px;

  &.has-border {
    border-bottom: 1px solid var(--xyf-border);
  }

  &.is-dense {
    margin-bottom: 18px;
    padding-bottom: 14px;

    .xyf-page-header__title {
      font-size: 22px;
    }
  }

  &__left {
    min-width: 0;
  }

  &__back {
    padding: 2px 6px 2px 0;
    height: auto;
    color: var(--xyf-ink-2);
    font-size: 13px;
    margin-bottom: 4px;

    &:hover {
      color: var(--xyf-primary);
      background: transparent;
    }
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__title {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    color: var(--xyf-ink);
    line-height: 1.2;
    letter-spacing: -0.02em;
  }

  &__meta {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  &__subtitle {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
    line-height: 1.5;
    max-width: 680px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .xyf-page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;

    &__title {
      font-size: 20px;
    }

    &__actions {
      width: 100%;
    }
  }
}
</style>
