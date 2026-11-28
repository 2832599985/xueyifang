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

export function formatSessionTimeRange(
  start: DateInput,
  end?: DateInput,
  fallback = '时间待定'
): string {
  const s = toDate(start)
  if (!s) return fallback
  const pad = (n: number) => String(n).padStart(2, '0')
  const sDate = `${s.getMonth() + 1}月${s.getDate()}日`
  const sTime = `${pad(s.getHours())}:${pad(s.getMinutes())}`
  const e = toDate(end)
  if (!e) return `${sDate} ${sTime}`
  const eTime = `${pad(e.getHours())}:${pad(e.getMinutes())}`
  const sameDay =
    s.getFullYear() === e.getFullYear() &&
    s.getMonth() === e.getMonth() &&
    s.getDate() === e.getDate()
  if (sameDay) {
    return `${sDate} ${sTime}–${eTime}`
  }
  const eDate = `${e.getMonth() + 1}月${e.getDate()}日`
  return `${sDate} ${sTime} – ${eDate} ${eTime}`
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
