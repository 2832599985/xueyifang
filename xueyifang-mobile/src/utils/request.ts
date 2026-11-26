const DEFAULT_API_BASE = 'http://localhost:8080/api'

export function getApiBaseUrl(): string {
  const custom = uni.getStorageSync('XYF_API_BASE_URL')
  return custom || DEFAULT_API_BASE
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export function httpRequest<T>(options: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, any>
}): Promise<T> {
  const token = uni.getStorageSync('XYF_TOKEN')
  const header: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${getApiBaseUrl()}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        const body = res.data as ApiEnvelope<T>
        if (res.statusCode === 200 && body && body.code === 0) {
          resolve(body.data)
          return
        }
        if (body?.code === 40103 || res.statusCode === 401) {
          uni.removeStorageSync('XYF_TOKEN')
          uni.removeStorageSync('XYF_USER')
          uni.navigateTo({ url: '/pages/auth/login' })
          reject(new Error('请先登录校园账号'))
          return
        }
        const msg = body?.message || '网络请求异常'
        uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      },
      fail: (err) => {
        uni.showToast({ title: '连接服务端失败，请检查网络', icon: 'none' })
        reject(err)
      }
    })
  })
}
