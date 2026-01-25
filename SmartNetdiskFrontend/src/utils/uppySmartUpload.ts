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
        console.log('[SmartUpload] 预处理开始，文件数量:', fileIDs.length)

        // 缓存已创建的文件夹路径 -> ID Promise，避免重复请求
        const folderCache = new Map<string, Promise<number>>()

        // 并发控制函数
        const processFile = async (fileID: string) => {
            const file = this.uppy.getFile(fileID)
            if (!file?.data) return

            const fileData = file.data as File
            const meta = file.meta as any
            const relativePath = meta.relativePath as string

            console.log('[SmartUpload] 开始处理文件:', file.name, {
                size: fileData.size,
                relativePath
            })

            // 0. 确定目标文件夹ID
            let targetFolderId = this.folderId

            // 如果是文件夹上传，计算并获取/创建目标文件夹
            if (relativePath && relativePath.includes('/')) {
                const lastSlashIndex = relativePath.lastIndexOf('/')
                if (lastSlashIndex > 0) {
                    const folderPath = relativePath.substring(0, lastSlashIndex)

                    // 检查缓存
                    let folderPromise = folderCache.get(folderPath)

                    if (!folderPromise) {
                        // 如果没有进行中的请求，创建一个新的 Promise
                        folderPromise = (async () => {
                            try {
                                // 动态导入避免循环依赖
                                const { createFolderPath } = await import('@/api/file')
                                const id = await createFolderPath(folderPath, this.folderId)
                                console.log('[SmartUpload] 文件夹就绪(新创建):', folderPath, 'ID:', id)
                                return id
                            } catch (error) {
                                console.error('[SmartUpload] 创建文件夹结构失败:', error)
                                throw error
                            }
                        })()

                        // 存入缓存
                        folderCache.set(folderPath, folderPromise)
                    }

                    try {
                        // 等待结果
                        targetFolderId = await folderPromise
                    } catch (error) {
                        // 如果创建失败，可能需要重试策略或者简单的错误处理
                        // 这里简单处理：如果失败，从缓存移除，让后续请求重试（或者直接失败）
                        folderCache.delete(folderPath)
                        console.error('[SmartUpload] 等待文件夹创建失败:', error)
                    }
                }
            }

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
                folderId: targetFolderId
            })

            // 3. 更新文件 meta (一次性更新)
            this.uppy.setFileMeta(fileID, {
                fileMd5,
                folderId: targetFolderId,
                fastUploaded: checkResult.fastUploaded,
                uploadResult: checkResult.uploadResult,
                uploadedChunks: checkResult.uploadedChunks
            } as SmartUploadMeta)

            this.uppy.emit('preprocess-complete', file)
        }

        // 简单的并发限制 (Concurrency Limit = 3)
        // 既能加快速度，又能避免瞬间发起过多请求导致卡顿
        const CONCURRENT_LIMIT = 3
        const queue = [...fileIDs]
        const workers = Array(Math.min(fileIDs.length, CONCURRENT_LIMIT)).fill(null).map(async () => {
            while (queue.length > 0) {
                const fileID = queue.shift()
                if (fileID) await processFile(fileID)
            }
        })

        await Promise.all(workers)
        console.log('[SmartUpload] 预处理完成')
    }

    /**
     * 上传处理器
     */
    private uploader = async (fileIDs: string[]) => {
        console.log('[SmartUpload] 上传开始，文件数量:', fileIDs.length)

        const promises = fileIDs.map(async (fileID) => {
            const file = this.uppy.getFile(fileID)
            if (!file?.data) return

            const fileData = file.data as File
            const meta = file.meta as SmartUploadMeta

            console.log('[SmartUpload] 开始上传文件:', file.name, {
                size: fileData.size,
                fastUploaded: meta.fastUploaded,
                uploadedChunks: meta.uploadedChunks?.length || 0
            })

            try {
                // 1. 秒传成功，直接返回
                if (meta.fastUploaded && meta.uploadResult) {
                    console.log('[SmartUpload] 秒传成功，直接完成')
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
                    console.log('[SmartUpload] 使用分片上传')
                    await this.uploadWithChunks(file, fileData)
                } else {
                    console.log('[SmartUpload] 使用普通上传')
                    await this.uploadDirectly(file, fileData)
                }
            } catch (error) {
                console.error('[SmartUpload] 上传失败:', error)
                this.uppy.emit('upload-error', file, error as Error)
            }
        })

        await Promise.all(promises)
        console.log('[SmartUpload] 所有文件上传完成')
    }

    /**
     * 普通上传（小文件）
     */
    private async uploadDirectly(file: UppyFile<any, any>, fileData: File) {
        const meta = file.meta as SmartUploadMeta
        console.log('[SmartUpload] 开始普通上传:', file.name, '大小:', fileData.size, '目标文件夹:', meta.folderId)

        const uploadStarted = Date.now()

        // 发送初始进度，告诉 Uppy 上传已开始
        this.uppy.emit('upload-progress', file, {
            uploadStarted,
            bytesUploaded: 0,
            bytesTotal: fileData.size
        })
        // 直接设置文件状态
        this.uppy.setFileState(file.id, {
            progress: {
                uploadStarted,
                uploadComplete: false,
                percentage: 0,
                bytesUploaded: 0,
                bytesTotal: fileData.size
            }
        })

        const result = await uploadFile(fileData, meta.folderId ?? this.folderId, (percent) => {
            const bytesUploaded = Math.floor((fileData.size * percent) / 100)
            console.log(`[SmartUpload] 普通上传进度: ${percent}% (${bytesUploaded}/${fileData.size} bytes)`)

            this.uppy.emit('upload-progress', file, {
                uploadStarted,
                bytesUploaded,
                bytesTotal: fileData.size
            })
            // 直接设置文件状态
            this.uppy.setFileState(file.id, {
                progress: {
                    uploadStarted,
                    uploadComplete: false,
                    percentage: percent,
                    bytesUploaded,
                    bytesTotal: fileData.size
                }
            })
        })

        console.log('[SmartUpload] 普通上传完成:', result)

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

        console.log('[SmartUpload] 分片上传配置:', {
            fileName: file.name,
            fileSize: fileData.size,
            totalChunks,
            alreadyUploadedChunks: uploadedChunks.size,
            chunkSize: DEFAULT_CHUNK_SIZE
        })

        // 1. 正确计算已上传的初始字节数
        let initialUploadedBytes = 0
        for (const idx of uploadedChunks) {
            // 过滤无效的分片索引（防止后端返回脏数据导致进度计算溢出）
            if (idx >= totalChunks) continue

            // 如果是最后一个分片，大小可能不满 DEFAULT_CHUNK_SIZE
            if (idx === totalChunks - 1) {
                initialUploadedBytes += (fileData.size % DEFAULT_CHUNK_SIZE) || DEFAULT_CHUNK_SIZE
            } else {
                initialUploadedBytes += DEFAULT_CHUNK_SIZE
            }
        }

        let uploadedBytes = initialUploadedBytes
        const uploadStarted = Date.now()

        console.log('[SmartUpload] 初始已上传字节:', initialUploadedBytes, '进度:', ((initialUploadedBytes / fileData.size) * 100).toFixed(2) + '%')

        // 发送初始进度，告诉 Uppy 上传已开始
        const initialPercent = Math.round((initialUploadedBytes / fileData.size) * 100)
        this.uppy.emit('upload-progress', file, {
            uploadStarted,
            bytesUploaded: initialUploadedBytes,
            bytesTotal: fileData.size
        })
        // 直接设置文件状态
        this.uppy.setFileState(file.id, {
            progress: {
                uploadStarted,
                uploadComplete: false,
                percentage: initialPercent,
                bytesUploaded: initialUploadedBytes,
                bytesTotal: fileData.size
            }
        })

        // 用于追踪每个正在上传的分片的进度
        const chunkProgressMap = new Map<number, number>()

        // 分片上传占95%，合并分片占5%
        const UPLOAD_PHASE_RATIO = 0.95
        const MERGE_PHASE_RATIO = 0.05

        // 更新总进度的函数
        const updateProgress = (phase: 'upload' | 'merge' = 'upload', mergeProgress = 0) => {
            let currentBytes = initialUploadedBytes

            if (phase === 'upload') {
                // 上传阶段：累加所有正在上传的分片的已上传字节数
                for (const bytes of chunkProgressMap.values()) {
                    currentBytes += bytes
                }
                // 确保不倒退且不超过总大小
                if (currentBytes > uploadedBytes) {
                    uploadedBytes = currentBytes
                }
                // 限制上传阶段最多到95%
                const maxUploadBytes = Math.floor(fileData.size * UPLOAD_PHASE_RATIO)
                if (uploadedBytes > maxUploadBytes) {
                    uploadedBytes = maxUploadBytes
                }
            } else {
                // 合并阶段：上传部分占95%，合并部分占5%
                const uploadPhaseBytes = Math.floor(fileData.size * UPLOAD_PHASE_RATIO)
                const mergePhaseBytes = Math.floor(fileData.size * MERGE_PHASE_RATIO)
                currentBytes = uploadPhaseBytes + Math.floor(mergePhaseBytes * mergeProgress)
                uploadedBytes = currentBytes
            }

            const progressPercent = uploadedBytes / fileData.size
            console.log(`[SmartUpload] 进度更新 [${phase}]:`, (progressPercent * 100).toFixed(2) + '%', `(${uploadedBytes}/${fileData.size} bytes)`)

            // 方法1: emit 事件
            this.uppy.emit('upload-progress', file, {
                uploadStarted,
                bytesUploaded: uploadedBytes,
                bytesTotal: fileData.size
            })

            // 方法2: 直接设置文件状态（确保 Dashboard 能收到更新）
            const currentFile = this.uppy.getFile(file.id)
            if (currentFile) {
                this.uppy.setFileState(file.id, {
                    progress: {
                        uploadStarted,
                        uploadComplete: false,
                        percentage: Math.round(progressPercent * 100),
                        bytesUploaded: uploadedBytes,
                        bytesTotal: fileData.size
                    }
                })
            }
        }

        // 并发上传分片
        const CONCURRENT_LIMIT = 4 // 降低并发数以避免浏览器卡顿，同时配合详细进度
        const pendingChunks: number[] = []

        for (let i = 0; i < totalChunks; i++) {
            if (!uploadedChunks.has(i)) {
                pendingChunks.push(i)
            }
        }

        console.log('[SmartUpload] 待上传分片数量:', pendingChunks.length, '并发数:', CONCURRENT_LIMIT)

        // 批量并发上传
        for (let i = 0; i < pendingChunks.length; i += CONCURRENT_LIMIT) {
            const batch = pendingChunks.slice(i, i + CONCURRENT_LIMIT)
            console.log(`[SmartUpload] 开始上传批次 ${Math.floor(i / CONCURRENT_LIMIT) + 1}, 分片索引:`, batch)

            await Promise.all(batch.map(async (chunkIndex) => {
                const start = chunkIndex * DEFAULT_CHUNK_SIZE
                const end = Math.min(start + DEFAULT_CHUNK_SIZE, fileData.size)
                const chunk = fileData.slice(start, end)
                const currentChunkSize = chunk.size

                console.log(`[SmartUpload] 分片 ${chunkIndex + 1}/${totalChunks} 开始上传, 大小:`, currentChunkSize)

                // 初始化该分片的进度为 0
                chunkProgressMap.set(chunkIndex, 0)

                // 分片进度分为两阶段：
                // - 发送阶段（onUploadProgress）：占 80%
                // - 服务器确认阶段：占 20%
                const CHUNK_SEND_RATIO = 0.8

                await uploadChunk(chunk, {
                    fileMd5,
                    chunkIndex,
                    totalChunks,
                    chunkSize: currentChunkSize
                }, 3, (percent) => {
                    // onUploadProgress 只代表浏览器发送进度，最多占分片进度的 80%
                    // 服务器响应后才算 100%
                    const loaded = Math.floor(currentChunkSize * CHUNK_SEND_RATIO * percent / 100)
                    chunkProgressMap.set(chunkIndex, loaded)
                    updateProgress()
                })

                console.log(`[SmartUpload] 分片 ${chunkIndex + 1}/${totalChunks} 上传完成`)
                // 服务器响应后（Promise resolve），该分片进度设为 100%
                chunkProgressMap.set(chunkIndex, currentChunkSize)
                updateProgress()
            }))

            console.log(`[SmartUpload] 批次 ${Math.floor(i / CONCURRENT_LIMIT) + 1} 完成`)
        }

        console.log('[SmartUpload] 所有分片上传完成，准备合并')

        // 合并分片（显示合并进度）
        updateProgress('merge', 0)  // 开始合并，进度为95%
        console.log('[SmartUpload] 开始合并分片...')

        const result = await mergeChunks({
            fileMd5,
            fileName: file.name,
            totalSize: fileData.size,
            totalChunks,
            folderId: meta.folderId ?? this.folderId
        })

        console.log('[SmartUpload] 分片合并完成:', result)
        // 合并完成，更新进度到100%
        updateProgress('merge', 1)

        this.uppy.emit('upload-success', file, {
            status: 200,
            body: result as any
        })
        console.log('[SmartUpload] 文件上传完全完成:', file.name)
    }
}
