<template>
  <div class="real-campus-map-wrap">
    <div ref="mapContainerRef" class="leaflet-stage" :style="{ height: `${height}px` }" />
    <div class="map-overlay-bar">
      <span class="coord-chip xyf-mono" v-if="latitude && longitude">
        GCJ-02: {{ Number(latitude).toFixed(6) }}, {{ Number(longitude).toFixed(6) }} · 范围 {{ radiusMeters }}m 内
      </span>
      <span class="coord-chip" v-else>
        点击地图任意教学楼/路口即可标记精确交接坐标
      </span>
      <button v-if="interactive" type="button" class="locate-btn" @click="locateBrowserGps">
        定位当前位置
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

export interface OverviewLocationMarker {
  id: number
  name: string
  lat: number
  lng: number
  radiusMeters: number
  isAvailable?: number
}

const props = withDefaults(
  defineProps<{
    latitude?: number
    longitude?: number
    radiusMeters?: number
    centerLat?: number
    centerLng?: number
    geofencePolygon?: string
    interactive?: boolean
    fenceValid?: boolean
    height?: number
    overviewMarkers?: OverviewLocationMarker[]
  }>(),
  {
    radiusMeters: 80,
    centerLat: 30.5128,
    centerLng: 114.3562,
    interactive: true,
    fenceValid: true,
    height: 300,
    overviewMarkers: () => []
  }
)

const emit = defineEmits<{
  (e: 'pick', payload: { latitude: number; longitude: number }): void
  (e: 'selectMarker', id: number): void
}>()

const mapContainerRef = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let activeMarker: L.Marker | null = null
let activeCircle: L.Circle | null = null
let fenceLayer: L.Polygon | null = null
let overviewGroup: L.LayerGroup | null = null

