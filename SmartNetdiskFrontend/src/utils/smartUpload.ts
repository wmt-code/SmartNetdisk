import { uploadFile, checkChunks, uploadChunk, mergeChunks, type UploadResult } from '@/api/file'
import {
    calculateFileMD5,
    splitFileIntoChunks,
    shouldUseChunkUpload,
    DEFAULT_CHUNK_SIZE
} from './fileUpload'

/**
 * 上传进度回调
 */
export interface UploadProgress {
    /** 整体进度百分比 (0-100) */
    percent: number
    /** 当前阶段 */
    stage: 'calculating' | 'checking' | 'uploading' | 'merging' | 'completed'
    /** 阶段描述 */
    stageText: string
    /** 已上传字节数 */
    uploadedBytes?: number
    /** 总字节数 */
    totalBytes?: number
    /** 上传速度 (字节/秒) */
    speed?: number
}

/**
 * 智能上传文件（自动选择普通上传或分片上传）
 * @param file 文件对象
 * @param folderId 目标文件夹 ID
 * @param onProgress 进度回调
 * @returns 上传结果
 */
export async function smartUploadFile(
    file: File,
    folderId: number = 0,
    onProgress?: (progress: UploadProgress) => void
): Promise<UploadResult> {
    const fileSize = file.size
    const fileName = file.name

    // 1. 计算文件 MD5（所有文件都需要）
    if (onProgress) {
        onProgress({
            percent: 0,
            stage: 'calculating',
            stageText: '计算文件 MD5...',
            totalBytes: fileSize
        })
    }

    const fileMd5 = await calculateFileMD5(file, (percent) => {
        if (onProgress) {
            onProgress({
                percent: Math.floor(percent * 0.15), // MD5 计算占总进度的 15%
                stage: 'calculating',
                stageText: `计算文件 MD5... ${percent}%`,
                totalBytes: fileSize
            })
        }
    })

    // 2. 检测秒传（所有文件都检测）
    if (onProgress) {
        onProgress({
            percent: 15,
            stage: 'checking',
            stageText: '检测秒传...',
            totalBytes: fileSize
        })
    }

    const checkResult = await checkChunks({
        fileMd5,
        fileName,
        fileSize,
        folderId
    })

    // 2.1 秒传成功
    if (checkResult.fastUploaded && checkResult.uploadResult) {
        if (onProgress) {
            onProgress({
                percent: 100,
                stage: 'completed',
                stageText: '秒传成功！',
                uploadedBytes: fileSize,
                totalBytes: fileSize
            })
        }
        return checkResult.uploadResult
    }

    // 3. 判断是否需要分片上传
    if (shouldUseChunkUpload(fileSize)) {
        return await uploadFileWithChunks(
            file,
            fileMd5,
            checkResult.uploadedChunks || [],
            folderId,
            onProgress
        )
    } else {
        return await uploadFileDirectly(file, folderId, onProgress)
    }
}

/**
 * 普通上传（小文件）
 */
async function uploadFileDirectly(
    file: File,
    folderId: number,
    onProgress?: (progress: UploadProgress) => void
): Promise<UploadResult> {
    if (onProgress) {
        onProgress({
            percent: 20,
            stage: 'uploading',
            stageText: '上传中...',
            uploadedBytes: 0,
            totalBytes: file.size
        })
    }

    const result = await uploadFile(file, folderId, (percent) => {
        if (onProgress) {
            // 20% ~ 100%
            const actualPercent = 20 + Math.floor(percent * 0.8)
            onProgress({
                percent: actualPercent,
                stage: 'uploading',
                stageText: '上传中...',
                uploadedBytes: Math.floor((file.size * percent) / 100),
                totalBytes: file.size
            })
        }
    })

    if (onProgress) {
        onProgress({
            percent: 100,
            stage: 'completed',
            stageText: '上传完成',
            uploadedBytes: file.size,
            totalBytes: file.size
        })
    }

    return result
}

/**
 * 分片上传（大文件，支持断点续传）
 */
async function uploadFileWithChunks(
    file: File,
    fileMd5: string,
    uploadedChunkIndexes: number[],
    folderId: number,
    onProgress?: (progress: UploadProgress) => void
): Promise<UploadResult> {
    const fileName = file.name
    const fileSize = file.size

    // 分片上传
    const chunks = splitFileIntoChunks(file, DEFAULT_CHUNK_SIZE)
    const totalChunks = chunks.length
    const uploadedChunkSet = new Set(uploadedChunkIndexes)

    if (onProgress) {
        onProgress({
            percent: 20,
            stage: 'uploading',
            stageText: `上传分片 (0/${totalChunks})`,
            uploadedBytes: uploadedChunkSet.size * DEFAULT_CHUNK_SIZE,
            totalBytes: fileSize
        })
    }

    // 记录开始时间和已上传字节数，用于计算速度
    const startTime = Date.now()
    let uploadedBytes = uploadedChunkSet.size * DEFAULT_CHUNK_SIZE
    let completedChunks = uploadedChunkSet.size

    // 并发上传分片（8个并发，平衡速度和服务器压力）
    const CONCURRENT_LIMIT = 8
    const pendingChunks: number[] = []

    for (let i = 0; i < totalChunks; i++) {
        if (!uploadedChunkSet.has(i)) {
            pendingChunks.push(i)
        }
    }

    // 使用并发控制上传
    const uploadChunkWithIndex = async (chunkIndex: number) => {
        const chunk = chunks[chunkIndex]
        if (!chunk) return

        await uploadChunk(chunk, {
            fileMd5,
            chunkIndex,
            totalChunks,
            chunkSize: chunk.size
        })

        uploadedBytes += chunk.size
        completedChunks++

        // 计算速度
        const elapsedTime = (Date.now() - startTime) / 1000 // 秒
        const speed = elapsedTime > 0 ? uploadedBytes / elapsedTime : 0

        // 更新进度：20% ~ 90%
        const uploadProgress = (completedChunks / totalChunks) * 70 + 20

        if (onProgress) {
            onProgress({
                percent: Math.floor(uploadProgress),
                stage: 'uploading',
                stageText: `上传分片 (${completedChunks}/${totalChunks})`,
                uploadedBytes,
                totalBytes: fileSize,
                speed
            })
        }
    }

    // 并发上传
    for (let i = 0; i < pendingChunks.length; i += CONCURRENT_LIMIT) {
        const batch = pendingChunks.slice(i, i + CONCURRENT_LIMIT)
        await Promise.all(batch.map(index => uploadChunkWithIndex(index)))
    }

    // 4. 合并分片
    if (onProgress) {
        onProgress({
            percent: 90,
            stage: 'merging',
            stageText: '合并分片中...',
            uploadedBytes: fileSize,
            totalBytes: fileSize
        })
    }

    const result = await mergeChunks({
        fileMd5,
        fileName,
        totalSize: fileSize,
        totalChunks,
        folderId
    })

    // 5. 完成
    if (onProgress) {
        onProgress({
            percent: 100,
            stage: 'completed',
            stageText: '上传完成',
            uploadedBytes: fileSize,
            totalBytes: fileSize
        })
    }

    return result
}
