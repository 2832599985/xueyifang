<template>
  <aside class="auth-stage" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
    <!-- Subtle Warm-Titanium & Pearl Architectural Ambient Light -->
    <div class="auth-stage__glow is-one" />
    <div class="auth-stage__glow is-two" />
    <div class="auth-stage__grid" />

    <!-- Top Brand -->
    <div class="auth-stage__top">
      <XyfBrandLogo theme="light" compact title="学艺坊" subtitle="" />
      <span class="stage-pill">校园技能平台</span>
    </div>

    <!-- Monochrome Titanium Kinetic SVG Sculpture -->
    <div class="auth-stage__visual" :style="parallaxStyle">
      <svg class="kinetic-svg" viewBox="0 0 380 300" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <defs>
          <linearGradient id="mono-ring-grad" x1="40" y1="30" x2="340" y2="270" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.55" />
            <stop offset="50%" stop-color="#E5C79E" stop-opacity="0.4" />
            <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.06" />
          </linearGradient>
          <linearGradient id="mono-core-grad" x1="160" y1="120" x2="220" y2="180" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#27272A" />
            <stop offset="100%" stop-color="#09090B" />
          </linearGradient>
          <radialGradient id="mono-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#E5C79E" stop-opacity="0.22" />
            <stop offset="60%" stop-color="#FFFFFF" stop-opacity="0.06" />
            <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
          </radialGradient>
        </defs>

        <!-- Soft Architectural Halo -->
        <circle cx="190" cy="150" r="115" fill="url(#mono-glow)" class="svg-breathe" />

        <!-- Outer Rotating Precision Compass Ring -->
        <circle
          cx="190"
          cy="150"
          r="116"
          stroke="url(#mono-ring-grad)"
          stroke-width="1.1"
          stroke-dasharray="4 10"
          class="svg-spin-slow"
        />

        <!-- Tilted Elliptical Exchange Orbits -->
        <ellipse
          cx="190"
          cy="150"
          rx="148"
          ry="56"
          transform="rotate(-18 190 150)"
          stroke="rgba(255, 255, 255, 0.14)"
          stroke-width="1.1"
        />
        <ellipse
          cx="190"
          cy="150"
          rx="148"
          ry="56"
          transform="rotate(-18 190 150)"
          stroke="#FFFFFF"
          stroke-width="1.8"
          stroke-dasharray="42 384"
          stroke-linecap="round"
          class="svg-dash-flow"
        />

        <ellipse
          cx="190"
          cy="150"
          rx="132"
          ry="48"
          transform="rotate(24 190 150)"
          stroke="rgba(229, 199, 158, 0.16)"
          stroke-width="1.1"
        />
        <ellipse
          cx="190"
          cy="150"
          rx="132"
          ry="48"
          transform="rotate(24 190 150)"
          stroke="#E5C79E"
          stroke-width="1.8"
          stroke-dasharray="36 352"
          stroke-linecap="round"
          class="svg-dash-flow-rev"
        />

        <!-- Inner Fine Ring -->
        <circle cx="190" cy="150" r="64" stroke="rgba(255,255,255,0.12)" stroke-width="1" />

        <!-- Center Titanium Emblem -->
        <g class="svg-float-core">
          <rect
            x="160"
            y="120"
            width="60"
            height="60"
            rx="16"
            fill="url(#mono-core-grad)"
          />
          <rect
            x="160.75"
            y="120.75"
            width="58.5"
            height="58.5"
            rx="15.25"
            stroke="rgba(255,255,255,0.28)"
            stroke-width="1.5"
          />
          <path
            d="M177 162L190 138L203 162"
            stroke="#FFFFFF"
            stroke-width="3.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M182 154H198"
            stroke="#E5C79E"
            stroke-width="2.8"
            stroke-linecap="round"
          />
        </g>

        <!-- Subtle Orbit Satellites -->
        <circle cx="88" cy="108" r="3.5" fill="#FFFFFF" class="svg-pulse-dot" />
        <circle cx="294" cy="192" r="3.5" fill="#E5C79E" class="svg-pulse-dot-2" />
        <circle cx="268" cy="82" r="2.8" fill="rgba(255,255,255,0.7)" class="svg-pulse-dot" />
      </svg>

      <!-- Minimal Translucent Titanium Chips -->
      <div class="glass-chip is-top-left">
        <span class="chip-dot is-white" />
        <span>学号实名认证</span>
      </div>
      <div class="glass-chip is-bottom-right">
        <span class="chip-dot is-gold" />
        <span>担保托管 · 满意结算</span>
      </div>
    </div>

    <!-- Bottom Minimal Statement & Links -->
    <div class="auth-stage__bottom">
      <h2 class="stage-headline">{{ headline }}</h2>
      <p class="stage-sub">{{ subtitle }}</p>

      <div class="stage-links">
        <slot name="links">
          <router-link to="/services">浏览服务大厅</router-link>
          <span>·</span>
          <router-link to="/admin/login">管理后台</router-link>
        </slot>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import XyfBrandLogo from '@/components/ui/XyfBrandLogo.vue'

interface Props {
  headline?: string
  subtitle?: string
}

