import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import type { ChatMessage } from '@/types/chat'
import type { NotificationItem } from '@/types/notification'

export interface WebSocketMessage {
  type: string
  data: any
}

const MESSAGE_TYPES = {
  NEW_CHAT: 'NEW_CHAT',
  NEW_NOTIFICATION: 'NEW_NOTIFICATION',
  UNREAD_UPDATE: 'UNREAD_UPDATE',
  PONG: 'PONG',
  CONNECTED: 'CONNECTED'
}

/**
 * 统一走当前源的 `/api/ws`：
 * - 开发环境通过 Vite proxy (`ws: true`) 自动转发到 `VITE_API_TARGET`
 * - 生产环境通过 Nginx `/api/` 转发到后端
 */
const WS_BASE_URL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/api/ws`

const RECONNECT_INTERVAL = 3000
const MAX_RECONNECT_ATTEMPTS = 10
const HEARTBEAT_INTERVAL = 30000

let wsInstance: WebSocket | null = null
let reconnectAttempts = 0
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let manualClose = false

const sharedConnected = ref(false)
const sharedConnectionError = ref<string | null>(null)

type MessageHandler = (message: WebSocketMessage) => void
const messageHandlers: Set<MessageHandler> = new Set()

export function useWebSocket() {
  const userStore = useUserStore()
  const notificationStore = useNotificationStore()
  const chatStore = useChatStore()

  const getWsUrl = () => {
    const token = localStorage.getItem('token')
    if (!token) return null
    return `${WS_BASE_URL}?token=${encodeURIComponent(token)}`
  }

  const sendHeartbeat = () => {
    if (wsInstance && wsInstance.readyState === WebSocket.OPEN) {
      wsInstance.send(JSON.stringify({ type: 'PING' }))
    }
  }

  const startHeartbeat = () => {
    stopHeartbeat()
    heartbeatTimer = setInterval(sendHeartbeat, HEARTBEAT_INTERVAL)
  }

  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  const handleNewChat = (_chatMessage: ChatMessage) => {
    chatStore.fetchConversations()
  }

  const handleNewNotification = (notification: NotificationItem) => {
    notificationStore.unreadCount++
    notificationStore.recentNotifications.unshift(notification)
    if (notificationStore.recentNotifications.length > 5) {
      notificationStore.recentNotifications.pop()
    }
  }

  const handleMessage = (event: MessageEvent) => {
    try {
      const message: WebSocketMessage = JSON.parse(event.data)

      switch (message.type) {
        case MESSAGE_TYPES.CONNECTED:
          break
        case MESSAGE_TYPES.NEW_CHAT:
          handleNewChat(message.data)
          break
        case MESSAGE_TYPES.NEW_NOTIFICATION:
          handleNewNotification(message.data)
          break
        case MESSAGE_TYPES.PONG:
          break
        default:
          break
      }

      messageHandlers.forEach((handler) => handler(message))
    } catch (error) {
      console.error('[WebSocket] 消息解析失败:', error)
    }
  }

  const scheduleReconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }
    reconnectAttempts++
    reconnectTimer = setTimeout(() => {
      connect()
    }, RECONNECT_INTERVAL)
  }

  const connect = () => {
    if (wsInstance && (wsInstance.readyState === WebSocket.OPEN || wsInstance.readyState === WebSocket.CONNECTING)) {
      return
    }

    const url = getWsUrl()
    if (!url) return

    try {
      wsInstance = new WebSocket(url)

      wsInstance.onopen = () => {
        sharedConnected.value = true
        sharedConnectionError.value = null
        reconnectAttempts = 0
        startHeartbeat()
      }

      wsInstance.onmessage = handleMessage

      wsInstance.onclose = () => {
        sharedConnected.value = false
        wsInstance = null
        stopHeartbeat()

        if (!manualClose && userStore.isLogin && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          scheduleReconnect()
        }
        manualClose = false
      }

      wsInstance.onerror = () => {
        sharedConnectionError.value = '连接中断'
        sharedConnected.value = false
      }
    } catch {
      sharedConnectionError.value = '连接失败'
    }
  }

  const disconnect = () => {
    manualClose = true
    stopHeartbeat()

    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (wsInstance) {
      wsInstance.close()
      wsInstance = null
    }

    sharedConnected.value = false
    reconnectAttempts = 0
  }

  watch(
    () => userStore.isLogin,
    (isLogin) => {
      if (isLogin) {
        connect()
      } else {
        disconnect()
      }
    }
  )

  onMounted(() => {
    if (userStore.isLogin) {
      connect()
    }
  })

  onUnmounted(() => {
    // 全局状态由 App 生命周期管理，组件卸载不主动断开共享连接
  })

  return {
    isConnected: sharedConnected,
    connectionError: sharedConnectionError,
    connect,
    disconnect
  }
}

export function onWebSocketMessage(handler: MessageHandler) {
  messageHandlers.add(handler)
  return () => {
    messageHandlers.delete(handler)
  }
}

export function isWebSocketConnected(): boolean {
  return sharedConnected.value
}

export { MESSAGE_TYPES }
