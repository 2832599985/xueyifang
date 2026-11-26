<template>
  <div class="my-services-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="我的服务管理"
        subtitle="集中管理已上架、审核中及下架修正的校园技能服务"
      >
        <template #actions>
          <el-button @click="router.push('/orders/sales')">销售订单</el-button>
          <el-button @click="router.push('/statistics/sales')">销售统计</el-button>
          <el-button type="primary" @click="handlePublishClick">
            <el-icon><Plus /></el-icon>
            发布新服务
          </el-button>
        </template>
      </XyfPageHeader>

      <!-- Status Tabs -->
      <div class="toolbar-row">
        <div class="xyf-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.label"
            type="button"
            class="xyf-tab"
            :class="{ 'is-active': activeStatus === tab.value }"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <span class="total-hint">
          共 <strong class="xyf-tabular">{{ total }}</strong> 个服务
        </span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- Empty -->
      <div v-else-if="services.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="暂无对应状态的服务"
          description="发布一项你的专业技能服务，经过审核即可向全校同学展示。"
        >
          <template #action>
            <el-button type="primary" @click="handlePublishClick">发布新服务</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <!-- Service List -->
      <div v-else class="services-stack">
        <article v-for="item in services" :key="item.id" class="xyf-surface-card my-service-card">
          <div class="my-service-card__left" @click="router.push(`/services/${item.id}`)">
            <XyfImage
              :src="buildImageUrl(item.coverImage)"
              :alt="item.serviceTitle"
              width="136px"
              height="96px"
              :radius="10"
              fit="cover"
            />
          </div>

          <div class="my-service-card__body">
            <div class="title-row">
              <h3 class="service-title ellipsis" @click="router.push(`/services/${item.id}`)">
                {{ item.serviceTitle }}
              </h3>
              <div class="status-pills">
                <XyfStatusBadge
                  :tone="statusTone(item.status)"
                  :label="getServiceStatusText(item.status)"
                />
                <XyfStatusBadge
                  v-if="item.reviewStatus"
                  :tone="reviewTone(item.reviewStatus)"
                  :label="`审核：${getReviewStatusText(item.reviewStatus)}`"
                  :dot="false"
                />
              </div>
            </div>

            <div class="meta-row xyf-tabular">
              <span class="price">{{ formatPrice(item.price) }}</span>
              <span class="dot">·</span>
              <span>{{ getTradeTypeText(item.tradeType) }}</span>
              <span class="dot" v-if="item.tagName">·</span>
              <span v-if="item.tagName">{{ item.tagName }}</span>
              <span class="dot">·</span>
              <span>销量 {{ item.salesCount ?? item.currentPurchaseCount ?? 0 }}</span>
              <span class="dot">·</span>
              <span>
                名额 {{ item.maxPurchases === -1 ? '不限' : `${item.currentPurchaseCount || 0}/${item.maxPurchases}` }}
              </span>
            </div>

            <div v-if="item.reviewReason" class="reject-reason">
              驳回意见：{{ item.reviewReason }}
            </div>
          </div>

          <div class="my-service-card__actions">
            <el-button size="small" @click="router.push(`/services/${item.id}`)">
              查看详情
            </el-button>

            <el-button
              v-if="canEditService(item.status)"
              size="small"
              @click="router.push(`/service/edit/${item.id}`)"
            >
              编辑
            </el-button>

            <el-button
              v-if="canOfflineService(item.status)"
              size="small"
              :loading="operatingId === item.id"
              @click="handleOffline(item)"
            >
              {{ item.status === ServiceStatus.PENDING_REVIEW ? '撤回审核' : '下架' }}
            </el-button>

            <el-button
              v-if="canOnlineService(item.status, item.reviewStatus)"
              size="small"
              type="primary"
              :loading="operatingId === item.id"
              @click="handleOnline(item)"
            >
              {{
                item.status === ServiceStatus.OFFLINE && item.reviewStatus === ReviewStatus.APPROVED
                  ? '恢复上架'
                  : '重新提交上架'
              }}
            </el-button>

            <el-button
              size="small"
              type="danger"
              text
              :loading="operatingId === item.id"
              @click="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </article>
      </div>

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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getMyServicesEnhanced,
  onlineService,
  offlineService,
  deleteService,
  buildImageUrl
} from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import {
  ServiceStatus,
  ReviewStatus,
  getTradeTypeText,
  getServiceStatusText,
  getReviewStatusText,
  canEditService,
  canOnlineService,
  canOfflineService,
  type MyServiceItem
} from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const userStore = useUserStore()

