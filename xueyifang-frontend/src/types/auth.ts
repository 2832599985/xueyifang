export interface LoginRequest {
  studentId: string
  password: string
}

export interface RegisterRequest {
  studentId: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
  professionalId: number
  email: string
  dormitory: string
}

export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  dormitory?: string
  grade?: string
  professionalId?: number
  avatar?: string
  bio?: string
  phone?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserInfo {
  id: number
  studentId: string
  realName: string
  nickname: string
  phone: string
  email: string
  dormitory: string
  grade: string
  professionalId: number
  professionalName: string
  campusId?: number
  campusName?: string
  avatar: string
  bio: string
  role: number
  publishPermission: number
  permissionReviewStatus: number
  walletBalance: number
  accountStatus: number
  eduVerified?: number
  verifiedEduEmail?: string
  creditScore?: number
  sellerDeposit?: number
}

export interface CreditScoreLogItem {
  id: number
  userId: number
  delta: number
  scoreBefore: number
  scoreAfter: number
  reasonCode: string
  reasonDesc: string
  relatedOrderId?: number
  createTime: string
}

export interface LoginResponse {
  userInfo: UserInfo
  token: string
}
