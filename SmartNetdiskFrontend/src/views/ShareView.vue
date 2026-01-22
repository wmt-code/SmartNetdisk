<template>
  <div class="share-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-blob bg-blob-1"></div>
      <div class="bg-blob bg-blob-2"></div>
      <div class="bg-blob bg-blob-3"></div>
    </div>

    <div class="glass-card share-card" :class="{ 'shake': shakeCard }">
      <!-- Logo 和标题 -->
      <div class="logo-section">
        <div class="logo-icon" :class="{ 'unlocked': !needPassword && fileInfo }">
          <el-icon :size="32" color="#fff">
            <Lock v-if="needPassword || (!fileInfo && !loading)" />
            <Unlock v-else />
          </el-icon>
        </div>
        <h1 class="title">文件分享</h1>
        <p class="subtitle" v-if="needPassword">此链接已加密保护</p>
      </div>

      <!-- 密码验证区域 -->
      <div v-if="needPassword" class="password-section">
        <div class="password-form">
          <label class="input-label">
            <el-icon :size="16"><Key /></el-icon>
            提取码
          </label>
          <div class="input-wrapper" :class="{ 'error': passwordError, 'focus': inputFocused }">
            <el-input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入4位提取码"
              size="large"
              class="password-input"
              maxlength="6"
              @keyup.enter="verifyPassword"
              @focus="inputFocused = true; passwordError = false"
              @blur="inputFocused = false"
            >
              <template #suffix>
                <el-icon 
                  class="password-toggle cursor-pointer" 
                  @click="showPassword = !showPassword"
                >
                  <View v-if="!showPassword" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </div>
          <p v-if="passwordError" class="error-message">
            <el-icon><WarningFilled /></el-icon>
            {{ passwordErrorMsg }}
          </p>
        </div>

        <el-button
          type="primary"
          size="large"
          class="verify-btn"
          :loading="loading"
          :disabled="!password.trim()"
          @click="verifyPassword"
        >
          <el-icon v-if="!loading"><Unlock /></el-icon>
          {{ loading ? '验证中...' : '提取文件' }}
        </el-button>

        <p class="hint-text">
          <el-icon><InfoFilled /></el-icon>
          提取码由分享者提供，请向分享者索取
        </p>
      </div>

      <!-- 文件信息展示 -->
      <div v-else-if="fileInfo" class="file-section">
        <div class="file-preview">
          <div class="file-icon-wrapper">
            <el-icon :size="48" color="#7C3AED"><Document /></el-icon>
            <div class="file-type-badge">{{ fileInfo.fileType?.toUpperCase() || 'FILE' }}</div>
          </div>
        </div>
        <h2 class="file-name">{{ fileInfo.fileName }}</h2>
        <div class="file-meta">
          <span class="file-size">
            <el-icon><Coin /></el-icon>
            {{ fileInfo.fileSizeStr }}
          </span>
        </div>
        <el-button
          type="primary"
          size="large"
          class="download-btn"
          :loading="downloading"
          @click="handleDownload"
        >
          <el-icon v-if="!downloading"><Download /></el-icon>
          {{ downloading ? '准备下载...' : '立即下载' }}
        </el-button>
      </div>

      <!-- 加载状态 -->
      <div v-else-if="loading" class="loading-section">
        <div class="loading-spinner">
          <div class="spinner"></div>
        </div>
        <p>正在加载分享信息...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else class="error-section">
        <div class="error-icon">
          <el-icon :size="48" color="#EF4444"><CircleClose /></el-icon>
        </div>
        <h3>分享不存在或已过期</h3>
        <p>{{ errorMessage || '该链接可能已失效，请联系分享者获取新链接' }}</p>
        <el-button type="primary" plain @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>

    <!-- 底部品牌 -->
    <div class="footer-brand">
      <span>SmartNetdisk</span> · 安全分享，轻松传递
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { 
  Lock, Unlock, Document, Download, CircleClose, HomeFilled,
  Key, View, Hide, WarningFilled, InfoFilled, Coin
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

const route = useRoute()

const loading = ref(true)
const downloading = ref(false)
const needPassword = ref(false)
const password = ref('')
const errorMessage = ref('')

// 新增：表单交互状态
const showPassword = ref(false)
const inputFocused = ref(false)
const passwordError = ref(false)
const passwordErrorMsg = ref('')
const shakeCard = ref(false)
const verifiedPassword = ref('')  // 保存验证成功的密码，用于下载

interface ShareFileInfo {
  fileName: string
  fileSize: number
  fileSizeStr: string
  fileType: string
}

const fileInfo = ref<ShareFileInfo | null>(null)

// 获取分享信息
async function fetchShareInfo() {
  const code = route.params.code as string
  if (!code) {
    errorMessage.value = '分享链接无效'
    loading.value = false
    return
  }

  try {
    const res = await api.get(`/s/${code}`)
    // 后端返回 hasPassword 表示是否需要密码
    if (res.data?.hasPassword) {
      needPassword.value = true
      // 如果有密码保护，暂存部分信息用于显示
      fileInfo.value = res.data
    } else {
      fileInfo.value = res.data
    }
  } catch (error: unknown) {
    console.error('获取分享信息失败:', error)
    errorMessage.value = '分享不存在或已过期'
  } finally {
    loading.value = false
  }
}

// 验证密码
async function verifyPassword() {
  if (!password.value.trim()) {
    passwordError.value = true
    passwordErrorMsg.value = '请输入提取码'
    triggerShake()
    return
  }

  const code = route.params.code as string
  loading.value = true
  passwordError.value = false

  try {
    const res = await api.post(`/s/${code}/verify`, { password: password.value })
    verifiedPassword.value = password.value  // 保存验证成功的密码
    fileInfo.value = res.data
    needPassword.value = false
  } catch (error: unknown) {
    console.error('验证失败:', error)
    passwordError.value = true
    passwordErrorMsg.value = '提取码错误，请重新输入'
    password.value = ''
    triggerShake()
  } finally {
    loading.value = false
  }
}

// 触发卡片抖动动画
function triggerShake() {
  shakeCard.value = true
  setTimeout(() => {
    shakeCard.value = false
  }, 500)
}

// 下载文件
function handleDownload() {
  const code = route.params.code as string
  downloading.value = true

  try {
    // 使用流式下载端点，确保文件名正确
    const params = new URLSearchParams()
    if (verifiedPassword.value) {
      params.append('password', verifiedPassword.value)
    }

    // 获取 API 基础 URL
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
    const downloadUrl = `${baseURL}/s/${code}/download/stream${params.toString() ? '?' + params.toString() : ''}`

    // 创建隐藏的 a 标签触发下载
    const link = document.createElement('a')
    link.href = downloadUrl
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('开始下载')
  } catch (error: unknown) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  } finally {
    // 延迟重置下载状态，避免按钮闪烁
    setTimeout(() => {
      downloading.value = false
    }, 1000)
  }
}

