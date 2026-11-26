<template>
  <div class="admin-location-page">
    <XyfPageHeader
      kicker="CAMPUS LANDMARKS"
      title="校内线下交易地点管理"
      subtitle="配置校内推荐公开会面交接点，维护校园线下协作安全规范"
    >
      <template #actions>
        <el-button @click="fetchList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增交易地点
        </el-button>
      </template>
    </XyfPageHeader>

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
        description="点击右上角「新增交易地点」添加校园公共会面地标。"
      />

      <el-table v-else :data="locations" stripe>
        <el-table-column prop="id" label="ID" width="80">
          <template #default="{ row }">
            <span class="xyf-mono">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="locationName" label="地点名称" min-width="170" />
        <el-table-column prop="locationAddress" label="具体位置" min-width="200" />

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="row.isAvailable === 1 ? 'success' : 'neutral'"
              :label="row.isAvailable === 1 ? '启用中' : '已停用'"
            />
          </template>
        </el-table-column>

        <el-table-column label="说明" min-width="200">
          <template #default="{ row }">
            <span>{{ row.locationDescription || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" text @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑校内交易地点' : '新增校内交易地点'"
      width="460px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="地标名称" prop="locationName">
          <el-input v-model="form.locationName" placeholder="例如：主图书馆一楼大厅服务台" />
        </el-form-item>

        <el-form-item label="具体位置" prop="locationAddress">
          <el-input v-model="form.locationAddress" placeholder="例如：中心广场北侧主楼入口处" />
        </el-form-item>

        <el-form-item label="启用状态" prop="isAvailable">
          <el-radio-group v-model="form.isAvailable">
            <el-radio :label="1">开放使用</el-radio>
            <el-radio :label="0">暂不选用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="交接提示 / 开放时段说明" prop="locationDescription">
          <el-input
            v-model="form.locationDescription"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="例如：每日 08:00-22:00 开放，人流较多、视野开阔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import {
  listTradeLocations,
  addTradeLocation,
  updateTradeLocation,
  deleteTradeLocation
} from '@/api/tradeLocation'
import { notifyFallbackError } from '@/api/request'
import type { TradeLocation } from '@/types/dict'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const dictStore = useDictStore()

const loading = ref(true)
const submitting = ref(false)
const nameLike = ref('')
const locations = ref<TradeLocation[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  locationName: '',
  locationAddress: '',
  locationDescription: '',
  isAvailable: 1
})

const rules: FormRules = {
  locationName: [{ required: true, message: '请输入地标名称', trigger: 'blur' }],
  locationAddress: [{ required: true, message: '请输入具体位置', trigger: 'blur' }]
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

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

function openCreateDialog() {
  editingId.value = null
  form.locationName = ''
  form.locationAddress = ''
  form.locationDescription = ''
  form.isAvailable = 1
  dialogVisible.value = true
}

function openEditDialog(row: TradeLocation) {
  editingId.value = row.id
  form.locationName = row.locationName || ''
  form.locationAddress = row.locationAddress || ''
  form.locationDescription = row.locationDescription || ''
  form.isAvailable = row.isAvailable ?? 1
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value || submitting.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      locationName: form.locationName.trim(),
      locationAddress: form.locationAddress.trim(),
      locationDescription: form.locationDescription.trim() || undefined,
      isAvailable: form.isAvailable
    }
    if (editingId.value) {
      await updateTradeLocation({ id: editingId.value, ...payload })
      ElMessage.success('交易地点已更新')
    } else {
      await addTradeLocation(payload)
      ElMessage.success('已新增校内交易地点')
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

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
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

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
