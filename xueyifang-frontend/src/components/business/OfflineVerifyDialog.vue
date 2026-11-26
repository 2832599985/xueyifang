<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="500px"
    :close-on-click-modal="false"
    @update:model-value="handleVisibleChange"
  >
    <div class="verify-shell">
      <!-- Monochrome Segmented Mode Switch -->
      <div class="mode-switch" role="tablist">
        <button
          type="button"
          class="mode-tab"
          :class="{ 'is-active': activeMode === 'qr' }"
          @click="switchMode('qr')"
        >
          实时摄像头扫码（10s 防截图）
        </button>
        <button
          type="button"
          class="mode-tab"
          :class="{ 'is-active': activeMode === 'code' }"
          @click="switchMode('code')"
        >
          输入 30s 动态核销码
        </button>
      </div>

      <!-- MODE 1: Live Camera-Only QR Scanner (STRICTLY NO SCREENSHOT/FILE IMPORT) -->
      <div v-if="activeMode === 'qr'" class="scanner-panel">
        <div class="scanner-stage">
          <video
            v-show="cameraReady"
            ref="videoRef"
            class="scanner-video"
            playsinline
            muted
            autoplay
          />

          <!-- Camera HUD Overlay -->
          <div v-if="cameraReady" class="scanner-hud">
            <div class="hud-top-bar">
              <span class="hud-live-pill">
                <span class="hud-pulse" />
                实时摄像头识别中 · 禁止截图导入
              </span>
              <button
                v-if="hasMultipleCameras"
                type="button"
                class="hud-flip-btn"
                @click="toggleCameraFacing"
              >
                切换镜头
              </button>
            </div>

            <div class="hud-reticle">
              <span class="reticle-c is-tl" />
              <span class="reticle-c is-tr" />
              <span class="reticle-c is-bl" />
              <span class="reticle-c is-br" />
              <div class="reticle-laser" />
            </div>

            <div class="hud-bottom-tip">
              请对准买家手机上的 10 秒动态核销二维码（自动识别结算）
            </div>
          </div>

          <!-- Camera Initializing -->
          <div v-else-if="cameraStarting" class="scanner-fallback">
            <div class="fallback-title">正在请求实时摄像头权限...</div>
            <p class="fallback-desc">
              为确保校园线下真实当面交接，扫码核销仅允许调用本机实时摄像头流，不支持从相册或本地导入截图。
            </p>
          </div>

          <!-- Camera Unavailable / Denied Fallback -->
          <div v-else class="scanner-fallback">
            <div class="fallback-title">{{ cameraErrorTitle }}</div>
            <p class="fallback-desc">{{ cameraErrorMessage }}</p>
            <div class="fallback-actions">
              <button type="button" class="fb-btn-secondary" @click="startCamera">
                重新开启摄像头
              </button>
              <button type="button" class="fb-btn-primary" @click="switchMode('code')">
                改用 30 秒动态核销码
              </button>
            </div>
          </div>
        </div>

        <p class="anti-fraud-note">
          安全机制：动态二维码每 10 秒自动失效重签，且系统仅采集实时视频流解码，严禁识别相册截图或静态图片。
        </p>

        <!-- Hidden offscreen canvas strictly for live video frame sampling -->
        <canvas ref="decodeCanvasRef" class="decode-canvas" />
      </div>

      <!-- MODE 2: 30-Second Dynamic 6-Digit Code Fallback -->
      <div v-else class="code-input-panel">
        <div v-if="!orderId && pendingOfflineOrders?.length" class="order-picker-field">
          <label class="field-label">选择待核销的校内线下订单</label>
          <el-select v-model="selectedOrderId" placeholder="请选择线下订单" style="width: 100%">
            <el-option
              v-for="item in pendingOfflineOrders"
              :key="item.id"
              :label="`${item.serviceTitle}（${item.orderNumber}）`"
              :value="item.id"
            />
          </el-select>
        </div>

        <div v-else-if="!orderId && !pendingOfflineOrders?.length" class="empty-offline-hint">
          当前暂无待核销的校内线下订单。您可直接切换至「实时摄像头扫码」，扫描买家二维码自动匹配订单。
        </div>

        <p v-else class="code-guide-text">
          请当面完成「<strong>{{ orderTitle || `订单 #${orderId}` }}</strong>」的交接后，输入买家端每 30 秒刷新的 <strong>6 位动态数字核销码</strong>：
        </p>

        <el-input
          v-model="codeInput"
          maxlength="6"
          inputmode="numeric"
          placeholder="请输入买家 6 位动态核销码"
          class="verify-code-input xyf-mono"
          :disabled="!effectiveOrderId || submitting"
          @keyup.enter="submitCodeVerify"
        />

        <p class="anti-fraud-note">
          核销码每 30 秒动态刷新一次；连续输错 5 次将自动触发 15 分钟防暴力枚举锁定。
        </p>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">关闭</el-button>
      <el-button
        v-if="activeMode === 'code'"
        type="primary"
        :loading="submitting"
        :disabled="!effectiveOrderId"
        @click="submitCodeVerify"
      >
        验证核销并立即收款
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import jsQR from 'jsqr'
import { ElMessage } from 'element-plus'
import { verifyOfflineCode, verifyOfflineQr } from '@/api/order'
import { notifyFallbackError } from '@/api/request'
import type { OrderListItem } from '@/types/order'