onMounted(() => {
  fetchShareInfo()
})
</script>

<style scoped lang="scss">
.share-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FAF5FF 0%, #EDE9FE 50%, #DDD6FE 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

// 背景装饰球
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 20s ease-in-out infinite;
}

.bg-blob-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #8B5CF6, #A78BFA);
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.bg-blob-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #C4B5FD, #DDD6FE);
  bottom: -50px;
  left: -50px;
  animation-delay: -7s;
}

.bg-blob-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #7C3AED, #8B5CF6);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

// 卡片样式
.share-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  text-align: center;
  position: relative;
  z-index: 10;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 
    0 25px 50px -12px rgba(124, 58, 237, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    box-shadow: 
      0 30px 60px -12px rgba(124, 58, 237, 0.2),
      0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  }

  &.shake {
    animation: shake 0.5s ease-in-out;
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

// Logo 区域
.logo-section {
  margin-bottom: 32px;

  .logo-icon {
    width: 72px;
    height: 72px;
    margin: 0 auto 16px;
    background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 10px 30px -5px rgba(124, 58, 237, 0.4);
    transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);

    &.unlocked {
      background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
      box-shadow: 0 10px 30px -5px rgba(16, 185, 129, 0.4);
      transform: scale(1.1);
    }
  }

  .title {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 700;
    background: linear-gradient(135deg, #7C3AED, #8B5CF6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .subtitle {
    margin: 0;
    font-size: 14px;
    color: #6B7280;
  }
}

// 密码输入区域
.password-section {
  .password-form {
    margin-bottom: 20px;
    text-align: left;
  }

  .input-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 8px;
  }

  .input-wrapper {
    transition: all 0.2s ease;
    border-radius: 12px;

    &.focus {
      .el-input {
        :deep(.el-input__wrapper) {
          box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.15);
          border-color: #7C3AED;
        }
      }
    }

    &.error {
      .el-input {
        :deep(.el-input__wrapper) {
          box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
          border-color: #EF4444;
        }
      }
    }
  }

  .password-input {
    :deep(.el-input__wrapper) {
      padding: 12px 16px;
      border-radius: 12px;
      border: 2px solid #E5E7EB;
      box-shadow: none;
      transition: all 0.2s ease;

      &:hover {
        border-color: #C4B5FD;
      }
    }

    :deep(.el-input__inner) {
      font-size: 16px;
      letter-spacing: 2px;
    }
  }

  .password-toggle {
    color: #9CA3AF;
    transition: color 0.2s;

    &:hover {
      color: #7C3AED;
    }
  }

  .error-message {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 8px 0 0;
    font-size: 13px;
    color: #EF4444;
    animation: fadeIn 0.2s ease;
  }

  .verify-btn {
    width: 100%;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 12px;
    background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 25px -5px rgba(124, 58, 237, 0.4);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .hint-text {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin: 20px 0 0;
    font-size: 13px;
    color: #9CA3AF;
  }
}

