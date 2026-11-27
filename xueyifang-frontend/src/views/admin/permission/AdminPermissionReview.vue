<template>
  <div class="admin-review-page">
    <XyfPageHeader title="权限审核" dense>
      <template #meta>
        <span class="head-count xyf-tabular">待审核 {{ total }} 项</span>
      </template>
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
        compact
        title="当前暂无待审核的发布权限申请"
        description="所有用户提交的发布资质申请已处理完毕。"
      />

      <el-table v-else :data="list" @row-click="openDrawer">
        <el-table-column label="申请同学" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <XyfAvatar :name="row.realName || row.studentId" :size="30" />
              <div class="user-cell__text">
                <div class="user-name">{{ row.realName || row.studentId }}</div>
                <div class="user-major" v-if="row.professionalName || row.grade">
                  {{ row.professionalName || '' }}
                  <span v-if="row.professionalName && row.grade"> · </span>
                  {{ row.grade || '' }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="studentId" label="学号" width="130">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.studentId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="联系方式" min-width="170">
          <template #default="{ row }">
            <div class="contact-cell">
              <span v-if="row.phone" class="xyf-tabular">{{ row.phone }}</span>
              <span v-if="row.email" class="sub-email ellipsis">{{ row.email }}</span>
              <span v-if="!row.phone && !row.email">-</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="申请说明" min-width="240">
          <template #default="{ row }">
            <span class="reason-text ellipsis-2">{{ row.applyReason || '未填写详细申请说明' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申请时间" width="145">
          <template #default="{ row }">
            <span class="time-cell xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <div class="row-actions" @click.stop>
              <el-button text size="small" @click="openDrawer(row)">查看</el-button>
              <el-button
                size="small"
                type="primary"
                :loading="operatingId === row.userId"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
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
          @current-change="fetchPending"
        />
      </div>
    </section>

    <!-- Slide-over Permission Review Drawer (480px desktop / 100% mobile) -->
    <el-drawer
      v-model="drawerVisible"
      title="发布权限审核"
      size="480px"
      class="perm-drawer"
      destroy-on-close
    >
      <div v-if="activeUser" class="drawer-body">
        <div class="drawer-identity">
          <div class="drawer-identity__head">
            <XyfAvatar :name="activeUser.realName || activeUser.studentId" :size="44" />
            <div>
              <h3>{{ activeUser.realName || activeUser.studentId }}</h3>
              <span class="xyf-mono id-sub">学号 {{ activeUser.studentId }}</span>
            </div>
          </div>

          <dl class="identity-grid">
            <div class="id-item">
              <dt>专业</dt>
              <dd>{{ activeUser.professionalName || '-' }}</dd>
            </div>
            <div class="id-item">
              <dt>年级</dt>
              <dd>{{ activeUser.grade || '-' }}</dd>
            </div>
            <div class="id-item">
              <dt>手机</dt>
              <dd class="xyf-tabular">{{ activeUser.phone || '-' }}</dd>
            </div>
            <div class="id-item">
              <dt>邮箱</dt>
              <dd class="ellipsis">{{ activeUser.email || '-' }}</dd>
            </div>
            <div class="id-item">
              <dt>申请时间</dt>
              <dd class="xyf-tabular">{{ formatDateTimeMinute(activeUser.createTime) }}</dd>
            </div>
          </dl>
        </div>

        <div class="drawer-reason-section">
          <div class="section-label">申请技能与特长说明</div>
          <p class="full-reason">{{ activeUser.applyReason || '申请人未填写详细说明' }}</p>
        </div>
      </div>

      <template #footer v-if="activeUser">
        <div class="drawer-footer">
          <div v-if="showRejectInput" class="inline-reject">
            <el-input
              v-model="rejectReason"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请清晰说明驳回原因，将通知申请人修改后重提..."
            />
            <div class="inline-reject__actions">
              <el-button size="small" @click="showRejectInput = false">取消</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="submittingReject"
                @click="confirmReject"
              >
                确认驳回
              </el-button>
            </div>
          </div>

          <div v-else class="drawer-footer__btns">
            <el-button type="danger" plain @click="showRejectInput = true">驳回</el-button>
            <el-button
              type="primary"
              :loading="operatingId === activeUser.userId"
              @click="handleApprove(activeUser)"
            >
              通过并授予权限
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>
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

const drawerVisible = ref(false)
const activeUser = ref<PendingUser | null>(null)
const showRejectInput = ref(false)
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

function openDrawer(row: PendingUser) {
  activeUser.value = row
  showRejectInput.value = false
  rejectReason.value = ''
  drawerVisible.value = true
}

async function removeHandledRow(userId: number) {
  list.value = list.value.filter((item) => item.userId !== userId)
  total.value = Math.max(0, total.value - 1)
  if (list.value.length === 0 && pageNum.value > 1) {
    pageNum.value -= 1
    await fetchPending()
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
    drawerVisible.value = false
    await removeHandledRow(row.userId)
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '审批操作失败')
  } finally {
    operatingId.value = null
  }
}

async function confirmReject() {
  if (!activeUser.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  submittingReject.value = true
  try {
    const uid = activeUser.value.userId
    await reviewPermission({
      userId: uid,
      approved: false,
      reason: rejectReason.value.trim()
    })
    ElMessage.success('已驳回该权限申请')
    drawerVisible.value = false
    showRejectInput.value = false
    await removeHandledRow(uid)
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
.head-count {
  font-size: 13px;
  color: var(--xyf-ink-3);
}

.table-card {
  padding: 20px 24px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;

  &__text {
    min-width: 0;
  }

  .user-name {
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .user-major {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.contact-cell {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  color: var(--xyf-ink-2);

  .sub-email {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.reason-text {
  font-size: 13px;
  color: var(--xyf-ink-2);
  line-height: 1.5;
}

.time-cell {
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.drawer-identity {
  padding: 18px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &__head {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 14px;
    margin-bottom: 14px;
    border-bottom: 1px solid var(--xyf-border-subtle);

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    .id-sub {
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }
}

.identity-grid {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
}

.id-item {
  min-width: 0;

  dt {
    font-size: 12px;
    color: var(--xyf-ink-3);
    margin-bottom: 2px;
  }

  dd {
    margin: 0;
    font-size: 13px;
    font-weight: 500;
    color: var(--xyf-ink);
  }
}

.drawer-reason-section {
  .section-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
    margin-bottom: 8px;
  }

  .full-reason {
    margin: 0;
    font-size: 14px;
    line-height: 1.75;
    color: var(--xyf-ink);
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.drawer-footer {
  width: 100%;

  &__btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}

.inline-reject {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

@media (max-width: 640px) {
  :deep(.perm-drawer) {
    width: 100% !important;
  }
}
</style>
