import request from './request'

export interface CreatorBadgeItem {
  badgeCode: string
  badgeName: string
  badgeTone: 'gold' | 'emerald' | 'blue' | 'purple' | string
  badgeDesc: string
}

export interface CreatorLeaderboardItem {
  rank: number
  userId: number
  displayName: string
  avatar?: string
  grade?: string
  professionalName: string
  campusId?: number
  campusName: string
  bio: string
  eduVerified: number
  creditScore: number
  creatorLevelTitle: string
  sellerDeposit: number
  activeServiceCount: number
  completedOrderCount: number
  totalPracticeHours: number
  averageRating: number
  reviewCount: number
  compositeInfluenceScore: number
  topSkillTags: string[]
  badges: CreatorBadgeItem[]
}

export function getCreatorLeaderboard(params?: {
  campusId?: number
  tagId?: number
  rankBy?: 'COMPREHENSIVE' | 'COMPLETED_ORDERS' | 'CREDIT_SCORE' | 'RATING' | string
  limit?: number
}): Promise<CreatorLeaderboardItem[]> {
  return request<CreatorLeaderboardItem[]>({
    url: '/creator/leaderboard',
    method: 'get',
    params
  })
}
