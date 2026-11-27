<template>
  <div class="campus-map-picker">
    <!-- Top Fence Status & Quick Preset Landmarks -->
    <div class="picker-head">
      <div class="fence-banner" :class="{ 'is-err': !fenceValid }">
        <span class="fence-dot" />
        <strong>{{ activeCampus?.campusName || '校区电子围栏' }}</strong>
        <span class="fence-msg">{{ fenceMessage }}</span>
      </div>

      <div v-if="availablePresets.length > 0" class="landmark-scroll">
        <button
          v-for="spot in availablePresets"
          :key="spot.name"
          type="button"
          class="landmark-chip"
          :class="{ 'is-selected': modelValue.poiName === spot.name }"
          @click="selectPresetSpot(spot)"
        >
          <span class="landmark-chip__name">{{ spot.name }}</span>
          <span class="landmark-chip__rad xyf-tabular">{{ spot.radius }}m</span>
        </button>
      </div>
    </div>

    <RealCampusMap
      ref="realMapRef"
      :height="252"
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

    <!-- Radius Quick Presets + Slider + 2-Column Landmark Inputs -->
    <div class="picker-controls">
      <div class="radius-bar">
        <span class="radius-bar__lbl">核销有效半径</span>
        <div class="radius-quick">
          <button
            v-for="r in [50, 80, 120, 200]"
            :key="r"
            type="button"
            class="r-pill xyf-tabular"
            :class="{ 'is-active': (modelValue.radiusMeters || 80) === r }"
            @click="emitUpdate({ radiusMeters: r })"
          >
            {{ r }}m
          </button>
        </div>
        <el-slider
          :model-value="modelValue.radiusMeters || 80"
          :min="20"
          :max="300"
          :step="10"
          style="flex: 1; min-width: 110px"
          @update:model-value="(val: any) => emitUpdate({ radiusMeters: Number(val) })"
        />
        <strong class="radius-val xyf-tabular">{{ modelValue.radiusMeters || 80 }}m</strong>
      </div>

      <div class="addr-grid">
        <el-input
          :model-value="modelValue.poiName"
          placeholder="会面建筑/地标（如：南湖图书馆一楼大厅）"
          @update:model-value="(val: string) => emitUpdate({ poiName: val })"
        />
        <el-input
          :model-value="modelValue.detailAddress"
          placeholder="具体碰面方位（如：刷卡闸机外右侧长椅）"
          @update:model-value="(val: string) => emitUpdate({ detailAddress: val })"
        />
      </div>
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
  gap: 10px;
}

.picker-head {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fence-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--xyf-ink);

  .fence-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #15803d;
  }

  .fence-msg {
    color: var(--xyf-ink-3);
  }

  &.is-err {
    color: #b91c1c;
    .fence-dot {
      background: #dc2626;
    }
    .fence-msg {
      color: #dc2626;
    }
  }
}

.landmark-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.landmark-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &__name {
    font-weight: 550;
  }

  &__rad {
    font-size: 11px;
    color: var(--xyf-ink-3);
  }

  &:hover {
    border-color: #18181b;
    color: #18181b;
  }

  &.is-selected {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;

    .landmark-chip__rad {
      color: #e5c79e;
    }
  }
}

.picker-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
}

.radius-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  &__lbl {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }

  .radius-quick {
    display: flex;
    gap: 4px;
  }

  .r-pill {
    padding: 2px 8px;
    border-radius: 6px;
    border: 1px solid var(--xyf-border);
    background: #ffffff;
    font-size: 11.5px;
    font-weight: 600;
    color: var(--xyf-ink-2);
    cursor: pointer;

    &.is-active {
      background: #18181b;
      border-color: #18181b;
      color: #ffffff;
    }
  }

  .radius-val {
    font-size: 13px;
    color: var(--xyf-ink);
    min-width: 42px;
    text-align: right;
  }
}

.addr-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}
</style>
