import api, { type ApiResponse } from '@/utils/api'

// ==================== 类型定义 ====================

/**
 * 语义搜索结果
 */
export interface SearchResult {
    fileId: number
    fileName: string
    fileType: string
    score: number
    matchedContent: string
    chunkIndex: number
}

/**
 * 聊天消息
 */
export interface ChatMessage {
    role: 'user' | 'assistant'
    content: string
    references?: FileReference[]
}

/**
 * 文件引用
 */
export interface FileReference {
    fileId: number
    fileName: string
    matchedContent: string
}

/**
 * 向量化状态
 */
export interface VectorizeStatus {
    fileId: number
    status: 'pending' | 'processing' | 'completed' | 'failed'
    progress: number
    chunksTotal: number
    chunksProcessed: number
    errorMessage?: string
}

/**
 * 智能问答请求参数
 */
export interface ChatParams {
    question: string  // 后端期望 question 字段
    history?: { role: 'user' | 'assistant', content: string }[]
    fileIds?: number[]  // 可选，限制在特定文件中搜索
}

/**
 * 后端返回的原始响应
 */
interface ChatBackendResponse {
    answer: string
    sources: { fileId: number; fileName: string; content: string }[]
    costMs?: number
}

/**
 * 智能问答响应（前端使用）
 */
export interface ChatResponse {
    answer: string
    references: FileReference[]
}

// ==================== AI API ====================

/**
 * 语义搜索
 * @param query 搜索查询语句
 * @param topK 返回结果数量 (默认 10)
 */
export async function semanticSearch(query: string, topK: number = 10): Promise<SearchResult[]> {
    const res = await api.post<unknown, ApiResponse<SearchResult[]>>('/ai/search', { query, topK })
    return res.data
}

/**
 * 智能问答 (RAG)
 * @param params 问答参数
 */
export async function chat(params: ChatParams): Promise<ChatResponse> {
    const res = await api.post<unknown, ApiResponse<ChatBackendResponse>>('/ai/chat', params)
    // 转换后端响应格式为前端期望的格式
    return {
        answer: res.data.answer,
        references: (res.data.sources || []).map(s => ({
            fileId: s.fileId,
            fileName: s.fileName,
            matchedContent: s.content
        }))
    }
}

/**
 * 流式智能问答 (SSE)
 * @param params 问答参数
 * @param onMessage 消息回调
 * @param onError 错误回调
 * @param onComplete 完成回调
 */
export function chatStream(
    params: ChatParams,
    onMessage: (content: string) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void
): () => void {
    const controller = new AbortController()

    const fetchData = async () => {
        try {
            const response = await fetch('/ai/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('satoken') || ''
                },
                body: JSON.stringify(params),
                signal: controller.signal
            })

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }

            const reader = response.body?.getReader()
            if (!reader) {
                throw new Error('No response body')
            }

            const decoder = new TextDecoder()

            while (true) {
                const { done, value } = await reader.read()
                if (done) break

                const text = decoder.decode(value, { stream: true })
                const lines = text.split('\n')

                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const data = line.slice(6)
                        if (data === '[DONE]') {
                            onComplete?.()
                        } else {
                            try {
                                const parsed = JSON.parse(data)
                                if (parsed.content) {
                                    onMessage(parsed.content)
                                }
                            } catch {
                                // 非 JSON 数据，直接输出
                                onMessage(data)
                            }
                        }
                    }
                }
            }
        } catch (error) {
            if ((error as Error).name !== 'AbortError') {
                onError?.(error as Error)
            }
        }
    }

    fetchData()

    // 返回取消函数
    return () => controller.abort()
}

/**
 * 文档向量化
 * @param fileId 文件 ID
 */
export async function vectorizeFile(fileId: number): Promise<void> {
    await api.post<unknown, ApiResponse<void>>(`/ai/vectorize/${fileId}`)
}

/**
 * 获取向量化状态
 * @param fileId 文件 ID
 */
export async function getVectorizeStatus(fileId: number): Promise<VectorizeStatus> {
    const res = await api.get<unknown, ApiResponse<VectorizeStatus>>(`/ai/vectorize/status/${fileId}`)
    return res.data
}

/**
 * 生成文档摘要
 * @param fileId 文件 ID
 */
export async function generateSummary(fileId: number): Promise<string> {
    const res = await api.post<unknown, ApiResponse<{ summary: string }>>(`/ai/summary/${fileId}`)
    return res.data.summary
}
