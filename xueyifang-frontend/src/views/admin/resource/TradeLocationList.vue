<template>
  <div class="admin-location-page">
    <XyfPageHeader
      kicker="CAMPUS LANDMARKS & GEOFENCE RADIUS"
      title="校内线下交易地点管理"
      subtitle="在真实校园地图上标定安全会面地标，并自由设置每个交接点允许扫码/输码/蓝牙核销的有效米数半径"
    >
      <template #actions>
        <el-button @click="fetchList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          地图打点新增交接点
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- Live Campus Overview Map showing all Trade Locations & their Meter Coverage Rings -->
    <section class="xyf-surface-card overview-map-card">
      <div class="overview-map-head">
        <div>
          <h3>校内预设安全交接点空间分布总览</h3>
          <p>香槟金圆圈代表各点位配置的真实当面核销有效范围（超出设定米数将自动拦截远程核销），点击表格行或图标可聚焦查看</p>
        </div>
        <el-select v-model="selectedCampusId" style="width: 220px" @change="onCampusSwitch">
          <el-option
            v-for="c in campuses"
            :key="c.id"
            :label="c.campusName"
            :value="c.id"
          />
        </el-select>
      </div>

      <RealCampusMap
        ref="overviewMapRef"
        :interactive="false"
        :height="270"
        :center-lat="activeCampus?.centerLat || 30.5128"
        :center-lng="activeCampus?.centerLng || 114.3562"
        :geofence-polygon="activeCampus?.geofencePolygon"
        :overview-markers="overviewMarkers"
        @select-marker="handleSelectMarkerOnOverview"
      />
    </section>

    <section class="xyf-surface-card filter-card">
      <el-input
        v-model="nameLike"
        placeholder="按地标名称搜索..."
        clearable
        class="search-box"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleSearch">检索</el-button>
        </template>
      </el-input>
    </section>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="locations.length === 0"
        title="暂无校内交易地点配置"
        description="点击右上角「地图打点新增交接点」在真实校园地图上标定安全交接点。"
      />

      <el-table v-else :data="locations" stripe @row-click="focusLocationRow">
        <el-table-column prop="id" label="ID" width="72">
          <template #default="{ row }">
            <span class="xyf-mono">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="地点名称与安防" min-width="190">
          <template #default="{ row }">
            <div class="loc-title-cell">
              <strong>{{ row.locationName }}</strong>
              <span v-if="row.hasCctv !== 0" class="cctv-badge">监控区</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="locationAddress" label="具体方位说明" min-width="180" />

        <el-table-column label="真实地图坐标 (GCJ-02)" width="205">
          <template #default="{ row }">
            <span v-if="row.latitude && row.longitude" class="xyf-mono coord-txt">
              {{ Number(row.latitude).toFixed(5) }}, {{ Number(row.longitude).toFixed(5) }}
            </span>
            <span v-else class="unmarked-txt">待地图标定</span>
          </template>
        </el-table-column>

        <el-table-column label="核销有效范围" width="135">
          <template #default="{ row }">
            <span class="radius-pill xyf-tabular">{{ row.validRadiusMeters || 80 }} 米内</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="row.isAvailable === 1 ? 'success' : 'neutral'"
              :label="row.isAvailable === 1 ? '启用中' : '已停用'"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="155" align="right">
          <template #default="{ row }">
            <el-button size="small" plain @click.stop="openEditDialog(row)">地图编辑</el-button>
            <el-button size="small" type="danger" text @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchList"
        />
      </div>
    </section>

    <!-- Create / Edit Dialog with Real Interactive Slippy Map & Adjustable Meter Radius -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑校内交接点坐标与核销半径' : '在真实校园地图上新增交易地点'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="1. 点击或拖动图标在真实校园地图上标定精确交接点（自动校验校区电子围栏）" required>
          <RealCampusMap
            style="width: 100%"
            :height="280"
            :interactive="true"
            :latitude="form.latitude"
            :longitude="form.longitude"
            :radius-meters="form.validRadiusMeters"
            :center-lat="activeCampus?.centerLat || 30.5128"
            :center-lng="activeCampus?.centerLng || 114.3562"
            :geofence-polygon="activeCampus?.geofencePolygon"
            :fence-valid="fenceValid"
            @pick="onDialogMapPick"
          />
          <div class="fence-tip" :class="{ 'is-err': !fenceValid }">
            {{ fenceMessage }}
          </div>
        </el-form-item>

        <el-form-item label="2. 允许当面核销的有效范围（米）—— 超出该半径将拦截远程输码/扫码/蓝牙核销">
          <div class="radius-control-box">
            <div class="radius-presets">
              <button
                v-for="preset in [30, 50, 80, 120, 200]"
                :key="preset"
                type="button"
                class="preset-btn"
                :class="{ 'is-active': form.validRadiusMeters === preset }"
                @click="form.validRadiusMeters = preset"
              >
                {{ preset }} 米内
              </button>
            </div>
            <div class="slider-row">
              <el-slider
                v-model="form.validRadiusMeters"
                :min="10"
                :max="500"
                :step="5"
                style="flex: 1"
              />
              <el-input-number
                v-model="form.validRadiusMeters"
                :min="10"
                :max="2000"
                :step="10"
                style="width: 135px"
              />
              <span class="unit-lbl">米</span>
            </div>
          </div>
        </el-form-item>

        <div class="form-grid-2">
          <el-form-item label="地标名称" prop="locationName">
            <el-input v-model="form.locationName" placeholder="例如：主图书馆正门一楼大厅" />
          </el-form-item>

          <el-form-item label="具体方位补充" prop="locationAddress">
            <el-input v-model="form.locationAddress" placeholder="例如：东侧刷卡闸机外长椅" />
          </el-form-item>
        </div>

        <div class="form-grid-2">
          <el-form-item label="安防监控覆盖">
            <el-radio-group v-model="form.hasCctv">
              <el-radio :label="1">校园监控覆盖安全区</el-radio>
              <el-radio :label="0">普通公共区域</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="启用状态" prop="isAvailable">
            <el-radio-group v-model="form.isAvailable">
              <el-radio :label="1">开放使用</el-radio>
              <el-radio :label="0">暂不选用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="交接提示 / 开放时段说明" prop="locationDescription">
          <el-input
            v-model="form.locationDescription"
            type="textarea"
            :rows="2"
            maxlength="200"
            placeholder="例如：每日 08:00-22:30 开放，人流充足，安保值班亭旁"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!fenceValid || !form.latitude || !form.longitude"
          :loading="submitting"
          @click="handleSave"
        >
          保存坐标与半径配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import { listCampuses, checkCampusFence } from '@/api/campus'
