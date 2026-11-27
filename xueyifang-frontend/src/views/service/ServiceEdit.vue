<template>
  <div class="service-form-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="编辑服务"
        :show-back="true"
        back-text="返回我的服务"
        back-to="/my-services"
      />

      <div v-if="loading" class="xyf-surface-card gate-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!detail" class="xyf-surface-card gate-card">
        <XyfEmptyState
          title="无法加载该服务信息"
          description="请确认服务是否存在或您是否有权操作此服务。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/my-services')">返回我的服务</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <div v-if="!canEditService(detail.status)" class="edit-notice is-warn">
          <span>当前状态不可直接编辑，请先在「我的服务」下架后再修改。</span>
          <el-button size="small" @click="router.push('/my-services')">前往管理</el-button>
        </div>

        <div v-if="detail.reviewReason" class="edit-notice is-danger">
          <span>上次审核意见：{{ detail.reviewReason }}</span>
        </div>

        <ServiceEditorForm
          :initial-value="initialSnapshot"
          submit-text="保存修改并提交审核"
          :submitting="submitting"
          :submit-disabled="!canEditService(detail.status)"
          :show-templates="false"
          @submit="handleSubmit"
          @cancel="router.push('/my-services')"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDictStore } from '@/stores/dict'
import { getServiceDetail, updateService } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import { canEditService, type ServiceDetail, type ServiceFormData } from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import ServiceEditorForm, {
  type ServiceFormDraftSnapshot
} from '@/components/service/ServiceEditorForm.vue'

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const serviceId = computed(() => Number(route.params.id))

const loading = ref(true)
const submitting = ref(false)
const detail = ref<ServiceDetail | null>(null)
const initialSnapshot = ref<Partial<ServiceFormDraftSnapshot> | null>(null)

async function handleSubmit(payload: ServiceFormData) {
  if (!detail.value) return
  submitting.value = true
  try {
    await updateService(detail.value.id, payload)
    ElMessage.success('服务已更新，请等待管理员重新审核')
    router.replace('/my-services')
  } catch (err) {
    notifyFallbackError(err, '保存修改失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [svc] = await Promise.all([
      getServiceDetail(serviceId.value),
      dictStore.fetchServiceTags().catch(() => {}),
      dictStore.fetchTradeLocations().catch(() => {})
    ])
    detail.value = svc
    initialSnapshot.value = {
      form: {
        serviceTitle: svc.serviceTitle,
        serviceDescription: svc.serviceDescription,
        tagId: svc.tagId,
        price: svc.price,
        tradeType: svc.tradeType,
        tradeLocationId: svc.tradeLocationId,
        isPhysical: svc.isPhysical
      },
      limitMode: svc.maxPurchases === -1 ? 'unlimited' : 'limited',
      limitCount: svc.maxPurchases === -1 ? 20 : svc.maxPurchases || 1,
      images: svc.images || []
    }
  } catch (err) {
    notifyFallbackError(err, '获取服务详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.gate-card {
  padding: 28px;
}

.edit-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border-radius: var(--xyf-radius-sm);
  font-size: 13px;

  &.is-warn {
    background: var(--xyf-champagne-soft);
    border: 1px solid var(--xyf-champagne-border);
    color: #9a5b13;
  }

  &.is-danger {
    background: var(--xyf-danger-soft);
    border: 1px solid #f5caca;
    color: #b91c1c;
  }
}
</style>
