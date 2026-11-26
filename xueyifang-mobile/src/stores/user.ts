import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface MobileUser {
  id: number
  studentId: string
  realName: string
  nickname: string
  campusId?: number
  campusName?: string
  eduVerified?: number
  verifiedEduEmail?: string
  creditScore?: number
  walletBalance?: number
  publishPermission?: number
  avatar?: string
}

export const useMobileUserStore = defineStore('mobileUser', () => {
  const token = ref<string>('')
  const userInfo = ref<MobileUser | null>(null)
  const activeCampusId = ref<number>(1)

  const isLoggedIn = computed(() => Boolean(token.value && userInfo.value))

  function hydrateFromStorage() {
    token.value = uni.getStorageSync('XYF_TOKEN') || ''
    const rawUser = uni.getStorageSync('XYF_USER')
    if (rawUser) {
      try {
        userInfo.value = typeof rawUser === 'string' ? JSON.parse(rawUser) : rawUser
      } catch {
        userInfo.value = null
      }
    }
    const savedCampus = Number(uni.getStorageSync('XYF_CAMPUS_ID'))
    if (savedCampus > 0) {
      activeCampusId.value = savedCampus
    }
  }

  function setSession(newToken: string, user: MobileUser) {
    token.value = newToken
    userInfo.value = user
    if (user.campusId) {
      activeCampusId.value = user.campusId
      uni.setStorageSync('XYF_CAMPUS_ID', user.campusId)
    }
    uni.setStorageSync('XYF_TOKEN', newToken)
    uni.setStorageSync('XYF_USER', JSON.stringify(user))
  }

  function clearSession() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('XYF_TOKEN')
    uni.removeStorageSync('XYF_USER')
  }

  return {
    token,
    userInfo,
    activeCampusId,
    isLoggedIn,
    hydrateFromStorage,
    setSession,
    clearSession
  }
})