import {
  listTradeLocations,
  addTradeLocation,
  updateTradeLocation,
  deleteTradeLocation
} from '@/api/tradeLocation'
import { notifyFallbackError } from '@/api/request'
import type { TradeLocation, CampusInfo } from '@/types/dict'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import RealCampusMap, { type OverviewLocationMarker } from '@/components/business/RealCampusMap.vue'

const dictStore = useDictStore()

const loading = ref(true)
const submitting = ref(false)
const nameLike = ref('')
const locations = ref<TradeLocation[]>([])
const campuses = ref<CampusInfo[]>([])
const selectedCampusId = ref<number>(1)
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15

const overviewMapRef = ref<InstanceType<typeof RealCampusMap> | null>(null)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const fenceValid = ref(true)
const fenceMessage = ref('已落在本校区安全电子围栏范围内')

const activeCampus = computed(() =>
  campuses.value.find(c => c.id === selectedCampusId.value) || campuses.value[0] || null
)

const overviewMarkers = computed<OverviewLocationMarker[]>(() =>
  locations.value
    .filter(loc => Boolean(loc.latitude && loc.longitude))
    .map(loc => ({
      id: loc.id,
      name: loc.locationName,
      lat: Number(loc.latitude),
      lng: Number(loc.longitude),
      radiusMeters: loc.validRadiusMeters || 80,
      isAvailable: loc.isAvailable
    }))
)

const form = reactive({
  campusId: 1,
  locationName: '',
  locationAddress: '',
  locationDescription: '',
  latitude: undefined as number | undefined,
  longitude: undefined as number | undefined,
  validRadiusMeters: 80,
  hasCctv: 1,
  isAvailable: 1
})

