import SparkMD5 from 'spark-md5'

/**
 * 文件上传相关工具函数
 */

// 默认分片大小：20MB（减少请求次数，提高上传效率）
export const DEFAULT_CHUNK_SIZE = 20 * 1024 * 1024

// 大文件阈值：50MB，超过此大小使用分片上传
export const LARGE_FILE_THRESHOLD = 50 * 1024 * 1024

/**
 * 计算文件 MD5（使用 Web Worker 风格的分片处理）
 * @param file 文件对象
 * @param onProgress 进度回调 (0-100)
 * @returns MD5 字符串
 */
export async function calculateFileMD5(
    file: File,
    onProgress?: (percent: number) => void
): Promise<string> {
    return new Promise((resolve, reject) => {
        const chunkSize = 10 * 1024 * 1024 // 10MB per chunk for faster MD5 calculation
        const chunks = Math.ceil(file.size / chunkSize)
        let currentChunk = 0
        const spark = new SparkMD5.ArrayBuffer()
        const fileReader = new FileReader()

        fileReader.onload = (e) => {
            if (e.target?.result) {
                spark.append(e.target.result as ArrayBuffer)
                currentChunk++

                if (onProgress) {
                    const percent = Math.floor((currentChunk / chunks) * 100)
                    onProgress(percent)
                }

                if (currentChunk < chunks) {
                    loadNext()
                } else {
                    const md5 = spark.end()
                    resolve(md5)
                }
            }
        }

        fileReader.onerror = () => {
            reject(new Error('文件读取失败'))
        }

        function loadNext() {
            const start = currentChunk * chunkSize
            const end = Math.min(start + chunkSize, file.size)
            const blob = file.slice(start, end)
            fileReader.readAsArrayBuffer(blob)
        }

        loadNext()
    })
}

/**
 * 将文件分割成多个分片
 * @param file 文件对象
 * @param chunkSize 分片大小（字节）
 * @returns 分片数组
 */
export function splitFileIntoChunks(file: File, chunkSize: number = DEFAULT_CHUNK_SIZE): Blob[] {
    const chunks: Blob[] = []
    let start = 0

    while (start < file.size) {
        const end = Math.min(start + chunkSize, file.size)
        const chunk = file.slice(start, end)
        chunks.push(chunk)
        start = end
    }

    return chunks
}

/**
 * 判断文件是否需要分片上传
 * @param fileSize 文件大小（字节）
 * @returns 是否需要分片上传
 */
export function shouldUseChunkUpload(fileSize: number): boolean {
    return fileSize > LARGE_FILE_THRESHOLD
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的字符串
 */
export function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化上传速度
 * @param bytesPerSecond 每秒字节数
 * @returns 格式化后的速度字符串
 */
export function formatSpeed(bytesPerSecond: number): string {
    return formatFileSize(bytesPerSecond) + '/s'
}
