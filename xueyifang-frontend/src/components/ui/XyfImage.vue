<template>
  <div ref="containerRef" class="xyf-image" :style="containerStyle">
    <div v-if="shouldShowSkeleton" class="xyf-image__skeleton">
      <div class="xyf-image__shimmer" />
    </div>

    <img
      v-if="isInView && activeSrc && !hasError"
      :src="activeSrc"
      :alt="alt"
      :style="imgStyle"
      class="xyf-image__img"
      :class="{ 'is-loaded': isLoaded }"
      @load="onLoad"
      @error="onError"
    />

    <div v-else-if="shouldShowFallback" class="xyf-image__fallback">
      <svg viewBox="0 0 80 64" fill="none" xmlns="http://www.w3.org/2000/svg" class="xyf-image__art">
        <rect x="8" y="10" width="64" height="44" rx="8" stroke="currentColor" stroke-opacity="0.28" stroke-width="2" />
        <circle cx="26" cy="25" r="4.5" fill="currentColor" fill-opacity="0.26" />
        <path
          d="M14 48L31 32L43 42L53 34L66 48"
          stroke="currentColor"
          stroke-opacity="0.35"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <span v-if="showEmptyText" class="xyf-image__empty-text">学艺坊 · 技能展示</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  src?: string | null
  width?: number | string
  height?: number | string
  radius?: number | string
  alt?: string
  fallback?: string
  lazy?: boolean
  fit?: 'fill' | 'contain' | 'cover' | 'none' | 'scale-down'
  showEmptyText?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  src: '',
  width: '100%',
  height: 'auto',
  radius: 0,
  alt: '图片',
  fallback: '',
  lazy: true,
  fit: 'cover',
  showEmptyText: false
})

const containerRef = ref<HTMLDivElement | null>(null)
const isInView = ref(!props.lazy)
const isLoaded = ref(false)
const hasError = ref(false)
const triedFallback = ref(false)

const activeSrc = computed(() => {
  if (triedFallback.value && props.fallback) return props.fallback
  return (props.src || '').trim()
})

watch(
  () => props.src,
  () => {
    isLoaded.value = false
    hasError.value = false
    triedFallback.value = false
  }
)

const containerStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  borderRadius: typeof props.radius === 'number' ? `${props.radius}px` : props.radius
}))

const imgStyle = computed(() => ({
  objectFit: props.fit,
  borderRadius: typeof props.radius === 'number' ? `${props.radius}px` : props.radius
}))

const shouldShowSkeleton = computed(() => Boolean(activeSrc.value) && !isLoaded.value && !hasError.value)
const shouldShowFallback = computed(() => !activeSrc.value || hasError.value)

function onLoad() {
  isLoaded.value = true
  hasError.value = false
}

function onError() {
  if (!triedFallback.value && props.fallback) {
    triedFallback.value = true
    return
  }
  isLoaded.value = false
  hasError.value = true
}

let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!props.lazy || !containerRef.value) {
    isInView.value = true
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        isInView.value = true
        observer?.disconnect()
      }
    },
    { rootMargin: '420px', threshold: 0.01 }
  )

  observer.observe(containerRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<style scoped lang="scss">
.xyf-image {
  position: relative;
  overflow: hidden;
  background: #f2f2f5;

  &__skeleton {
    position: absolute;
    inset: 0;
    background: var(--xyf-surface-muted);
    overflow: hidden;
  }

  &__shimmer {
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.55) 50%, transparent 100%);
    animation: xyf-shimmer 1.4s infinite;
  }

  &__img {
    width: 100%;
    height: 100%;
    display: block;
    opacity: 0;
    transition: opacity 0.25s ease, transform 0.35s ease;

    &.is-loaded {
      opacity: 1;
    }
  }

  &__fallback {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: var(--xyf-primary);
    background: radial-gradient(circle at 20% 20%, rgba(24, 24, 27, 0.05), transparent 58%),
      radial-gradient(circle at 80% 80%, rgba(217, 175, 118, 0.06), transparent 55%),
      var(--xyf-surface-muted);
  }

  &__art {
    width: 44px;
    height: 36px;
    opacity: 0.72;
  }

  &__empty-text {
    font-size: 11.5px;
    font-weight: 500;
    color: var(--xyf-ink-3);
    letter-spacing: 0.04em;
  }
}

@keyframes xyf-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
