<template>
  <div class="admin-service-review">
    <XyfPageHeader title="服务审核" dense>
      <template #actions>
        <el-button @click="refreshActiveList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
      </template>
    </XyfPageHeader>

    <XyfFilterBar>
      <template #tabs>
        <div class="xyf-segmented">
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': viewMode === 'pending' }"
            @click="switchMode('pending')"
          >
            待审核队列
            <span v-if="pendingBadge !== null" class="xyf-segmented__count">{{ pendingBadge }}</span>
          </button>
          <button
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': viewMode === 'all' }"
            @click="switchMode('all')"
          >
            全量巡检
          </button>
        </div>
      </template>

      <template #fields v-if="viewMode === 'all'">
        <el-select
          v-model="filterStatus"
          placeholder="全部状态"
          clearable
          class="xyf-filter-select"
          @change="handleSearchAll"
        >
          <el-option :value="1" label="已上架" />
          <el-option :value="2" label="已下架" />
          <el-option :value="3" label="待审核" />
          <el-option :value="4" label="已驳回" />
        </el-select>

        <el-input
          v-model="keyword"
          placeholder="搜索服务标题或描述"
          clearable
          :prefix-icon="Search"
          class="xyf-filter-search"
          @clear="handleSearchAll"
          @keyup.enter="handleSearchAll"
        />
      </template>

      <template #actions v-if="viewMode === 'all'">
        <el-button v-if="filterStatus || keyword" @click="resetAllFilters">重置</el-button>
        <el-button type="primary" @click="handleSearchAll">查询</el-button>
      </template>
    </XyfFilterBar>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="services.length === 0"
        compact
        :title="viewMode === 'pending' ? '当前暂无待审核服务' : '未查询到符合条件的服务'"
        :description="
          viewMode === 'pending'
            ? '所有新发布或修改的服务均已完成审查。'
            : '尝试更换筛选条件或关键词。'
        "
      />

      <el-table v-else :data="services">
        <el-table-column label="服务" min-width="280">
          <template #default="{ row }">
            <div class="svc-cell" @click="openDrawer(row)">
              <XyfImage
                :src="buildImageUrl(row.coverImage)"
                :alt="row.serviceTitle"
                width="72px"
                height="48px"
                :radius="6"
                fit="cover"
              />
              <div class="svc-cell__info">
                <strong class="svc-title ellipsis">{{ row.serviceTitle }}</strong>
                <span class="svc-sub">
                  {{ row.tagName || '未分类' }} · {{ row.sellerName || '-' }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" v-if="viewMode === 'all'">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="statusTone(row.status)"
              :label="statusLabel(row.status)"
              :dot="row.status === 3"
            />
          </template>
        </el-table-column>

        <el-table-column label="定价" width="110" align="right">
          <template #default="{ row }">
            <strong class="price-text xyf-mono">{{ formatPrice(row.price) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">
            <span class="time-cell xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text size="small" @click="openDrawer(row)">
                {{ canReviewRow(row) ? '审核' : '查看' }}
              </el-button>

              <el-button
                v-if="canReviewRow(row)"
                type="primary"
                size="small"
                :loading="operatingId === row.serviceId"
                @click="quickApprove(row)"
              >
                通过
              </el-button>

              <el-dropdown trigger="click" @command="(cmd: string) => handleRowCommand(cmd, row)">
                <el-button text size="small" aria-label="更多操作">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="archive">查看完整档案</el-dropdown-item>
                    <el-dropdown-item v-if="canReviewRow(row)" command="reject">
                      <span class="dropdown-danger">驳回上架</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="row.status === 1" command="offline">
                      <span class="dropdown-danger">强制下架</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="xyf-pagination">
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

    <!-- Slide-over Quick Review Drawer (520px desktop / 100% mobile) -->
    <el-drawer
      v-model="drawerVisible"
      :title="activeRow ? '服务审查' : ''"
      size="520px"
      class="review-drawer"
      destroy-on-close
    >
      <div v-if="activeRow" class="drawer-body">
        <div class="drawer-cover">
          <el-image
            v-if="drawerGallery.length > 0"
            :src="drawerGallery[0]"
            :preview-src-list="drawerGallery"
            fit="cover"
            class="drawer-cover__img"
          />
          <div v-else class="drawer-cover__empty">暂无展示图片</div>
          <span v-if="drawerGallery.length > 1" class="drawer-cover__count xyf-tabular">
            共 {{ drawerGallery.length }} 张
          </span>
        </div>

        <div class="drawer-title-block">
          <div class="drawer-title-top">
            <h3>{{ activeDetail?.serviceTitle || activeRow.serviceTitle }}</h3>
            <XyfStatusBadge
              :tone="statusTone(activeDetail?.status ?? activeRow.status)"
              :label="statusLabel(activeDetail?.status ?? activeRow.status)"
              :dot="(activeDetail?.status ?? activeRow.status) === 3"
            />
          </div>
          <div class="drawer-price xyf-mono">
            {{ formatPrice(activeDetail?.price ?? activeRow.price) }}
          </div>
        </div>

        <dl class="drawer-dl">
          <div class="drawer-dl__row">
            <dt>发布者</dt>
            <dd>{{ activeDetail?.sellerName || activeRow.sellerName || '-' }}</dd>
          </div>
          <div class="drawer-dl__row">
            <dt>技能分类</dt>
            <dd>{{ activeDetail?.tagName || activeRow.tagName || '-' }}</dd>
          </div>
          <div class="drawer-dl__row">
            <dt>交易方式</dt>
            <dd>{{ activeDetail ? getTradeTypeText(activeDetail.tradeType) : (drawerLoading ? '加载中...' : '-') }}</dd>
          </div>
          <div class="drawer-dl__row">
            <dt>可购名额</dt>
            <dd class="xyf-tabular">
              <template v-if="activeDetail">
                {{ activeDetail.maxPurchases === -1 ? '不限名额' : `${activeDetail.maxPurchases} 份` }}
              </template>
              <template v-else>{{ drawerLoading ? '加载中...' : '-' }}</template>
            </dd>
          </div>
          <div class="drawer-dl__row">
            <dt>提交时间</dt>
            <dd class="xyf-tabular">{{ formatDateTimeMinute(activeRow.createTime) }}</dd>
          </div>
        </dl>

        <div class="drawer-desc-block">
          <div class="drawer-section-label">服务说明</div>
          <p class="drawer-desc" :class="{ 'is-clamped': !descExpanded }">
            {{ activeDetail?.serviceDescription || activeRow.description || '未填写详细描述' }}
          </p>
          <button
            v-if="(activeDetail?.serviceDescription || activeRow.description || '').length > 110"
            type="button"
            class="expand-btn"
            @click="descExpanded = !descExpanded"
          >
            {{ descExpanded ? '收起' : '展开全文' }}
          </button>
        </div>

        <div class="drawer-archive-link">
          <el-button text size="small" @click="router.push(`/admin/services/${activeRow.serviceId}`)">
            查看完整档案 →
          </el-button>
        </div>
      </div>

      <template #footer v-if="activeRow">
        <div class="drawer-footer">
          <!-- Inline Rejection / Force-Offline Reason Input -->
          <div v-if="inlineReasonMode" class="inline-reason-box">
            <el-input
              v-model="inlineReason"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              :placeholder="
                inlineReasonMode === 'offline'
                  ? '请输入强制下架原因（将通知卖家）...'
                  : '请输入驳回原因（将发送给卖家修改）...'
              "
            />
            <div class="inline-reason-actions">
              <el-button size="small" @click="cancelInlineReason">取消</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="submitting"
                @click="submitInlineReason"
              >
                {{ inlineReasonMode === 'offline' ? '确认强制下架' : '确认驳回' }}
              </el-button>
            </div>
          </div>

          <div v-else class="drawer-footer__btns">
            <template v-if="canReviewRow(activeRow)">
              <el-button type="danger" plain @click="startInlineReason('reject')">驳回</el-button>
              <el-button type="primary" :loading="submitting" @click="approveInDrawer">
                通过并上架
              </el-button>
            </template>
            <el-button
              v-else-if="activeRow.status === 1"
              type="danger"
              plain
              @click="startInlineReason('offline')"
            >
              强制下架
            </el-button>
            <el-button v-else @click="drawerVisible = false">关闭</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search, MoreFilled } from '@element-plus/icons-vue'
import { getPendingServices, listAllAdminServices } from '@/api/adminService'
import { getServiceDetail, buildImageUrl } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTimeMinute } from '@/utils/format'
import { getTradeTypeText, type ServiceDetail } from '@/types/service'
import type { PendingService } from '@/types/admin'
import { useAdminServiceReview } from '@/composables/useAdminServiceReview'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfFilterBar from '@/components/ui/XyfFilterBar.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const { submitting, approveService, rejectService, forceOfflineService } = useAdminServiceReview()

const viewMode = ref<'pending' | 'all'>('pending')
const filterStatus = ref<number | undefined>(undefined)
const keyword = ref('')

const loading = ref(true)
const operatingId = ref<number | null>(null)
const services = ref<PendingService[]>([])
const total = ref(0)
const pendingBadge = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = 10

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const activeRow = ref<PendingService | null>(null)
const activeDetail = ref<ServiceDetail | null>(null)
const descExpanded = ref(false)
const inlineReasonMode = ref<'reject' | 'offline' | null>(null)
const inlineReason = ref('')

const drawerGallery = computed(() => {
  const imgs = activeDetail.value?.images?.length
    ? activeDetail.value.images
    : activeRow.value?.coverImage
      ? [activeRow.value.coverImage]
      : []
  return imgs.map((img) => buildImageUrl(img)).filter(Boolean)
})

function canReviewRow(row: PendingService) {
  return viewMode.value === 'pending' || row.status === 3 || row.reviewStatus === 2
}

function statusTone(status?: number): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
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

function resetAllFilters() {
  filterStatus.value = undefined
  keyword.value = ''
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
      pendingBadge.value = total.value
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

async function openDrawer(row: PendingService, openMode: 'reject' | 'offline' | null = null) {
  activeRow.value = row
  activeDetail.value = null
  descExpanded.value = false
  inlineReasonMode.value = openMode
  inlineReason.value = ''
  drawerVisible.value = true

  drawerLoading.value = true
  try {
    activeDetail.value = await getServiceDetail(row.serviceId)
  } catch {
    activeDetail.value = null
  } finally {
    drawerLoading.value = false
  }
}

function handleRowCommand(cmd: string, row: PendingService) {
  if (cmd === 'archive') router.push(`/admin/services/${row.serviceId}`)
  else if (cmd === 'reject') openDrawer(row, 'reject')
  else if (cmd === 'offline') openDrawer(row, 'offline')
}

async function onDecisionComplete(serviceId: number) {
  if (viewMode.value === 'pending') {
    services.value = services.value.filter((s) => s.serviceId !== serviceId)
    total.value = Math.max(0, total.value - 1)
    pendingBadge.value = total.value
    if (services.value.length === 0 && pageNum.value > 1) {
      pageNum.value -= 1
      await refreshActiveList()
    }
  } else {
    await refreshActiveList()
  }
}

async function quickApprove(row: PendingService) {
  operatingId.value = row.serviceId
  try {
    const ok = await approveService(row.serviceId, row.serviceTitle)
    if (ok) await onDecisionComplete(row.serviceId)
  } finally {
    operatingId.value = null
  }
}

async function approveInDrawer() {
  if (!activeRow.value) return
  const ok = await approveService(activeRow.value.serviceId, activeRow.value.serviceTitle)
  if (ok) {
    const id = activeRow.value.serviceId
    drawerVisible.value = false
    await onDecisionComplete(id)
  }
}

function startInlineReason(mode: 'reject' | 'offline') {
  inlineReasonMode.value = mode
  inlineReason.value = ''
}

function cancelInlineReason() {
  inlineReasonMode.value = null
  inlineReason.value = ''
}

async function submitInlineReason() {
  if (!activeRow.value || !inlineReasonMode.value) return
  const id = activeRow.value.serviceId
  const ok =
    inlineReasonMode.value === 'offline'
      ? await forceOfflineService(id, inlineReason.value)
      : await rejectService(id, inlineReason.value)

  if (ok) {
    drawerVisible.value = false
    inlineReasonMode.value = null
    inlineReason.value = ''
    await onDecisionComplete(id)
  }
}

onMounted(() => {
  refreshActiveList()
})
</script>

<style scoped lang="scss">
.table-card {
  padding: 20px 24px;
}

.svc-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;

  &:hover .svc-title {
    text-decoration: underline;
    text-underline-offset: 3px;
  }

  &__info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .svc-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .svc-sub {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.price-text {
  color: var(--xyf-ink);
  font-weight: 600;
}

.time-cell {
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dropdown-danger {
  color: var(--xyf-danger);
}

/* Slide-over Drawer */
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &__img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__empty {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__count {
    position: absolute;
    right: 10px;
    bottom: 10px;
    padding: 2px 8px;
    border-radius: 999px;
    background: rgba(17, 17, 19, 0.76);
    color: #ffffff;
    font-size: 12px;
  }
}

.drawer-title-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-title-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.3;
    color: var(--xyf-ink);
  }
}

.drawer-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.drawer-dl {
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;

    dt {
      color: var(--xyf-ink-3);
    }

    dd {
      margin: 0;
      font-weight: 500;
      color: var(--xyf-ink);
    }
  }
}

.drawer-desc-block {
  .drawer-section-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
    margin-bottom: 6px;
  }

  .drawer-desc {
    margin: 0;
    font-size: 14px;
    line-height: 1.7;
    color: var(--xyf-ink-2);
    white-space: pre-wrap;
    word-break: break-word;

    &.is-clamped {
      display: -webkit-box;
      -webkit-line-clamp: 4;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .expand-btn {
    margin-top: 6px;
    border: none;
    background: transparent;
    padding: 0;
    font-size: 12px;
    font-weight: 500;
    color: var(--xyf-ink);
    text-decoration: underline;
    cursor: pointer;
    font-family: inherit;
  }
}

.drawer-archive-link {
  display: flex;
  justify-content: flex-start;
}

.drawer-footer {
  width: 100%;

  &__btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}

.inline-reason-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.inline-reason-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  :deep(.review-drawer) {
    width: 100% !important;
  }
}
</style>
