<template>
  <div class="real-campus-map-wrap">
    <div ref="mapContainerRef" class="leaflet-stage" :style="{ height: `${height}px` }" />
    <div class="map-overlay-bar">
      <span class="coord-chip xyf-mono" v-if="latitude && longitude">
        <span class="pin-dot" :class="{ 'is-err': !fenceValid }" />
        {{ Number(latitude).toFixed(5) }}, {{ Number(longitude).toFixed(5) }} · 半径 {{ radiusMeters }}m
      </span>
      <span class="coord-chip" v-else>
        点击地图任意教学楼或广场标记交接坐标
      </span>
      <button v-if="interactive" type="button" class="locate-btn" @click="locateBrowserGps">
        当前定位
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
    html: `<div style="width:26px;height:26px;border-radius:50% 50% 50% 0;background:${color};transform:rotate(-45deg);border:2.5px solid #fff;box-shadow:0 6px 14px rgba(9,9,11,0.32);display:flex;align-items:center;justify-content:center;"><span style="width:8px;height:8px;border-radius:50%;background:#e5c79e;transform:rotate(45deg);"></span></div>`,
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
        color: '#18181b',
        weight: 1.8,
        dashArray: '6 5',
        fillColor: '#18181b',
        fillOpacity: 0.04
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
      .bindTooltip(`${m.name}（${m.radiusMeters || 80}m 核销范围）`, { direction: 'top', offset: [0, -22] })
      .on('click', () => emit('selectMarker', m.id))

    const ring = L.circle([m.lat, m.lng], {
      radius: m.radiusMeters || 80,
      color: enabled ? '#18181b' : '#9ca3af',
      weight: 1.5,
      fillColor: enabled ? '#c8a97e' : '#9ca3af',
      fillOpacity: 0.18
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
  const stroke = props.fenceValid ? '#18181b' : '#dc2626'
  const fill = props.fenceValid ? '#c8a97e' : '#dc2626'
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
      weight: 1.8,
      fillColor: fill,
      fillOpacity: 0.22
    }).addTo(map)
  } else {
    activeCircle.setLatLng(latLng)
    activeCircle.setRadius(r)
    activeCircle.setStyle({ color: stroke, fillColor: fill })
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
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--xyf-border);
  background: #f4f4f5;
}

.leaflet-stage {
  width: 100%;
  z-index: 1;
}

.map-overlay-bar {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  z-index: 400;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  pointer-events: none;

  .coord-chip {
    pointer-events: auto;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    border-radius: 999px;
    background: rgba(17, 17, 19, 0.86);
    color: #ffffff;
    font-size: 11.5px;
    backdrop-filter: blur(10px);

    .pin-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #e5c79e;

      &.is-err {
        background: #ef4444;
      }
    }
  }

  .locate-btn {
    pointer-events: auto;
    padding: 5px 13px;
    border-radius: 999px;
    border: 1px solid rgba(24, 24, 27, 0.14);
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(8px);
    color: #18181b;
    font-size: 12px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.18s ease;

    &:hover {
      background: #18181b;
      color: #ffffff;
    }
  }
}
</style>
