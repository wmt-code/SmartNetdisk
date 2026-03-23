import api, { type ApiResponse } from '@/utils/api'

export interface NotificationItem {
  id: number
  type: string  // upload | share | ai | system
  title: string
  content: string | null
  isRead: number  // 0=unread, 1=read
  relatedId: number | null
  createTime: string
}

/**
 * 获取通知列表
 */
export async function getNotifications(): Promise<NotificationItem[]> {
  const res = await api.get<unknown, ApiResponse<NotificationItem[]>>('/notification/list')
  return res.data
}

/**
 * 获取未读数量
 */
export async function getUnreadCount(): Promise<number> {
  const res = await api.get<unknown, ApiResponse<{ count: number }>>('/notification/unread-count')
  return res.data.count
}

/**
 * 标记单条已读
 */
export async function markAsRead(id: number): Promise<void> {
  await api.put<unknown, ApiResponse<void>>(`/notification/${id}/read`)
}

/**
 * 全部标记已读
 */
export async function markAllAsRead(): Promise<void> {
  await api.put<unknown, ApiResponse<void>>('/notification/read-all')
}