const statusTabs: Array<{ label: string; value: number | undefined }> = [
  { label: '全部服务', value: undefined },
  { label: '已上架', value: ServiceStatus.ONLINE },
  { label: '审核中', value: ServiceStatus.PENDING_REVIEW },
  { label: '已下架', value: ServiceStatus.OFFLINE },
  { label: '已驳回', value: ServiceStatus.REJECTED }
]

const loading = ref(false)
const operatingId = ref<number | null>(null)
const activeStatus = ref<number | undefined>(undefined)
const services = ref<MyServiceItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

function statusTone(status: number) {
  if (status === ServiceStatus.ONLINE) return 'success'
  if (status === ServiceStatus.PENDING_REVIEW) return 'warning'
  if (status === ServiceStatus.REJECTED) return 'danger'
  return 'neutral'
}

function reviewTone(reviewStatus: number) {
  if (reviewStatus === ReviewStatus.APPROVED) return 'success'
  if (reviewStatus === ReviewStatus.PENDING) return 'warning'
  if (reviewStatus === ReviewStatus.REJECTED) return 'danger'
  return 'neutral'
}

function handlePublishClick() {
  if (userStore.hasPublishPermission) {
    router.push('/service/publish')
  } else {
    ElMessage.info('请先在个人中心申请发布权限')
    router.push({ path: '/user/center', query: { tab: 'permission' } })
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getMyServicesEnhanced(activeStatus.value, {
      pageNum: pageNum.value,
      pageSize
    })
    services.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '加载我的服务列表失败')
    services.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchStatus(status: number | undefined) {
  activeStatus.value = status
  pageNum.value = 1
  fetchList()
}

async function handleOffline(item: MyServiceItem) {
  const isWithdrawing = item.status === ServiceStatus.PENDING_REVIEW
  try {
    await ElMessageBox.confirm(
      isWithdrawing
        ? `确认撤回「${item.serviceTitle}」的审核申请并转为下架可编辑状态吗？`
        : `确认将「${item.serviceTitle}」暂时下架吗？`,
      isWithdrawing ? '撤回审核确认' : '下架确认',
      {
        confirmButtonText: isWithdrawing ? '确认撤回' : '确认下架',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    operatingId.value = item.id
    await offlineService(item.id)
    ElMessage.success(isWithdrawing ? '已撤回审核，现在可以编辑或删除该服务' : '服务已下架')
    await fetchList()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, isWithdrawing ? '撤回失败' : '下架失败')
  } finally {
    operatingId.value = null
  }
}

async function handleOnline(item: MyServiceItem) {
  const instantRestore =
    item.status === ServiceStatus.OFFLINE && item.reviewStatus === ReviewStatus.APPROVED
  operatingId.value = item.id
  try {
    await onlineService(item.id)
    ElMessage.success(instantRestore ? '服务已恢复上架' : '已提交重新上架审核')
    await fetchList()
  } catch (err) {
    notifyFallbackError(err, '上架失败')
  } finally {
    operatingId.value = null
  }
}

async function handleDelete(item: MyServiceItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${item.serviceTitle}」吗？删除后将无法恢复。`,
      '删除服务',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    operatingId.value = item.id
    await deleteService(item.id)
    ElMessage.success('服务已删除')
    await fetchList()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '删除失败')
  } finally {
    operatingId.value = null
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.total-hint {
  font-size: 13px;
  color: var(--xyf-ink-muted);

  strong {
    color: var(--xyf-ink);
  }
}

.loading-card {
  padding: 24px;
}

.services-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.my-service-card {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;

  &__left {
    cursor: pointer;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 240px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;

  .service-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
    cursor: pointer;
    max-width: 460px;

    &:hover {
      color: var(--xyf-primary);
    }
  }

  .status-pills {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: var(--xyf-ink-muted);

  .price {
    font-size: 15px;
    font-weight: 750;
    color: var(--xyf-accent);
  }
}

.reject-reason {
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: var(--xyf-radius-xs);
  background: var(--xyf-danger-soft);
  color: var(--xyf-danger);
  font-size: 12.5px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
