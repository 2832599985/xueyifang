<template>
  <section class="xyf-surface-card meetup-pass">
    <div class="meetup-pass__head">
      <div class="pass-title-group">
        <h3>校内当面动态核销凭证</h3>
        <span class="pass-guard-pill">仅买方本人可见 · 动态加密防截图</span>
      </div>

      <div class="pass-live-meta">
        <span class="live-clock xyf-mono">{{ liveClockText }}</span>
        <button
          type="button"
          class="refresh-btn"
          :disabled="syncing"
          @click="syncCredential(true)"
        >
          {{ syncing ? '同步中...' : '立即刷新' }}
        </button>
      </div>
    </div>

    <div class="meetup-pass__grid">
      <!-- Left: 10-Second Anti-Screenshot Dynamic QR Code -->
      <div class="qr-panel">
        <div class="qr-panel__stage">
          <div class="qr-frame">
            <span class="qr-corner is-tl" />
            <span class="qr-corner is-tr" />
            <span class="qr-corner is-bl" />
            <span class="qr-corner is-br" />
            <div class="qr-sweep-beam" aria-hidden="true" />
            <canvas ref="qrCanvasRef" class="qr-canvas" />
          </div>

          <div class="qr-timer-row">
            <div class="timer-track">
              <div
                class="timer-track__fill"
                :style="{ width: `${qrProgressPercent}%` }"
              />
            </div>
            <span class="timer-label xyf-tabular">
              动态二维码 · <strong>{{ qrRemaining }}s</strong> 后刷新
            </span>
          </div>
        </div>

        <div class="qr-panel__caption">
          <div class="caption-badge">推荐 · 卖家现场扫码秒结单</div>
          <p>
            二维码每 <strong>{{ qrPeriod }} 秒</strong>自动重签刷新，仅支持卖家当面调用实时摄像头扫描核销，远程截图转发即刻失效。
          </p>
        </div>
      </div>

      <!-- Right: 30-Second Rotating TOTP 6-Digit Backup Code -->
      <div class="code-panel">
        <div class="code-panel__top">
          <span class="code-title">备用 · 30 秒动态数字核销码</span>
          <span class="code-countdown xyf-tabular">{{ codeRemaining }}s 后更换</span>
        </div>

        <div class="code-digits xyf-mono">
          <span
            v-for="(digit, idx) in codeDigits"
            :key="idx"
            class="digit-cell"
          >
            {{ digit }}
          </span>
        </div>

        <div class="code-timer-track">
          <div
            class="code-timer-track__fill"
            :style="{ width: `${codeProgressPercent}%` }"
          />
        </div>

        <p class="code-desc">
          若现场无摄像头或光线不足，当面验收完成后可向卖家口头报出上方 6 位动态码（每 {{ codePeriod }} 秒刷新，连续输错 5 次自动锁定）。
        </p>

        <div class="code-actions">
          <button type="button" class="copy-code-btn" @click="copyDynamicCode">
            复制当前动态码
          </button>
          <span class="safety-tip">验收无误后再出示，核销即刻结算资金</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import { getOfflineVerifyCredential } from '@/api/order'
import { onWebSocketMessage, MESSAGE_TYPES } from '@/composables/useWebSocket'
import { OrderStatus } from '@/types/order'

interface Props {
  orderId: number
  initialCode?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  verified: []
}>()

const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const syncing = ref(false)
const verifyCode = ref(props.initialCode || '------')
const qrToken = ref('')
const codePeriod = ref(30)
const codeRemaining = ref(30)
const qrPeriod = ref(10)
const qrRemaining = ref(10)
const liveClockText = ref(formatLiveClock())

let tickTimer: number | null = null
let stopWsListener: (() => void) | null = null
let notifiedVerified = false

const codeDigits = computed(() => {
  const clean = (verifyCode.value || '').padEnd(6, '-').slice(0, 6)
  return clean.split('')
})

const qrProgressPercent = computed(() =>
  Math.max(0, Math.min(100, Math.round((qrRemaining.value / Math.max(1, qrPeriod.value)) * 100)))
)

