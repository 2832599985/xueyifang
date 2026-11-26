import request from '@/api/request'
import type { PageResult } from '@/types/http'
import type { TradeLocation } from '@/types/dict'

const BASE_URL = '/admin/trade-location'

export interface TradeLocationQueryParams {
  pageNum?: number
  pageSize?: number
  nameLike?: string
  isAvailable?: number | null
}

export interface TradeLocationForm {
  id?: number
  campusId?: number
  locationName: string
  locationDescription?: string
  locationAddress?: string
  latitude?: number
  longitude?: number
  validRadiusMeters?: number
  hasCctv?: number
  isAvailable?: number
}

export function listTradeLocations(params: TradeLocationQueryParams): Promise<PageResult<TradeLocation>> {
  return request<PageResult<TradeLocation>>({
    url: `${BASE_URL}/list`,
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
      nameLike: params.nameLike || undefined,
      isAvailable: params.isAvailable ?? undefined
    }
  })
}

export function addTradeLocation(data: TradeLocationForm): Promise<boolean> {
  return request<boolean>({
    url: `${BASE_URL}/add`,
    method: 'post',
    data
  })
}

export function updateTradeLocation(data: TradeLocationForm): Promise<boolean> {
  return request<boolean>({
    url: `${BASE_URL}/update`,
    method: 'put',
    data
  })
}

export function deleteTradeLocation(id: number): Promise<boolean> {
  return request<boolean>({
    url: `${BASE_URL}/${id}`,
    method: 'delete'
  })
}
