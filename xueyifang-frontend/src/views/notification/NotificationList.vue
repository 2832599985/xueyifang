<template>
  <div class="notifications-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        kicker="INBOX ALERTS"
        title="系统通知中心"
        subtitle="实时汇聚订单进度、服务审核意见、发布权限与平台仲裁通知"
      >
        <template #actions>
          <el-button
            :disabled="notificationStore.unreadCount === 0"
            @click="handleMarkAllRead"
          >
            全部标为已读
          </el-button>
        </template>
      </XyfPageHeader>

      <div class="toolbar-row">
        <div class="xyf-chip-group">
          <button
            type="button"
            class="xyf-chip"
            :class="{ 'is-active': selectedType === undefined }"
            @click="selectType(undefined)"
          >
            全部通知
          </button>
          <button
            v-for="opt in notificationTypeOptions"
            :key="opt.value"
            type="button"
            class="xyf-chip"
            :class="{ 'is-active': selectedType === opt.value }"
            @click="selectType(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>

        <span class="unread-summary">
          未读 <strong class="xyf-tabular">{{ notificationStore.unreadCount }}</strong> 条
        </span>
      </div>

      <section class="xyf-surface-card list-card">
        <el-skeleton v-if="loading" :rows="6" animated />

        <XyfEmptyState
          v-else-if="notifications.length === 0"
          title="暂无相关系统通知"
          description="当订单状态变更、服务审核或管理员处理申请时，会第一时间在此通知您。"
        />

        <div v-else class="notify-stack">
          <article
            v-for="item in notifications"
            :key="item.id"
            class="notify-item"
            :class="{ 'is-unread': item.isRead === 0 }"
            @click="handleOpenItem(item)"
          >
            <div class="notify-item__main">
              <div class="notify-top">
                <XyfStatusBadge
                  :tone="typeTone(item.notificationType)"
                  :label="notificationTypeMap[item.notificationType] || '系统通知'"
                  :dot="item.isRead === 0"
                />
                <span class="notify-time xyf-tabular">{{ formatNotificationTime(item.createTime) }}</span>
              </div>
              <h3 class="notify-title">{{ item.title }}</h3>
              <p class="notify-content">{{ item.content }}</p>
            </div>

            <div class="notify-item__actions" @click.stop>
              <el-button
                v-if="item.isRead === 0"
                size="small"
                text
                type="primary"
                @click="handleMarkRead(item)"
              >
                标为已读
              </el-button>
              <el-button
                v-if="getNotificationRoute(item)"
                size="small"
                plain
                @click="handleOpenItem(item)"
              >
                去查看
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
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import {
  listMyNotifications,
  markNotificationRead,
  markAllRead
} from '@/api/notification'
import { notifyFallbackError } from '@/api/request'
import {
  notificationTypeMap,
  notificationTypeOptions,
  getNotificationRoute,
  formatNotificationTime,
  type NotificationItem,
  type NotificationType
} from '@/types/notification'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const notificationStore = useNotificationStore()

const loading = ref(true)
const selectedType = ref<NotificationType | undefined>(undefined)
const notifications = ref<NotificationItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12

function typeTone(type: NotificationType) {
  if (type === 1) return 'success'
  if (type === 2 || type === 4) return 'danger'
  if (type === 3) return 'teal'
  return 'warning'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listMyNotifications({
      pageNum: pageNum.value,
      pageSize,
      notificationType: selectedType.value
    })
    notifications.value = res.records || []
    total.value = res.total || 0
    await notificationStore.fetchUnreadCount().catch(() => {})
  } catch (err) {
    notifyFallbackError(err, '获取通知列表失败')
  } finally {
    loading.value = false
  }
}

function selectType(type: NotificationType | undefined) {
  selectedType.value = type
  pageNum.value = 1
  fetchList()
}

async function handleMarkRead(item: NotificationItem) {
  if (item.isRead === 1) return
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
    notificationStore.fetchUnreadCount().catch(() => {})
  } catch (err) {
    notifyFallbackError(err, '操作失败')
  }
}

async function handleOpenItem(item: NotificationItem) {
  if (item.isRead === 0) {
    await handleMarkRead(item)
  }
  const target = getNotificationRoute(item)
  if (target) {
    router.push(target.path)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    notifications.value.forEach((n) => {
      n.isRead = 1
    })
    await notificationStore.fetchUnreadCount().catch(() => {})
    ElMessage.success('已全部标为已读')
  } catch (err) {
    notifyFallbackError(err, '操作失败')
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
  margin-bottom: 16px;
}

.unread-summary {
  font-size: 13px;
  color: var(--xyf-ink-muted);

  strong {
    color: var(--xyf-accent);
  }
}

.list-card {
  padding: 18px 22px;
}

.notify-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: var(--xyf-radius-md);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-bg);
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-primary);
    background: var(--xyf-surface);
  }

  &.is-unread {
    background: var(--xyf-surface);
    border-color: var(--xyf-primary-border);
    box-shadow: var(--xyf-shadow-sm);
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }
}

.notify-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .notify-time {
    font-size: 12px;
    color: var(--xyf-ink-light);
  }
}

.notify-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.notify-content {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--xyf-ink-secondary);
}

.pagination-wrap {
  margin-top: 22px;
  display: flex;
  justify-content: center;
}
</style>
