<template>
  <div class="admin-sys-config">
    <XyfPageHeader
      kicker="PLATFORM SETTINGS"
      title="系统参数配置与学籍导入"
      subtitle="控制校园平台注册开关、动态系统参数及管理员批量导入学生账号"
    >
      <template #actions>
        <el-button @click="fetchConfigs" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新配置
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- Registration Switch Quick Card -->
    <section class="xyf-surface-card switch-card" v-if="registerConfigItem">
      <div class="switch-card__info">
        <div class="switch-head">
          <h3>用户自助注册通道开关</h3>
          <XyfStatusBadge
            :tone="isConfigTruthy(registerConfigItem.configValue) ? 'success' : 'warning'"
            :label="isConfigTruthy(registerConfigItem.configValue) ? '当前开放注册' : '当前已关闭注册'"
          />
        </div>
        <p>
          开启后，同学可在登录/注册页填写学号自主完成注册；关闭后仅管理员可通过下方「学籍批量导入」开通新账号。
        </p>
      </div>

      <el-switch
        :model-value="isConfigTruthy(registerConfigItem.configValue)"
        :loading="switchUpdating"
        active-text="开放注册"
        inactive-text="暂停注册"
        @change="handleToggleRegister"
      />
    </section>

    <div class="settings-grid">
      <!-- All Sys Configs Table -->
      <section class="xyf-surface-card panel-card">
        <div class="xyf-section-heading">
          <h2>系统运行时参数列表</h2>
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />

        <XyfEmptyState v-else-if="configs.length === 0" title="暂无系统配置项" />

        <el-table v-else :data="configs" stripe>
          <el-table-column prop="configKey" label="配置键" min-width="180">
            <template #default="{ row }">
              <div class="xyf-mono">{{ row.configKey }}</div>
              <div class="config-label-sub" v-if="CONFIG_KEY_LABELS[row.configKey]">
                {{ CONFIG_KEY_LABELS[row.configKey] }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="configValue" label="当前值" width="120">
            <template #default="{ row }">
              <span class="xyf-mono">{{ row.configValue }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="description" label="参数说明" min-width="190">
            <template #default="{ row }">
              <span>{{ row.description || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="90" align="right">
            <template #default="{ row }">
              <el-button size="small" plain @click="openEditConfig(row)">修改</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <!-- Batch Student Import -->
      <section class="xyf-surface-card panel-card">
        <div class="xyf-section-heading">
          <h2>批量导入校园学生账号</h2>
        </div>

        <p class="import-desc">
          支持上传 `.xlsx` 或 `.csv` 格式学籍名单文件，系统将自动校重、分配初始密码并创建校园钱包账户。
        </p>

        <div class="template-downloads">
          <a :href="getImportTemplateUrl('xlsx')" target="_blank" class="tpl-link">
            <el-icon><Download /></el-icon>
            下载 Excel 导入模版 (.xlsx)
          </a>
          <a :href="getImportTemplateUrl('csv')" target="_blank" class="tpl-link">
            <el-icon><Download /></el-icon>
            下载 CSV 导入模版 (.csv)
          </a>
        </div>

        <el-upload
          class="import-uploader"
          drag
          action="#"
          :show-file-list="false"
          :http-request="handleUploadImportFile"
          accept=".xlsx,.xls,.csv"
        >
          <div class="upload-inner">
            <el-icon class="upload-icon"><Upload /></el-icon>
            <div class="upload-text">
              将学籍文件拖到此处，或 <em>点击选择文件上传</em>
            </div>
            <div class="upload-hint">支持 .xlsx / .csv，单文件最大 10MB</div>
          </div>
        </el-upload>

        <div v-if="importResult" class="import-result-card">
          <h4>最近一次导入汇总</h4>
          <div class="result-pills xyf-tabular">
            <span>总处理：{{ importResult.totalCount }}</span>
            <span class="is-success">成功：{{ importResult.successCount }}</span>
            <span>跳过：{{ importResult.skippedCount }}</span>
            <span class="is-danger">失败：{{ importResult.failedCount }}</span>
          </div>

          <el-table
            v-if="importResult.failedRows && importResult.failedRows.length"
            :data="importResult.failedRows"
            size="small"
            max-height="220"
            style="margin-top: 12px"
          >
            <el-table-column prop="rowNum" label="行号" width="70" />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="reason" label="失败原因" min-width="180" />
          </el-table>
        </div>
      </section>
    </div>

    <!-- Edit Config Modal -->
    <el-dialog v-model="editDialogVisible" title="修改系统参数" width="440px">
      <el-form label-position="top">
        <el-form-item label="配置键">
          <el-input :model-value="editingConfig.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值">
          <el-input v-model="editingConfig.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="参数说明">
          <el-input v-model="editingConfig.description" placeholder="说明用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingConfig" @click="saveEditedConfig">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Refresh, Download, Upload } from '@element-plus/icons-vue'
import { useRegisterConfigStore } from '@/stores/registerConfig'
import {
  listSysConfigs,
  updateSysConfig,
  importUsers,
  getImportTemplateUrl
} from '@/api/sysConfig'
import { notifyFallbackError } from '@/api/request'
import { CONFIG_KEY_LABELS, isConfigTruthy, type SysConfig } from '@/types/sysConfig'
import type { UserImportResultVO } from '@/types/userImport'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const registerConfigStore = useRegisterConfigStore()

const loading = ref(true)
const switchUpdating = ref(false)
const savingConfig = ref(false)
const configs = ref<SysConfig[]>([])
const importResult = ref<UserImportResultVO | null>(null)

const editDialogVisible = ref(false)
const editingConfig = reactive({
  id: 0,
  configKey: '',
  configValue: '',
  description: ''
})

const registerConfigItem = computed(() =>
  configs.value.find((c) => c.configKey === 'REGISTER_ENABLED')
)

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await listSysConfigs({ pageNum: 1, pageSize: 50 })
    configs.value = res.records || []
  } catch (err) {
    notifyFallbackError(err, '获取系统配置失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleRegister(val: string | number | boolean) {
  if (!registerConfigItem.value) return
  switchUpdating.value = true
  try {
    const nextVal = val ? '1' : '0'
    await updateSysConfig({
      id: registerConfigItem.value.id,
      configValue: nextVal,
      description: registerConfigItem.value.description || '是否开放新用户自助注册'
    })
    ElMessage.success(val ? '已开放自助注册' : '已关闭自助注册')
    await Promise.all([
      fetchConfigs(),
      registerConfigStore.fetchRegisterStatus().catch(() => {})
    ])
  } catch (err) {
    notifyFallbackError(err, '切换注册开关失败')
  } finally {
    switchUpdating.value = false
  }
}

function openEditConfig(row: SysConfig) {
  editingConfig.id = row.id
  editingConfig.configKey = row.configKey
  editingConfig.configValue = row.configValue
  editingConfig.description = row.description || ''
  editDialogVisible.value = true
}

async function saveEditedConfig() {
  savingConfig.value = true
  try {
    await updateSysConfig({
      id: editingConfig.id,
      configValue: editingConfig.configValue.trim(),
      description: editingConfig.description.trim() || undefined
    })
    ElMessage.success('系统参数已更新')
    editDialogVisible.value = false
    await Promise.all([
      fetchConfigs(),
      registerConfigStore.fetchRegisterStatus().catch(() => {})
    ])
  } catch (err) {
    notifyFallbackError(err, '更新参数失败')
  } finally {
    savingConfig.value = false
  }
}

async function handleUploadImportFile(options: UploadRequestOptions) {
  const file = options.file as File
  try {
    const res = await importUsers(file)
    importResult.value = res
    ElMessage.success(`批量导入完成：成功 ${res.successCount} 条，失败 ${res.failedCount} 条`)
  } catch (err) {
    notifyFallbackError(err, '批量导入文件处理失败')
  }
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped lang="scss">
.switch-card {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;

  &__info {
    max-width: 680px;

    .switch-head {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 6px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: var(--xyf-ink);
      }
    }

    p {
      margin: 0;
      font-size: 13.5px;
      color: var(--xyf-ink-secondary);
      line-height: 1.6;
    }
  }
}

.settings-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 20px;
  align-items: start;
}

.panel-card {
  padding: 22px 24px;
}

.config-label-sub {
  font-size: 11.5px;
  color: var(--xyf-ink-muted);
  margin-top: 2px;
}

.import-desc {
  margin: 0 0 14px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--xyf-ink-secondary);
}

.template-downloads {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;

  .tpl-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 7px 14px;
    border-radius: var(--xyf-radius-sm);
    border: 1px solid var(--xyf-primary-border);
    background: var(--xyf-primary-soft);
    color: var(--xyf-primary-dark);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;

    &:hover {
      background: var(--xyf-primary);
      color: #fff;
    }
  }
}

.import-uploader {
  :deep(.el-upload-dragger) {
    border-color: var(--xyf-border);
    background: var(--xyf-bg);
    border-radius: var(--xyf-radius-md);

    &:hover {
      border-color: var(--xyf-primary);
    }
  }
}

.upload-inner {
  padding: 12px 0;

  .upload-icon {
    font-size: 32px;
    color: var(--xyf-primary);
    margin-bottom: 8px;
  }

  .upload-text {
    font-size: 14px;
    color: var(--xyf-ink);

    em {
      color: var(--xyf-primary);
      font-style: normal;
      font-weight: 600;
    }
  }

  .upload-hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.import-result-card {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.result-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: var(--xyf-ink-secondary);

  .is-success {
    color: var(--xyf-success);
    font-weight: 650;
  }

  .is-danger {
    color: var(--xyf-danger);
    font-weight: 650;
  }
}

@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
