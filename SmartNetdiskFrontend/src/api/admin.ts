import api, { type ApiResponse } from '@/utils/api'

export interface AdminStats {
  totalUsers: number
  activeUsers: number
  totalFiles: number
  totalUsedSpace: number
  totalAllocatedSpace: number
  vectorizedFiles: number
}

export interface AdminUser {
  id: number
  username: string
  email: string
  avatar: string | null
  role: string
  status: number
  usedSpace: number
  totalSpace: number
  usedPercent: number
  createTime: string
}

export interface AdminFile {
  id: number
  fileName: string
  fileSize: number
  fileType: string
  userId: number
  ownerName: string
  createTime: string
  isVectorized: number
}

export async function getAdminStats(): Promise<AdminStats> {
  const res = await api.get<unknown, ApiResponse<AdminStats>>('/admin/stats')
  return res.data
}

export async function getAdminUsers(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
}): Promise<{ records: AdminUser[]; total: number }> {
  const res = await api.get<unknown, ApiResponse<{ records: AdminUser[]; total: number }>>(
    '/admin/users',
    { params },
  )
  return res.data
}

export async function updateUserStatus(userId: number, status: number): Promise<void> {
  await api.put<unknown, ApiResponse<void>>(`/admin/users/${userId}/status`, { status })
}

export async function updateUserSpace(userId: number, totalSpace: number): Promise<void> {
  await api.put<unknown, ApiResponse<void>>(`/admin/users/${userId}/space`, { totalSpace })
}

export async function updateUserRole(userId: number, role: string): Promise<void> {
  await api.put<unknown, ApiResponse<void>>(`/admin/users/${userId}/role`, { role })
}

export async function deleteAdminUser(userId: number): Promise<void> {
  await api.delete<unknown, ApiResponse<void>>(`/admin/users/${userId}`)
}

export async function getAdminFiles(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  ownerName?: string
  fileType?: string
  startDate?: string
  endDate?: string
}): Promise<{ records: AdminFile[]; total: number }> {
  const res = await api.get<unknown, ApiResponse<{ records: AdminFile[]; total: number }>>(
    '/admin/files',
    { params },
  )
  return res.data
}

export async function deleteAdminFile(fileId: number): Promise<void> {
  await api.delete<unknown, ApiResponse<void>>(`/admin/files/${fileId}`)
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export { formatSize }
