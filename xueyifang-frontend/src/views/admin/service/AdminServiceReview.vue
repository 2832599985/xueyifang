<template>
  <div class="admin-service-review">
    <XyfPageHeader
      kicker="SERVICE MODERATION & INSPECTION"
      title="校园服务审核与在架巡检"
      subtitle="审查待上架技能服务，并对全量已上架服务进行合规巡检与违规强制下架"
    >
      <template #actions>
        <el-button @click="refreshActiveList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- Mode Switch & Filters -->
    <section class="xyf-surface-card filter-bar">
      <div class="xyf-tabs">
        <button
          type="button"
          class="xyf-tab"
          :class="{ 'is-active': viewMode === 'pending' }"
          @click="switchMode('pending')"
        >
          待审核队列
        </button>
        <button
          type="button"
          class="xyf-tab"
          :class="{ 'is-active': viewMode === 'all' }"
          @click="switchMode('all')"
        >
          全量服务巡检（支持违规强制下架）
        </button>
      </div>

      <div v-if="viewMode === 'all'" class="filter-inputs">
        <el-select
          v-model="filterStatus"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="handleSearchAll"
        >
          <el-option :value="1" label="已上架 (1)" />
          <el-option :value="2" label="已下架 (2)" />
          <el-option :value="3" label="待审核 (3)" />
          <el-option :value="4" label="已驳回 (4)" />
        </el-select>

        <el-input
          v-model="keyword"
          placeholder="搜索服务标题或描述..."
          clearable
          style="width: 240px"
          @clear="handleSearchAll"
          @keyup.enter="handleSearchAll"
        >
          <template #append>
            <el-button @click="handleSearchAll">搜索</el-button>
          </template>
        </el-input>
      </div>
    </section>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="services.length === 0"
        :title="viewMode === 'pending' ? '当前暂无待审核服务' : '未查询到符合条件的服务'"
        :description="
          viewMode === 'pending'
            ? '所有新发布或修改的服务均已完成审查，可切换至「全量服务巡检」管理在架服务。'
            : '请尝试更换筛选条件或关键词。'
        "
      />

      <el-table v-else :data="services" stripe>
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <XyfImage
              :src="buildImageUrl(row.coverImage)"
              :alt="row.serviceTitle"
              width="82px"
              height="56px"
              :radius="6"
              fit="cover"
            />
          </template>
        </el-table-column>

        <el-table-column label="服务标题与分类" min-width="230">
          <template #default="{ row }">
            <button
              type="button"
              class="svc-title-link"
              @click="router.push(`/admin/services/${row.serviceId}`)"
            >
              {{ row.serviceTitle }}
            </button>
            <div class="svc-meta">
              <span v-if="row.tagName">{{ row.tagName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布者" width="130">
          <template #default="{ row }">
            <span>{{ row.sellerName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" v-if="viewMode === 'all'">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="statusTone(row.status)"
              :label="statusLabel(row.status)"
            />
          </template>
        </el-table-column>

        <el-table-column label="定价" width="110" align="right">
          <template #default="{ row }">
            <strong class="price-text xyf-tabular">{{ formatPrice(row.price) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="155">
          <template #default="{ row }">
            <span class="xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="管理操作" width="240" align="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="router.push(`/admin/services/${row.serviceId}`)"
            >
              详情
            </el-button>

            <template v-if="viewMode === 'pending' || row.reviewStatus === 2">
              <el-button
                size="small"
                type="primary"
                :loading="operatingId === row.serviceId"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                @click="openReject(row, 'reject')"
              >
                驳回
              </el-button>
            </template>

            <el-button
              v-else-if="row.status === 1"
              size="small"
              @click="openReject(row, 'offline')"
            >
              强制下架
            </el-button>
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
          @current-change="refreshActiveList"
        />
      </div>
    </section>

    <el-dialog
      v-model="rejectDialogVisible"
      :title="dialogMode === 'offline' ? '违规服务强制下架' : '驳回服务上架'"
      width="440px"
    >
      <el-form label-position="top">
        <el-form-item
          :label="
            dialogMode === 'offline'
              ? '强制下架原因（将通过系统通知推送给卖家）'
              : '驳回原因（将发送至卖家编辑修正）'
          "
        >
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请指出需整改的问题，例如：图片包含外部诱导信息、服务范围违反校园规定或价格异常..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submittingReject" @click="confirmReject">
          {{ dialogMode === 'offline' ? '确认强制下架' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getPendingServices,
  reviewService,
  listAllAdminServices,
  forceOfflineAdminService
} from '@/api/adminService'
import { buildImageUrl } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTimeMinute } from '@/utils/format'
import type { PendingService } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const viewMode = ref<'pending' | 'all'>('pending')
const filterStatus = ref<number | undefined>(undefined)
const keyword = ref('')

const loading = ref(true)
const operatingId = ref<number | null>(null)
const services = ref<PendingService[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const rejectDialogVisible = ref(false)
const dialogMode = ref<'reject' | 'offline'>('reject')
const targetService = ref<PendingService | null>(null)
const rejectReason = ref('')
const submittingReject = ref(false)

function statusTone(status?: number) {
  if (status === 1) return 'success'
  if (status === 3) return 'warning'
  if (status === 4) return 'danger'
  return 'neutral'
}

function statusLabel(status?: number) {
  if (status === 1) return '已上架'
  if (status === 2) return '已下架'
  if (status === 3) return '待审核'
  if (status === 4) return '已驳回'
  return '未知'
}

function switchMode(mode: 'pending' | 'all') {
  viewMode.value = mode
  pageNum.value = 1
  refreshActiveList()
}

function handleSearchAll() {
  pageNum.value = 1
  refreshActiveList()
}

async function refreshActiveList() {
  loading.value = true
  try {
    if (viewMode.value === 'pending') {
      const res = await getPendingServices(pageNum.value, pageSize)
      services.value = res.records || []
      total.value = res.total || 0
    } else {
      const res = await listAllAdminServices({
        pageNum: pageNum.value,
        pageSize,
        status: filterStatus.value,
        keyword: keyword.value.trim() || undefined
      })
      services.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err) {
    notifyFallbackError(err, '获取服务列表失败')
    services.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: PendingService) {
  try {
    await ElMessageBox.confirm(`确认审核通过并上架「${row.serviceTitle}」吗？`, '审核通过确认', {
      confirmButtonText: '批准上架',
      cancelButtonText: '取消',
      type: 'success'
    })
    operatingId.value = row.serviceId
    await reviewService({ serviceId: row.serviceId, approved: true })
    ElMessage.success('服务已批准上架')
    await refreshActiveList()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '审核操作失败')
  } finally {
    operatingId.value = null
  }
}

function openReject(row: PendingService, mode: 'reject' | 'offline') {
  targetService.value = row
  dialogMode.value = mode
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!targetService.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写原因说明')
    return
  }

  submittingReject.value = true
  try {
    if (dialogMode.value === 'offline') {
      await forceOfflineAdminService(targetService.value.serviceId, rejectReason.value.trim())
      ElMessage.success('已强制下架该服务并通知卖家')
    } else {
      await reviewService({
        serviceId: targetService.value.serviceId,
        approved: false,
        reason: rejectReason.value.trim()
      })
      ElMessage.success('已驳回该服务')
    }
    rejectDialogVisible.value = false
    await refreshActiveList()
  } catch (err) {
    notifyFallbackError(err, '操作失败')
  } finally {
    submittingReject.value = false
  }
}

onMounted(() => {
  refreshActiveList()
})
</script>

<style scoped lang="scss">
.filter-bar {
  padding: 14px 20px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-card {
  padding: 20px 24px;
}

.svc-title-link {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--xyf-ink);
  cursor: pointer;
  text-align: left;

  &:hover {
    color: var(--xyf-primary);
    text-decoration: underline;
  }
}

.svc-meta {
  font-size: 12px;
  color: var(--xyf-ink-muted);
  margin-top: 2px;
}

.price-text {
  color: var(--xyf-accent);
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
