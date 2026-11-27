<template>
  <div class="chat-page xyf-page">
    <div class="xyf-container">
      <div class="chat-shell xyf-surface-card">
        <!-- Left Conversations List -->
        <aside class="chat-sidebar" :class="{ 'is-hidden-mobile': Boolean(selectedUserId) }">
          <div class="chat-sidebar__head">
            <div>
              <h2>站内消息</h2>
            </div>
            <el-button text size="small" @click="fetchConversations">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>

          <div class="chat-sidebar__search">
            <el-input
              v-model="searchPeer"
              placeholder="搜索联系人姓名..."
              clearable
              size="default"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="conversation-list">
            <el-skeleton v-if="convLoading && conversations.length === 0" :rows="5" animated style="padding: 14px" />

            <XyfEmptyState
              v-else-if="!convLoading && filteredConversations.length === 0"
              title="暂无对话记录"
              description="在服务详情或订单页面点击“联系卖家/买家”即可发起即时沟通。"
            />

            <button
              v-else
              v-for="conv in filteredConversations"
              :key="conv.userId"
              type="button"
              class="conv-item"
              :class="{ 'is-active': selectedUserId === conv.userId }"
              @click="selectConversation(conv)"
            >
              <XyfAvatar :src="conv.avatar" :name="conv.realName" :size="40" />
              <div class="conv-item__body">
                <div class="conv-top">
                  <span class="conv-name ellipsis">{{ conv.realName }}</span>
                  <span class="conv-time xyf-tabular">
                    {{ formatDateTimeMinute(conv.lastMessageTime) }}
                  </span>
                </div>
                <div class="conv-bottom">
                  <span class="conv-preview ellipsis">{{ conv.lastMessage || '点击开始对话' }}</span>
                  <span v-if="conv.unreadCount > 0" class="unread-badge xyf-tabular">
                    {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                  </span>
                </div>
              </div>
            </button>
          </div>
        </aside>

        <!-- Right Message Thread -->
        <section class="chat-thread" :class="{ 'is-hidden-mobile': !selectedUserId }">
          <template v-if="!selectedUserId">
            <div class="thread-placeholder">
              <XyfEmptyState
                title="选择左侧会话开始沟通"
                description="支持文字、图片发送与订单沟通，相关消息将留存记录。"
              />
            </div>
          </template>

          <template v-else>
            <header class="thread-head">
              <div class="thread-head__peer">
                <el-button
                  text
                  class="mobile-back-btn"
                  @click="clearSelectionMobile"
                >
                  <el-icon><ArrowLeft /></el-icon>
                </el-button>
                <XyfAvatar
                  :src="activePeerAvatar"
                  :name="activePeerName"
                  :size="36"
                />
                <div>
                  <div class="peer-title">{{ activePeerName }}</div>
                  <div class="peer-sub">学艺坊校内实名对话 · 沟通内容可作担保凭证</div>
                </div>
              </div>

              <div class="thread-head__context" v-if="relatedOrderId || relatedServiceId">
                <el-button
                  v-if="relatedOrderId"
                  size="small"
                  plain
                  @click="router.push(`/orders/${relatedOrderId}`)"
                >
                  查看关联订单 #{{ relatedOrderId }}
                </el-button>
                <el-button
                  v-if="relatedServiceId"
                  size="small"
                  plain
                  @click="router.push(`/services/${relatedServiceId}`)"
                >
                  查看咨询服务 #{{ relatedServiceId }}
                </el-button>
              </div>
            </header>

            <div ref="messagesContainerRef" class="thread-messages">
              <div v-if="hasMoreHistory" class="load-more-row">
                <el-button text size="small" :loading="msgLoading" @click="loadOlderMessages">
                  加载更早聊天记录
                </el-button>
              </div>

              <div v-if="msgLoading && messages.length === 0" style="padding: 16px">
                <el-skeleton :rows="4" animated />
              </div>

              <div
                v-for="msg in messages"
                :key="msg.messageId"
                class="msg-row"
                :class="{ 'is-mine': msg.sender?.userId === userStore.userInfo?.id }"
              >
                <XyfAvatar
                  v-if="msg.sender?.userId !== userStore.userInfo?.id"
                  :src="msg.sender?.avatar || activePeerAvatar"
                  :name="msg.sender?.realName || activePeerName"
                  :size="32"
                />

                <div class="msg-bubble-wrap">
                  <button
                    v-if="msg.relatedOrderId"
                    type="button"
                    class="msg-ref-chip xyf-mono"
                    @click="router.push(`/orders/${msg.relatedOrderId}`)"
                  >
                    关联订单 #{{ msg.relatedOrderId }}
                  </button>
                  <button
                    v-else-if="msg.relatedServiceId"
                    type="button"
                    class="msg-ref-chip xyf-mono"
                    @click="router.push(`/services/${msg.relatedServiceId}`)"
                  >
                    咨询服务 #{{ msg.relatedServiceId }}
                  </button>

                  <div v-if="msg.messageType === 2" class="msg-image-box">
                    <el-image
                      :src="msg.content"
                      :preview-src-list="[msg.content]"
                      fit="cover"
                      class="msg-image"
                    />
                  </div>
                  <div v-else class="msg-bubble">
                    {{ msg.content }}
                  </div>

                  <div class="msg-meta xyf-tabular">
                    {{ formatDateTimeMinute(msg.createTime) }}
                  </div>
                </div>

                <XyfAvatar
                  v-if="msg.sender?.userId === userStore.userInfo?.id"
                  :src="userStore.userInfo?.avatar"
                  :name="userStore.displayName"
                  :size="32"
                />
              </div>
            </div>

            <footer class="thread-composer">
              <!-- Campus Quick Phrases + Image Upload Bar -->
              <div class="composer-tools">
                <label class="tool-upload-btn">
                  <input type="file" accept="image/*" hidden :disabled="uploadingImg || sending" @change="handleSendImage" />
                  <span>{{ uploadingImg ? '图片发送中...' : '发送图片' }}</span>
                </label>

                <button
                  v-for="phrase in quickPhrases"
                  :key="phrase"
                  type="button"
                  class="phrase-chip"
                  @click="fillQuickPhrase(phrase)"
                >
                  {{ phrase }}
                </button>
              </div>

              <el-input
                v-model="draftMessage"
                type="textarea"
                :rows="3"
                resize="none"
                maxlength="500"
                placeholder="输入消息内容，按 Enter 发送，Shift + Enter 换行..."
                @keydown.enter.exact.prevent="handleSend"
              />
              <div class="composer-bar">
                <span class="composer-hint">校内交易建议在此明确交付标准、时间及线下会面地点</span>
                <el-button
                  type="primary"
                  :loading="sending"
                  :disabled="!draftMessage.trim()"
                  @click="handleSend"
                >
                  发送消息
                </el-button>
              </div>
            </footer>
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Refresh, ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { listConversations, listMessages, sendChatMessage } from '@/api/chat'
import { uploadFile } from '@/api/file'
import { notifyFallbackError } from '@/api/request'
import { formatDateTimeMinute } from '@/utils/format'
import { onWebSocketMessage, MESSAGE_TYPES } from '@/composables/useWebSocket'
import type { ChatConversation, ChatMessage } from '@/types/chat'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const convLoading = ref(false)
const msgLoading = ref(false)
const sending = ref(false)
const uploadingImg = ref(false)
const searchPeer = ref('')
const conversations = ref<ChatConversation[]>([])
const messages = ref<ChatMessage[]>([])
const draftMessage = ref('')
const pageNum = ref(1)
const pageSize = 20
const totalMessages = ref(0)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

const quickPhrases = [
  '同学你好，请问今天什么时段方便？',
  '我已到达校内约定交接地点',
  '交付成果已提交，辛苦查收验收'
]

const selectedUserId = computed<number | null>(() => {
  const raw = Number(route.params.userId)
  return Number.isFinite(raw) && raw > 0 ? raw : null
})

const relatedOrderId = computed(() => {
  const v = Number(route.query.relatedOrderId)
  return Number.isFinite(v) && v > 0 ? v : undefined
})

const relatedServiceId = computed(() => {
  const v = Number(route.query.relatedServiceId)
  return Number.isFinite(v) && v > 0 ? v : undefined
})

const activeConversation = computed(() =>
  conversations.value.find((c) => c.userId === selectedUserId.value)
)

const activePeerName = computed(
  () => activeConversation.value?.realName || `用户 #${selectedUserId.value || ''}`
)

const activePeerAvatar = computed(() => activeConversation.value?.avatar || '')

const filteredConversations = computed(() => {
  const q = searchPeer.value.trim().toLowerCase()
  if (!q) return conversations.value
  return conversations.value.filter((c) => (c.realName || '').toLowerCase().includes(q))
})

const hasMoreHistory = computed(() => messages.value.length < totalMessages.value)

function scrollToBottom() {
  nextTick(() => {
    const el = messagesContainerRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function fillQuickPhrase(phrase: string) {
  draftMessage.value = draftMessage.value ? `${draftMessage.value} ${phrase}` : phrase
}

async function fetchConversations() {
  convLoading.value = true
  try {
    conversations.value = await listConversations()
    await chatStore.fetchConversations().catch(() => {})
  } catch (err) {
    notifyFallbackError(err, '加载会话列表失败')
  } finally {
    convLoading.value = false
  }
}

async function fetchMessages(reset = true) {
  if (!selectedUserId.value) return
  if (reset) {
    pageNum.value = 1
    messages.value = []
  }
  msgLoading.value = true
  try {
    const res = await listMessages(selectedUserId.value, {
      pageNum: pageNum.value,
      pageSize
    })
    const batch = (res.records || []).slice().reverse()
    if (reset) {
      messages.value = batch
      scrollToBottom()
    } else {
      messages.value = [...batch, ...messages.value]
    }
    totalMessages.value = res.total || 0
    chatStore.clearConversationUnread(selectedUserId.value)
    await fetchConversations()
  } catch (err) {
    notifyFallbackError(err, '加载消息记录失败')
  } finally {
    msgLoading.value = false
  }
}

async function loadOlderMessages() {
  pageNum.value += 1
  await fetchMessages(false)
}

function selectConversation(conv: ChatConversation) {
  router.push({ name: 'ChatWithUser', params: { userId: conv.userId } })
}

function clearSelectionMobile() {
  router.push({ name: 'Chat' })
}

async function handleSend() {
  const text = draftMessage.value.trim()
  if (!text || !selectedUserId.value || sending.value) return

  sending.value = true
  try {
    await sendChatMessage({
      receiverId: selectedUserId.value,
      content: text,
      messageType: 1,
      relatedOrderId: relatedOrderId.value,
      relatedServiceId: relatedServiceId.value
    })
    draftMessage.value = ''
    await fetchMessages(true)
  } catch (err) {
    notifyFallbackError(err, '发送消息失败')
  } finally {
    sending.value = false
  }
}

async function handleSendImage(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !selectedUserId.value) return

  uploadingImg.value = true
  try {
    const imageUrl = await uploadFile(file, 'chat_image')
    await sendChatMessage({
      receiverId: selectedUserId.value,
      content: imageUrl,
      messageType: 2,
      relatedOrderId: relatedOrderId.value,
      relatedServiceId: relatedServiceId.value
    })
    await fetchMessages(true)
  } catch (err) {
    notifyFallbackError(err, '发送图片消息失败')
  } finally {
    uploadingImg.value = false
  }
}

watch(
  selectedUserId,
  (id) => {
    if (id) {
      fetchMessages(true)
    }
  },
  { immediate: true }
)

let stopWsListener: (() => void) | null = null

onMounted(() => {
  fetchConversations()

  stopWsListener = onWebSocketMessage((msg) => {
    if (msg.type === MESSAGE_TYPES.NEW_CHAT) {
      const incoming = msg.data as ChatMessage
      fetchConversations()
      if (
        selectedUserId.value &&
        (incoming.sender?.userId === selectedUserId.value ||
          incoming.receiver?.userId === selectedUserId.value)
      ) {
        messages.value.push(incoming)
        scrollToBottom()
      }
    }
  })
})

onUnmounted(() => {
  if (stopWsListener) {
    stopWsListener()
    stopWsListener = null
  }
})
</script>

<style scoped lang="scss">
.chat-page {
  padding-top: 20px;
  padding-bottom: 24px;
}

.chat-shell {
  height: calc(100vh - 140px);
  min-height: 560px;
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  overflow: hidden;
}

.chat-sidebar {
  border-right: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  background: var(--xyf-surface);
  min-height: 0;

  &__head {
    padding: 16px 18px 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    h2 {
      margin: 0 0 4px;
      font-size: 16.5px;
      font-weight: 700;
      color: var(--xyf-ink);
    }
  }

  &__search {
    padding: 0 16px 12px;
    border-bottom: 1px solid var(--xyf-border-light);
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 12px;
  border: none;
  border-radius: var(--xyf-radius-sm);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background var(--xyf-transition);

  &:hover {
    background: var(--xyf-bg);
  }

  &.is-active {
    background: #f4f4f5;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }
}

.conv-top,
.conv-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conv-top {
  margin-bottom: 4px;

  .conv-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .conv-time {
    font-size: 11px;
    color: var(--xyf-ink-light);
    flex-shrink: 0;
  }
}

.conv-bottom {
  .conv-preview {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }

  .unread-badge {
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 999px;
    background: #18181b;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
}

.chat-thread {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--xyf-bg);
}

.thread-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thread-head {
  padding: 14px 20px;
  background: var(--xyf-surface);
  border-bottom: 1px solid var(--xyf-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;

  &__peer {
    display: flex;
    align-items: center;
    gap: 10px;

    .peer-title {
      font-size: 15px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    .peer-sub {
      font-size: 12px;
      color: var(--xyf-ink-muted);
    }
  }

  &__context {
    display: flex;
    gap: 8px;
  }
}

.mobile-back-btn {
  display: none;
  padding: 4px 8px;
}

.thread-messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.load-more-row {
  text-align: center;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 78%;

  &.is-mine {
    align-self: flex-end;

    .msg-bubble-wrap {
      align-items: flex-end;
    }

    .msg-bubble {
      background: #18181b;
      color: #ffffff;
      border-color: #18181b;
      border-top-right-radius: 4px;
    }
  }
}

.msg-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.msg-ref-chip {
  padding: 2px 8px;
  border-radius: 5px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-3);
  font-size: 11px;
  cursor: pointer;

  &:hover {
    border-color: #18181b;
    color: var(--xyf-ink);
  }
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: var(--xyf-radius-md);
  border-top-left-radius: 4px;
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  color: var(--xyf-ink);
  font-size: 14px;
  line-height: 1.58;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-image-box {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
}

.msg-image {
  width: 196px;
  max-height: 196px;
  display: block;
  cursor: zoom-in;
}

.msg-meta {
  font-size: 11px;
  color: var(--xyf-ink-light);
}

.thread-composer {
  background: var(--xyf-surface);
  border-top: 1px solid var(--xyf-border);
  padding: 10px 18px 14px;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.tool-upload-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #18181b;
  background: #18181b;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  transition: opacity var(--xyf-transition);

  &:hover {
    opacity: 0.88;
  }
}

.phrase-chip {
  height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface-soft);
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #a1a1aa;
    color: var(--xyf-ink);
  }
}

.composer-bar {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  .composer-hint {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

@media (max-width: 768px) {
  .chat-shell {
    grid-template-columns: 1fr;
    height: calc(100vh - 110px);
  }

  .is-hidden-mobile {
    display: none;
  }

  .mobile-back-btn {
    display: inline-flex;
  }

  .msg-row {
    max-width: 90%;
  }
}
</style>
