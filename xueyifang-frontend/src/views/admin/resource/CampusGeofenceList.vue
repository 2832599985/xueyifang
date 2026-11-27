<template>
  <div class="admin-campus-page">
    <!-- Platform Commission Revenue KPI Banner -->
    <div class="kpi-strip">
      <div class="kpi-card">
        <span class="kpi-label">平台累计技术服务费分账收入</span>
        <strong class="kpi-value xyf-tabular">¥{{ Number(revenueSummary.totalCommissionRevenue || 0).toFixed(2) }}</strong>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">已完成分账结算订单笔数</span>
        <strong class="kpi-value xyf-tabular">{{ revenueSummary.settledOrderCount || 0 }} 笔</strong>
      </div>
      <div class="kpi-card kpi-card--action">
        <div>
          <span class="kpi-label">多校区真实地图电子围栏治理</span>
          <p class="kpi-desc">在真实高德校园地图上可视化点选勾勒多边形安全电子围栏与教育邮箱域名</p>
        </div>
        <el-button type="primary" @click="openEditDialog()">+ 新增开放校区</el-button>
      </div>
    </div>

    <div class="xyf-surface-card table-card">
      <el-table :data="campuses" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="campusCode" label="校区编码" width="140" />
        <el-table-column prop="campusName" label="高校与校区名称" min-width="200" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="中心坐标 (GCJ-02)" width="210">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.centerLat }}, {{ row.centerLng }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safeRadiusMeters" label="兜底半径(m)" width="120" />
        <el-table-column prop="eduEmailDomains" label="实名邮箱后缀" width="150" />
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '运营中' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">地图编辑围栏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '在真实校园地图编辑校区电子围栏' : '新增开放校区'" width="720px" destroy-on-close>
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
        <el-form-item label="真实校园地图可视化勾勒多边形电子围栏（点击地图依次追加围栏边界顶点或设中心）">
          <div class="fence-editor-bar">
            <el-radio-group v-model="mapPickMode" size="small">
              <el-radio-button label="vertex">点击地图追加多边形顶点</el-radio-button>
              <el-radio-button label="center">点击地图设置校区中心点</el-radio-button>
            </el-radio-group>
            <div class="fence-btns">
              <el-button size="small" @click="generateRectAroundCenter">按中心生成标准矩形围栏</el-button>
              <el-button size="small" :disabled="polygonVertices.length === 0" @click="undoLastVertex">撤销顶点</el-button>
              <el-button size="small" type="danger" plain :disabled="polygonVertices.length === 0" @click="clearVertices">清空顶点</el-button>
            </div>
          </div>

          <RealCampusMap
            :height="300"
            :interactive="true"
            :latitude="form.centerLat"
            :longitude="form.centerLng"
            :radius-meters="form.safeRadiusMeters || 1800"
            :center-lat="form.centerLat || 30.5128"
            :center-lng="form.centerLng || 114.3562"
            :geofence-polygon="form.geofencePolygon"
            :fence-valid="true"
            style="width: 100%; margin-top: 8px"
            @pick="handleMapClick"
          />

          <div class="vertex-summary xyf-mono">
            当前已标定多边形顶点数：<strong>{{ polygonVertices.length }}</strong> 个（至少 3 个顶点构成闭合电子围栏，少于 3 个时自动按中心安全半径兜底）
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

        <el-form-item label="多边形电子围栏顶点 JSON（已与上方地图实时双向同步）">
          <el-input
            v-model="form.geofencePolygon"
            type="textarea"
            :rows="2"
            placeholder="[[114.3465,30.5050],[114.3665,30.5050],[114.3665,30.5210],[114.3465,30.5210]]"
          />
        </el-form-item>

        <el-form-item label="开放状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常开放</el-radio>
            <el-radio :label="0">暂停开放</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { adminListCampuses, adminSaveCampus, adminGetRevenueSummary } from '@/api/campus'
import { notifyFallbackError } from '@/api/request'
import type { CampusInfo } from '@/types/dict'
import RealCampusMap from '@/components/business/RealCampusMap.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const mapPickMode = ref<'vertex' | 'center'>('vertex')
const campuses = ref<CampusInfo[]>([])
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

.kpi-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.kpi-card {
  padding: 20px 22px;
  border-radius: var(--xyf-radius-md);
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  gap: 6px;

  &--action {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .kpi-label {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
    font-weight: 600;
  }

  .kpi-value {
    font-size: 26px;
    font-weight: 800;
    color: var(--xyf-ink);
  }

  .kpi-desc {
    margin: 4px 0 0;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }
}

.table-card {
  padding: 18px;
}

.row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  color: var(--xyf-ink-muted);
}

@media (max-width: 768px) {
  .row-3 {
    grid-template-columns: 1fr;
  }
}
</style>
