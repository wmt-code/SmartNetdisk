import api, { type ApiResponse, type PageResult } from '@/utils/api'

// ==================== 类型定义 ====================

/**
 * 分享信息
 */
export interface ShareInfo {
    id: number
    fileId: number
    fileName: string
    shareCode: string
    password: string | null
    expireTime: string | null
    viewCount: number
    downloadCount: number
    maxViewCount: number | null
    status: number
    createTime: string
}

/**
 * 创建分享参数
 */
export interface CreateShareParams {
    fileId: number
    password?: string
    expireDays?: 1 | 7 | 30 | 0  // 0 表示永久
}

/**
 * 分享查询参数
 */
export interface ShareListParams {
    pageNum?: number
    pageSize?: number
    status?: number
}

/**
 * 公开分享文件信息
 */
export interface ShareFileInfo {
    fileName: string
    fileSize: number
    fileSizeStr: string
    fileType: string
    needPassword?: boolean
}

// ==================== 分享管理 API (需要登录) ====================

/**
 * 创建分享
 */
export async function createShare(params: CreateShareParams): Promise<ShareInfo> {
    const res = await api.post<unknown, ApiResponse<ShareInfo>>('/share', params)
    return res.data
}

/**
 * 获取我的分享列表
 */
export async function getMyShares(params: ShareListParams = {}): Promise<PageResult<ShareInfo>> {
    const res = await api.get<unknown, ApiResponse<PageResult<ShareInfo>>>('/share/list', { params })
    return res.data
}

/**
 * 取消分享
 */
export async function cancelShare(shareId: number): Promise<void> {
    await api.delete<unknown, ApiResponse<void>>(`/share/${shareId}`)
}

// ==================== 公开访问 API (无需登录) ====================

/**
 * 获取分享信息
 */
export async function getShareInfo(code: string): Promise<ShareFileInfo> {
    const res = await api.get<unknown, ApiResponse<ShareFileInfo>>(`/s/${code}`)
    return res.data
}

/**
 * 验证分享提取码
 */
export async function verifySharePassword(code: string, password: string): Promise<ShareFileInfo> {
    const res = await api.post<unknown, ApiResponse<ShareFileInfo>>(`/s/${code}/verify`, { password })
    return res.data
}

/**
 * 获取分享文件下载链接
 */
export async function getShareDownloadUrl(code: string): Promise<string> {
    const res = await api.get<unknown, ApiResponse<{ url: string }>>(`/s/${code}/download`)
    return res.data.url
}

/**
 * 生成分享链接
 */
export function generateShareLink(code: string): string {
    return `${window.location.origin}/s/${code}`
}

/**
 * 生成随机提取码 (4位字母数字)
 */
export function generatePassword(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
    let result = ''
    for (let i = 0; i < 4; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
}

/**
 * 格式化过期时间
 */
export function formatExpireText(expireDays: 1 | 7 | 30 | 0): string {
    switch (expireDays) {
        case 1: return '1天后过期'
        case 7: return '7天后过期'
        case 30: return '30天后过期'
        default: return '永久有效'
    }
}
