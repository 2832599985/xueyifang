<template>
  <div class="admin-campus-page">
    <XyfPageHeader title="校区与围栏管理" dense>
      <template #actions>
        <el-button :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openEditDialog()">
          <el-icon><Plus /></el-icon>
          新增校区
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- 3 KPI Tiles -->
    <section class="kpi-row">
      <XyfStatTile
        label="累计服务费收入"
        :value="`¥${Number(revenueSummary.totalCommissionRevenue || 0).toFixed(2)}`"
        hint="已完成订单分成累计"
      />
      <XyfStatTile
        label="已结算订单"
        :value="`${revenueSummary.settledOrderCount || 0} 笔`"
        hint="全部校区已结算订单"
      />
      <XyfStatTile
        label="已配置校区"
        :value="`${campuses.length} 个校区`"
        hint="支持围栏范围与安全半径"
      />
    </section>

    <!-- Master-Detail Spatial Governance Workbench -->
    <section class="spatial-workbench">
      <!-- Left: Campus Selector List -->
      <div class="campus-list-col">
        <div
          v-for="item in campuses"
          :key="item.id"
          class="xyf-surface-card campus-card"
          :class="{ 'is-selected': selectedCampus?.id === item.id }"
          @click="selectCampusCard(item)"
        >
          <div class="campus-card__top">
            <div>
              <span class="c-code xyf-mono">{{ item.campusCode }}</span>
              <h3>{{ item.campusName }}</h3>
            </div>
            <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">
              {{ item.status === 1 ? '运营中' : '已暂停' }}
            </el-tag>
          </div>

          <div class="campus-card__meta xyf-tabular">
            <span>城市：{{ item.city || '未填写' }}</span>
            <span>·</span>
            <span>兜底半径：{{ item.safeRadiusMeters }}m</span>
          </div>

          <div class="campus-card__foot">
            <span class="edu-domain xyf-mono">邮箱后缀：{{ item.eduEmailDomains || 'edu.cn' }}</span>
            <el-button size="small" plain @click.stop="openEditDialog(item)">
              编辑围栏多边形
            </el-button>
          </div>
        </div>
      </div>

      <!-- Right: Live Polygon Geofence Map Stage -->
      <div class="xyf-surface-card map-stage-col">
        <div class="map-stage-head">
          <div>
            <h3>{{ selectedCampus?.campusName || '选择左侧校区预览电子围栏' }}</h3>
          </div>
          <el-button
            v-if="selectedCampus"
            size="small"
            type="primary"
            @click="openEditDialog(selectedCampus)"
          >
            进入地图顶点编辑器
          </el-button>
        </div>

        <RealCampusMap
          ref="previewMapRef"
          :height="390"
          :interactive="false"
          :latitude="selectedCampus?.centerLat || 30.5128"
          :longitude="selectedCampus?.centerLng || 114.3562"
          :radius-meters="selectedCampus?.safeRadiusMeters || 1800"
          :center-lat="selectedCampus?.centerLat || 30.5128"
          :center-lng="selectedCampus?.centerLng || 114.3562"
          :geofence-polygon="selectedCampus?.geofencePolygon"
          :fence-valid="true"
        />
      </div>
    </section>

    <!-- Interactive Polygon Fence Editor Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '在真实校园地图编辑校区电子围栏' : '新增开放校区与电子围栏'"
      width="740px"
      destroy-on-close
    >
      <el-form label-position="top">
        <div class="row-3">
          <el-form-item label="校区编码" required>
            <el-input v-model="form.campusCode" placeholder="如 MAIN_CAMPUS" />
          </el-form-item>
          <el-form-item label="所在城市">
            <el-input v-model="form.city" placeholder="如 武汉市" />
          </el-form-item>
          <el-form-item label="允许实名认证邮箱后缀">
            <el-input v-model="form.eduEmailDomains" placeholder="edu.cn, stu.xx.edu.cn" />
          </el-form-item>
        </div>

        <el-form-item label="高校与校区全称" required>
          <el-input v-model="form.campusName" placeholder="如 示范大学 · 南湖主校区" />
        </el-form-item>

        <!-- Interactive Map Polygon Fence Editor -->
        <el-form-item label="真实校园地图可视化勾勒多边形电子围栏">
          <div class="fence-editor-bar">
            <div class="xyf-segmented">
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': mapPickMode === 'vertex' }"
                @click="mapPickMode = 'vertex'"
              >
                点击地图追加多边形顶点
              </button>
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': mapPickMode === 'center' }"
                @click="mapPickMode = 'center'"
              >
                点击地图设置校区中心点
              </button>
            </div>

            <div class="fence-btns">
              <el-button size="small" @click="generateRectAroundCenter">一键生成矩形围栏</el-button>
              <el-button size="small" :disabled="polygonVertices.length === 0" @click="undoLastVertex">撤销顶点</el-button>
              <el-button size="small" type="danger" plain :disabled="polygonVertices.length === 0" @click="clearVertices">清空</el-button>
            </div>
          </div>

          <RealCampusMap
            :height="290"
            :interactive="true"
            :latitude="form.centerLat"
            :longitude="form.centerLng"
            :radius-meters="form.safeRadiusMeters || 1800"
            :center-lat="form.centerLat || 30.5128"
            :center-lng="form.centerLng || 114.3562"
            :geofence-polygon="form.geofencePolygon"
            :fence-valid="true"
            style="width: 100%; margin-top: 10px"
            @pick="handleMapClick"
          />

          <div class="vertex-summary xyf-mono">
            已标定多边形顶点：<strong>{{ polygonVertices.length }}</strong> 个（满 3 个顶点构成闭合围栏，不足 3 个自动按中心安全半径兜底）
          </div>
        </el-form-item>

        <div class="row-3">
          <el-form-item label="中心纬度 (GCJ-02)" required>
            <el-input-number v-model="form.centerLat" :precision="6" :step="0.001" style="width: 100%" />
          </el-form-item>
          <el-form-item label="中心经度 (GCJ-02)" required>
            <el-input-number v-model="form.centerLng" :precision="6" :step="0.001" style="width: 100%" />
          </el-form-item>
          <el-form-item label="安全半径兜底(米)">
            <el-input-number v-model="form.safeRadiusMeters" :min="200" :max="10000" :step="100" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="多边形顶点 JSON（实时双向同步）">
            <el-input v-model="form.geofencePolygon" placeholder="[[lng,lat],...]" />
          </el-form-item>

          <el-form-item label="开放状态">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">正常开放运营</el-radio>
              <el-radio :label="0">暂停开放</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存校区围栏配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { adminListCampuses, adminSaveCampus, adminGetRevenueSummary } from '@/api/campus'
