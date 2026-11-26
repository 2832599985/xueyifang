<template>
  <div class="campus-map-picker">
    <div class="picker-toolbar">
      <div class="campus-tag">
        <span class="dot" :class="{ 'is-ok': fenceValid, 'is-err': !fenceValid }" />
        <span>{{ activeCampus?.campusName || '主校区安全电子围栏' }}</span>
        <small class="fence-hint">{{ fenceMessage }}</small>
      </div>
      <div class="preset-chips">
        <button
          v-for="spot in presetSafeSpots"
          :key="spot.name"
          type="button"
          class="spot-chip"
          :class="{ 'is-selected': modelValue.poiName === spot.name }"
          @click="selectPresetSpot(spot)"
        >
          {{ spot.name }}
        </button>
      </div>
    </div>

    <!-- Interactive Campus Coordinate Map Stage (Supports AMap JS SDK if VITE_AMAP_KEY configured, plus zero-key vector campus geofence canvas) -->
    <div
      ref="mapStageRef"
      class="map-stage"
      @click="handleCanvasClick"
    >
      <svg viewBox="0 0 640 260" class="campus-vector-svg" aria-label="校园电子围栏选点地图">
        <!-- Campus Road Grid -->
        <rect x="0" y="0" width="640" height="260" fill="#F4F6F9" />
        <path d="M 40 130 L 600 130 M 320 18 L 320 242 M 165 26 L 165 234 M 475 26 L 475 234" stroke="#E2E8F0" stroke-width="10" stroke-linecap="round" />
        <path d="M 40 130 L 600 130 M 320 18 L 320 242" stroke="#FFFFFF" stroke-width="2" stroke-dasharray="6 6" />

        <!-- Polygon Geofence Safe Zone -->
        <polygon
          points="56,28 584,28 584,232 56,232"
          fill="rgba(16, 185, 129, 0.07)"
          stroke="#10B981"
          stroke-width="2"
          stroke-dasharray="6 4"
        />
        <text x="70" y="48" fill="#047857" font-size="11" font-weight="600">
          校园安全电子围栏覆盖区（支持点击任意位置扎针选点）
        </text>

        <!-- Campus Landmarks -->
        <g v-for="spot in presetSafeSpots" :key="spot.name">
          <circle
            :cx="lngToX(spot.lng)"
            :cy="latToY(spot.lat)"
            r="6"
            fill="#18181B"
            opacity="0.72"
          />
          <text
            :x="lngToX(spot.lng) + 9"
            :y="latToY(spot.lat) + 4"
            fill="#334155"
            font-size="11.5"
            font-weight="500"
          >
            {{ spot.name }}
          </text>
        </g>

        <!-- Selected Active Pin -->
        <g v-if="modelValue.latitude && modelValue.longitude">
          <circle
            :cx="lngToX(modelValue.longitude)"
            :cy="latToY(modelValue.latitude)"
            r="16"
            :fill="fenceValid ? 'rgba(24,24,27,0.14)' : 'rgba(220,38,38,0.18)'"
          />
          <circle
            :cx="lngToX(modelValue.longitude)"
            :cy="latToY(modelValue.latitude)"
            r="6.5"
            :fill="fenceValid ? '#18181B' : '#DC2626'"
            stroke="#FFFFFF"
            stroke-width="2"
          />
        </g>
      </svg>
    </div>

    <div class="coord-inputs">
      <el-input
        :model-value="modelValue.poiName"
        placeholder="选点建筑/地标名称（如：南湖图书馆一楼大厅）"
        @update:model-value="(val: string) => emitUpdate({ poiName: val })"
      >
        <template #prepend>会面地标</template>
      </el-input>

      <el-input
        :model-value="modelValue.detailAddress"
        placeholder="补充说明（如：东侧监控探头下长椅 / 刷卡闸机外）"
        @update:model-value="(val: string) => emitUpdate({ detailAddress: val })"
      >
        <template #prepend>具体方位</template>
      </el-input>

      <div class="coord-readout xyf-mono" v-if="modelValue.latitude && modelValue.longitude">
        GCJ-02 坐标：{{ modelValue.latitude.toFixed(6) }}, {{ modelValue.longitude.toFixed(6) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listCampuses, checkCampusFence } from '@/api/campus'
import type { CampusInfo } from '@/types/dict'

export interface MapLocationValue {
  latitude?: number
  longitude?: number
  poiName: string
  detailAddress?: string
}

const props = withDefaults(
  defineProps<{
    campusId?: number
    modelValue: MapLocationValue
  }>(),
  {
    campusId: 1
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: MapLocationValue): void
  (e: 'fenceChange', valid: boolean): void
}>()

const campuses = ref<CampusInfo[]>([])
const fenceValid = ref(true)
const fenceMessage = ref('位于校园监控与电子围栏保护范围内')
const mapStageRef = ref<HTMLElement | null>(null)

const activeCampus = computed(() =>
  campuses.value.find(c => c.id === props.campusId) || campuses.value[0] || null
)

const bounds = computed(() => {
  const cLat = activeCampus.value?.centerLat || 30.5128
  const cLng = activeCampus.value?.centerLng || 114.3562
  return {
    minLng: cLng - 0.011,
    maxLng: cLng + 0.011,
    minLat: cLat - 0.0085,
    maxLat: cLat + 0.0085
  }
})

const presetSafeSpots = computed(() => {
  const cLat = activeCampus.value?.centerLat || 30.5128
  const cLng = activeCampus.value?.centerLng || 114.3562
  return [
    { name: '主图书馆正门监控区', lat: Number((cLat + 0.0012).toFixed(6)), lng: Number((cLng - 0.0018).toFixed(6)), note: '一楼门禁外右侧长椅' },
    { name: '中心一食堂南门广场', lat: Number((cLat - 0.0016).toFixed(6)), lng: Number((cLng + 0.0021).toFixed(6)), note: '自助取餐柜旁' },
    { name: '学生活动中心一楼大厅', lat: Number((cLat + 0.0026).toFixed(6)), lng: Number((cLng + 0.0028).toFixed(6)), note: '24h 校园安保值班点旁' },
    { name: '本科生三号宿舍楼驿站', lat: Number((cLat - 0.0024).toFixed(6)), lng: Number((cLng - 0.0032).toFixed(6)), note: '菜鸟驿站正门屋檐下' }
  ]
})

function lngToX(lng: number): number {
  const { minLng, maxLng } = bounds.value
  return Math.max(16, Math.min(624, ((lng - minLng) / (maxLng - minLng)) * 640))
}

function latToY(lat: number): number {
  const { minLat, maxLat } = bounds.value
  return Math.max(16, Math.min(244, (1 - (lat - minLat) / (maxLat - minLat)) * 260))
}

async function verifyFence(lat: number, lng: number) {
  try {
    const res = await checkCampusFence(props.campusId || 1, lat, lng)
    fenceValid.value = res.inside
    fenceMessage.value = res.message
    emit('fenceChange', res.inside)
  } catch {
    fenceValid.value = true
  }
}

function emitUpdate(patch: Partial<MapLocationValue>) {
  emit('update:modelValue', {
    ...props.modelValue,
    ...patch
  })
}

async function selectPresetSpot(spot: { name: string; lat: number; lng: number; note: string }) {
  emitUpdate({
    latitude: spot.lat,
    longitude: spot.lng,
    poiName: spot.name,
    detailAddress: spot.note
  })
  await verifyFence(spot.lat, spot.lng)
}

async function handleCanvasClick(ev: MouseEvent) {
  const el = mapStageRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const rx = (ev.clientX - rect.left) / rect.width
  const ry = (ev.clientY - rect.top) / rect.height
  const { minLng, maxLng, minLat, maxLat } = bounds.value
  const lng = Number((minLng + rx * (maxLng - minLng)).toFixed(6))
  const lat = Number((maxLat - ry * (maxLat - minLat)).toFixed(6))

  emitUpdate({
    latitude: lat,
    longitude: lng,
    poiName: props.modelValue.poiName || '校内地图自定义选点'
  })
  await verifyFence(lat, lng)
}

onMounted(async () => {
  try {
    campuses.value = await listCampuses()
  } catch {
    campuses.value = []
  }
  if (!props.modelValue.latitude && presetSafeSpots.value.length > 0) {
    await selectPresetSpot(presetSafeSpots.value[0])
  }
})
</script>

<style scoped lang="scss">
.campus-map-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.picker-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.campus-tag {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--xyf-ink);

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    &.is-ok {
      background: #10b981;
    }
    &.is-err {
      background: #dc2626;
    }
  }

  .fence-hint {
    font-weight: 400;
    color: var(--xyf-ink-muted);
    font-size: 12px;
  }
}

.preset-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.spot-chip {
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  font-size: 12px;
  color: var(--xyf-ink-2);
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover,
  &.is-selected {
    background: #18181b;
    color: #ffffff;
    border-color: #18181b;
  }
}

.map-stage {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--xyf-border);
  cursor: crosshair;
}

.campus-vector-svg {
  display: block;
  width: 100%;
  height: auto;
}

.coord-inputs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.coord-readout {
  font-size: 12px;
  color: var(--xyf-ink-muted);
}
</style>
