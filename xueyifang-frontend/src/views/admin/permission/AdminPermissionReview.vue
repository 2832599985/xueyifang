<template>
  <div class="admin-review-page">
    <XyfPageHeader
      kicker="MODERATION"
      title="发布权限申请审核"
      subtitle="核验申请用户的校内学号与专业信息，审批通过后将授予服务发布权限"
    >
      <template #actions>
        <el-button @click="fetchPending" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新队列
        </el-button>
      </template>
    </XyfPageHeader>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="list.length === 0"
        title="当前暂无待审核的发布权限申请"
        description="所有用户提交的发布资质申请已处理完毕。"
      />

      <el-table v-else :data="list" stripe>
        <el-table-column prop="studentId" label="学号" width="130">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.studentId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申请同学" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <XyfAvatar :name="row.realName || row.studentId" :size="30" />
              <div>
                <div class="user-name">{{ row.realName || row.studentId }}</div>
                <div class="user-major" v-if="row.professionalName">
                  {{ row.professionalName }}
                  <span v-if="row.grade"> · {{ row.grade }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="联系方式" min-width="160">
          <template #default="{ row }">
            <div class="contact-cell">
              <span v-if="row.phone">{{ row.phone }}</span>
              <span v-if="row.email" class="sub-email">{{ row.email }}</span>
              <span v-if="!row.phone && !row.email">-</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="申请技能与理由说明" min-width="230">
          <template #default="{ row }">
            <span class="reason-text">{{ row.applyReason || '未填写详细申请说明' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">
            <span class="xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="审批操作" width="190" align="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :loading="operatingId === row.userId"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              @click="openReject(row)"
            >
              驳回
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
          @current-change="fetchPending"
        />
      </div>
    </section>

    <el-dialog v-model="rejectDialogVisible" title="驳回发布权限申请" width="440px">
      <el-form label-position="top">
        <el-form-item label="驳回原因（将通知申请人修改后重提）">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请清晰说明未能通过的原因，例如：请补充具体专业擅长方向与过往作品说明..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submittingReject" @click="confirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getPendingUsers, reviewPermission } from '@/api/admin'
import { notifyFallbackError } from '@/api/request'
import { formatDateTimeMinute } from '@/utils/format'
import type { PendingUser } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const loading = ref(true)
const operatingId = ref<number | null>(null)
const list = ref<PendingUser[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const rejectDialogVisible = ref(false)
const targetUser = ref<PendingUser | null>(null)
const rejectReason = ref('')
const submittingReject = ref(false)

async function fetchPending() {
  loading.value = true
  try {
    const res = await getPendingUsers(pageNum.value, pageSize)
    list.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取待审核权限列表失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: PendingUser) {
  try {
    await ElMessageBox.confirm(
      `确认授予「${row.realName || row.studentId}（${row.studentId}）」校园服务发布权限吗？`,
      '审批通过确认',
      { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'success' }
    )
    operatingId.value = row.userId
    await reviewPermission({ userId: row.userId, approved: true })
    ElMessage.success('已批准发布权限')
    await fetchPending()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '审批操作失败')
  } finally {
    operatingId.value = null
  }
}

function openReject(row: PendingUser) {
  targetUser.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!targetUser.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  submittingReject.value = true
  try {
    await reviewPermission({
      userId: targetUser.value.userId,
      approved: false,
      reason: rejectReason.value.trim()
    })
    ElMessage.success('已驳回该权限申请')
    rejectDialogVisible.value = false
    await fetchPending()
  } catch (err) {
    notifyFallbackError(err, '驳回操作失败')
  } finally {
    submittingReject.value = false
  }
}

onMounted(() => {
  fetchPending()
})
</script>

<style scoped lang="scss">
.table-card {
  padding: 20px 24px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-name {
    font-weight: 650;
    color: var(--xyf-ink);
  }

  .user-major {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.contact-cell {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  color: var(--xyf-ink-secondary);

  .sub-email {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