const rules: FormRules = {
  locationName: [{ required: true, message: '请输入地标名称', trigger: 'blur' }],
  locationAddress: [{ required: true, message: '请输入具体方位说明', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listTradeLocations({
      pageNum: pageNum.value,
      pageSize,
      nameLike: nameLike.value.trim() || undefined
    })
    locations.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取校内交易地点失败')
    locations.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onCampusSwitch() {
  if (activeCampus.value && overviewMapRef.value) {
    overviewMapRef.value.focusCoordinate(activeCampus.value.centerLat, activeCampus.value.centerLng, 16)
  }
}

function focusLocationRow(row: TradeLocation) {
  if (row.latitude && row.longitude && overviewMapRef.value) {
    overviewMapRef.value.focusCoordinate(Number(row.latitude), Number(row.longitude), 17)
  }
}

function handleSelectMarkerOnOverview(id: number) {
  const found = locations.value.find(l => l.id === id)
  if (found) {
    openEditDialog(found)
  }
}

async function onDialogMapPick(coords: { latitude: number; longitude: number }) {
  form.latitude = coords.latitude
  form.longitude = coords.longitude
  try {
    const res = await checkCampusFence(form.campusId || 1, coords.latitude, coords.longitude)
    fenceValid.value = res.inside
    fenceMessage.value = res.message
  } catch {
    fenceValid.value = true
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

function openCreateDialog() {
  editingId.value = null
  form.campusId = selectedCampusId.value || 1
  form.locationName = ''
  form.locationAddress = ''
  form.locationDescription = ''
  form.latitude = activeCampus.value?.centerLat || 30.5128
  form.longitude = activeCampus.value?.centerLng || 114.3562
  form.validRadiusMeters = 80
  form.hasCctv = 1
  form.isAvailable = 1
  fenceValid.value = true
  fenceMessage.value = '请在地图上点击或拖拽图标标定交接点坐标'
  dialogVisible.value = true
}

function openEditDialog(row: TradeLocation) {
  editingId.value = row.id
  form.campusId = row.campusId || selectedCampusId.value || 1
  form.locationName = row.locationName || ''
  form.locationAddress = row.locationAddress || ''
  form.locationDescription = row.locationDescription || ''
  form.latitude = row.latitude ? Number(row.latitude) : (activeCampus.value?.centerLat || 30.5128)
  form.longitude = row.longitude ? Number(row.longitude) : (activeCampus.value?.centerLng || 114.3562)
  form.validRadiusMeters = row.validRadiusMeters || 80
  form.hasCctv = row.hasCctv ?? 1
  form.isAvailable = row.isAvailable ?? 1
  fenceValid.value = true
  fenceMessage.value = '可直接在上方真实地图拖动图标或调整下方有效半径'
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value || submitting.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.latitude || !form.longitude) {
    ElMessage.warning('请在地图上点击标定交接点坐标')
    return
  }

  submitting.value = true
  try {
    const payload = {
      campusId: form.campusId,
      locationName: form.locationName.trim(),
      locationAddress: form.locationAddress.trim(),
      locationDescription: form.locationDescription.trim() || undefined,
      latitude: form.latitude,
      longitude: form.longitude,
      validRadiusMeters: form.validRadiusMeters,
      hasCctv: form.hasCctv,
      isAvailable: form.isAvailable
    }
    if (editingId.value) {
      await updateTradeLocation({ id: editingId.value, ...payload })
      ElMessage.success('交接点地图坐标与有效核销半径已更新')
    } else {
      await addTradeLocation(payload)
      ElMessage.success('已新增真实地图交接点')
    }
    dialogVisible.value = false
    await Promise.all([fetchList(), dictStore.fetchTradeLocations(true).catch(() => {})])
  } catch (err) {
    notifyFallbackError(err, '保存地点失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: TradeLocation) {
  try {
    await ElMessageBox.confirm(
      `确认删除交易地点「${row.locationName}」吗？`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteTradeLocation(row.id)
    ElMessage.success('已删除地点')
    await Promise.all([fetchList(), dictStore.fetchTradeLocations(true).catch(() => {})])
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '删除地点失败')
  }
}

onMounted(async () => {
  const [campusList] = await Promise.all([
    listCampuses().catch(() => [] as CampusInfo[]),
    fetchList()
  ])
  campuses.value = campusList
  if (campusList.length > 0) {
    selectedCampusId.value = campusList[0].id
  }
})
</script>

<style scoped lang="scss">
.overview-map-card {
  padding: 18px 22px;
  margin-bottom: 18px;
}

.overview-map-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;

  h3 {
    margin: 0 0 4px;
    font-size: 15.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.filter-card {
  padding: 14px 20px;
  margin-bottom: 18px;
}

.search-box {
  width: 320px;
  max-width: 100%;
}

.table-card {
  padding: 20px 24px;
}

.loc-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .cctv-badge {
    font-size: 11px;
    font-weight: 600;
    padding: 1px 7px;
    border-radius: 999px;
    background: #faf6ef;
    color: #9a5b13;
    border: 1px solid #e8dac4;
  }
}

.coord-txt {
  font-size: 12px;
  color: var(--xyf-ink-2);
}

.unmarked-txt {
  font-size: 12px;
  color: var(--xyf-warning);
}

.radius-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  color: #18181b;
  font-size: 12.5px;
  font-weight: 650;
}

.fence-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #18181b;
  &.is-err {
    color: #dc2626;
    font-weight: 600;
  }
}

.radius-control-box {
  width: 100%;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.radius-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-btn {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  font-size: 12px;
  color: var(--xyf-ink-2);
  cursor: pointer;

  &.is-active {
    background: #18181b;
    color: #ffffff;
    border-color: #18181b;
  }
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 14px;

  .unit-lbl {
    font-size: 13px;
    color: var(--xyf-ink-muted);
  }
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
