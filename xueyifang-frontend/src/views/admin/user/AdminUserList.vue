<template>
  <div class="admin-user-page">
    <XyfPageHeader title="用户管理">
      <template #actions>
        <el-button @click="fetchList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </XyfPageHeader>

    <section class="xyf-surface-card filter-card">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索学号 / 姓名 / 昵称 / 手机号..."
          clearable
          style="width: 280px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="accountStatus"
          placeholder="账号状态"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option :value="1" label="状态：正常" />
          <el-option :value="2" label="状态：已禁用" />
        </el-select>

        <el-select
          v-model="publishPermission"
          placeholder="发布权限"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option :value="1" label="已开通发布权" />
          <el-option :value="0" label="无发布权" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </section>

    <section class="xyf-surface-card table-card">
      <el-skeleton v-if="loading" :rows="6" animated />

      <XyfEmptyState
        v-else-if="users.length === 0"
        title="未查询到匹配的用户账号"
        description="请尝试调整搜索关键词或状态筛选条件。"
      />

      <el-table v-else :data="users" stripe>
        <el-table-column prop="studentId" label="学号" width="125">
          <template #default="{ row }">
            <span class="xyf-mono">{{ row.studentId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="姓名与专业" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <XyfAvatar :name="row.realName || row.nickname || row.studentId" :size="30" />
              <div>
                <div class="user-name">
                  {{ row.realName || row.studentId }}
                  <span v-if="row.role === 2" class="role-tag">管理员</span>
                </div>
                <div class="user-sub">
                  {{ row.professionalName || '未关联专业' }}
                  <span v-if="row.grade"> · {{ row.grade }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="联系方式" min-width="150">
          <template #default="{ row }">
            <div class="contact-col">
              <span>{{ row.phone || '-' }}</span>
              <span v-if="row.dormitory" class="sub-text">{{ row.dormitory }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="钱包余额 / 冻结" width="160" align="right">
          <template #default="{ row }">
            <div class="xyf-tabular">
              <div>{{ formatPrice(row.walletBalance || 0) }}</div>
              <div class="sub-text" v-if="row.frozenAmount">
                冻结 {{ formatPrice(row.frozenAmount) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布权限" width="125">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="row.publishPermission === 1 ? 'success' : 'neutral'"
              :label="row.publishPermission === 1 ? '已开通' : '未开通'"
            />
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="110">
          <template #default="{ row }">
            <XyfStatusBadge
              :tone="row.accountStatus === 1 ? 'success' : 'danger'"
              :label="row.accountStatus === 1 ? '正常' : '已禁用'"
            />
          </template>
        </el-table-column>

        <el-table-column label="治理操作" width="260" align="right">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 2"
              size="small"
              :type="row.publishPermission === 1 ? 'warning' : 'success'"
              plain
              :loading="operatingId === row.userId"
              @click="togglePublishPermission(row)"
            >
              {{ row.publishPermission === 1 ? '回收发布权' : '授予发布权' }}
            </el-button>

            <el-button
              v-if="row.role !== 2"
              size="small"
              :type="row.accountStatus === 1 ? 'danger' : 'primary'"
              plain
              :loading="operatingId === row.userId"
              @click="toggleAccountStatus(row)"
            >
              {{ row.accountStatus === 1 ? '封禁' : '解封' }}
            </el-button>

            <el-button
              size="small"
              text
              :loading="operatingId === row.userId"
              @click="handleResetPassword(row)"
            >
              重置密码
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
          @current-change="fetchList"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  listAdminUsers,
  updateAdminUserStatus,
  resetAdminUserPassword
} from '@/api/admin'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import type { AdminUser } from '@/types/admin'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const loading = ref(true)
const operatingId = ref<number | null>(null)
const keyword = ref('')
const accountStatus = ref<number | undefined>(undefined)
const publishPermission = ref<number | undefined>(undefined)

const users = ref<AdminUser[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12

async function fetchList() {
  loading.value = true
  try {
    const res = await listAdminUsers({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value.trim() || undefined,
      accountStatus: accountStatus.value,
      publishPermission: publishPermission.value
    })
    users.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

async function togglePublishPermission(row: AdminUser) {
  const nextPerm = row.publishPermission === 1 ? 0 : 1
  const actionLabel = nextPerm === 1 ? '授予' : '回收'
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}「${row.realName || row.studentId}」的服务发布权限吗？`,
      `${actionLabel}发布权限`,
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    operatingId.value = row.userId
    await updateAdminUserStatus({ userId: row.userId, publishPermission: nextPerm })
    ElMessage.success(`已${actionLabel}发布权限`)
    await fetchList()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '更新发布权限失败')
  } finally {
    operatingId.value = null
  }
}

async function toggleAccountStatus(row: AdminUser) {
  const nextStatus = row.accountStatus === 1 ? 2 : 1
  const actionLabel = nextStatus === 2 ? '封禁禁用' : '恢复启用'
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}账号「${row.realName || row.studentId}（${row.studentId}）」吗？`,
      `${actionLabel}账号`,
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    operatingId.value = row.userId
    await updateAdminUserStatus({ userId: row.userId, accountStatus: nextStatus })
    ElMessage.success(`账号已${actionLabel}`)
    await fetchList()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '更新账号状态失败')
  } finally {
    operatingId.value = null
  }
}

async function handleResetPassword(row: AdminUser) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.realName || row.studentId}（${row.studentId}）」的登录密码重置为默认密码 password123 吗？`,
      '重置密码确认',
      { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' }
    )
    operatingId.value = row.userId
    await resetAdminUserPassword(row.userId)
    ElMessage.success('密码已重置为默认密码 password123')
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '重置密码失败')
  } finally {
    operatingId.value = null
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.filter-card {
  padding: 14px 20px;
  margin-bottom: 18px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

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
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .role-tag {
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 999px;
    background: var(--xyf-primary-soft);
    color: var(--xyf-primary-dark);
    font-weight: 600;
  }

  .user-sub {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.contact-col {
  display: flex;
  flex-direction: column;
  font-size: 13px;
}

.sub-text {
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
