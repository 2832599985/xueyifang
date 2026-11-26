/**
 * 登录态相关工具：统一处理 token 失效、清理本地状态、安全跳转、记住学号
 */
import { ElMessage } from 'element-plus'
import { ErrorCode } from '@/types/http'

const REMEMBERED_STUDENT_ID_KEY = 'xyf_remembered_student_id'

/** 后端表示"登录态失效"的业务码 */
export const AUTH_EXPIRED_CODES: number[] = [
  ErrorCode.USER_NOT_LOGIN,
  ErrorCode.TOKEN_INVALID,
  ErrorCode.TOKEN_EXPIRED
]

export function isAuthError(error: any): boolean {
  if (!error) return false
  if (error.response?.status === 401) return true
  const code = typeof error.code === 'number' ? error.code : error.response?.data?.code
  return typeof code === 'number' && AUTH_EXPIRED_CODES.includes(code)
}

export function getRememberedStudentId(): string {
  return localStorage.getItem(REMEMBERED_STUDENT_ID_KEY) || ''
}

export function setRememberedStudentId(studentId: string | null): void {
  if (studentId && studentId.trim()) {
    localStorage.setItem(REMEMBERED_STUDENT_ID_KEY, studentId.trim())
  } else {
    localStorage.removeItem(REMEMBERED_STUDENT_ID_KEY)
  }
}

export async function clearAuthState(): Promise<void> {
  const [{ useAppStore }, { useUserStore }, { useChatStore }, { useNotificationStore }] = await Promise.all([
    import('@/stores/app'),
    import('@/stores/user'),
    import('@/stores/chat'),
    import('@/stores/notification')
  ])
  useAppStore().clearToken()
  useUserStore().clearUserInfo()
  useChatStore().reset()
  useNotificationStore().reset()
}

/**
 * 校验登录后的 redirect 参数，只允许站内相对路径，防止开放重定向
 */
export function sanitizeRedirect(raw: unknown, fallback: string): string {
  if (typeof raw !== 'string') return fallback
  if (!raw.startsWith('/') || raw.startsWith('//')) return fallback
  return raw
}

let handlingAuthExpired = false

export async function handleAuthExpired(): Promise<void> {
  if (handlingAuthExpired) return
  handlingAuthExpired = true
  try {
    await clearAuthState()
    const { default: router } = await import('@/router')
    const route = router.currentRoute.value
    const needLogin = route.matched.some(r => r.meta.requiresAuth || r.meta.requiresAdmin)
    if (!needLogin) return

    ElMessage.warning('登录已过期，请重新登录')
    const loginPath = route.path.startsWith('/admin') ? '/admin/login' : '/login'
    await router.replace({ path: loginPath, query: { redirect: route.fullPath } })
  } finally {
    setTimeout(() => {
      handlingAuthExpired = false
    }, 500)
  }
}
