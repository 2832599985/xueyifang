<template>
  <div class="my-services-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="我的服务">
        <template #meta>
          <span class="head-count xyf-tabular">共 {{ total }} 项</span>
        </template>
        <template #actions>
          <el-button type="primary" @click="handlePublishClick">
            <el-icon><Plus /></el-icon>
            发布新服务
          </el-button>
        </template>
      </XyfPageHeader>

      <SellerSuiteTabs />

      <!-- Status Tabs -->
      <div class="toolbar-row">
        <div class="xyf-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.label"
            type="button"
            class="xyf-tabs__btn"
            :class="{ 'is-active': activeStatus === tab.value }"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
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
              width="124px"
              height="88px"
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
                  :dot="item.status === ServiceStatus.ONLINE || item.status === ServiceStatus.PENDING_REVIEW"
                />
                <XyfStatusBadge
                  v-if="item.reviewStatus && item.reviewStatus !== ReviewStatus.APPROVED"
                  :tone="reviewTone(item.reviewStatus)"
                  :label="`审核：${getReviewStatusText(item.reviewStatus)}`"
                />
              </div>
            </div>

            <div class="meta-row xyf-tabular">
              <span class="price">{{ formatPrice(item.price) }}</span>
              <span class="meta-sep">·</span>
              <span>{{ getTradeTypeText(item.tradeType) }}</span>
              <span class="meta-sep" v-if="item.tagName">·</span>
              <span v-if="item.tagName">{{ item.tagName }}</span>
              <span class="meta-sep">·</span>
              <span>销量 {{ item.salesCount ?? item.currentPurchaseCount ?? 0 }}</span>
              <span class="meta-sep">·</span>
              <span>
                名额 {{ item.maxPurchases === -1 ? '不限' : `${item.currentPurchaseCount || 0}/${item.maxPurchases}` }}
              </span>
            </div>

            <div v-if="item.reviewReason" class="reject-reason">
              驳回意见：{{ item.reviewReason }}
            </div>
          </div>

          <!-- Single Primary Action + Overflow Dropdown -->
          <div class="my-service-card__actions">
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
                  : '重新上架'
              }}
            </el-button>

            <el-button
              v-else-if="canEditService(item.status)"
              size="small"
              type="primary"
              @click="router.push(`/service/edit/${item.id}`)"
            >
              编辑
            </el-button>

            <el-button
              v-else
              size="small"
              @click="router.push(`/services/${item.id}`)"
            >
              查看详情
            </el-button>

            <el-dropdown trigger="click" @command="(cmd: string) => handleRowCommand(cmd, item)">
              <el-button size="small" class="more-btn" aria-label="更多操作">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                  <el-dropdown-item v-if="canEditService(item.status)" command="edit">
                    编辑服务
                  </el-dropdown-item>
                  <el-dropdown-item v-if="canOfflineService(item.status)" command="offline">
                    {{ item.status === ServiceStatus.PENDING_REVIEW ? '撤回审核' : '暂时下架' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <span class="dropdown-danger">删除服务</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </article>
      </div>

      <div v-if="total > pageSize" class="xyf-pagination">
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
import { Plus, MoreFilled } from '@element-plus/icons-vue'
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
import SellerSuiteTabs from '@/components/service/SellerSuiteTabs.vue'

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

function statusTone(status: number): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  if (status === ServiceStatus.ONLINE) return 'success'
  if (status === ServiceStatus.PENDING_REVIEW) return 'warning'
  if (status === ServiceStatus.REJECTED) return 'danger'
  return 'neutral'
}

function reviewTone(reviewStatus: number): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
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

function handleRowCommand(cmd: string, item: MyServiceItem) {
  if (cmd === 'detail') router.push(`/services/${item.id}`)
  else if (cmd === 'edit') router.push(`/service/edit/${item.id}`)
  else if (cmd === 'offline') handleOffline(item)
  else if (cmd === 'delete') handleDelete(item)
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
.head-count {
  font-size: 13px;
  color: var(--xyf-ink-3);
}

.toolbar-row {
  margin-bottom: 20px;
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
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    box-shadow: var(--xyf-shadow-sm);
  }

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
    flex-shrink: 0;
  }
}

.more-btn {
  padding: 0 9px;
}

.dropdown-danger {
  color: var(--xyf-danger);
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
    font-weight: 600;
    color: var(--xyf-ink);
    cursor: pointer;
    max-width: 480px;

    &:hover {
      opacity: 0.72;
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
  color: var(--xyf-ink-3);

  .price {
    font-size: 15px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .meta-sep {
    color: var(--xyf-border-strong);
  }
}

.reject-reason {
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: var(--xyf-radius-xs);
  background: var(--xyf-danger-soft);
  border: 1px solid #f5caca;
  color: #b91c1c;
  font-size: 12px;
}
</style>
