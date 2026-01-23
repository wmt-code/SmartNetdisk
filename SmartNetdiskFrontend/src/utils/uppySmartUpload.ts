/**
 * Uppy 智能上传插件
 * 支持秒传、分片上传、断点续传
 */
import { BasePlugin } from '@uppy/core'
import type { Uppy, UppyFile } from '@uppy/core'
import { checkChunks, uploadFile, uploadChunk, mergeChunks, type UploadResult } from '@/api/file'
import { calculateFileMD5, shouldUseChunkUpload, DEFAULT_CHUNK_SIZE } from './fileUpload'

// 扩展文件 meta 类型
interface SmartUploadMeta {
    fileMd5?: string
    folderId?: number
    fastUploaded?: boolean
    uploadResult?: UploadResult
    uploadedChunks?: number[]
}

export interface SmartUploadPluginOptions {
    folderId?: number
    id?: string
}

/**
 * 智能上传插件
 * 处理 MD5 计算、秒传检测、分片上传逻辑
 */
export class SmartUploadPlugin extends BasePlugin<SmartUploadPluginOptions, any, any> {
    id = 'SmartUpload'
    type = 'modifier' as const

    private folderId: number

    constructor(uppy: Uppy, opts?: SmartUploadPluginOptions) {
        super(uppy, opts)
        this.folderId = opts?.folderId ?? 0
    }

    /**
     * 更新目标文件夹 ID
     */
    setFolderId(folderId: number) {
        this.folderId = folderId
    }

    install() {
        // 1. 预处理：计算 MD5
        this.uppy.addPreProcessor(this.preprocessor)
        // 2. 上传处理器
        this.uppy.addUploader(this.uploader)
    }

    uninstall() {
        this.uppy.removePreProcessor(this.preprocessor)
        this.uppy.removeUploader(this.uploader)
    }

    /**
     * 预处理：计算 MD5 并检测秒传
     */
    private preprocessor = async (fileIDs: string[]) => {
        const promises = fileIDs.map(async (fileID) => {
            const file = this.uppy.getFile(fileID)
            if (!file?.data) return

            const fileData = file.data as File

            // 1. 计算 MD5
            this.uppy.emit('preprocess-progress', file, {
                mode: 'determinate',
                message: '计算文件 MD5...',
                value: 0
            })

            const fileMd5 = await calculateFileMD5(fileData, (percent) => {
                this.uppy.emit('preprocess-progress', file, {
                    mode: 'determinate',
                    message: `计算 MD5... ${percent}%`,
                    value: percent / 100
                })
            })

            // 2. 检测秒传
            this.uppy.emit('preprocess-progress', file, {
                mode: 'indeterminate',
                message: '检测秒传...'
            })

            const checkResult = await checkChunks({
                fileMd5,
                fileName: file.name,
                fileSize: fileData.size,
                folderId: this.folderId
            })

            // 3. 更新文件 meta
            this.uppy.setFileMeta(fileID, {
                fileMd5,
                folderId: this.folderId,
                fastUploaded: checkResult.fastUploaded,
                uploadResult: checkResult.uploadResult,
                uploadedChunks: checkResult.uploadedChunks
            } as SmartUploadMeta)

            this.uppy.emit('preprocess-complete', file)
        })

        await Promise.all(promises)
    }

    /**
     * 上传处理器
     */
    private uploader = async (fileIDs: string[]) => {
        const promises = fileIDs.map(async (fileID) => {
            const file = this.uppy.getFile(fileID)
            if (!file?.data) return

            const fileData = file.data as File
            const meta = file.meta as SmartUploadMeta

            try {
                // 1. 秒传成功，直接返回
                if (meta.fastUploaded && meta.uploadResult) {
                    this.uppy.emit('upload-progress', file, {
                        uploadStarted: Date.now(),
                        bytesUploaded: fileData.size,
                        bytesTotal: fileData.size
                    })
                    this.uppy.emit('upload-success', file, {
                        status: 200,
                        body: meta.uploadResult as any
                    })
                    return
                }

                // 2. 根据文件大小选择上传方式
                if (shouldUseChunkUpload(fileData.size)) {
                    await this.uploadWithChunks(file, fileData)
                } else {
                    await this.uploadDirectly(file, fileData)
                }
            } catch (error) {
                this.uppy.emit('upload-error', file, error as Error)
            }
        })

        await Promise.all(promises)
    }

    /**
     * 普通上传（小文件）
     */
    private async uploadDirectly(file: UppyFile<any, any>, fileData: File) {
        const uploadStarted = Date.now()
        const result = await uploadFile(fileData, this.folderId, (percent) => {
            this.uppy.emit('upload-progress', file, {
                uploadStarted,
                bytesUploaded: Math.floor((fileData.size * percent) / 100),
                bytesTotal: fileData.size
            })
        })

        this.uppy.emit('upload-success', file, {
            status: 200,
            body: result as any
        })
    }

    /**
     * 分片上传（大文件）
     */
    private async uploadWithChunks(file: UppyFile<any, any>, fileData: File) {
        const meta = file.meta as SmartUploadMeta
        const fileMd5 = meta.fileMd5 as string
        const uploadedChunks = new Set(meta.uploadedChunks || [])

        const totalChunks = Math.ceil(fileData.size / DEFAULT_CHUNK_SIZE)
        let uploadedBytes = uploadedChunks.size * DEFAULT_CHUNK_SIZE
        let completedChunks = uploadedChunks.size
        const uploadStarted = Date.now()

        // 并发上传分片
        const CONCURRENT_LIMIT = 8
        const pendingChunks: number[] = []

        for (let i = 0; i < totalChunks; i++) {
            if (!uploadedChunks.has(i)) {
                pendingChunks.push(i)
            }
        }

        // 批量并发上传
        for (let i = 0; i < pendingChunks.length; i += CONCURRENT_LIMIT) {
            const batch = pendingChunks.slice(i, i + CONCURRENT_LIMIT)

            await Promise.all(batch.map(async (chunkIndex) => {
                const start = chunkIndex * DEFAULT_CHUNK_SIZE
                const end = Math.min(start + DEFAULT_CHUNK_SIZE, fileData.size)
                const chunk = fileData.slice(start, end)

                await uploadChunk(chunk, {
                    fileMd5,
                    chunkIndex,
                    totalChunks,
                    chunkSize: chunk.size
                })

                uploadedBytes += chunk.size
                completedChunks++

                this.uppy.emit('upload-progress', file, {
                    uploadStarted,
                    bytesUploaded: uploadedBytes,
                    bytesTotal: fileData.size
                })
            }))
        }

        // 合并分片
        const result = await mergeChunks({
            fileMd5,
            fileName: file.name,
            totalSize: fileData.size,
            totalChunks,
            folderId: this.folderId
        })

        this.uppy.emit('upload-success', file, {
            status: 200,
            body: result as any
        })
    }
}
