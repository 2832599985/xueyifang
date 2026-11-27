<template>
  <div class="notifications-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="消息通知">
        <template #meta>
          <span class="unread-meta xyf-tabular">
            未读 <strong>{{ notificationStore.unreadCount }}</strong> 条
          </span>
        </template>
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
      </div>

      <section class="xyf-surface-card list-card">
        <el-skeleton v-if="loading" :rows="6" animated />

        <XyfEmptyState
          v-else-if="notifications.length === 0"
          title="暂无相关系统通知"
          description="当订单状态变更、服务审核或管理员处理申请时，会第一时间在此通知您。"
        />

        <div v-else class="inbox-sections">
          <!-- Unread Group (within current page) -->
          <div v-if="unreadItems.length > 0" class="inbox-group">
            <div class="inbox-group__label">未读（本页 {{ unreadItems.length }}）</div>
            <div class="notify-list">
              <article
                v-for="item in unreadItems"
                :key="item.id"
                class="notify-row is-unread"
                @click="handleOpenItem(item)"
              >
                <div class="notify-row__main">
                  <div class="notify-top">
                    <XyfStatusBadge
                      :tone="typeTone(item.notificationType)"
                      :label="notificationTypeMap[item.notificationType] || '系统通知'"
                      :dot="true"
                    />
                    <span class="notify-time xyf-tabular">{{ formatNotificationTime(item.createTime) }}</span>
                  </div>
                  <h3 class="notify-title">{{ item.title }}</h3>
                  <p class="notify-content">{{ item.content }}</p>
                </div>

                <div class="notify-row__actions" @click.stop>
                  <el-button
                    size="small"
                    text
                    class="read-action-btn"
                    @click="handleMarkRead(item)"
                  >
                    标为已读
                  </el-button>
                  <el-button
                    v-if="getNotificationRoute(item)"
                    size="small"
                    @click="handleOpenItem(item)"
                  >
                    查看
                  </el-button>
                </div>
              </article>
            </div>
          </div>

          <!-- Read / Earlier Group (within current page) -->
          <div v-if="readItems.length > 0" class="inbox-group">
            <div v-if="unreadItems.length > 0" class="inbox-group__label">更早</div>
            <div class="notify-list">
              <article
                v-for="item in readItems"
                :key="item.id"
                class="notify-row"
                @click="handleOpenItem(item)"
              >
                <div class="notify-row__main">
                  <div class="notify-top">
                    <XyfStatusBadge
                      :tone="item.notificationType === 2 || item.notificationType === 4 ? 'danger' : 'neutral'"
                      :label="notificationTypeMap[item.notificationType] || '系统通知'"
                    />
                    <span class="notify-time xyf-tabular">{{ formatNotificationTime(item.createTime) }}</span>
                  </div>
                  <h3 class="notify-title">{{ item.title }}</h3>
                  <p class="notify-content">{{ item.content }}</p>
                </div>

                <div class="notify-row__actions" @click.stop>
                  <el-button
                    v-if="getNotificationRoute(item)"
                    size="small"
                    text
                    @click="handleOpenItem(item)"
                  >
                    查看
                  </el-button>
                </div>
              </article>
            </div>
          </div>
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
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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

const unreadItems = computed(() => notifications.value.filter((n) => n.isRead === 0))
const readItems = computed(() => notifications.value.filter((n) => n.isRead !== 0))

function typeTone(type: NotificationType): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  if (type === 2 || type === 4) return 'danger'
  if (type === 1) return 'success'
  if (type === 3) return 'info'
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
.unread-meta {
  font-size: 13px;
  color: var(--xyf-ink-3);

  strong {
    color: var(--xyf-ink);
    font-weight: 600;
  }
}

.toolbar-row {
  margin-bottom: 16px;
}

.list-card {
  padding: 20px 24px;
}

.inbox-sections {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.inbox-group {
  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
    padding-bottom: 8px;
    border-bottom: 1px solid var(--xyf-border);
  }
}

.notify-list {
  display: flex;
  flex-direction: column;
}

.notify-row {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 14px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  cursor: pointer;
  transition: background-color var(--xyf-transition);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--xyf-surface-soft);

    .read-action-btn {
      opacity: 1;
    }
  }

  &.is-unread {
    border-left: 2px solid var(--xyf-champagne);
    padding-left: 16px;

    .notify-title {
      font-weight: 600;
      color: var(--xyf-ink);
    }
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

.read-action-btn {
  opacity: 0;
  transition: opacity var(--xyf-transition);
}

.notify-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;

  .notify-time {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.notify-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--xyf-ink-2);
}

.notify-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--xyf-ink-3);
}

@media (hover: none) {
  .read-action-btn {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .list-card {
    padding: 14px;
  }

  .notify-row {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
