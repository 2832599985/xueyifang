<template>
  <div class="xyf-empty">
    <div class="xyf-empty__art" aria-hidden="true">
      <svg viewBox="0 0 120 96" fill="none" xmlns="http://www.w3.org/2000/svg">
        <ellipse cx="60" cy="80" rx="32" ry="5.5" fill="#E4E4E7" opacity="0.7" class="empty-shadow" />
        <circle
          cx="60"
          cy="46"
          r="33"
          stroke="#D4D4D8"
          stroke-width="1.2"
          stroke-dasharray="4 6"
          class="empty-orbit"
        />
        <g class="empty-float">
          <rect
            x="28"
            y="22"
            width="64"
            height="46"
            rx="12"
            fill="#FFFFFF"
            stroke="#D4D4D8"
            stroke-width="1.5"
          />
          <rect x="38" y="34" width="26" height="5" rx="2.5" fill="#E4E4E7" />
          <rect x="38" y="45" width="44" height="4" rx="2" fill="#F4F4F5" />
          <rect x="38" y="54" width="32" height="4" rx="2" fill="#F4F4F5" />
          <circle cx="84" cy="26" r="10" fill="#18181B" />
          <path
            d="M84 21.8L85.1 24.9L88.2 26L85.1 27.1L84 30.2L82.9 27.1L79.8 26L82.9 24.9L84 21.8Z"
            fill="#FFFFFF"
            class="empty-spark"
          />
        </g>
        <circle cx="20" cy="36" r="2.2" fill="#A1A1AA" class="empty-dot-1" />
        <circle cx="101" cy="54" r="2" fill="#71717A" class="empty-dot-2" />
      </svg>
    </div>
    <h3 class="xyf-empty__title">{{ title }}</h3>
    <p v-if="description" class="xyf-empty__desc">{{ description }}</p>
    <div v-if="$slots.action" class="xyf-empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  title: '暂无数据',
  description: ''
})
</script>

<style scoped lang="scss">
.xyf-empty {
  padding: 56px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__art {
    width: 112px;
    height: 90px;
    margin-bottom: 14px;

    svg {
      width: 100%;
      height: 100%;
      overflow: visible;
    }
  }

  .empty-orbit {
    transform-origin: 60px 46px;
    animation: empty-spin 26s linear infinite;
  }

  .empty-float {
    animation: empty-bob 4.2s ease-in-out infinite;
  }

  .empty-shadow {
    transform-origin: 60px 80px;
    animation: empty-shadow-pulse 4.2s ease-in-out infinite;
  }

  .empty-spark {
    transform-origin: 84px 26px;
    animation: empty-spark-twinkle 2.8s ease-in-out infinite;
  }

  .empty-dot-1 {
    animation: empty-bob 3.5s ease-in-out infinite reverse;
  }

  .empty-dot-2 {
    animation: empty-bob 4.8s ease-in-out infinite;
  }

  &__title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  &__desc {
    margin: 6px 0 0;
    font-size: 13.5px;
    color: var(--xyf-ink-3);
    max-width: 360px;
  }

  &__action {
    margin-top: 18px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: center;
  }
}

@keyframes empty-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes empty-bob {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-5px);
  }
}

@keyframes empty-shadow-pulse {
  0%,
  100% {
    transform: scaleX(1);
    opacity: 0.7;
  }
  50% {
    transform: scaleX(0.88);
    opacity: 0.42;
  }
}

@keyframes empty-spark-twinkle {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.18);
    opacity: 1;
  }
}
</style>
