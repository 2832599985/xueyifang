import request from '@/api/request'
import type { PageResult } from '@/types/http'
import type { SysConfig, SysConfigQueryRequest, SysConfigUpdateRequest } from '@/types/sysConfig'
import type { UserImportResultVO } from '@/types/userImport'

const BASE_URL = '/admin/sys-config'
const IMPORT_BASE_URL = '/admin/user-import'

export function listSysConfigs(params: SysConfigQueryRequest): Promise<PageResult<SysConfig>> {
  return request<PageResult<SysConfig>>({
    url: `${BASE_URL}/list`,
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
      keyLike: params.keyLike || undefined
    }
  })
}

export function getSysConfigDetail(id: number): Promise<SysConfig> {
  return request<SysConfig>({
    url: `${BASE_URL}/${id}`,
    method: 'get'
  })
}

export function updateSysConfig(data: SysConfigUpdateRequest): Promise<boolean> {
  return request<boolean>({
    url: `${BASE_URL}/update`,
    method: 'put',
    data
  })
}

export function getSysConfigKeyValues(keys: string[]): Promise<Record<string, string>> {
  return request<Record<string, string>>({
    url: `${BASE_URL}/key-values`,
    method: 'get',
    params: {
      keys: keys.join(',')
    }
  })
}

export function getRegisterStatus(): Promise<{ registerEnabled: boolean }> {
  return request<{ registerEnabled: boolean }>({
    url: '/sys-config/register-status',
    method: 'get'
  })
}

export function importUsers(file: File): Promise<UserImportResultVO> {
  const formData = new FormData()
  formData.append('file', file)

  return request<UserImportResultVO>({
    url: `${IMPORT_BASE_URL}/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getImportTemplateUrl(format: 'xlsx' | 'csv' = 'xlsx'): string {
  return `/api${IMPORT_BASE_URL}/template?format=${format}`
}
