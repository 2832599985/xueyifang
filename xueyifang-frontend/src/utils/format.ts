/**
 * 通用格式化工具
 */

export type DateInput = string | number | Date | null | undefined

function toDate(value: DateInput): Date | null {
  if (value === null || value === undefined || value === '') return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value: DateInput, fallback = '-'): string {
  const date = toDate(value)
  return date ? date.toLocaleString('zh-CN') : fallback
}

export function formatDateTimeMinute(value: DateInput, fallback = '-'): string {
  const date = toDate(value)
  if (!date) return fallback
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatShortDateTime(value: DateInput, fallback = '-'): string {
  const date = toDate(value)
  if (!date) return fallback
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatDate(value: DateInput, fallback = '-'): string {
  const date = toDate(value)
  if (!date) return fallback
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

export function formatPrice(value: number | string | null | undefined, fallback = '¥0.00'): string {
  if (value === null || value === undefined || value === '') return fallback
  const num = typeof value === 'string' ? Number(value) : value
  return Number.isFinite(num) ? `¥${num.toFixed(2)}` : fallback
}

export function formatMoneyNumber(value: number | string | null | undefined, fallback = '0.00'): string {
  if (value === null || value === undefined || value === '') return fallback
  const num = typeof value === 'string' ? Number(value) : value
  return Number.isFinite(num)
    ? num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : fallback
}

export function formatCompactNumber(value: number | null | undefined): string {
  const num = Number(value || 0)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

export function getInitials(name?: string | null, fallback = '学'): string {
  const cleaned = (name || '').trim()
  if (!cleaned) return fallback
  return cleaned.charAt(0).toUpperCase()
}
