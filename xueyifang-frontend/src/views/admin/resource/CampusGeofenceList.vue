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
          <span class="kpi-label">多校区运营治理</span>
          <p class="kpi-desc">配置各校区 GCJ-02 多边形安全电子围栏与允许认证的校园邮箱域名后缀</p>
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
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑围栏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑校区与电子围栏' : '新增开放校区'" width="560px">
      <el-form label-position="top">
        <div class="row-2">
          <el-form-item label="校区编码" required>
            <el-input v-model="form.campusCode" placeholder="如 MAIN_CAMPUS" />
          </el-form-item>
          <el-form-item label="所在城市">
            <el-input v-model="form.city" placeholder="如 武汉市" />
          </el-form-item>
        </div>

        <el-form-item label="高校与校区全称" required>
          <el-input v-model="form.campusName" placeholder="如 示范大学 · 南湖主校区" />
        </el-form-item>

        <div class="row-2">
          <el-form-item label="中心纬度 (GCJ-02)" required>
            <el-input-number v-model="form.centerLat" :precision="6" :step="0.001" style="width: 100%" />
          </el-form-item>
          <el-form-item label="中心经度 (GCJ-02)" required>
            <el-input-number v-model="form.centerLng" :precision="6" :step="0.001" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="安全半径兜底(米)">
            <el-input-number v-model="form.safeRadiusMeters" :min="200" :max="10000" :step="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="允许实名认证邮箱后缀">
            <el-input v-model="form.eduEmailDomains" placeholder="edu.cn, stu.xx.edu.cn" />
          </el-form-item>
        </div>

        <el-form-item label="多边形电子围栏顶点 JSON（格式 [[lng,lat],[lng,lat],...]）">
          <el-input
            v-model="form.geofencePolygon"
            type="textarea"
            :rows="3"
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
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminListCampuses, adminSaveCampus, adminGetRevenueSummary } from '@/api/campus'
import { notifyFallbackError } from '@/api/request'
import type { CampusInfo } from '@/types/dict'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
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
    ElMessage.success('校区电子围栏配置已更新')
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

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
</style>
