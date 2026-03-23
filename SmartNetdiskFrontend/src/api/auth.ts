import api, { type ApiResponse, setToken, clearToken } from '@/utils/api'

// ==================== 类型定义 ====================

/**
 * 登录请求参数
 */
export interface LoginParams {
  username: string
  password: string
  rememberMe?: boolean
}

/**
 * 注册请求参数
 */
export interface RegisterParams {
  username: string
  email: string
  password: string
  confirmPassword: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  email: string
  avatar: string | null
  role: string
  usedSpace: number
  totalSpace: number
  usedPercent: number
  createTime: string
}

/**
 * 登录响应
 */
export interface LoginResult {
  token: string
  tokenName: string
  userInfo: UserInfo
}

// ==================== API 方法 ====================

/**
 * 用户登录
 */
export async function login(params: LoginParams): Promise<LoginResult> {
  const res = await api.post<unknown, ApiResponse<LoginResult>>('/auth/login', params)
  // 保存 Token
  if (res.data) {
    setToken(res.data.token, res.data.tokenName)
  }
  return res.data
}

/**
 * 用户注册
 */
export async function register(params: RegisterParams): Promise<void> {
  await api.post<unknown, ApiResponse<void>>('/auth/register', params)
}

/**
 * 退出登录
 */
export async function logout(): Promise<void> {
  try {
    await api.post<unknown, ApiResponse<void>>('/auth/logout')
  } finally {
    clearToken()
  }
}

/**
 * 刷新 Token
 */
export async function refreshToken(): Promise<LoginResult> {
  const res = await api.post<unknown, ApiResponse<LoginResult>>('/auth/refresh')
  if (res.data) {
    setToken(res.data.token, res.data.tokenName)
  }
  return res.data
}

/**
 * 获取当前用户信息
 */
export async function getCurrentUser(): Promise<UserInfo> {
  const res = await api.get<unknown, ApiResponse<UserInfo>>('/auth/info')
  return res.data
}

/**
 * 检查登录状态
 */
export async function checkLogin(): Promise<boolean> {
  const res = await api.get<unknown, ApiResponse<boolean>>('/auth/check')
  return res.data
}

/**
 * 获取用户资料
 */
export async function getUserProfile(): Promise<any> {
  const res = await api.get<unknown, ApiResponse<any>>('/user/profile')
  return res.data
}

/**
 * 更新用户资料
 */
export async function updateProfile(data: { username?: string; avatar?: string }): Promise<void> {
  await api.put<unknown, ApiResponse<void>>('/user/profile', data)
}

/**
 * 修改密码
 */
export async function changePassword(data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}): Promise<void> {
  await api.put<unknown, ApiResponse<void>>('/user/password', data)
}

/**
 * 获取空间使用情况
 */
export async function getSpaceInfo(): Promise<{
  usedSpace: number
  totalSpace: number
  usedPercent: number
}> {
  const res = await api.get<
    unknown,
    ApiResponse<{ usedSpace: number; totalSpace: number; usedPercent: number }>
  >('/user/space')
  return res.data
}
