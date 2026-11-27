<template>
  <div class="service-form-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="发布服务"
        :show-back="true"
        back-text="返回我的服务"
        back-to="/my-services"
      >
        <template #actions v-if="userStore.hasPublishPermission">
          <el-button @click="saveDraft">保存草稿</el-button>
          <el-button v-if="hasDraft" text type="danger" @click="clearDraft">清除草稿</el-button>
        </template>
      </XyfPageHeader>

      <div v-if="checkingPermission" class="xyf-surface-card gate-card">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else-if="!userStore.hasPublishPermission" class="xyf-surface-card gate-card">
        <XyfEmptyState
          title="尚未开通服务发布权限"
          description="首次发布前需要在「个人中心」提交一次技能特长说明，管理员审核通过后即可发布服务。"
        >
          <template #action>
            <el-button
              type="primary"
              @click="router.push({ path: '/user/center', query: { tab: 'permission' } })"
            >
              前往个人中心申请权限
            </el-button>
          </template>
        </XyfEmptyState>
      </div>

      <ServiceEditorForm
        v-else
        ref="editorRef"
        :initial-value="draftInitialValue"
        submit-text="提交发布审核"
        :submitting="submitting"
        @submit="handleSubmit"
        @cancel="router.back()"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { publishService } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import type { ServiceFormData } from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import ServiceEditorForm, {
  type ServiceFormDraftSnapshot
} from '@/components/service/ServiceEditorForm.vue'

const router = useRouter()
const userStore = useUserStore()
const dictStore = useDictStore()

const editorRef = ref<InstanceType<typeof ServiceEditorForm> | null>(null)
const checkingPermission = ref(true)
const submitting = ref(false)
const hasDraft = ref(false)
const draftInitialValue = ref<Partial<ServiceFormDraftSnapshot> | null>(null)

const draftStorageKey = computed(() => `xyf_service_draft_${userStore.userInfo?.id || 'guest'}`)

function saveDraft() {
  const snapshot = editorRef.value?.getSnapshot()
  if (!snapshot) return
  try {
    localStorage.setItem(draftStorageKey.value, JSON.stringify(snapshot))
    hasDraft.value = true
    ElMessage.success('草稿已保存至当前浏览器')
  } catch {
    ElMessage.warning('草稿保存失败')
  }
}

function restoreDraftIfAny() {
  try {
    const raw = localStorage.getItem(draftStorageKey.value)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed?.form) {
      draftInitialValue.value = parsed
      hasDraft.value = true
    }
  } catch {
    // ignore broken local draft
  }
}

function clearDraft(silent = false) {
  localStorage.removeItem(draftStorageKey.value)
  hasDraft.value = false
  if (!silent) ElMessage.info('草稿已清除')
}

async function handleSubmit(payload: ServiceFormData) {
  submitting.value = true
  try {
    await publishService(payload)
    clearDraft(true)
    ElMessage.success('服务已提交，等待管理员审核上架')
    router.replace('/my-services')
  } catch (err) {
    notifyFallbackError(err, '服务发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    await userStore.ensureUserInfo()
    if (userStore.hasPublishPermission) {
      await Promise.all([
        dictStore.fetchServiceTags().catch(() => {}),
        dictStore.fetchTradeLocations().catch(() => {})
      ])
      restoreDraftIfAny()
    }
  } finally {
    checkingPermission.value = false
  }
})
</script>

<style scoped lang="scss">
.gate-card {
  padding: 28px;
}
</style>
