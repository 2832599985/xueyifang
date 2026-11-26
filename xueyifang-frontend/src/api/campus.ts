import request from './request'
import type { CampusInfo } from '@/types/dict'

export function listCampuses(): Promise<CampusInfo[]> {
  return request<CampusInfo[]>({
    url: '/campus/list',
    method: 'get'
  })
}

export function checkCampusFence(
  campusId: number,
  lat: number,
  lng: number
): Promise<{ inside: boolean; message: string }> {
  return request<{ inside: boolean; message: string }>({
    url: '/campus/check-fence',
    method: 'get',
    params: { campusId, lat, lng }
  })
}

export function adminListCampuses(): Promise<CampusInfo[]> {
  return request<CampusInfo[]>({
    url: '/admin/campus/list',
    method: 'get'
  })
}

export function adminSaveCampus(data: Partial<CampusInfo>): Promise<number> {
  return request<number>({
    url: '/admin/campus/save',
    method: 'post',
    data
  })
}

export function adminGetRevenueSummary(): Promise<{
  totalCommissionRevenue: number
  settledOrderCount: number
}> {
  return request<{ totalCommissionRevenue: number; settledOrderCount: number }>({
    url: '/admin/campus/revenue-summary',
    method: 'get'
  })
}