const codeProgressPercent = computed(() =>
  Math.max(0, Math.min(100, Math.round((codeRemaining.value / Math.max(1, codePeriod.value)) * 100)))
)

function formatLiveClock() {
  const d = new Date()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

async function renderQrCanvas(payload: string) {
  if (!payload) return
  await nextTick()
  const canvas = qrCanvasRef.value
  if (!canvas) return
  try {
    await QRCode.toCanvas(canvas, payload, {
      width: 164,
      margin: 1,
      errorCorrectionLevel: 'M',
      color: {
        dark: '#18181b',
        light: '#ffffff'
      }
    })
  } catch (err) {
    console.error('渲染动态核销二维码失败:', err)
  }
}

async function syncCredential(manual = false) {
  if (syncing.value || notifiedVerified) return
  syncing.value = true
  try {
    const res = await getOfflineVerifyCredential(props.orderId)
    if (res.orderStatus === OrderStatus.COMPLETED) {
      handleVerifiedOnce()
      return
    }
    codePeriod.value = res.codePeriodSeconds || 30
    codeRemaining.value = res.codeRemainingSeconds || 30
    qrPeriod.value = res.qrPeriodSeconds || 10
    qrRemaining.value = res.qrRemainingSeconds || 10

    if (res.verifyCode) {
      verifyCode.value = res.verifyCode
    }
    if (res.qrToken && res.qrToken !== qrToken.value) {
      qrToken.value = res.qrToken
      await renderQrCanvas(res.qrToken)
    } else if (res.qrToken) {
      await renderQrCanvas(res.qrToken)
    }
    if (manual) {
      ElMessage.success('动态核销凭证已同步最新周期')
    }
  } catch {
    // 静默重试，避免网络波动打断展示
  } finally {
    syncing.value = false
  }
}

function handleVerifiedOnce() {
  if (notifiedVerified) return
  notifiedVerified = true
  stopTick()
  ElMessage.success('卖家已核销通过，当面交接完成')
  emit('verified')
}

function startTick() {
  stopTick()
  tickTimer = window.setInterval(() => {
    liveClockText.value = formatLiveClock()
    if (document.visibilityState === 'hidden' || notifiedVerified) return

    qrRemaining.value = Math.max(0, qrRemaining.value - 1)
    codeRemaining.value = Math.max(0, codeRemaining.value - 1)

    if (qrRemaining.value <= 0 || codeRemaining.value <= 0) {
      syncCredential(false)
    }
  }, 1000)
}

function stopTick() {
  if (tickTimer !== null) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible' && !notifiedVerified) {
    syncCredential(false)
  }
}

async function copyDynamicCode() {
  if (!verifyCode.value || verifyCode.value.includes('-')) return
  try {
    await navigator.clipboard.writeText(verifyCode.value)
    ElMessage.success('当前 30 秒动态核销码已复制')
  } catch {
    ElMessage.info(verifyCode.value)
  }
}

watch(
  () => props.orderId,
  () => {
    notifiedVerified = false
    syncCredential(false)
  }
)

onMounted(() => {
  syncCredential(false)
  startTick()
  document.addEventListener('visibilitychange', handleVisibilityChange)

  stopWsListener = onWebSocketMessage((msg) => {
    if (msg.type === MESSAGE_TYPES.NEW_NOTIFICATION) {
      const payload = msg.data as { relatedOrderId?: number; title?: string } | undefined
      if (
        payload &&
        Number(payload.relatedOrderId) === props.orderId &&
        String(payload.title || '').includes('核销完成')
      ) {
        handleVerifiedOnce()
      }
    }
  })
})

onUnmounted(() => {
  stopTick()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (stopWsListener) {
    stopWsListener()
    stopWsListener = null
  }
})
</script>

<style scoped lang="scss">
.meetup-pass {
  padding: 20px 24px 22px;
  margin-bottom: 18px;
  border: 1px solid #18181b;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
}

.meetup-pass__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--xyf-border);
  margin-bottom: 18px;
}

.pass-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 750;
    color: var(--xyf-ink);
    letter-spacing: -0.015em;
  }
}

