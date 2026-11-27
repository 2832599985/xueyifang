import request from './request'
import type { PageResult } from '@/types/http'
import type { DemandItem, DemandBidItem, DemandCreatePayload } from '@/types/demand'

export function listDemands(params?: {
  pageNum?: number
  pageSize?: number
  campusId?: number
  tagId?: number
  tradeType?: number
  urgencyLevel?: number
  demandStatus?: number
  keyword?: string
}): Promise<PageResult<DemandItem>> {
  return request<PageResult<DemandItem>>({
    url: '/demand/list',
    method: 'get',
    params
  })
}

export function getDemandDetail(id: number | string): Promise<DemandRequestDetail> {
  return request<DemandItem>({
    url: `/demand/detail/${id}`,
    method: 'get'
  })
}

type DemandRequestDetail = DemandItem

export function publishDemand(data: DemandCreatePayload): Promise<number> {
  return request<number>({
    url: '/demand/publish',
    method: 'post',
    data
  })
}

export function submitDemandBid(
  demandId: number,
  data: { bidPrice: number; estimatedHours: number; bidPlan: string }
): Promise<number> {
  return request<number>({
    url: `/demand/${demandId}/bid`,
    method: 'post',
    data
  })
}

export function acceptDemandBid(demandId: number, bidId: number): Promise<number> {
  return request<number>({
    url: `/demand/${demandId}/accept/${bidId}`,
    method: 'post'
  })
}

export function cancelDemand(demandId: number, reason?: string): Promise<boolean> {
  return request<boolean>({
    url: `/demand/${demandId}/cancel`,
    method: 'post',
    data: { reason }
  })
}

export function listMyPublishedDemands(): Promise<DemandItem[]> {
  return request<DemandItem[]>({
    url: '/demand/my-published',
    method: 'get'
  })
}

export function listMyDemandBids(): Promise<DemandBidItem[]> {
  return request<DemandBidItem[]>({
    url: '/demand/my-bids',
    method: 'get'
  })
}

export function adminListDemands(params?: {
  pageNum?: number
  pageSize?: number
  status?: number
  keyword?: string
}): Promise<PageResult<DemandItem>> {
  return request<PageResult<DemandItem>>({
    url: '/admin/demand/list',
    method: 'get',
    params
  })
}

export function adminCloseDemand(id: number, reason: string): Promise<boolean> {
  return request<boolean>({
    url: `/admin/demand/${id}/close`,
    method: 'post',
    data: { reason }
  })
}
