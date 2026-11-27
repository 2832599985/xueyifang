<template>
  <div class="admin-tag-page">
    <XyfPageHeader title="服务分类管理">
      <template #actions>
        <el-button @click="fetchList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增分类标签
        </el-button>
      </template>
    </XyfPageHeader>

    <section class="xyf-surface-card filter-card">
      <el-input
        v-model="nameLike"
        placeholder="按分类标签名称搜索..."
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
        v-else-if="tags.length === 0"
        title="暂无服务分类标签"
        description="点击右上角「新增分类标签」创建校园服务分类。"
      />

      <el-table v-else :data="tags" stripe>
        <el-table-column prop="id" label="ID" width="80">
          <template #default="{ row }">
            <span class="xyf-mono">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="tagName" label="标签名称" min-width="180" />

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
      :title="editingId ? '编辑服务分类标签' : '新增服务分类标签'"
      width="440px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类标签名称" prop="tagName">
          <el-input v-model="form.tagName" maxlength="30" placeholder="例如：学业辅导 / 跑腿代办 / 摄影修图" />
        </el-form-item>

        <el-form-item label="分类说明（选填）" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="150"
            placeholder="例如：高数、编程、英语四六级及专业课一对一答疑辅导"
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
  listAdminServiceTags,
  addAdminServiceTag,
  updateAdminServiceTag,
  deleteAdminServiceTag
} from '@/api/admin'
import { notifyFallbackError } from '@/api/request'
import type { AdminServiceTag } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const dictStore = useDictStore()

const loading = ref(true)
const submitting = ref(false)
const nameLike = ref('')
const tags = ref<AdminServiceTag[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  tagName: '',
  description: ''
})

const rules: FormRules = {
  tagName: [{ required: true, message: '请输入分类标签名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listAdminServiceTags({
      pageNum: pageNum.value,
      pageSize,
      nameLike: nameLike.value.trim() || undefined
    })
    tags.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取服务标签列表失败')
    tags.value = []
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
  form.tagName = ''
  form.description = ''
  dialogVisible.value = true
}

function openEditDialog(row: AdminServiceTag) {
  editingId.value = row.id
  form.tagName = row.tagName || ''
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
      await updateAdminServiceTag({
        id: editingId.value,
        tagName: form.tagName.trim(),
        description: form.description.trim()
      })
      ElMessage.success('服务分类标签已更新')
    } else {
      await addAdminServiceTag({
        tagName: form.tagName.trim(),
        description: form.description.trim()
      })
      ElMessage.success('已新增服务分类标签')
    }
    dialogVisible.value = false
    await Promise.all([fetchList(), dictStore.fetchServiceTags(true).catch(() => {})])
  } catch (err) {
    notifyFallbackError(err, '保存服务分类标签失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AdminServiceTag) {
  try {
    await ElMessageBox.confirm(
      `确认删除服务分类标签「${row.tagName}」吗？`,
      '删除分类标签',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAdminServiceTag(row.id)
    ElMessage.success('已删除服务分类标签')
    await Promise.all([fetchList(), dictStore.fetchServiceTags(true).catch(() => {})])
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '删除服务分类标签失败')
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