interface Props {
  visible: boolean
  orderId?: number | null
  orderTitle?: string
  initialMode?: 'qr' | 'code'
  pendingOfflineOrders?: OrderListItem[]
}

const props = withDefaults(defineProps<Props>(), {
  orderId: null,
  orderTitle: '',
  initialMode: 'qr',
  pendingOfflineOrders: () => []
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  verified: [orderId: number]
}>()

const activeMode = ref<'qr' | 'code'>(props.initialMode)
const selectedOrderId = ref<number | undefined>(undefined)
const codeInput = ref('')
const submitting = ref(false)

const videoRef = ref<HTMLVideoElement | null>(null)
const decodeCanvasRef = ref<HTMLCanvasElement | null>(null)
const cameraStarting = ref(false)
const cameraReady = ref(false)
const cameraErrorTitle = ref('无法开启实时摄像头')
const cameraErrorMessage = ref('请检查摄像头设备或浏览器权限后重试，或切换为输入 30 秒动态核销码。')
const facingMode = ref<'environment' | 'user'>('environment')
const hasMultipleCameras = ref(false)

let activeStream: MediaStream | null = null
let scanIntervalId: number | null = null
let decodingBusy = false
let lastRejectedToken = ''
let lastRejectedAt = 0

const dialogTitle = computed(() =>
  props.orderId ? '校内当面核销结单' : '校内线下实时扫码核销'
)

const effectiveOrderId = computed(() => props.orderId || selectedOrderId.value || null)

function handleVisibleChange(val: boolean) {
  emit('update:visible', val)
}

function switchMode(mode: 'qr' | 'code') {
  activeMode.value = mode
  if (mode === 'qr') {
    startCamera()
  } else {
    stopCamera()
  }
}

function stopCamera() {
  if (scanIntervalId !== null) {
    clearInterval(scanIntervalId)
    scanIntervalId = null
  }
  if (activeStream) {
    activeStream.getTracks().forEach((track) => track.stop())
    activeStream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  cameraReady.value = false
  cameraStarting.value = false
  decodingBusy = false
}

async function startCamera() {
  stopCamera()
  if (!props.visible || activeMode.value !== 'qr') return

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraErrorTitle.value = '当前浏览器环境不支持调用实时摄像头'
    cameraErrorMessage.value =
      '实时扫码核销需要在 HTTPS 或 localhost 安全上下文下调用真实摄像头（系统严禁导入本地截图）。请点击下方切换为「30 秒动态核销码」。'
    return
  }

  cameraStarting.value = true
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: { ideal: facingMode.value },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    })

    // 若弹窗在授权期间已关闭或切换模式，立刻释放摄像头
    if (!props.visible || activeMode.value !== 'qr') {
      stream.getTracks().forEach((t) => t.stop())
      return
    }

    activeStream = stream
    const devices = await navigator.mediaDevices.enumerateDevices().catch(() => [])
    hasMultipleCameras.value = devices.filter((d) => d.kind === 'videoinput').length > 1

    const video = videoRef.value
    if (video) {
      video.srcObject = stream
      await video.play()
      cameraReady.value = true
      startLiveDecodeLoop()
    }
  } catch (err: any) {
    const name = err?.name || ''
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      cameraErrorTitle.value = '摄像头权限被拒绝'
      cameraErrorMessage.value =
        '请在浏览器地址栏允许访问摄像头以进行当面扫码（系统不支持截图导入），或直接使用「30 秒动态核销码」。'
    } else if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
      cameraErrorTitle.value = '未检测到可用摄像头设备'
      cameraErrorMessage.value =
        '当前终端未连接摄像头，扫码核销不支持从相册或截图识别。请点击下方改用「30 秒动态核销码」完成当面交接。'
    } else {
      cameraErrorTitle.value = '摄像头启动失败'
      cameraErrorMessage.value =
        '摄像头可能正被其他应用占用，请关闭占用后重试，或切换为「30 秒动态核销码」。'
    }
  } finally {
    cameraStarting.value = false
  }
}

