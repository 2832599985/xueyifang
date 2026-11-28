import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/http'
import { AUTH_EXPIRED_CODES, handleAuthExpired } from '@/utils/auth'

export type BizError = Error & {
  code?: number
  handled?: boolean
}

function createBizError(message: string, code?: number, handled = true): BizError {
  const error = new Error(message) as BizError
  error.code = code
  error.handled = handled
  return error
}

/**
 * 在组件 catch 块中兜底提示错误，若拦截器已弹窗则跳过，彻底避免双弹窗
 */
export function notifyFallbackError(error: any, fallback = '操作失败，请稍后重试'): void {
  if (error === 'cancel' || error === 'close') return
  if (error?.handled || typeof error?.code === 'number') return
  ElMessage.error(error?.message || fallback)
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse

    if (res.code === 0) {
      return res.data
    }

    const message = res.message || '请求失败'
    if (AUTH_EXPIRED_CODES.includes(res.code)) {
      void handleAuthExpired()
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(createBizError(message, res.code, true))
  },
  (error: AxiosError) => {
    const responseData = error.response?.data as ApiResponse | undefined
    const bizCode = typeof responseData?.code === 'number' ? responseData.code : undefined

    if (error.response?.status === 401 || (bizCode !== undefined && AUTH_EXPIRED_CODES.includes(bizCode))) {
      void handleAuthExpired()
      return Promise.reject(createBizError(responseData?.message || '登录已过期', bizCode ?? 40103, true))
    }

    if (bizCode !== undefined) {
      const message = responseData?.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(createBizError(message, bizCode, true))
    }

    let message = '网络错误'
    if (error.response) {
      message = `请求失败: ${error.response.status}`
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.request) {
      message = '网络连接失败'
    }
    ElMessage.error(message)
    return Promise.reject(createBizError(message, undefined, true))
  }
)

export default function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request<any, T>(config) as unknown as Promise<T>
}

export type { ApiResponse }
