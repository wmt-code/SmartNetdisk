import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import {
  login as loginApi,
  logout as logoutApi,
  getCurrentUser,
  type LoginParams,
  type UserInfo,
} from '@/api/auth'
import { getToken, clearToken } from '@/utils/api'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  // ==================== State ====================

  /**
   * 用户信息
   */
  const userInfo = ref<UserInfo | null>(null)

  /**
   * 是否已登录
   */
  const isLoggedIn = computed(() => !!getToken())

  /**
   * 用户名
   */
  const username = computed(() => userInfo.value?.username || '')

  /**
   * 头像 URL
   */
  const avatar = computed(() => userInfo.value?.avatar || '')

  /**
   * 已用空间（字节）
   */
  const usedSpace = computed(() => userInfo.value?.usedSpace || 0)

  /**
   * 总空间（字节）
   */
  const totalSpace = computed(() => userInfo.value?.totalSpace || 0)

  /**
   * 空间使用百分比
   */
  const usedPercent = computed(() => {
    if (!userInfo.value || userInfo.value.totalSpace === 0) return 0
    const pct = (userInfo.value.usedSpace / userInfo.value.totalSpace) * 100
    return pct < 1 && pct > 0 ? Math.round(pct * 10) / 10 : Math.round(pct)
  })

  /**
   * 格式化已用空间
   */
  const usedSpaceStr = computed(() => formatSize(usedSpace.value))

  /**
   * 格式化总空间
   */
  const totalSpaceStr = computed(() => formatSize(totalSpace.value))

  // ==================== Actions ====================

  /**
   * 用户登录
   */
  async function login(params: LoginParams): Promise<void> {
    const result = await loginApi(params)
    userInfo.value = result.userInfo
  }

  /**
   * 用户登出
   */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      userInfo.value = null
      clearToken()
    }
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo(): Promise<void> {
    if (!getToken()) return
    try {
      userInfo.value = await getCurrentUser()
    } catch (error) {
      // 获取用户信息失败，清除登录状态
      userInfo.value = null
      clearToken()
      throw error
    }
  }

  /**
   * 更新用户信息（本地）
   */
  function updateUserInfo(info: Partial<UserInfo>): void {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
    }
  }

  /**
   * 更新空间使用量
   */
  function updateUsedSpace(delta: number): void {
    if (userInfo.value) {
      userInfo.value.usedSpace += delta
    }
  }

  // ==================== Helpers ====================

  /**
   * 格式化文件大小
   */
  function formatSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return {
    // State
    userInfo,
    isLoggedIn,
    username,
    avatar,
    usedSpace,
    totalSpace,
    usedPercent,
    usedSpaceStr,
    totalSpaceStr,
    // Actions
    login,
    logout,
    fetchUserInfo,
    updateUserInfo,
    updateUsedSpace,
    // Helpers
    formatSize,
  }
})
