import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewService, forceOfflineAdminService } from '@/api/adminService'
import { notifyFallbackError } from '@/api/request'

export function useAdminServiceReview() {
  const submitting = ref(false)

  async function approveService(serviceId: number, serviceTitle: string): Promise<boolean> {
    try {
      await ElMessageBox.confirm(`确认审核通过并上架「${serviceTitle}」吗？`, '审核通过确认', {
        confirmButtonText: '批准上架',
        cancelButtonText: '取消',
        type: 'success'
      })
      submitting.value = true
      await reviewService({ serviceId, approved: true })
      ElMessage.success('服务已批准上架')
      return true
    } catch (err) {
      if (err !== 'cancel') notifyFallbackError(err, '审核通过失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  async function rejectService(serviceId: number, reason: string): Promise<boolean> {
    const trimmed = reason.trim()
    if (!trimmed) {
      ElMessage.warning('请填写驳回原因')
      return false
    }
    submitting.value = true
    try {
      await reviewService({ serviceId, approved: false, reason: trimmed })
      ElMessage.success('已驳回该服务')
      return true
    } catch (err) {
      notifyFallbackError(err, '驳回操作失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  async function forceOfflineService(serviceId: number, reason: string): Promise<boolean> {
    const trimmed = reason.trim()
    if (!trimmed) {
      ElMessage.warning('请填写强制下架原因')
      return false
    }
    submitting.value = true
    try {
      await forceOfflineAdminService(serviceId, trimmed)
      ElMessage.success('已强制下架该服务并通知卖家')
      return true
    } catch (err) {
      notifyFallbackError(err, '强制下架失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting,
    approveService,
    rejectService,
    forceOfflineService
  }
}