// 文件信息区域
.file-section {
  animation: fadeIn 0.5s ease;

  .file-preview {
    margin-bottom: 20px;
  }

  .file-icon-wrapper {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    padding: 24px 32px;
    background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
    border-radius: 20px;
    position: relative;
  }

  .file-type-badge {
    position: absolute;
    bottom: -8px;
    padding: 4px 12px;
    background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
    color: white;
    font-size: 11px;
    font-weight: 700;
    border-radius: 20px;
    letter-spacing: 0.5px;
  }

  .file-name {
    font-size: 18px;
    font-weight: 600;
    color: #1F2937;
    margin: 16px 0 8px;
    word-break: break-all;
    line-height: 1.4;
  }

  .file-meta {
    margin-bottom: 24px;
  }

  .file-size {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: #F3F4F6;
    border-radius: 20px;
    font-size: 13px;
    color: #6B7280;
  }

  .download-btn {
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    padding: 0 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 25px -5px rgba(16, 185, 129, 0.4);
    }
  }
}

// 加载区域
.loading-section {
  color: #7C3AED;
  padding: 40px 0;

  .loading-spinner {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;
  }

  .spinner {
    width: 48px;
    height: 48px;
    border: 4px solid #EDE9FE;
    border-top-color: #7C3AED;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  p {
    color: #6B7280;
    margin: 0;
  }
}

// 错误区域
.error-section {
  animation: fadeIn 0.5s ease;

  .error-icon {
    margin-bottom: 16px;
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #1F2937;
    margin: 0 0 8px;
  }

  p {
    color: #6B7280;
    margin: 0 0 24px;
    font-size: 14px;
    line-height: 1.5;
  }

  .el-button {
    border-radius: 10px;
  }
}

// 底部品牌
.footer-brand {
  position: absolute;
  bottom: 24px;
  font-size: 13px;
  color: #9CA3AF;

  span {
    font-weight: 600;
    color: #7C3AED;
  }
}

// 通用工具类
.cursor-pointer {
  cursor: pointer;
}

// 动画
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
