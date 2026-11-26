<template>
  <div class="campus-map-picker">
    <div class="picker-toolbar">
      <div class="campus-tag">
        <span class="dot" :class="{ 'is-ok': fenceValid, 'is-err': !fenceValid }" />
        <span>{{ activeCampus?.campusName || '主校区安全电子围栏' }}</span>
        <small class="fence-hint">{{ fenceMessage }}</small>
      </div>

      <!-- Preset Safe TradeLocations from Backend Dictionary (with real coordinates & configured radius) -->
      <div class="preset-chips" v-if="availablePresets.length > 0">
        <button
          v-for="spot in availablePresets"
          :key="spot.name"
          type="button"
          class="spot-chip"
          :class="{ 'is-selected': modelValue.poiName === spot.name }"
          @click="selectPresetSpot(spot)"
        >
          {{ spot.name }}（{{ spot.radius }}m内）
        </button>
      </div>
    </div>

    <RealCampusMap
      ref="realMapRef"
      :height="265"
      :interactive="true"
      :latitude="modelValue.latitude"
      :longitude="modelValue.longitude"
      :radius-meters="modelValue.radiusMeters || 80"
      :center-lat="activeCampus?.centerLat || 30.5128"
      :center-lng="activeCampus?.centerLng || 114.3562"
      :geofence-polygon="activeCampus?.geofencePolygon"
      :fence-valid="fenceValid"
      @pick="onMapPick"
    />

    <div class="coord-inputs">
      <div class="radius-inline">
        <span>当面核销有效范围半径：</span>
        <el-slider
          :model-value="modelValue.radiusMeters || 80"
          :min="20"
          :max="300"
          :step="10"
          style="flex: 1"
          @update:model-value="(val: any) => emitUpdate({ radiusMeters: Number(val) })"
        />
        <strong class="xyf-tabular">{{ modelValue.radiusMeters || 80 }} 米内</strong>
      </div>

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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useDictStore } from '@/stores/dict'
import { listCampuses, checkCampusFence } from '@/api/campus'
import type { CampusInfo } from '@/types/dict'
import RealCampusMap from './RealCampusMap.vue'

export interface MapLocationValue {
  latitude?: number
  longitude?: number
  poiName: string
  detailAddress?: string
  radiusMeters?: number
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

const dictStore = useDictStore()
const campuses = ref<CampusInfo[]>([])
const fenceValid = ref(true)
const fenceMessage = ref('位于校园监控与电子围栏保护范围内')
const realMapRef = ref<InstanceType<typeof RealCampusMap> | null>(null)

const activeCampus = computed(() =>
  campuses.value.find(c => c.id === props.campusId) || campuses.value[0] || null
)

const availablePresets = computed(() => {
  const fromDict = (dictStore.tradeLocations || [])
    .filter(l => l.isAvailable === 1 && l.latitude && l.longitude)
    .map(l => ({
      name: l.locationName,
      lat: Number(l.latitude),
      lng: Number(l.longitude),
      note: l.locationAddress || '',
      radius: l.validRadiusMeters || 80
    }))
  if (fromDict.length > 0) return fromDict

  const cLat = activeCampus.value?.centerLat || 30.5128
  const cLng = activeCampus.value?.centerLng || 114.3562
  return [
    { name: '主图书馆正门监控区', lat: Number((cLat + 0.0012).toFixed(6)), lng: Number((cLng - 0.0018).toFixed(6)), note: '一楼门禁外右侧长椅', radius: 60 },
    { name: '中心一食堂南门广场', lat: Number((cLat - 0.0016).toFixed(6)), lng: Number((cLng + 0.0021).toFixed(6)), note: '自助取餐柜旁', radius: 80 },
    { name: '学生活动中心一楼大厅', lat: Number((cLat + 0.0026).toFixed(6)), lng: Number((cLng + 0.0028).toFixed(6)), note: '24h 校园安保值班点旁', radius: 100 }
  ]
})

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

async function selectPresetSpot(spot: { name: string; lat: number; lng: number; note: string; radius: number }) {
  emitUpdate({
    latitude: spot.lat,
    longitude: spot.lng,
    poiName: spot.name,
    detailAddress: spot.note,
    radiusMeters: spot.radius
  })
  realMapRef.value?.focusCoordinate(spot.lat, spot.lng, 17)
  await verifyFence(spot.lat, spot.lng)
}

async function onMapPick(coords: { latitude: number; longitude: number }) {
  emitUpdate({
    latitude: coords.latitude,
    longitude: coords.longitude,
    poiName: props.modelValue.poiName || '校园地图自选会面点',
    radiusMeters: props.modelValue.radiusMeters || 80
  })
  await verifyFence(coords.latitude, coords.longitude)
}

onMounted(async () => {
  await Promise.all([
    listCampuses().then(res => (campuses.value = res)).catch(() => {}),
    dictStore.fetchTradeLocations().catch(() => {})
  ])
  if (!props.modelValue.latitude && availablePresets.value.length > 0) {
    await selectPresetSpot(availablePresets.value[0])
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

.coord-inputs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.radius-inline {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12.5px;
  color: var(--xyf-ink-2);
  padding: 4px 2px;
}
</style>