function toggleCameraFacing() {
  facingMode.value = facingMode.value === 'environment' ? 'user' : 'environment'
  startCamera()
}

function startLiveDecodeLoop() {
  if (scanIntervalId !== null) clearInterval(scanIntervalId)

  const BarcodeDetectorCtor = (window as any).BarcodeDetector
  const detector = BarcodeDetectorCtor ? new BarcodeDetectorCtor({ formats: ['qr_code'] }) : null

  scanIntervalId = window.setInterval(async () => {
    if (decodingBusy || submitting.value || !cameraReady.value) return
    const video = videoRef.value
    const canvas = decodeCanvasRef.value
    if (!video || !canvas || video.readyState < 2 || !video.videoWidth || !video.videoHeight) return

    // 校验视频轨必须是存活的实时流
    const videoTrack = activeStream?.getVideoTracks()?.[0]
    if (!videoTrack || videoTrack.readyState !== 'live') return

    decodingBusy = true
    try {
      let decodedText = ''
      if (detector) {
        try {
          const barcodes = await detector.detect(video)
          if (barcodes?.length && barcodes[0].rawValue) {
            decodedText = String(barcodes[0].rawValue).trim()
          }
        } catch {
          // 自动降级到 jsQR 帧解码
        }
      }

      if (!decodedText) {
        const targetW = Math.min(640, video.videoWidth)
        const scale = targetW / video.videoWidth
        const targetH = Math.round(video.videoHeight * scale)
        canvas.width = targetW
        canvas.height = targetH
        const ctx = canvas.getContext('2d', { willReadFrequently: true })
        if (ctx) {
          ctx.drawImage(video, 0, 0, targetW, targetH)
          const imageData = ctx.getImageData(0, 0, targetW, targetH)
          const qr = jsQR(imageData.data, targetW, targetH, { inversionAttempts: 'dontInvert' })
          if (qr?.data) {
            decodedText = qr.data.trim()
          }
        }
      }

      if (decodedText) {
        await handleScannedQrToken(decodedText)
      }
    } finally {
      decodingBusy = false
    }
  }, 140)
}

async function handleScannedQrToken(rawToken: string) {
  const now = Date.now()
  // 同一错误二维码 2.5 秒内防重复刷接口，避免瞬间触发暴力限流
  if (rawToken === lastRejectedToken && now - lastRejectedAt < 2500) {
    return
  }
  if (!rawToken.startsWith('XYF1:')) {
    lastRejectedToken = rawToken
    lastRejectedAt = now
    ElMessage.warning('请扫描学艺坊买家订单页的实时动态核销二维码')
    return
  }

  submitting.value = true
  try {
    const verifiedOrderId = await verifyOfflineQr({
      qrToken: rawToken,
      expectedOrderId: props.orderId || undefined
    })
    stopCamera()
    ElMessage.success('实时扫码核销成功，担保资金已结算至您的钱包')
    emit('verified', verifiedOrderId)
    emit('update:visible', false)
  } catch (err) {
    lastRejectedToken = rawToken
    lastRejectedAt = Date.now()
    notifyFallbackError(err, '扫码核销失败')
  } finally {
    submitting.value = false
  }
}

async function submitCodeVerify() {
  const targetId = effectiveOrderId.value
  if (!targetId) {
    ElMessage.warning('请先选择要核销的线下订单')
    return
  }
  const code = codeInput.value.trim()
  if (!/^\d{6}$/.test(code)) {
    ElMessage.warning('请输入完整的 6 位动态数字核销码')
    return
  }

  submitting.value = true
  try {
    await verifyOfflineCode(targetId, code)
    ElMessage.success('当面动态核销码验证通过，资金已结算至您的钱包')
    emit('verified', targetId)
    emit('update:visible', false)
  } catch (err) {
    notifyFallbackError(err, '动态核销码校验失败')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.visible,
  (isOpen) => {
    if (isOpen) {
      activeMode.value = props.initialMode
      codeInput.value = ''
      lastRejectedToken = ''
      selectedOrderId.value = props.orderId || props.pendingOfflineOrders?.[0]?.id
      if (activeMode.value === 'qr') {
        startCamera()
      }
    } else {
      stopCamera()
    }
  }
)

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped lang="scss">
.verify-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 4px;
  border-radius: 12px;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
}

