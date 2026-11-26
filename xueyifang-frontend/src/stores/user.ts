import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginRequest } from '@/types/auth'
import { authApi } from '@/api/auth'
import { useAppStore } from './app'
import { useChatStore } from './chat'
import { useNotificationStore } from './notification'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const appStore = useAppStore()

  const setUserInfo = (user: UserInfo) => {
    userInfo.value = user
  }

  const clearUserInfo = () => {
    userInfo.value = null
  }

  const fetchCurrentUser = async (): Promise<UserInfo | undefined> => {
    try {
      if (!appStore.token) {
        clearUserInfo()
        return undefined
      }
      const user = await authApi.getCurrentUser()
      setUserInfo(user)
      return user
    } catch (error) {
      console.error('获取用户信息失败:', error)
      clearUserInfo()
      throw error
    }
  }

  const ensureUserInfo = async (): Promise<UserInfo | null> => {
    if (userInfo.value) return userInfo.value
    if (!appStore.token) return null
    try {
      return (await fetchCurrentUser()) ?? null
    } catch {
      return null
    }
  }

  const login = async (data: LoginRequest): Promise<UserInfo> => {
    const res = await authApi.login(data)
    appStore.setToken(res.token)
    setUserInfo(res.userInfo)
    return res.userInfo
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      appStore.clearToken()
      clearUserInfo()
      useChatStore().reset()
      useNotificationStore().reset()
    }
  }

  const isLogin = computed(() => !!appStore.token && !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 2)
  const hasPublishPermission = computed(() => userInfo.value?.publishPermission === 1)
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.realName || ''
  )

  return {
    userInfo,
    isLogin,
    isAdmin,
    hasPublishPermission,
    displayName,
    setUserInfo,
    clearUserInfo,
    fetchCurrentUser,
    ensureUserInfo,
    login,
    logout
  }
})