.pass-guard-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 5px;
  background: #18181b;
  color: #ffffff;
}

.pass-live-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-clock {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--xyf-ink-2);
  padding: 4px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
}

.refresh-btn {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover:not(:disabled) {
    border-color: #18181b;
    background: var(--xyf-surface-soft);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.meetup-pass__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
}

/* Left QR Panel */
.qr-panel {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-right: 22px;
  border-right: 1px dashed var(--xyf-border);
}

.qr-panel__stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.qr-frame {
  position: relative;
  width: 184px;
  height: 184px;
  padding: 10px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  box-shadow: 0 10px 24px -14px rgba(9, 9, 11, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.qr-canvas {
  display: block;
  width: 164px !important;
  height: 164px !important;
  position: relative;
  z-index: 1;
}

.qr-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: #18181b;
  border-style: solid;
  z-index: 2;

  &.is-tl {
    top: 5px;
    left: 5px;
    border-width: 2.5px 0 0 2.5px;
    border-top-left-radius: 5px;
  }
  &.is-tr {
    top: 5px;
    right: 5px;
    border-width: 2.5px 2.5px 0 0;
    border-top-right-radius: 5px;
  }
  &.is-bl {
    bottom: 5px;
    left: 5px;
    border-width: 0 0 2.5px 2.5px;
    border-bottom-left-radius: 5px;
  }
  &.is-br {
    bottom: 5px;
    right: 5px;
    border-width: 0 2.5px 2.5px 0;
    border-bottom-right-radius: 5px;
  }
}

.qr-sweep-beam {
  position: absolute;
  left: 10px;
  right: 10px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(24, 24, 27, 0.45), transparent);
  z-index: 3;
  pointer-events: none;
  animation: qr-sweep 2.6s ease-in-out infinite;
}

@keyframes qr-sweep {
  0%,
  100% {
    top: 14px;
    opacity: 0.2;
  }
  50% {
    top: 166px;
    opacity: 0.75;
  }
}

.qr-timer-row {
  width: 184px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.timer-track {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: #e4e4e7;
  overflow: hidden;

  &__fill {
    height: 100%;
    background: #18181b;
    border-radius: 999px;
    transition: width 0.85s linear;
  }
}

.timer-label {
  font-size: 11.5px;
  color: var(--xyf-ink-3);
  text-align: center;

  strong {
    color: var(--xyf-ink);
  }
}

.qr-panel__caption {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .caption-badge {
    align-self: flex-start;
    font-size: 11.5px;
    font-weight: 650;
    padding: 3px 9px;
    border-radius: 6px;
    background: #f4f4f5;
    border: 1px solid #e4e4e7;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: var(--xyf-ink-2);
  }
}

/* Right 30s Code Panel */
.code-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  .code-title {
    font-size: 13.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .code-countdown {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-2);
    padding: 2px 8px;
    border-radius: 999px;
    background: #f4f4f5;
  }
}

.code-digits {
  display: flex;
  gap: 8px;
}

.digit-cell {
  flex: 1;
  max-width: 44px;
  height: 48px;
  border-radius: 10px;
  background: #18181b;
  color: #ffffff;
  font-size: 21px;
  font-weight: 750;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px -8px rgba(9, 9, 11, 0.35);
}

.code-timer-track {
  width: 100%;
  max-width: 304px;
  height: 4px;
  border-radius: 999px;
  background: #e4e4e7;
  overflow: hidden;

  &__fill {
    height: 100%;
    background: #3f3f46;
    border-radius: 999px;
    transition: width 0.85s linear;
  }
}

.code-desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--xyf-ink-3);
}

.code-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.copy-code-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #18181b;
    background: var(--xyf-surface-soft);
  }
}

.safety-tip {
  font-size: 11.5px;
  color: var(--xyf-ink-3);
}

@media (max-width: 960px) {
  .meetup-pass__grid {
    grid-template-columns: 1fr;
  }

  .qr-panel {
    padding-right: 0;
    padding-bottom: 18px;
    border-right: none;
    border-bottom: 1px dashed var(--xyf-border);
  }
}

@media (max-width: 560px) {
  .qr-panel {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
