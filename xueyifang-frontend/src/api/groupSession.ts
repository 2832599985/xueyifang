import request from './request'
import type { PageResult } from '@/types/http'

export interface GroupSessionParticipantItem {
  id: number
  buyerId: number
  buyerName: string
  buyerAvatar?: string
  orderId: number
  lockedPrice: number
  effectivePrice: number
  rebateAmount: number
  createTime: string
}

export interface GroupSessionItem {
  id: number
  sessionNo: string
  creatorId: number
  creatorName?: string
  creatorAvatar?: string
  creatorProfessional?: string
  creatorEduVerified?: number
  creatorCreditScore?: number
  creatorDeposit?: number
  campusId: number
  campusName?: string
  tagId?: number
  tagName?: string
  title: string
  description: string
  basePrice: number
  tierTargetCount: number
  tierDiscountPrice: number
  currentUnitPrice: number
  minParticipants: number
  maxParticipants: number
  joinedCount: number
  tierRebateApplied: number
  tradeType: number
  sessionStart: string
  sessionEnd: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
  sessionStatus: number // 0=招募中, 1=已成团待开课, 2=已满员锁定, 3=已完结, 4=已取消
  templateServiceId?: number
  closeReason?: string
  createTime: string
  participants?: GroupSessionParticipantItem[]
}

export function listGroupSessions(params?: {
  pageNum?: number
  pageSize?: number
  campusId?: number
  tagId?: number
  tradeType?: number
  sessionStatus?: number
  keyword?: string
}): Promise<PageResult<GroupSessionItem>> {
  return request<PageResult<GroupSessionItem>>({
    url: '/group-session/list',
    method: 'get',
    params
  })
}

export function getGroupSessionDetail(id: number | string): Promise<GroupSessionItem> {
  return request<GroupSessionItem>({
    url: `/group-session/detail/${id}`,
    method: 'get'
  })
}

export function createGroupSession(data: Record<string, any>): Promise<number> {
  return request<number>({
    url: '/group-session/create',
    method: 'post',
    data
  })
}

export function joinGroupSession(id: number): Promise<number> {
  return request<number>({
    url: `/group-session/${id}/join`,
    method: 'post'
  })
}

export function cancelGroupSession(id: number, reason?: string): Promise<boolean> {
  return request<boolean>({
    url: `/group-session/${id}/cancel`,
    method: 'post',
    data: { reason }
  })
}

export function listMyCreatedGroupSessions(): Promise<GroupSessionItem[]> {
  return request<GroupSessionItem[]>({
    url: '/group-session/my-created',
    method: 'get'
  })
}

export function listMyJoinedGroupSessions(): Promise<GroupSessionItem[]> {
  return request<GroupSessionItem[]>({
    url: '/group-session/my-joined',
    method: 'get'
  })
}
