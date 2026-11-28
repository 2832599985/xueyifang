<template>
  <span class="xyf-trust-marks">
    <!-- Animated Obsidian-Gold Edu Verified Shield SVG (zero text bloat) -->
    <el-tooltip
      v-if="verified"
      content="高校 .edu.cn 邮箱已实名核验"
      placement="top"
      :show-after="120"
    >
      <span class="xyf-verified-icon" aria-label="高校 .edu.cn 邮箱已实名核验">
        <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
          <defs>
            <clipPath :id="clipId">
              <path d="M9 1.6L14.8 4v4.6c0 3.7-2.4 6.6-5.8 7.8-3.4-1.2-5.8-4.1-5.8-7.8V4L9 1.6Z" />
            </clipPath>
          </defs>
          <path
            d="M9 1.6L14.8 4v4.6c0 3.7-2.4 6.6-5.8 7.8-3.4-1.2-5.8-4.1-5.8-7.8V4L9 1.6Z"
            fill="#18181b"
            stroke="rgba(229, 199, 158, 0.65)"
            stroke-width="0.95"
          />
          <!-- Metallic diagonal champagne light sheen -->
          <g :clip-path="`url(#${clipId})`">
            <rect
              class="shield-sheen"
              x="-12"
              y="-2"
              width="7"
              height="24"
              fill="rgba(243, 226, 199, 0.36)"
              transform="rotate(24 9 9)"
            />
          </g>
          <path
            d="M6.2 9.1l1.9 1.9 3.8-4"
            stroke="#e5c79e"
            stroke-width="1.75"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span v-if="showLabel" class="xyf-verified-icon__txt">已核验</span>
      </span>
    </el-tooltip>

    <!-- Animated Gold Star Credit Score Emblem -->
    <el-tooltip
      v-if="creditScore !== undefined && creditScore !== null"
      :content="`履约信用分 ${creditScore}`"
      placement="top"
      :show-after="120"
    >
      <span class="xyf-credit-mark xyf-tabular" :aria-label="`履约信用分 ${creditScore}`">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            class="credit-star"
            d="M8 2.1l1.7 3.5 3.9.5-2.8 2.7.7 3.9L8 10.9l-3.5 1.9.7-3.9-2.8-2.7 3.9-.5L8 2.1Z"
            fill="rgba(217, 119, 6, 0.14)"
            stroke="#c89246"
            stroke-width="1.3"
            stroke-linejoin="round"
          />
        </svg>
        <span>{{ creditScore }}</span>
      </span>
    </el-tooltip>
  </span>
</template>

<script lang="ts">
let shieldClipSeq = 0
</script>

<script setup lang="ts">
interface Props {
  verified?: boolean
  creditScore?: number | null
  showLabel?: boolean
}

withDefaults(defineProps<Props>(), {
  verified: false,
  creditScore: undefined,
  showLabel: false
})

const clipId = `xyf-shield-${++shieldClipSeq}`
</script>

<style scoped lang="scss">
.xyf-trust-marks {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  vertical-align: middle;
  flex-shrink: 0;
}

.xyf-verified-icon {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: help;
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    transform: scale(1.12);
  }

  svg {
    width: 17px;
    height: 17px;
    display: block;
    flex-shrink: 0;
  }

  &__txt {
    font-size: 11.5px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }
}

.shield-sheen {
  animation: xyf-shield-glint 4.2s ease-in-out infinite;
}

.xyf-credit-mark {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink-3);
  cursor: help;

  svg {
    width: 13.5px;
    height: 13.5px;
    flex-shrink: 0;
    transition: transform 0.2s ease;
  }

  &:hover svg {
    transform: rotate(15deg) scale(1.1);
  }
}

@keyframes xyf-shield-glint {
  0%,
  72% {
    transform: translateX(-14px) rotate(24deg);
    opacity: 0;
  }
  80% {
    opacity: 1;
  }
  94% {
    transform: translateX(28px) rotate(24deg);
    opacity: 0;
  }
  100% {
    transform: translateX(28px) rotate(24deg);
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shield-sheen {
    animation: none;
    opacity: 0;
  }
}
</style>