withDefaults(defineProps<Props>(), {
  headline: '连接校园里的专业行家',
  subtitle: '摄影、代码、设计与学业辅导，在可信担保中自由流转。'
})

const offset = ref({ x: 0, y: 0 })

function onMouseMove(e: MouseEvent) {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const nx = (e.clientX - rect.left) / rect.width - 0.5
  const ny = (e.clientY - rect.top) / rect.height - 0.5
  offset.value = { x: nx * 16, y: ny * 12 }
}

function onMouseLeave() {
  offset.value = { x: 0, y: 0 }
}

const parallaxStyle = computed(() => ({
  transform: `translate3d(${offset.value.x.toFixed(1)}px, ${offset.value.y.toFixed(1)}px, 0)`
}))
</script>

<style scoped lang="scss">
.auth-stage {
  position: relative;
  overflow: hidden;
  background: linear-gradient(155deg, #111114 0%, #1a1a1f 55%, #0e0e11 100%);
  color: #ffffff;
  padding: 36px 34px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
  min-height: 540px;

  &__glow {
    position: absolute;
    border-radius: 50%;
    filter: blur(64px);
    pointer-events: none;

    &.is-one {
      width: 260px;
      height: 260px;
      top: -60px;
      left: -50px;
      background: rgba(255, 255, 255, 0.08);
      animation: aurora-drift 12s ease-in-out infinite;
    }

    &.is-two {
      width: 260px;
      height: 260px;
      bottom: -50px;
      right: -50px;
      background: rgba(229, 199, 158, 0.14);
      animation: aurora-drift 15s ease-in-out infinite reverse;
    }
  }

  &__grid {
    position: absolute;
    inset: 0;
    background-image: radial-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px);
    background-size: 24px 24px;
    mask-image: radial-gradient(circle at 50% 48%, rgba(0, 0, 0, 0.85), transparent 82%);
    pointer-events: none;
  }

  &__top {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .stage-pill {
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.1em;
    color: rgba(255, 255, 255, 0.65);
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    padding: 4px 10px;
    border-radius: 999px;
  }

  &__visual {
    position: relative;
    z-index: 2;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  }

  .kinetic-svg {
    width: 100%;
    max-width: 340px;
    height: auto;
    overflow: visible;
  }

  .svg-spin-slow {
    transform-origin: 190px 150px;
    animation: auth-spin 34s linear infinite;
  }

  .svg-breathe {
    transform-origin: 190px 150px;
    animation: auth-breathe 5.5s ease-in-out infinite;
  }

  .svg-float-core {
    animation: auth-float 4.6s ease-in-out infinite;
  }

  .svg-dash-flow {
    animation: dash-move 5.2s linear infinite;
  }

  .svg-dash-flow-rev {
    animation: dash-move 6.8s linear infinite reverse;
  }

  .svg-pulse-dot {
    animation: dot-blink 3s ease-in-out infinite;
  }

  .svg-pulse-dot-2 {
    animation: dot-blink 3.8s ease-in-out infinite 1s;
  }

  .glass-chip {
    position: absolute;
    display: inline-flex;
    align-items: center;
    gap: 7px;
    padding: 7px 13px;
    border-radius: 999px;
    background: rgba(24, 24, 27, 0.72);
    border: 1px solid rgba(255, 255, 255, 0.14);
    backdrop-filter: blur(12px);
    font-size: 12px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.88);
    box-shadow: 0 10px 24px -8px rgba(0, 0, 0, 0.55);
    white-space: nowrap;

    &.is-top-left {
      top: 14%;
      left: 2%;
      animation: auth-float 5.2s ease-in-out infinite;
    }

    &.is-bottom-right {
      bottom: 14%;
      right: 2%;
      animation: auth-float 5.8s ease-in-out infinite reverse;
    }

    .chip-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;

      &.is-white {
        background: #ffffff;
        box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
      }

      &.is-gold {
        background: #e5c79e;
        box-shadow: 0 0 8px rgba(229, 199, 158, 0.85);
      }
    }
  }

  &__bottom {
    position: relative;
    z-index: 2;
  }

  .stage-headline {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #ffffff;
  }

  .stage-sub {
    margin: 8px 0 0;
    font-size: 13.5px;
    line-height: 1.6;
    color: rgba(255, 255, 255, 0.6);
  }

  .stage-links {
    margin-top: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12.5px;
    color: rgba(255, 255, 255, 0.42);

    :deep(a) {
      color: rgba(255, 255, 255, 0.78);
      text-decoration: none;
      transition: color 0.2s ease;

      &:hover {
        color: #ffffff;
      }
    }
  }
}

@keyframes auth-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes auth-breathe {
  0%,
  100% {
    transform: scale(0.95);
    opacity: 0.75;
  }
  50% {
    transform: scale(1.06);
    opacity: 1;
  }
}

@keyframes auth-float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-7px);
  }
}

@keyframes dash-move {
  from {
    stroke-dashoffset: 426;
  }
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes dot-blink {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 1;
  }
}

@keyframes aurora-drift {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(22px, 16px);
  }
}
</style>
