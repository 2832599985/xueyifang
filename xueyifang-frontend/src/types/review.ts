import { formatDateTimeMinute } from '@/utils/format'

export interface ServiceReviewCreateRequest {
  orderId: number
  rating: number
  content: string
  anonymous?: boolean
}

export interface ServiceReviewQuery {
  pageNum?: number
  pageSize?: number
}

export interface ServiceReview {
  id: number
  orderId: number
  rating: number
  content: string
  createTime: string
  reviewerId: number | null
  reviewerName: string
  reviewerAvatar: string | null
  anonymous: boolean
}

export const RATING_LABELS: Record<number, string> = {
  1: '非常差',
  2: '较差',
  3: '一般',
  4: '较好',
  5: '非常好'
}

export const DEFAULT_AVATAR = ''

export function getRatingLabel(rating: number): string {
  return RATING_LABELS[rating] || '未评分'
}

export function formatReviewTime(dateStr: string): string {
  return formatDateTimeMinute(dateStr)
}

export function getReviewerDisplayName(review: ServiceReview): string {
  return review.anonymous ? '匿名用户' : review.reviewerName
}

export function getReviewerAvatar(review: ServiceReview, buildImageUrl: (url?: string) => string): string {
  if (review.anonymous || !review.reviewerAvatar) {
    return ''
  }
  return buildImageUrl(review.reviewerAvatar)
}
