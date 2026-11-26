import request from './request'
import type { PageResult } from '@/types/http'
import type {
  ServiceListItem,
  ServiceDetail,
  ServiceQueryParams,
  ServicePublishRequest,
  ServiceUpdateRequest,
  MyServiceItem
} from '@/types/service'

export function getServiceList(params?: ServiceQueryParams): Promise<PageResult<ServiceListItem>> {
  return request<PageResult<ServiceListItem>>({
    url: '/service/list',
    method: 'get',
    params: {
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 12,
      ...params
    }
  })
}

export function getServiceDetail(id: number | string): Promise<ServiceDetail> {
  return request<ServiceDetail>({
    url: `/service/${id}`,
    method: 'get'
  })
}

export function searchServices(
  keyword: string,
  pageNum = 1,
  pageSize = 12
): Promise<PageResult<ServiceListItem>> {
  return getServiceList({ keyword, pageNum, pageSize })
}

export function getSellerServices(
  sellerId: number,
  params?: Omit<ServiceQueryParams, 'sellerId'>
): Promise<PageResult<ServiceListItem>> {
  return getServiceList({ sellerId, ...params })
}

export function getMyServices(
  status?: number,
  params?: ServiceQueryParams
): Promise<PageResult<ServiceListItem>> {
  return request<PageResult<ServiceListItem>>({
    url: '/service/myServices',
    method: 'get',
    params: {
      status,
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 10,
      ...params
    }
  })
}

export { listServiceTags as getServiceTags } from '@/api/dict'
export { listTradeLocations as getTradeLocations } from '@/api/dict'
export { listProfessionals as getProfessionals } from '@/api/dict'

export function getServicesByTag(
  tagId: number,
  params?: Omit<ServiceQueryParams, 'tagId'>
): Promise<PageResult<ServiceListItem>> {
  return getServiceList({ tagId, ...params })
}

export function getRecommendedServices(limit = 8): Promise<ServiceListItem[]> {
  return getServiceList({
    sortBy: 'view_desc',
    pageSize: limit,
    pageNum: 1
  }).then(result => result.records)
}

export function getLatestServices(limit = 8): Promise<ServiceListItem[]> {
  return getServiceList({
    sortBy: 'latest',
    pageSize: limit,
    pageNum: 1
  }).then(result => result.records)
}

export function publishService(data: ServicePublishRequest): Promise<number> {
  return request<number>({
    url: '/service/publish',
    method: 'post',
    data
  })
}

export function updateService(id: number | string, data: ServiceUpdateRequest): Promise<void> {
  return request<void>({
    url: `/service/${id}`,
    method: 'put',
    data
  })
}

export function onlineService(id: number | string): Promise<void> {
  return request<void>({
    url: `/service/${id}/online`,
    method: 'put'
  })
}

export function offlineService(id: number | string): Promise<void> {
  return request<void>({
    url: `/service/${id}/offline`,
    method: 'put'
  })
}

export function deleteService(id: number | string): Promise<void> {
  return request<void>({
    url: `/service/${id}`,
    method: 'delete'
  })
}

export function getMyServicesEnhanced(
  status?: number,
  params?: ServiceQueryParams
): Promise<PageResult<MyServiceItem>> {
  return getMyServices(status, params).then(result => {
    const enhancedRecords = result.records.map(item => {
      const myItem = item as MyServiceItem
      myItem.canEdit = myItem.status === 2 || myItem.status === 4
      myItem.canOnline = myItem.status === 2 || myItem.status === 4
      myItem.canOffline = myItem.status === 1
      return myItem
    })
    return {
      ...result,
      records: enhancedRecords
    }
  })
}

/**
 * 构建完整图片访问 URL；无图片时返回空字符串由 XyfImage/XyfAvatar 展示优雅占位
 */
export function buildImageUrl(imageUrl?: string | null): string {
  if (!imageUrl) return ''

  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://') || imageUrl.startsWith('data:')) {
    return imageUrl
  }

  if (imageUrl.startsWith('/api/file/view/')) {
    return imageUrl
  }

  const normalized = imageUrl.startsWith('/') ? imageUrl.slice(1) : imageUrl
  return `/api/file/view/${normalized}`
}