function createPinIcon(color: string): L.DivIcon {
  return L.divIcon({
    className: 'xyf-map-pin-icon',
    html: `<div style="width:26px;height:26px;border-radius:50% 50% 50% 0;background:${color};transform:rotate(-45deg);border:2.5px solid #fff;box-shadow:0 4px 10px rgba(0,0,0,0.32);display:flex;align-items:center;justify-content:center;"><span style="width:8px;height:8px;border-radius:50%;background:#fff;transform:rotate(45deg);"></span></div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 26]
  })
}

function renderGeofence() {
  if (!map) return
  if (fenceLayer) {
    fenceLayer.remove()
    fenceLayer = null
  }
  if (!props.geofencePolygon) return
  try {
    const pts = JSON.parse(props.geofencePolygon) as number[][]
    if (Array.isArray(pts) && pts.length >= 3) {
      const latLngs: L.LatLngExpression[] = pts.map(([lng, lat]) => [lat, lng])
      fenceLayer = L.polygon(latLngs, {
        color: '#059669',
        weight: 2,
        dashArray: '6 5',
        fillColor: '#10b981',
        fillOpacity: 0.06
      }).addTo(map)
    }
  } catch {
    // ignore malformed polygon
  }
}

function renderOverviewMarkers() {
  if (!map) return
  if (overviewGroup) {
    overviewGroup.clearLayers()
  } else {
    overviewGroup = L.layerGroup().addTo(map)
  }
  for (const m of props.overviewMarkers || []) {
    if (!m.lat || !m.lng) continue
    const enabled = m.isAvailable !== 0
    const color = enabled ? '#18181b' : '#9ca3af'
    const marker = L.marker([m.lat, m.lng], {
      icon: createPinIcon(color)
    })
      .bindTooltip(`${m.name}（${m.radiusMeters || 80}m 内有效）`, { direction: 'top', offset: [0, -22] })
      .on('click', () => emit('selectMarker', m.id))

    const ring = L.circle([m.lat, m.lng], {
      radius: m.radiusMeters || 80,
      color: enabled ? '#059669' : '#9ca3af',
      weight: 1.5,
      fillColor: enabled ? '#10b981' : '#9ca3af',
      fillOpacity: 0.14
    })

    overviewGroup.addLayer(ring)
    overviewGroup.addLayer(marker)
  }
}

function renderActiveSelection(pan = false) {
  if (!map) return
  if (!props.latitude || !props.longitude) {
    if (activeMarker) {
      activeMarker.remove()
      activeMarker = null
    }
    if (activeCircle) {
      activeCircle.remove()
      activeCircle = null
    }
    return
  }

  const latLng: L.LatLngExpression = [Number(props.latitude), Number(props.longitude)]
  const stroke = props.fenceValid ? '#059669' : '#dc2626'
  const pinColor = props.fenceValid ? '#18181b' : '#dc2626'

  if (!activeMarker) {
    activeMarker = L.marker(latLng, {
      draggable: props.interactive,
      icon: createPinIcon(pinColor)
    }).addTo(map)

    activeMarker.on('dragend', () => {
      if (!activeMarker) return
      const pos = activeMarker.getLatLng()
      emit('pick', {
        latitude: Number(pos.lat.toFixed(6)),
        longitude: Number(pos.lng.toFixed(6))
      })
    })
  } else {
    activeMarker.setLatLng(latLng)
    activeMarker.setIcon(createPinIcon(pinColor))
  }

  const r = Math.max(10, Number(props.radiusMeters || 80))
  if (!activeCircle) {
    activeCircle = L.circle(latLng, {
      radius: r,
      color: stroke,
      weight: 2,
      fillColor: stroke,
      fillOpacity: 0.18
    }).addTo(map)
  } else {
    activeCircle.setLatLng(latLng)
    activeCircle.setRadius(r)
    activeCircle.setStyle({ color: stroke, fillColor: stroke })
  }

  if (pan) {
    map.panTo(latLng)
  }
}

function focusCoordinate(lat: number, lng: number, zoom = 17) {
  if (map) {
    map.setView([lat, lng], zoom, { animate: true })
  }
}

function locateBrowserGps() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = Number(pos.coords.latitude.toFixed(6))
      const lng = Number(pos.coords.longitude.toFixed(6))
      emit('pick', { latitude: lat, longitude: lng })
      focusCoordinate(lat, lng, 17)
    },
    () => {
      // ignore if user denies geolocation in desktop browser
    },
    { enableHighAccuracy: true, timeout: 6000 }
  )
}

defineExpose({ focusCoordinate })

onMounted(async () => {
  await nextTick()
  if (!mapContainerRef.value) return

  const initLat = props.latitude || props.centerLat || 30.5128
  const initLng = props.longitude || props.centerLng || 114.3562

  map = L.map(mapContainerRef.value, {
    center: [initLat, initLng],
    zoom: 16,
    zoomControl: true,
    attributionControl: false
  })

  // 高德中文校园/街道矢量瓦片（GCJ-02 坐标系，与微信小程序及高德/腾讯地图完全对齐，零 Key 开箱即用）
  L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 19,
      minZoom: 12
    }
  ).addTo(map)

  if (props.interactive) {
    map.on('click', (e: L.LeafletMouseEvent) => {
      emit('pick', {
        latitude: Number(e.latlng.lat.toFixed(6)),
        longitude: Number(e.latlng.lng.toFixed(6))
      })
    })
  }

  renderGeofence()
  renderOverviewMarkers()
  renderActiveSelection(false)

  // Dialog 动画展开后自动校准瓦片尺寸
  setTimeout(() => {
    map?.invalidateSize()
  }, 220)
})

watch(
  () => [props.latitude, props.longitude, props.radiusMeters, props.fenceValid],
  () => {
    renderActiveSelection(false)
  }
)

watch(
  () => props.geofencePolygon,
  () => {
    renderGeofence()
  }
)

watch(
  () => props.overviewMarkers,
  () => {
    renderOverviewMarkers()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped lang="scss">
.real-campus-map-wrap {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--xyf-border);
  background: #eef2f6;
}

.leaflet-stage {
  width: 100%;
  z-index: 1;
}

.map-overlay-bar {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  z-index: 400;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  pointer-events: none;

  .coord-chip {
    pointer-events: auto;
    padding: 4px 10px;
    border-radius: 999px;
    background: rgba(24, 24, 27, 0.82);
    color: #ffffff;
    font-size: 11.5px;
    backdrop-filter: blur(6px);
  }

  .locate-btn {
    pointer-events: auto;
    padding: 5px 12px;
    border-radius: 999px;
    border: 1px solid #e4e4e7;
    background: #ffffff;
    color: #18181b;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);

    &:hover {
      background: #18181b;
      color: #ffffff;
    }
  }
}
</style>
