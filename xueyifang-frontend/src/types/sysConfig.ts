export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  isEnabled: number
  createTime: string
  updateTime: string
}

export interface SysConfigQueryRequest {
  pageNum?: number
  pageSize?: number
  keyLike?: string
}

export interface SysConfigUpdateRequest {
  id: number
  configValue?: string
  description?: string
  isEnabled?: number
}

export const IMPORTANT_CONFIG_KEYS = [
  'REVIEW_MODE',
  'ORDER_UNPAID_TIMEOUT_HOURS',
  'SELLER_REFUND_TIMEOUT_DAYS',
  'AUTO_CONFIRM_RECEIPT_DAYS',
  'REGISTER_ENABLED'
] as const

export const CONFIG_KEY_LABELS: Record<string, string> = {
  REVIEW_MODE: '服务审核模式',
  ORDER_UNPAID_TIMEOUT_HOURS: '订单未支付超时 (小时)',
  SELLER_REFUND_TIMEOUT_DAYS: '卖家处理退款超时 (天)',
  AUTO_CONFIRM_RECEIPT_DAYS: '自动确认收货时间 (天)',
  REGISTER_ENABLED: '自助注册开关'
}

export function formatReviewMode(value: string | undefined): string {
  if (!value) return '-'
  switch (value) {
    case '1':
      return '1 · 全部审核'
    case '2':
      return '2 · 免审核上架'
    default:
      return value
  }
}

export function isConfigTruthy(value: string | undefined): boolean {
  return value === '1' || value === 'true'
}

export function getEnabledText(isEnabled: number): string {
  return isEnabled === 1 ? '启用' : '禁用'
}

export function getEnabledTagType(isEnabled: number): 'success' | 'info' {
  return isEnabled === 1 ? 'success' : 'info'
}
