import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// API 基础配置
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

// 创建 axios 实例
const api: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Token 存储的 key
const TOKEN_KEY = 'satoken'
const TOKEN_NAME_KEY = 'satoken-name'

/**
 * 获取存储的 Token
 */
export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 Token
 */
export function setToken(token: string, tokenName?: string): void {
    localStorage.setItem(TOKEN_KEY, token)
    if (tokenName) {
        localStorage.setItem(TOKEN_NAME_KEY, tokenName)
    }
}

/**
 * 获取 Token 名称（请求头名称）
 */
export function getTokenName(): string {
    return localStorage.getItem(TOKEN_NAME_KEY) || 'satoken'
}

/**
 * 清除 Token
 */
export function clearToken(): void {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TOKEN_NAME_KEY)
}

// 请求拦截器
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = getToken()
        if (token) {
            const tokenName = getTokenName()
            config.headers[tokenName] = token
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
api.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data

        // 业务成功
        if (res.code === 200) {
            return res
        }

        // 未登录或 Token 过期
        if (res.code === 401) {
            clearToken()
            ElMessage.error('登录已过期，请重新登录')
            // 跳转到登录页
            window.location.href = '/login'
            return Promise.reject(new Error(res.message || '未授权'))
        }

        // 其他业务错误
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
    },
    (error) => {
        // 网络错误或服务器错误
        let message = '网络错误，请稍后重试'
        if (error.response) {
            switch (error.response.status) {
                case 400:
                    message = '请求参数错误'
                    break
                case 401:
                    message = '未授权，请先登录'
                    clearToken()
                    window.location.href = '/login'
                    break
                case 403:
                    message = '没有权限访问'
                    break
                case 404:
                    message = '请求的资源不存在'
                    break
                case 500:
                    message = '服务器内部错误'
                    break
                default:
                    message = error.response.data?.message || '请求失败'
            }
        } else if (error.message?.includes('timeout')) {
            message = '请求超时，请稍后重试'
        }

        ElMessage.error(message)
        return Promise.reject(error)
    }
)

// API 响应类型定义
export interface ApiResponse<T = unknown> {
    code: number
    message: string
    data: T
    timestamp: number
}

export interface PageResult<T> {
    pageNum: number
    pageSize: number
    total: number
    pages: number
    records: T[]
}

export default api
