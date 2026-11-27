<template>
  <div class="admin-service-detail">
    <XyfPageHeader
      :title="detail ? `服务审查 · ${detail.serviceTitle}` : '服务审查详情'"
      :show-back="true"
      back-text="返回审核队列"
      back-to="/admin/services"
    />

    <div v-if="loading" class="xyf-surface-card loading-card">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="!detail" class="xyf-surface-card loading-card">
      <XyfEmptyState title="未找到该服务信息" />
    </div>

    <template v-else>
      <div class="detail-grid">
        <div class="detail-main">
          <!-- Images -->
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>展示图集 ({{ gallery.length }})</h2>
            </div>

            <XyfEmptyState
              v-if="gallery.length === 0"
              title="卖家未上传展示图片"
              description="该服务暂无附加展示图片。"
            />

            <div v-else class="images-grid">
              <el-image
                v-for="(url, idx) in gallery"
                :key="url + idx"
                :src="url"
                :preview-src-list="gallery"
                :initial-index="idx"
                fit="cover"
                class="audit-img"
              />
            </div>
          </section>

          <!-- Description -->
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>服务详细介绍</h2>
            </div>
            <p class="desc-text">{{ detail.serviceDescription || '暂无详细描述' }}</p>

            <TradeLocationBadge
              v-if="detail.tradeType === TradeType.OFFLINE"
              :location-id="detail.tradeLocationId"
              :location-name="detail.tradeLocationName"
              :location="detail.tradeLocation"
              style="margin-top: 16px"
            />
          </section>
        </div>

        <aside class="detail-aside">
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>核心参数与审批</h2>
            </div>

            <div class="status-row">
              <XyfStatusBadge
                :tone="detail.status === ServiceStatus.ONLINE ? 'success' : 'warning'"
                :label="getServiceStatusText(detail.status)"
              />
              <XyfStatusBadge
                :tone="detail.reviewStatus === ReviewStatus.APPROVED ? 'success' : detail.reviewStatus === ReviewStatus.REJECTED ? 'danger' : 'warning'"
                :label="`审核：${getReviewStatusText(detail.reviewStatus)}`"
                :dot="false"
              />
            </div>

            <dl class="spec-list">
              <div class="spec-row">
                <dt>服务价格</dt>
                <dd class="price xyf-tabular">{{ formatPrice(detail.price) }}</dd>
              </div>
              <div class="spec-row">
                <dt>所属标签</dt>
                <dd>{{ detail.tagName || '-' }}</dd>
              </div>
              <div class="spec-row">
                <dt>交易方式</dt>
                <dd>{{ getTradeTypeText(detail.tradeType) }}</dd>
              </div>
              <div class="spec-row">
                <dt>名额限制</dt>
                <dd>{{ detail.maxPurchases === -1 ? '不限名额' : `${detail.maxPurchases} 份` }}</dd>
              </div>
              <div class="spec-row">
                <dt>发布者</dt>
                <dd>{{ detail.sellerName || `#${detail.sellerId}` }}</dd>
              </div>
              <div class="spec-row">
                <dt>提交时间</dt>
                <dd class="xyf-tabular">{{ formatDateTimeMinute(detail.createTime) }}</dd>
              </div>
            </dl>

            <div
              v-if="detail.status === ServiceStatus.PENDING_REVIEW || detail.reviewStatus === ReviewStatus.PENDING"
              class="audit-actions"
            >
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="handleApprove"
              >
                审核通过并上架
              </el-button>
              <el-button
                type="danger"
                plain
                size="large"
                @click="rejectVisible = true"
              >
                驳回此服务
              </el-button>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <el-dialog v-model="rejectVisible" title="驳回服务上架" width="440px">
      <el-form label-position="top">
        <el-form-item label="驳回理由">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请清晰说明驳回原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getServiceDetail, buildImageUrl } from '@/api/service'
import { reviewService } from '@/api/adminService'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTimeMinute } from '@/utils/format'
import {
  ServiceStatus,
  ReviewStatus,
  TradeType,
  getServiceStatusText,
  getReviewStatusText,
  getTradeTypeText,
  type ServiceDetail
} from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import TradeLocationBadge from '@/components/business/TradeLocationBadge.vue'

const router = useRouter()
const route = useRoute()

const serviceId = computed(() => Number(route.params.id))

const loading = ref(true)
const submitting = ref(false)
const detail = ref<ServiceDetail | null>(null)
const rejectVisible = ref(false)
const rejectReason = ref('')

const gallery = computed(() =>
  (detail.value?.images || []).map((img) => buildImageUrl(img)).filter(Boolean)
)

async function handleApprove() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(`确认批准上架「${detail.value.serviceTitle}」吗？`, '审核通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'success'
    })
    submitting.value = true
    await reviewService({ serviceId: detail.value.id, approved: true })
    ElMessage.success('审核通过，服务已上架')
    router.replace('/admin/services')
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '操作失败')
  } finally {
    submitting.value = false
  }
}

async function confirmReject() {
  if (!detail.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  submitting.value = true
  try {
    await reviewService({
      serviceId: detail.value.id,
      approved: false,
      reason: rejectReason.value.trim()
    })
    ElMessage.success('已驳回该服务')
    rejectVisible.value = false
    router.replace('/admin/services')
  } catch (err) {
    notifyFallbackError(err, '驳回操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getServiceDetail(serviceId.value)
  } catch (err) {
    notifyFallbackError(err, '获取服务审查详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(290px, 0.8fr);
  gap: 20px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card {
  padding: 22px 24px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;

  .audit-img {
    height: 128px;
    border-radius: var(--xyf-radius-sm);
    border: 1px solid var(--xyf-border);
  }
}

.desc-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: var(--xyf-ink-secondary);
  white-space: pre-wrap;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.spec-list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--xyf-border-light);

  dt {
    color: var(--xyf-ink-muted);
  }

  dd {
    margin: 0;
    color: var(--xyf-ink);
    font-weight: 500;

    &.price {
      color: var(--xyf-accent);
      font-weight: 800;
      font-size: 16px;
    }
  }
}

.audit-actions {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .el-button {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
