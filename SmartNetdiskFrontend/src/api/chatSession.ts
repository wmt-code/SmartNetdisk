import api, { type ApiResponse } from '@/utils/api'

export interface ChatSessionData {
  id: number
  title: string
  mode: string
  messages: string // JSON string
  scopedFileIds: string // JSON string
  createTime: string
  updateTime: string
}

/**
 * 获取会话列表
 */
export async function listSessions(): Promise<ChatSessionData[]> {
  const res = await api.get<unknown, ApiResponse<ChatSessionData[]>>('/chat-session/list')
  return res.data
}

/**
 * 创建会话
 */
export async function createSession(data: {
  title?: string
  mode?: string
  messages?: string
  scopedFileIds?: string
}): Promise<ChatSessionData> {
  const res = await api.post<unknown, ApiResponse<ChatSessionData>>('/chat-session', data)
  return res.data
}

/**
 * 更新会话
 */
export async function updateSession(
  id: number,
  data: { title?: string; messages?: string; scopedFileIds?: string; mode?: string }
): Promise<void> {
  await api.put<unknown, ApiResponse<void>>(`/chat-session/${id}`, data)
}

/**
 * 删除会话
 */
export async function deleteSession(id: number): Promise<void> {
  await api.delete<unknown, ApiResponse<void>>(`/chat-session/${id}`)
}

/**
 * 清空所有会话
 */
export async function deleteAllSessions(): Promise<void> {
  await api.delete<unknown, ApiResponse<void>>('/chat-session/all')
}
