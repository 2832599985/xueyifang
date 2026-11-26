<template>
  <div class="xyf-avatar" :style="avatarStyle">
    <img
      v-if="resolvedSrc && !imgError"
      :src="resolvedSrc"
      :alt="name || '头像'"
      class="xyf-avatar__img"
      @error="imgError = true"
    />
    <span v-else class="xyf-avatar__initials">{{ initials }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { buildImageUrl } from '@/api/service'
import { getInitials } from '@/utils/format'

interface Props {
  src?: string | null
  name?: string | null
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  src: '',
  name: '',
  size: 36
})

const imgError = ref(false)
const resolvedSrc = computed(() => buildImageUrl(props.src))

watch(
  () => props.src,
  () => {
    imgError.value = false
  }
)

const PALETTE = ['#18181B', '#27272A', '#3F3F46', '#52525B', '#B45309', '#0F172A', '#334155']

const pigment = computed(() => {
  const text = (props.name || '学艺坊').trim()
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = text.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
})

const initials = computed(() => getInitials(props.name, '学'))

const avatarStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.max(11, Math.round(props.size * 0.4))}px`,
  backgroundColor: resolvedSrc.value && !imgError.value ? 'var(--xyf-surface-muted)' : pigment.value
}))
</script>

<style scoped lang="scss">
.xyf-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  color: #ffffff;
  font-weight: 600;
  border: 1.5px solid rgba(255, 255, 255, 0.85);
  box-shadow: var(--xyf-shadow-xs);
  user-select: none;

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &__initials {
    line-height: 1;
  }
}
</style>
