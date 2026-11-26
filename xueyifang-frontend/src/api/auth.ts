import request from '@/api/request'
import type {
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  ChangePasswordRequest,
  UserInfo,
  LoginResponse,
  CreditScoreLogItem
} from '@/types/auth'

export const authApi = {
  login(data: LoginRequest) {
    return request<LoginResponse>({
      url: '/auth/login',
      method: 'post',
      data
    })
  },

  register(data: RegisterRequest) {
    return request<UserInfo>({
      url: '/auth/register',
      method: 'post',
      data
    })
  },

  getCurrentUser() {
    return request<UserInfo>({
      url: '/auth/currentUser',
      method: 'get'
    })
  },

  updateProfile(data: UpdateProfileRequest) {
    return request<void>({
      url: '/auth/updateProfile',
      method: 'put',
      data
    })
  },

  changePassword(data: ChangePasswordRequest) {
    return request<void>({
      url: '/auth/changePassword',
      method: 'put',
      data
    })
  },

  logout() {
    return request<boolean>({
      url: '/auth/logout',
      method: 'post'
    })
  },

  sendEduVerifyCode(eduEmail: string) {
    return request<{ eduEmail: string; expireSeconds: number; devPreviewCode?: string }>({
      url: '/auth/edu-verify/send',
      method: 'post',
      data: { eduEmail }
    })
  },

  confirmEduVerify(eduEmail: string, verifyCode: string) {
    return request<void>({
      url: '/auth/edu-verify/confirm',
      method: 'post',
      data: { eduEmail, verifyCode }
    })
  },

  getCreditLogs() {
    return request<CreditScoreLogItem[]>({
      url: '/auth/credit-logs',
      method: 'get'
    })
  }
}
