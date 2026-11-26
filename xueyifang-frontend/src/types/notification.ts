/**
 * 通知类型与路由解析
 * 1=权限通知, 2=服务下架, 3=订单通知, 4=纠纷通知, 5=服务审核通知
 */
export type NotificationType = 1 | 2 | 3 | 4 | 5

export const notificationTypeMap: Record<NotificationType, string> = {
  1: '权限通知',
  2: '服务下架',
  3: '订单通知',
  4: '纠纷通知',
  5: '服务审核'
}

export const notificationTypeOptions: { value: NotificationType; label: string }[] = [
  { value: 1, label: '权限通知' },
  { value: 2, label: '服务下架' },
  { value: 3, label: '订单通知' },
  { value: 4, label: '纠纷通知' },
  { value: 5, label: '服务审核' }
]

export interface NotificationItem {
  id: number
  title: string
  content: string
  createTime: string
  isRead: 0 | 1
  notificationType: NotificationType
  relatedId?: number
}

export interface NotificationQuery {
  pageNum?: number
  pageSize?: number
  notificationType?: NotificationType
}

export function getNotificationRoute(item: NotificationItem): { path: string } | null {
  switch (item.notificationType) {
    case 1:
      return { path: '/user/center' }
    case 2:
    case 5:
      return { path: '/my-services' }
    case 3:
      return item.relatedId ? { path: `/orders/${item.relatedId}` } : { path: '/orders' }
    case 4:
      return item.relatedId ? { path: `/disputes/${item.relatedId}` } : { path: '/disputes' }
    default:
      return null
  }
}

export function formatNotificationTime(timeStr: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  if (year === now.getFullYear()) {
    return `${month}-${day} ${hours}:${minutes}`
  }
  return `${year}-${month}-${day} ${hours}:${minutes}`
}