import { notifyFallbackError } from '@/api/request'
import type { CampusInfo } from '@/types/dict'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import RealCampusMap from '@/components/business/RealCampusMap.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const mapPickMode = ref<'vertex' | 'center'>('vertex')
const campuses = ref<CampusInfo[]>([])
const selectedCampus = ref<CampusInfo | null>(null)
const previewMapRef = ref<InstanceType<typeof RealCampusMap> | null>(null)

const revenueSummary = ref({
  totalCommissionRevenue: 0,
  settledOrderCount: 0
})

const form = ref<Partial<CampusInfo>>({
  campusCode: '',
  campusName: '',
  city: '武汉市',
  centerLat: 30.5128,
  centerLng: 114.3562,
  safeRadiusMeters: 1800,
  eduEmailDomains: 'edu.cn',
  geofencePolygon: '',
  status: 1
})

const polygonVertices = computed<Array<[number, number]>>(() => {
  if (!form.value.geofencePolygon) return []
  try {
    const arr = JSON.parse(form.value.geofencePolygon)
    if (Array.isArray(arr)) {
      return arr.filter(pt => Array.isArray(pt) && pt.length >= 2).map(pt => [Number(pt[0]), Number(pt[1])])
    }
  } catch {
    // ignore malformed json while typing
  }
  return []
})

function selectCampusCard(item: CampusInfo) {
  selectedCampus.value = item
  if (item.centerLat && item.centerLng) {
    previewMapRef.value?.focusCoordinate(item.centerLat, item.centerLng, 15)
  }
}

