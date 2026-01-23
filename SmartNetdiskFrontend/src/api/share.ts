import api, { type ApiResponse, type PageResult } from '@/utils/api'

// ==================== 类型定义 ====================

/**
 * 分享类型
 */
export const ShareType = {
    SINGLE: 0,    // 单文件
    FOLDER: 1,    // 目录
    BATCH: 2      // 批量
} as const

/**
 * 分享项类型
 */
export const ItemType = {
    FILE: 0,      // 文件
    FOLDER: 1     // 文件夹
} as const

/**
 * 分享项信息
 */
export interface ShareItemInfo {
    itemType: number
    fileId?: number
    folderId?: number
    name: string
    size: number
    sizeStr: string
    fileType?: string
    childCount?: number
}

/**
 * 分享信息
 */
export interface ShareInfo {
    id: number
    shareType: number
    fileId?: number
    folderId?: number
    folderName?: string
    shareTitle?: string
    fileName?: string
    fileSize?: number
    totalSize?: number
    fileSizeStr: string
    fileType?: string
    fileCount?: number
    shareCode: string
    shareUrl?: string
    hasPassword?: boolean
    password: string | null
    expireTime: string | null
    expired?: boolean
    viewCount: number
    downloadCount: number
    maxViewCount: number | null
    status: number
    createTime: string
    items?: ShareItemInfo[]
}

/**
 * 创建分享项参数
 */
export interface CreateShareItemParam {
    itemType: number
    fileId?: number
    folderId?: number
}

/**
 * 创建分享参数
 */
export interface CreateShareParams {
    shareType?: number
    fileId?: number
    folderId?: number
    items?: CreateShareItemParam[]
    shareTitle?: string
    password?: string
    expireDays?: 1 | 7 | 30 | 0  // 0 表示永久
    maxViewCount?: number
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
    shareType: number
    fileName?: string
    folderName?: string
    shareTitle?: string
    fileSize?: number
    totalSize?: number
    fileSizeStr: string
    fileType?: string
    fileCount?: number
    needPassword?: boolean
    hasPassword?: boolean
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
export async function verifySharePassword(code: string, password: string): Promise<ShareInfo> {
    const res = await api.post<unknown, ApiResponse<ShareInfo>>(`/s/${code}/verify`, { password })
    return res.data
}

/**
 * 获取分享项列表
 */
export async function getShareItems(code: string, password?: string): Promise<ShareItemInfo[]> {
    const params = password ? { password } : {}
    const res = await api.get<unknown, ApiResponse<ShareItemInfo[]>>(`/s/${code}/items`, { params })
    return res.data
}

/**
 * 获取分享文件下载链接
 */
export async function getShareDownloadUrl(code: string, password?: string): Promise<string> {
    const params = password ? { password } : {}
    const res = await api.get<unknown, ApiResponse<{ url: string }>>(`/s/${code}/download`, { params })
    return res.data.url
}

/**
 * 浏览分享文件夹内容（进入子文件夹）
 */
export async function browseFolderContents(code: string, password?: string, folderId?: number): Promise<ShareItemInfo[]> {
    const params: Record<string, unknown> = {}
    if (password) params.password = password
    if (folderId) params.folderId = folderId
    const res = await api.get<unknown, ApiResponse<ShareItemInfo[]>>(`/s/${code}/browse`, { params })
    return res.data
}

/**
 * 获取分享中指定文件的下载链接
 */
export async function getFileDownloadUrl(code: string, fileId: number, password?: string): Promise<string> {
    const params = password ? { password } : {}
    const res = await api.get<unknown, ApiResponse<{ url: string }>>(`/s/${code}/download/${fileId}`, { params })
    return res.data.url
}

/**
 * 流式下载分享文件（返回流式下载URL）
 */
export function getFileStreamUrl(code: string, fileId: number, password?: string): string {
    const base = `/api/s/${code}/download/${fileId}/stream`
    return password ? `${base}?password=${encodeURIComponent(password)}` : base
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

/**
 * 格式化分享类型显示
 */
export function formatShareType(shareType: number): string {
    switch (shareType) {
        case ShareType.FOLDER: return '目录'
        case ShareType.BATCH: return '批量'
        default: return '文件'
    }
}

