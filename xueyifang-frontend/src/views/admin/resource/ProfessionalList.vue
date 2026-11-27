<template>
  <div class="admin-professional-page">
    <XyfPageHeader title="专业目录管理">
      <template #actions>
        <el-button @click="fetchList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增专业
        </el-button>
      </template>
    </XyfPageHeader>

    <section class="xyf-surface-card filter-card">
      <el-input
        v-model="nameLike"
        placeholder="按专业名称搜索..."
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
        v-else-if="professionals.length === 0"
        title="暂无专业字典数据"
        description="点击右上角「新增专业」添加校园学院与专业选项。"
      />

      <el-table v-else :data="professionals" stripe>
        <el-table-column prop="id" label="ID" width="80">
          <template #default="{ row }">
            <span class="xyf-mono">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="professionalName" label="专业名称" min-width="220" />

        <el-table-column label="描述说明" min-width="260">
          <template #default="{ row }">
            <span>{{ row.description || '-' }}</span>
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
      :title="editingId ? '编辑专业信息' : '新增专业信息'"
      width="440px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="专业名称" prop="professionalName">
          <el-input v-model="form.professionalName" placeholder="例如：计算机科学与技术 / 软件工程" />
        </el-form-item>

        <el-form-item label="专业简介说明（选填）" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="例如：所属信息学院，涵盖算法与系统开发方向"
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
  listProfessionals,
  addProfessional,
  updateProfessional,
  deleteProfessional
} from '@/api/professional'
import { notifyFallbackError } from '@/api/request'
import type { Professional } from '@/types/dict'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const dictStore = useDictStore()

const loading = ref(true)
const submitting = ref(false)
const nameLike = ref('')
const professionals = ref<Professional[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  professionalName: '',
  description: ''
})

const rules: FormRules = {
  professionalName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listProfessionals({
      pageNum: pageNum.value,
      pageSize,
      nameLike: nameLike.value.trim() || undefined
    })
    professionals.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取专业列表失败')
    professionals.value = []
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
  form.professionalName = ''
  form.description = ''
  dialogVisible.value = true
}

function openEditDialog(row: Professional) {
  editingId.value = row.id
  form.professionalName = row.professionalName || ''
  form.description = row.description || ''
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
    if (editingId.value) {
      await updateProfessional({
        id: editingId.value,
        professionalName: form.professionalName.trim(),
        description: form.description.trim() || undefined
      })
      ElMessage.success('专业信息已更新')
    } else {
      await addProfessional({
        professionalName: form.professionalName.trim(),
        description: form.description.trim() || undefined
      })
      ElMessage.success('已新增专业')
    }
    dialogVisible.value = false
    await Promise.all([fetchList(), dictStore.fetchProfessionals(true).catch(() => {})])
  } catch (err) {
    notifyFallbackError(err, '保存专业信息失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Professional) {
  try {
    await ElMessageBox.confirm(
      `确认删除专业「${row.professionalName}」吗？`,
      '删除专业',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteProfessional(row.id)
    ElMessage.success('已删除专业')
    await Promise.all([fetchList(), dictStore.fetchProfessionals(true).catch(() => {})])
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '删除专业失败')
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