function syncVerticesToJson(pts: Array<[number, number]>) {
  form.value.geofencePolygon = pts.length > 0 ? JSON.stringify(pts) : ''
}

function handleMapClick(coords: { latitude: number; longitude: number }) {
  if (mapPickMode.value === 'center') {
    form.value.centerLat = coords.latitude
    form.value.centerLng = coords.longitude
    return
  }
  const next = [...polygonVertices.value, [coords.longitude, coords.latitude] as [number, number]]
  syncVerticesToJson(next)
}

function undoLastVertex() {
  const next = polygonVertices.value.slice(0, -1)
  syncVerticesToJson(next)
}

function clearVertices() {
  syncVerticesToJson([])
}

function generateRectAroundCenter() {
  const lat = form.value.centerLat || 30.5128
  const lng = form.value.centerLng || 114.3562
  const dLat = 0.008
  const dLng = 0.01
  const rect: Array<[number, number]> = [
    [Number((lng - dLng).toFixed(6)), Number((lat - dLat).toFixed(6))],
    [Number((lng + dLng).toFixed(6)), Number((lat - dLat).toFixed(6))],
    [Number((lng + dLng).toFixed(6)), Number((lat + dLat).toFixed(6))],
    [Number((lng - dLng).toFixed(6)), Number((lat + dLat).toFixed(6))]
  ]
  syncVerticesToJson(rect)
}

async function loadData() {
  loading.value = true
  try {
    const [list, rev] = await Promise.all([adminListCampuses(), adminGetRevenueSummary()])
    campuses.value = list
    revenueSummary.value = rev
    if (list.length > 0 && !selectedCampus.value) {
      selectedCampus.value = list[0]
    } else if (selectedCampus.value) {
      selectedCampus.value = list.find(c => c.id === selectedCampus.value?.id) || list[0] || null
    }
  } catch (err) {
    notifyFallbackError(err, '加载多校区与分账数据失败')
  } finally {
    loading.value = false
  }
}

function openEditDialog(row?: CampusInfo) {
  mapPickMode.value = 'vertex'
  if (row) {
    form.value = { ...row }
  } else {
    form.value = {
      campusCode: '',
      campusName: '',
      city: '武汉市',
      centerLat: 30.5128,
      centerLng: 114.3562,
      safeRadiusMeters: 1800,
      eduEmailDomains: 'edu.cn',
      geofencePolygon: '',
      status: 1
    }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.campusCode || !form.value.campusName) {
    ElMessage.warning('请填写校区编码与名称')
    return
  }
  saving.value = true
  try {
    await adminSaveCampus(form.value)
    ElMessage.success('校区真实地图电子围栏配置已更新')
    dialogVisible.value = false
    await loadData()
  } catch (err) {
    notifyFallbackError(err, '保存校区失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.admin-campus-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;

  @media (max-width: 820px) {
    grid-template-columns: 1fr;
  }
}

/* Master-Detail Spatial Workbench */
.spatial-workbench {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 18px;
  align-items: start;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.campus-list-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.campus-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
  }

  &.is-selected {
    border-color: #18181b;
    box-shadow: 0 0 0 1px #18181b inset, var(--xyf-shadow-sm);
  }

  &__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;

    .c-code {
      font-size: 11px;
      color: var(--xyf-ink-3);
    }

    h3 {
      margin: 2px 0 0;
      font-size: 16px;
      font-weight: 700;
      color: var(--xyf-ink);
    }
  }

  &__meta {
    display: flex;
    gap: 6px;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  &__foot {
    padding-top: 10px;
    border-top: 1px solid var(--xyf-border-subtle);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .edu-domain {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.map-stage-col {
  padding: 20px 22px;
}

.map-stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    margin: 3px 0 0;
    font-size: 17px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.row-2 {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 12px;
}

.fence-editor-bar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.fence-btns {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.vertex-summary {
  margin-top: 8px;
  font-size: 12px;
  color: var(--xyf-ink-3);
}

@media (max-width: 768px) {
  .row-3,
  .row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
