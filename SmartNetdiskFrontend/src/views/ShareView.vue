<template>
  <div class="share-container">
    <div class="glass-card share-card">
      <!-- Logo -->
      <div class="logo-section">
        <el-icon :size="48" color="#7C3AED"><Share /></el-icon>
        <h1 class="title">文件分享</h1>
      </div>

      <!-- 密码验证 -->
      <div v-if="needPassword" class="password-section">
        <p class="tip">此分享需要提取码</p>
        <el-input
          v-model="password"
          placeholder="请输入提取码"
          size="large"
          class="password-input"
          @keyup.enter="verifyPassword"
        />
        <el-button
          type="primary"
          size="large"
          class="verify-btn"
          :loading="loading"
          @click="verifyPassword"
        >
          提取文件
        </el-button>
      </div>

      <!-- 文件信息 -->
      <div v-else-if="fileInfo" class="file-section">
        <div class="file-icon">
          <el-icon :size="64" color="#7C3AED"><Document /></el-icon>
        </div>
        <h2 class="file-name">{{ fileInfo.fileName }}</h2>
        <p class="file-size">{{ fileInfo.fileSizeStr }}</p>
        <el-button
          type="primary"
          size="large"
          class="download-btn"
          :loading="downloading"
          @click="handleDownload"
        >
          <el-icon><Download /></el-icon>
          下载文件
        </el-button>
      </div>

      <!-- 加载中 -->
      <div v-else-if="loading" class="loading-section">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 错误或过期 -->
      <div v-else class="error-section">
        <el-icon :size="64" color="#EF4444"><CircleClose /></el-icon>
        <p>{{ errorMessage || '分享不存在或已过期' }}</p>
        <el-button @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Share, Document, Download, Loading, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

const route = useRoute()

const loading = ref(true)
const downloading = ref(false)
const needPassword = ref(false)
const password = ref('')
const errorMessage = ref('')

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
    if (res.data?.needPassword) {
      needPassword.value = true
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
    ElMessage.warning('请输入提取码')
    return
  }

  const code = route.params.code as string
  loading.value = true

  try {
    const res = await api.post(`/s/${code}/verify`, { password: password.value })
    fileInfo.value = res.data
    needPassword.value = false
  } catch (error: unknown) {
    console.error('验证失败:', error)
    ElMessage.error('提取码错误')
  } finally {
    loading.value = false
  }
}

// 下载文件
async function handleDownload() {
  const code = route.params.code as string
  downloading.value = true

  try {
    const res = await api.get(`/s/${code}/download`)
    if (res.data?.url) {
      window.open(res.data.url, '_blank')
    }
  } catch (error: unknown) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
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
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FAF5FF 0%, #EDE9FE 50%, #DDD6FE 100%);
  padding: 20px;
}

.share-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  text-align: center;
}

.logo-section {
  margin-bottom: 32px;

  .title {
    margin: 16px 0 0;
    font-size: 24px;
    font-weight: 700;
    color: #7C3AED;
  }
}

.password-section {
  .tip {
    color: #6B7280;
    margin-bottom: 16px;
  }

  .password-input {
    margin-bottom: 16px;
  }

  .verify-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
  }
}

.file-section {
  .file-icon {
    margin-bottom: 16px;
  }

  .file-name {
    font-size: 18px;
    font-weight: 600;
    color: #1F2937;
    margin: 0 0 8px;
    word-break: break-all;
  }

  .file-size {
    color: #6B7280;
    margin: 0 0 24px;
  }

  .download-btn {
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    padding: 0 32px;
  }
}

.loading-section {
  color: #7C3AED;

  .loading-icon {
    animation: spin 1s linear infinite;
  }
}

.error-section {
  p {
    color: #6B7280;
    margin: 16px 0 24px;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
