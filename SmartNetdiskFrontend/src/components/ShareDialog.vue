<template>
  <el-dialog
    v-model="visible"
    title="分享文件"
    width="480px"
    class="share-dialog"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div v-if="!shareResult" class="share-form">
      <!-- 文件信息 -->
      <div class="file-info glass-card p-4 mb-4 flex items-center gap-3">
        <el-icon :size="32" color="#7C3AED"><Document /></el-icon>
        <div class="flex-1 truncate">
          <p class="font-medium text-gray-800 truncate">{{ fileName }}</p>
          <p class="text-sm text-gray-500">{{ fileSizeStr || '文件' }}</p>
        </div>
      </div>

      <!-- 设置提取码 -->
      <div class="setting-item mb-4">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-medium text-gray-700">设置提取码</span>
          <el-switch v-model="usePassword" />
        </div>
        <div v-if="usePassword" class="flex gap-2">
          <el-input
            v-model="password"
            placeholder="4位提取码"
            maxlength="4"
            show-word-limit
            class="flex-1"
          />
          <el-button @click="generateRandomPassword">
            <el-icon><Refresh /></el-icon>
            随机生成
          </el-button>
        </div>
      </div>

      <!-- 有效期设置 -->
      <div class="setting-item mb-4">
        <span class="text-sm font-medium text-gray-700 mb-2 block">有效期</span>
        <el-radio-group v-model="expireDays" class="w-full">
          <el-radio-button :value="1" class="flex-1">1天</el-radio-button>
          <el-radio-button :value="7" class="flex-1">7天</el-radio-button>
          <el-radio-button :value="30" class="flex-1">30天</el-radio-button>
          <el-radio-button :value="0" class="flex-1">永久</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 分享成功结果 -->
    <div v-else class="share-result">
      <div class="success-icon mb-4">
        <el-icon :size="48" color="#10B981"><CircleCheck /></el-icon>
      </div>
      <p class="text-center text-lg font-medium text-gray-800 mb-4">分享创建成功！</p>
      
      <!-- 分享链接 -->
      <div class="link-section glass-card p-4 mb-4">
        <p class="text-sm text-gray-500 mb-2">分享链接</p>
        <div class="flex gap-2">
          <el-input :value="shareLink" readonly class="flex-1" />
          <el-button type="primary" @click="copyLink">
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
      </div>

      <!-- 提取码 -->
      <div v-if="shareResult.password" class="password-section glass-card p-4 mb-4">
        <p class="text-sm text-gray-500 mb-2">提取码</p>
        <div class="flex items-center gap-2">
          <span class="text-2xl font-mono font-bold text-primary tracking-widest">
            {{ shareResult.password }}
          </span>
          <el-button text @click="copyPassword">
            <el-icon><DocumentCopy /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 有效期提示 -->
      <p class="text-center text-sm text-gray-500">
        {{ expireText }}
      </p>
    </div>

    <template #footer>
      <div v-if="!shareResult" class="flex justify-end gap-2">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleCreate">
          创建分享
        </el-button>
      </div>
      <div v-else class="flex justify-center">
        <el-button type="primary" @click="visible = false">完成</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Document, Refresh, CircleCheck, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { 
  createShare, 
  generateShareLink, 
  generatePassword as genPwd,
  formatExpireText,
  type ShareInfo 
} from '@/api/share'

const props = defineProps<{
  modelValue: boolean
  fileId: number
  fileName: string
  fileSizeStr?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': [share: ShareInfo]
}>()

// 双向绑定 visible
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 表单状态
const usePassword = ref(true)
const password = ref('')
const expireDays = ref<1 | 7 | 30 | 0>(7)
const loading = ref(false)

// 结果状态
const shareResult = ref<ShareInfo | null>(null)

// 计算属性
const shareLink = computed(() => 
  shareResult.value ? generateShareLink(shareResult.value.shareCode) : ''
)

const expireText = computed(() => formatExpireText(expireDays.value))

// 初始化时生成随机提取码
watch(visible, (val) => {
  if (val && !password.value) {
    generateRandomPassword()
  }
})

// 生成随机提取码
function generateRandomPassword() {
  password.value = genPwd()
}

// 创建分享
async function handleCreate() {
  loading.value = true
  try {
    const result = await createShare({
      fileId: props.fileId,
      password: usePassword.value ? password.value : undefined,
      expireDays: expireDays.value
    })
    shareResult.value = result
    emit('success', result)
    ElMessage.success('分享创建成功')
  } catch (error) {
    console.error('创建分享失败:', error)
  } finally {
    loading.value = false
  }
}

// 复制链接
async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 复制提取码
async function copyPassword() {
  if (!shareResult.value?.password) return
  try {
    await navigator.clipboard.writeText(shareResult.value.password)
    ElMessage.success('提取码已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 关闭对话框时重置状态
function handleClosed() {
  shareResult.value = null
  password.value = ''
  usePassword.value = true
  expireDays.value = 7
}
</script>

<style scoped>
.share-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.success-icon {
  text-align: center;
}

.text-primary {
  color: #7C3AED;
}

.font-mono {
  font-family: 'Courier New', Courier, monospace;
}

.tracking-widest {
  letter-spacing: 0.2em;
}
</style>
