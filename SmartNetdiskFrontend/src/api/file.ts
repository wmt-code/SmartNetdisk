import api, { type ApiResponse, type PageResult } from '@/utils/api'

// ==================== 类型定义 ====================

/**
 * 文件信息
 */
export interface FileInfo {
    id: number
    fileName: string
    fileSize: number
    fileSizeStr: string
    fileType: string
    fileExt: string
    thumbnailPath: string | null
    isVectorized: boolean
    folderId: number
    createTime: string
    updateTime: string
}

/**
 * 文件夹信息
 */
export interface FolderInfo {
    id: number
    folderName: string
    parentId: number
    createTime: string
    updateTime: string
}

/**
 * 文件列表查询参数
 */
export interface FileListParams {
    folderId?: number
    fileType?: string
    keyword?: string
    pageNum?: number
    pageSize?: number
    orderBy?: string
    isAsc?: boolean
}

/**
 * 上传结果
 */
export interface UploadResult {
    fileId: number
    fileName: string
    fileSize?: number
    fileMd5?: string
    fastUpload?: boolean
}

/**
 * 分片检测结果
 */
export interface ChunkCheckResult {
    fastUploaded: boolean
    uploadResult?: UploadResult
    uploadedChunks: number[]
}

// ==================== 文件 API ====================

/**
 * 获取文件列表
 */
export async function getFileList(params: FileListParams): Promise<PageResult<FileInfo>> {
    const res = await api.get<unknown, ApiResponse<PageResult<FileInfo>>>('/file/list', { params })
    return res.data
}

/**
 * 获取文件详情
 */
export async function getFileDetail(fileId: number): Promise<FileInfo> {
    const res = await api.get<unknown, ApiResponse<FileInfo>>(`/file/${fileId}`)
    return res.data
}

/**
 * 普通文件上传
 */
export async function uploadFile(
    file: File,
    folderId: number = 0,
    onProgress?: (percent: number) => void
): Promise<UploadResult> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderId', String(folderId))

    const res = await api.post<unknown, ApiResponse<UploadResult>>('/file/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        timeout: 0, // 禁用超时，大文件上传需要较长时间
        onUploadProgress: (progressEvent) => {
            if (onProgress && progressEvent.total) {
                const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                onProgress(percent)
            }
        }
    })
    return res.data
}

/**
 * 秒传检测
 */
export async function checkFastUpload(params: {
    fileMd5: string
    fileName: string
    fileSize: number
    folderId?: number
}): Promise<UploadResult | null> {
    const res = await api.post<unknown, ApiResponse<UploadResult | null>>('/file/check', params)
    return res.data
}

/**
 * 分片上传检测（秒传 + 断点续传）
 */
export async function checkChunks(params: {
    fileMd5: string
    fileName: string
    fileSize: number
    folderId?: number
}): Promise<ChunkCheckResult> {
    const res = await api.post<unknown, ApiResponse<ChunkCheckResult>>('/file/chunk/check', params)
    return res.data
}

/**
 * 上传单个分片
 */
