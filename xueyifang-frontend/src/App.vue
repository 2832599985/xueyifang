<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import { useRegisterConfigStore } from '@/stores/registerConfig'
import { useWebSocket } from '@/composables/useWebSocket'

const appStore = useAppStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const chatStore = useChatStore()
const registerConfigStore = useRegisterConfigStore()

useWebSocket()

onMounted(async () => {
  registerConfigStore.fetchRegisterStatus()

  if (appStore.token) {
    try {
      await userStore.fetchCurrentUser()
      if (userStore.isLogin) {
        notificationStore.fetchUnreadCount()
        chatStore.fetchConversations()
      }
    } catch {
      // 登录态失效由拦截器或路由守卫处理
    }
  }
})
</script>