.mode-tab {
  height: 36px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--xyf-ink-2);
  font-size: 13px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &.is-active {
    background: #18181b;
    color: #ffffff;
    font-weight: 650;
    box-shadow: 0 4px 10px -4px rgba(9, 9, 11, 0.28);
  }
}

.scanner-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scanner-stage {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 14px;
  background: #09090b;
  border: 1px solid #27272a;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scanner-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scanner-hud {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12px 14px;
  background: radial-gradient(circle at center, transparent 38%, rgba(9, 9, 11, 0.45) 100%);
  pointer-events: none;
}

.hud-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hud-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 4px 11px;
  border-radius: 999px;
  background: rgba(9, 9, 11, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 11.5px;
  font-weight: 600;
  backdrop-filter: blur(8px);

  .hud-pulse {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #ffffff;
    animation: pulse-blink 1.4s ease-in-out infinite;
  }
}

@keyframes pulse-blink {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

.hud-flip-btn {
  pointer-events: auto;
  height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(9, 9, 11, 0.7);
  color: #ffffff;
  font-size: 11.5px;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    background: rgba(24, 24, 27, 0.9);
  }
}

.hud-reticle {
  position: relative;
  width: 196px;
  height: 196px;
  align-self: center;
}

.reticle-c {
  position: absolute;
  width: 22px;
  height: 22px;
  border-color: #ffffff;
  border-style: solid;

  &.is-tl {
    top: 0;
    left: 0;
    border-width: 3px 0 0 3px;
    border-top-left-radius: 8px;
  }
  &.is-tr {
    top: 0;
    right: 0;
    border-width: 3px 3px 0 0;
    border-top-right-radius: 8px;
  }
  &.is-bl {
    bottom: 0;
    left: 0;
    border-width: 0 0 3px 3px;
    border-bottom-left-radius: 8px;
  }
  &.is-br {
    bottom: 0;
    right: 0;
    border-width: 0 3px 3px 0;
    border-bottom-right-radius: 8px;
  }
}

.reticle-laser {
  position: absolute;
  left: 10px;
  right: 10px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #ffffff, transparent);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.8);
  animation: laser-scan 2.2s ease-in-out infinite;
}

@keyframes laser-scan {
  0%,
  100% {
    top: 12px;
    opacity: 0.35;
  }
  50% {
    top: 180px;
    opacity: 1;
  }
}

.hud-bottom-tip {
  align-self: center;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(9, 9, 11, 0.72);
  color: rgba(255, 255, 255, 0.88);
  font-size: 11.5px;
}

.scanner-fallback {
  padding: 28px 24px;
  text-align: center;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;

  .fallback-title {
    font-size: 15px;
    font-weight: 700;
    color: #ffffff;
  }

  .fallback-desc {
    margin: 0;
    max-width: 340px;
    font-size: 12.5px;
    line-height: 1.65;
    color: rgba(255, 255, 255, 0.68);
  }
}

.fallback-actions {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.fb-btn-primary {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid #ffffff;
  background: #ffffff;
  color: #18181b;
  font-size: 12.5px;
  font-weight: 650;
  font-family: inherit;
  cursor: pointer;
}

.fb-btn-secondary {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: transparent;
  color: #ffffff;
  font-size: 12.5px;
  font-family: inherit;
  cursor: pointer;
}

.decode-canvas {
  display: none;
}

.code-input-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
}

.order-picker-field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .field-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
  }
}

.empty-offline-hint {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 13px;
  line-height: 1.6;
  color: var(--xyf-ink-2);
}

.code-guide-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--xyf-ink-2);
}

.verify-code-input :deep(.el-input__inner) {
  height: 48px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.35em;
  text-align: center;

  &::placeholder {
    font-size: 14px;
    font-weight: 400;
    letter-spacing: 0.02em;
  }
}

.anti-fraud-note {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--xyf-ink-3);
}
</style>