export async function uploadChunk(
    chunk: Blob,
    params: {
        fileMd5: string
        chunkIndex: number
        totalChunks: number
        chunkSize: number
    }
): Promise<void> {
    const formData = new FormData()
    formData.append('file', chunk)
    formData.append('fileMd5', params.fileMd5)
    formData.append('chunkIndex', String(params.chunkIndex))
    formData.append('totalChunks', String(params.totalChunks))
    formData.append('chunkSize', String(params.chunkSize))

    await api.post<unknown, ApiResponse<void>>('/file/chunk', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

/**
 * 合并分片
 */
export async function mergeChunks(params: {
    fileMd5: string
    fileName: string
    totalSize: number
    totalChunks: number
    folderId?: number
}): Promise<UploadResult> {
    const res = await api.post<unknown, ApiResponse<UploadResult>>('/file/merge', params)
    return res.data
}

/**
 * 获取文件下载链接
 */
export async function getDownloadUrl(fileId: number): Promise<string> {
    const res = await api.get<unknown, ApiResponse<{ url: string }>>(`/file/${fileId}/download`)
    return res.data.url
}

/**
 * 流式下载文件（通过后端代理，确保文件名正确）
 * 直接打开带 token 的下载链接，让浏览器原生处理
 */
export function downloadFileStream(fileId: number): void {
    const token = localStorage.getItem('satoken') || ''
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
    // 直接打开下载链接，浏览器会根据 Content-Disposition 头处理文件名
    window.location.href = `${baseUrl}/file/${fileId}/download/stream?satoken=${token}`
}

/**
 * 获取文件预览链接
 */
export async function getPreviewUrl(fileId: number): Promise<string> {
    const res = await api.get<unknown, ApiResponse<{ url: string }>>(`/file/${fileId}/preview`)
    return res.data.url
}

/**
 * 重命名文件
 */
export async function renameFile(fileId: number, newName: string): Promise<void> {
    await api.put<unknown, ApiResponse<void>>(`/file/${fileId}`, { name: newName })
}

/**
 * 移动文件
 */
export async function moveFile(fileId: number, targetFolderId: number): Promise<void> {
    await api.put<unknown, ApiResponse<void>>(`/file/${fileId}/move`, { targetFolderId })
}

/**
 * 复制文件
 */
export async function copyFile(fileId: number, targetFolderId: number): Promise<FileInfo> {
    const res = await api.post<unknown, ApiResponse<FileInfo>>(`/file/${fileId}/copy`, { targetFolderId })
    return res.data
}

/**
 * 删除文件（移入回收站）
 */
export async function deleteFile(fileId: number): Promise<void> {
    await api.delete<unknown, ApiResponse<void>>(`/file/${fileId}`)
}

/**
 * 彻底删除文件
 */
export async function permanentDeleteFile(fileId: number): Promise<void> {
    await api.delete<unknown, ApiResponse<void>>(`/file/${fileId}/permanent`)
}

/**
 * 恢复文件
 */
export async function restoreFile(fileId: number): Promise<void> {
    await api.post<unknown, ApiResponse<void>>(`/file/${fileId}/restore`)
}

/**
 * 批量删除文件
 */
export async function batchDeleteFiles(fileIds: number[]): Promise<void> {
    await api.post<unknown, ApiResponse<void>>('/file/batch/delete', fileIds)
}

/**
 * 批量移动文件
 */
export async function batchMoveFiles(fileIds: number[], targetFolderId: number): Promise<void> {
    await api.post<unknown, ApiResponse<void>>('/file/batch/move', { fileIds, targetFolderId })
}

/**
 * 批量复制文件
 */
export async function batchCopyFiles(fileIds: number[], targetFolderId: number): Promise<FileInfo[]> {
    const res = await api.post<unknown, ApiResponse<FileInfo[]>>('/file/batch/copy', { fileIds, targetFolderId })
    return res.data
}

/**
 * 获取回收站文件列表
 */
export async function getRecycleList(params: FileListParams): Promise<PageResult<FileInfo>> {
    const res = await api.get<unknown, ApiResponse<PageResult<FileInfo>>>('/file/recycle', { params })
    return res.data
}

// ==================== 文件夹 API ====================

/**
 * 创建文件夹
 */
export async function createFolder(folderName: string, parentId: number = 0): Promise<FolderInfo> {
    const res = await api.post<unknown, ApiResponse<FolderInfo>>('/folder', { folderName, parentId })
    return res.data
}

/**
 * 获取文件夹详情
 */
export async function getFolderDetail(folderId: number): Promise<FolderInfo> {
    const res = await api.get<unknown, ApiResponse<FolderInfo>>(`/folder/${folderId}`)
    return res.data
}

/**
 * 重命名文件夹
 */
export async function renameFolder(folderId: number, newName: string): Promise<void> {
    await api.put<unknown, ApiResponse<void>>(`/folder/${folderId}`, { folderName: newName })
}

/**
 * 删除文件夹
 */
export async function deleteFolder(folderId: number): Promise<void> {
    await api.delete<unknown, ApiResponse<void>>(`/folder/${folderId}`)
}

/**
 * 恢复文件夹
 */
export async function restoreFolder(folderId: number): Promise<void> {
    await api.post<unknown, ApiResponse<void>>(`/folder/${folderId}/restore`)
}

/**
 * 彻底删除文件夹
 */
export async function permanentDeleteFolder(folderId: number): Promise<void> {
    await api.delete<unknown, ApiResponse<void>>(`/folder/${folderId}/permanent`)
}

/**
 * 获取文件夹树
 */
export async function getFolderTree(): Promise<FolderInfo[]> {
    const res = await api.get<unknown, ApiResponse<FolderInfo[]>>('/folder/tree')
    return res.data
}
