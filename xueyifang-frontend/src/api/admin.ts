/**
 * 管理员 API 模块
 */

import request from '@/api/request'
import type { PageResult } from '@/types/http'
import type {
  AdminStatistics,
  AdminTrend,
  AdminUser,
  AdminUserStatusRequest,
  AdminServiceTag,
  PendingUser,
  PermissionReviewRequest
} from '@/types/admin'

/**
 * 获取系统统计数据
 */
export function getAdminStatistics(): Promise<AdminStatistics> {
  return request<AdminStatistics>({
    url: '/admin/statistics',
    method: 'get'
  })
}

/**
 * 获取待审核用户列表
 */
export function getPendingUsers(pageNum = 1, pageSize = 10): Promise<PageResult<PendingUser>> {
  return request<PageResult<PendingUser>>({
    url: '/admin/users/pending',
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 审核用户权限申请
 */
export function reviewPermission(data: PermissionReviewRequest): Promise<void> {
  return request<void>({
    url: '/admin/permission/review',
    method: 'put',
    data
  })
}

/**
 * 获取近7天趋势统计数据
 */
export function getAdminTrend(): Promise<AdminTrend> {
  return request<AdminTrend>({
    url: '/admin/statistics/trend',
    method: 'get'
  })
}

export function listAdminUsers(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  accountStatus?: number
  publishPermission?: number
}): Promise<PageResult<AdminUser>> {
  return request<PageResult<AdminUser>>({
    url: '/admin/users/list',
    method: 'get',
    params
  })
}

export function updateAdminUserStatus(data: AdminUserStatusRequest): Promise<void> {
  return request<void>({
    url: '/admin/users/status',
    method: 'put',
    data
  })
}

export function resetAdminUserPassword(userId: number): Promise<void> {
  return request<void>({
    url: `/admin/users/${userId}/reset-password`,
    method: 'put'
  })
}

export function listAdminServiceTags(params: {
  pageNum?: number
  pageSize?: number
  nameLike?: string
}): Promise<PageResult<AdminServiceTag>> {
  return request<PageResult<AdminServiceTag>>({
    url: '/admin/service-tag/list',
    method: 'get',
    params
  })
}

export function addAdminServiceTag(data: {
  tagName: string
  description?: string
  iconUrl?: string
}): Promise<boolean> {
  return request<boolean>({
    url: '/admin/service-tag/add',
    method: 'post',
    data
  })
}

export function updateAdminServiceTag(data: {
  id: number
  tagName: string
  description?: string
  iconUrl?: string
}): Promise<boolean> {
  return request<boolean>({
    url: '/admin/service-tag/update',
    method: 'put',
    data
  })
}

export function deleteAdminServiceTag(id: number): Promise<boolean> {
  return request<boolean>({
    url: `/admin/service-tag/${id}`,
    method: 'delete'
  })
}
