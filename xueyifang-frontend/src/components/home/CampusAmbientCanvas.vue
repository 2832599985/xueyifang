<template>
  <canvas ref="canvasRef" class="campus-ambient-canvas" aria-hidden="true" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 校园建筑光影与丝绸等高线交互引擎（Monochrome Architectural Daylight）
 * - 纯粹高级的石墨灰建筑点阵 + 暖香槟/珍珠日光漫射（告别廉价紫与医院绿）
 * - 随鼠标弯曲流淌的极细建筑弧线与技能连结星轨
 */

interface NodePoint {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  tone: 'ink' | 'warm' | 'silver'
  phase: number
}

const canvasRef = ref<HTMLCanvasElement | null>(null)

let rafId = 0
let width = 0
let height = 0
let dpr = 1
let nodes: NodePoint[] = []

const pointer = {
  targetX: -9999,
  targetY: -9999,
  x: -9999,
  y: -9999,
  active: false
}

let scrollOffset = 0

function initNodes() {
  const count = Math.min(48, Math.max(22, Math.floor((width * height) / 26000)))
  const tones: Array<'ink' | 'warm' | 'silver'> = ['ink', 'silver', 'ink', 'warm']
  nodes = Array.from({ length: count }, (_, i) => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.38,
    vy: (Math.random() - 0.5) * 0.28,
    r: 1.8 + Math.random() * 2,
    tone: tones[i % tones.length],
    phase: Math.random() * Math.PI * 2
  }))
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  width = rect.width || window.innerWidth
  height = rect.height || window.innerHeight
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  initNodes()
}

function handleMouseMove(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  pointer.targetX = e.clientX - rect.left
  pointer.targetY = e.clientY - rect.top
  if (!pointer.active) {
    pointer.x = pointer.targetX
    pointer.y = pointer.targetY
    pointer.active = true
  }
}

function handleMouseLeave() {
  pointer.active = false
}

function handleScroll() {
  scrollOffset = window.scrollY || 0
}

function drawFlowingRibbons(ctx: CanvasRenderingContext2D, t: number) {
  const mx = pointer.active ? (pointer.x / width - 0.5) * 44 : 0
  const my = pointer.active ? (pointer.y / height - 0.5) * 32 : 0

  const ribbons = [
    { yBase: 0.3, amp: 48, freq: 0.0023, speed: 0.85, color: 'rgba(24, 24, 27, 0.09)', lineWidth: 1.3 },
    { yBase: 0.34, amp: 40, freq: 0.002, speed: 0.68, color: 'rgba(24, 24, 27, 0.055)', lineWidth: 1 },
    { yBase: 0.68, amp: 52, freq: 0.0019, speed: -0.74, color: 'rgba(180, 130, 70, 0.1)', lineWidth: 1.3 },
    { yBase: 0.73, amp: 34, freq: 0.0025, speed: -0.58, color: 'rgba(24, 24, 27, 0.06)', lineWidth: 1 }
  ]

  for (const rb of ribbons) {
    ctx.beginPath()
    const baseY = height * rb.yBase - scrollOffset * 0.14
    for (let x = -20; x <= width + 20; x += 18) {
      let cursorBend = 0
      if (pointer.active) {
        const dx = x - pointer.x
        const influence = Math.exp(-(dx * dx) / (2 * 220 * 220))
        cursorBend = (pointer.y - baseY) * 0.16 * influence
      }
      const y =
        baseY +
        Math.sin(x * rb.freq + t * rb.speed) * rb.amp +
        Math.cos(x * rb.freq * 0.55 - t * rb.speed * 0.6) * (rb.amp * 0.36) +
        my * 0.22 +
        cursorBend
      if (x === -20) {
        ctx.moveTo(x + mx * 0.12, y)
      } else {
        ctx.lineTo(x + mx * 0.12, y)
      }
    }
    ctx.strokeStyle = rb.color
    ctx.lineWidth = rb.lineWidth
    ctx.stroke()
  }
}

function draw(now: number) {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const t = now * 0.00065

  if (pointer.active) {
    pointer.x += (pointer.targetX - pointer.x) * 0.09
    pointer.y += (pointer.targetY - pointer.y) * 0.09
  }

  ctx.save()
  ctx.scale(dpr, dpr)
  ctx.clearRect(0, 0, width, height)

  // 1. Subtle Warm Champagne & Cool Silver Architectural Light
  const parallaxY = scrollOffset * 0.18
  const orb1X = width * (0.22 + Math.sin(t * 0.72) * 0.05) + (pointer.active ? (pointer.x - width / 2) * 0.035 : 0)
  const orb1Y = height * (0.26 + Math.cos(t * 0.62) * 0.06) - parallaxY * 0.42
  const g1 = ctx.createRadialGradient(orb1X, orb1Y, 0, orb1X, orb1Y, Math.max(width, height) * 0.44)
  g1.addColorStop(0, 'rgba(217, 175, 118, 0.13)')
  g1.addColorStop(0.55, 'rgba(217, 175, 118, 0.035)')
  g1.addColorStop(1, 'rgba(217, 175, 118, 0)')
  ctx.fillStyle = g1
  ctx.fillRect(0, 0, width, height)

  const orb2X = width * (0.78 + Math.cos(t * 0.58) * 0.06) - (pointer.active ? (pointer.x - width / 2) * 0.03 : 0)
  const orb2Y = height * (0.68 + Math.sin(t * 0.78) * 0.05) - parallaxY * 0.34
  const g2 = ctx.createRadialGradient(orb2X, orb2Y, 0, orb2X, orb2Y, Math.max(width, height) * 0.42)
  g2.addColorStop(0, 'rgba(148, 163, 184, 0.14)')
  g2.addColorStop(0.55, 'rgba(148, 163, 184, 0.04)')
  g2.addColorStop(1, 'rgba(148, 163, 184, 0)')
  ctx.fillStyle = g2
  ctx.fillRect(0, 0, width, height)

  // 2. Flowing silk architectural ribbons
  drawFlowingRibbons(ctx, t)

  // 3. Soft cursor pearl spotlight
  if (pointer.active) {
    const cursorGrad = ctx.createRadialGradient(pointer.x, pointer.y, 0, pointer.x, pointer.y, 240)
    cursorGrad.addColorStop(0, 'rgba(24, 24, 27, 0.065)')
    cursorGrad.addColorStop(0.55, 'rgba(24, 24, 27, 0.02)')
    cursorGrad.addColorStop(1, 'rgba(24, 24, 27, 0)')
    ctx.fillStyle = cursorGrad
    ctx.fillRect(0, 0, width, height)
  }

  // 4. Fine graphite drafting dot-grid
  const spacing = 42
  const offsetY = (-scrollOffset * 0.12) % spacing
  for (let gx = spacing * 0.5; gx < width; gx += spacing) {
    for (let gy = offsetY; gy < height; gy += spacing) {
      if (gy < 0) continue
      let alpha = 0.13
      let radius = 1
      if (pointer.active) {
        const dist = Math.hypot(gx - pointer.x, gy - pointer.y)
        if (dist < 185) {
          const boost = 1 - dist / 185
          alpha += boost * 0.34
          radius += boost * 1.35
        }
      }
      ctx.fillStyle = `rgba(24, 24, 27, ${alpha.toFixed(3)})`
      ctx.beginPath()
      ctx.arc(gx, gy, radius, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // 5. Constellation nodes + crisp graphite links
  for (let i = 0; i < nodes.length; i++) {
    const n = nodes[i]
    n.x += n.vx
    n.y += n.vy
    if (n.x < -20) n.x = width + 20
    if (n.x > width + 20) n.x = -20
    if (n.y < -20) n.y = height + 20
    if (n.y > height + 20) n.y = -20

    if (pointer.active) {
      const dx = n.x - pointer.x
      const dy = n.y - pointer.y
      const dist = Math.hypot(dx, dy)
      if (dist > 1 && dist < 160) {
        const force = (1 - dist / 160) * 0.25
        n.x += (dx / dist) * force
        n.y += (dy / dist) * force
      }
    }
  }

  const maxLinkDist = 152
  ctx.lineWidth = 1
  for (let i = 0; i < nodes.length; i++) {
    const a = nodes[i]
    for (let j = i + 1; j < nodes.length; j++) {
      const b = nodes[j]
      const dist = Math.hypot(a.x - b.x, a.y - b.y)
      if (dist < maxLinkDist) {
        const alpha = (1 - dist / maxLinkDist) * 0.14
        ctx.strokeStyle = `rgba(24, 24, 27, ${alpha.toFixed(3)})`
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }

    if (pointer.active) {
      const pDist = Math.hypot(a.x - pointer.x, a.y - pointer.y)
      if (pDist < 200) {
        const pAlpha = (1 - pDist / 200) * 0.28
        ctx.strokeStyle = `rgba(24, 24, 27, ${pAlpha.toFixed(3)})`
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(pointer.x, pointer.y)
        ctx.stroke()
      }
    }
  }

  for (let i = 0; i < nodes.length; i++) {
    const n = nodes[i]
    const pulse = 0.72 + Math.sin(t * 2.6 + n.phase) * 0.28
    const color =
      n.tone === 'warm'
        ? `rgba(180, 120, 48, ${(0.4 * pulse).toFixed(3)})`
        : n.tone === 'silver'
          ? `rgba(113, 113, 122, ${(0.34 * pulse).toFixed(3)})`
          : `rgba(24, 24, 27, ${(0.38 * pulse).toFixed(3)})`
    ctx.fillStyle = color
    ctx.beginPath()
    ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.restore()
  rafId = window.requestAnimationFrame(draw)
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize, { passive: true })
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
  window.addEventListener('mouseleave', handleMouseLeave, { passive: true })
  window.addEventListener('scroll', handleScroll, { passive: true })
  rafId = window.requestAnimationFrame(draw)
})

onUnmounted(() => {
  window.cancelAnimationFrame(rafId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseleave', handleMouseLeave)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
.campus-ambient-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
