<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="520px"
    class="share-batch-dialog"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div v-if="!shareResult" class="share-form">
      <!-- 分享预览区 -->
      <div class="share-preview glass-card">
        <div class="preview-header">
          <div class="preview-icon" :class="shareTypeClass">
            <el-icon :size="28">
              <component :is="shareTypeIcon" />
            </el-icon>
          </div>
          <div class="preview-info">
            <h3 class="preview-title">{{ shareTitle }}</h3>
            <p class="preview-meta">
              <span v-if="fileCountDisplay">{{ fileCountDisplay }}</span>
              <span v-if="totalSizeDisplay" class="meta-divider">{{ totalSizeDisplay }}</span>
            </p>
          </div>
        </div>
        
        <!-- 文件列表预览 -->
        <div v-if="selectedItems.length > 1" class="preview-list">
          <div 
            v-for="(item, index) in displayItems" 
            :key="index"
            class="preview-item"
          >
            <el-icon :size="18" :color="getItemColor(item)">
              <component :is="getItemIcon(item)" />
            </el-icon>
            <span class="item-name">{{ item.name }}</span>
            <span class="item-size">{{ item.sizeStr }}</span>
          </div>
          <div v-if="selectedItems.length > 3" class="preview-more">
            还有 {{ selectedItems.length - 3 }} 项...
          </div>
        </div>
      </div>

      <!-- 分享设置 -->
      <div class="share-settings">
        <!-- 提取码 -->
        <div class="setting-row">
          <div class="setting-label">
            <el-icon><Lock /></el-icon>
            <span>提取码</span>
          </div>
          <div class="setting-control">
            <el-switch v-model="usePassword" />
          </div>
        </div>
        <div v-if="usePassword" class="password-input-row">
          <el-input
            v-model="password"
            placeholder="4位提取码"
            maxlength="4"
            show-word-limit
            class="password-input"
          />
          <el-button text type="primary" @click="generateRandomPassword">
            <el-icon><Refresh /></el-icon>
            随机
          </el-button>
        </div>

        <!-- 有效期 -->
        <div class="setting-row">
          <div class="setting-label">
            <el-icon><Timer /></el-icon>
            <span>有效期</span>
          </div>
        </div>
        <div class="expire-options">
          <el-radio-group v-model="expireDays" class="expire-radio-group">
            <el-radio-button :value="1">1天</el-radio-button>
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
            <el-radio-button :value="0">永久</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </div>

    <!-- 分享成功结果 -->
    <div v-else class="share-result">
      <div class="result-animation">
        <div class="success-circle">
          <el-icon :size="36" color="#fff"><Check /></el-icon>
        </div>
      </div>
      <h3 class="result-title">分享创建成功！</h3>
      
      <!-- 分享链接 -->
      <div class="result-card">
        <div class="result-row">
          <span class="result-label">分享链接</span>
          <div class="result-value link-value">
            <span class="link-text">{{ shareLink }}</span>
            <el-button type="primary" size="small" @click="copyLink">
              复制链接
            </el-button>
          </div>
        </div>
        <div v-if="shareResult.password" class="result-row">
          <span class="result-label">提取码</span>
          <div class="result-value">
            <span class="password-display">{{ shareResult.password }}</span>
            <el-button text size="small" @click="copyPassword">
              <el-icon><DocumentCopy /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 一键复制 -->
      <el-button 
        type="primary" 
        size="large" 
        class="copy-all-btn"
        @click="copyAll"
      >
        <el-icon><DocumentCopy /></el-icon>
        复制链接及提取码
      </el-button>

      <p class="expire-hint">{{ expireText }}</p>
    </div>

    <template #footer>
      <div v-if="!shareResult" class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleCreate">
          创建分享
        </el-button>
      </div>
      <div v-else class="dialog-footer center">
        <el-button type="primary" @click="visible = false">完成</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { 
  Lock, Refresh, Timer, Check, DocumentCopy,
  Document, Folder, Picture, VideoPlay, Headset, FolderOpened
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { 
  createShare, 
  generateShareLink, 
  generatePassword as genPwd,
  formatExpireText,
  ShareType,
  ItemType,
  type ShareInfo,
  type CreateShareItemParam
} from '@/api/share'

// Props - 支持单文件、文件夹、多选
interface SelectedItem {
  type: 'file' | 'folder'
  id: number
  name: string
  size: number
  sizeStr: string
  fileType?: string
}

const props = defineProps<{
  modelValue: boolean
  selectedItems: SelectedItem[]
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

// 计算分享类型
const shareType = computed(() => {
  if (props.selectedItems.length === 1) {
    const firstItem = props.selectedItems[0]
    return firstItem?.type === 'folder' ? ShareType.FOLDER : ShareType.SINGLE
  }
  return ShareType.BATCH
})

// 分享类型样式
const shareTypeClass = computed(() => ({
  'type-file': shareType.value === ShareType.SINGLE,
  'type-folder': shareType.value === ShareType.FOLDER,
  'type-batch': shareType.value === ShareType.BATCH
}))

// 分享类型图标
const shareTypeIcon = computed(() => {
  switch (shareType.value) {
    case ShareType.FOLDER: return FolderOpened
    case ShareType.BATCH: return Folder
    default: return Document
  }
})

// 对话框标题
const dialogTitle = computed(() => {
  switch (shareType.value) {
    case ShareType.FOLDER: return '分享文件夹'
    case ShareType.BATCH: return '批量分享'
    default: return '分享文件'
  }
})

// 分享标题
const shareTitle = computed(() => {
  if (props.selectedItems.length === 1) {
    const firstItem = props.selectedItems[0]
    return firstItem?.name ?? '未命名'
  }
  return `${props.selectedItems.length} 个项目`
})

// 文件数显示
const fileCountDisplay = computed(() => {
  if (props.selectedItems.length > 1) {
    return `${props.selectedItems.length} 项`
  }
  const item = props.selectedItems[0]
  if (item?.type === 'folder') {
    return '文件夹'
  }
  return null
})

// 总大小显示
const totalSizeDisplay = computed(() => {
  const total = props.selectedItems.reduce((sum, item) => sum + item.size, 0)
  return formatSize(total)
})

// 显示的项目列表（最多3个）
const displayItems = computed(() => props.selectedItems.slice(0, 3))

// 分享链接
const shareLink = computed(() => 
  shareResult.value ? generateShareLink(shareResult.value.shareCode) : ''
)

// 过期时间文本
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

// 获取项目图标
function getItemIcon(item: SelectedItem) {
  if (item.type === 'folder') return FolderOpened
  const fileType = item.fileType ?? ''
  switch (fileType) {
    case 'image': return Picture
    case 'video': return VideoPlay
    case 'audio': return Headset
    default: return Document
  }
}

// 获取项目颜色
function getItemColor(item: SelectedItem) {
  if (item.type === 'folder') return '#F97316'
  const fileType = item.fileType ?? ''
  switch (fileType) {
    case 'image': return '#8B5CF6'
    case 'video': return '#EC4899'
    case 'audio': return '#06B6D4'
    default: return '#3B82F6'
  }
}

// 格式化大小
function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 创建分享
async function handleCreate() {
  loading.value = true
  try {
    const params: any = {
      shareType: shareType.value,
      password: usePassword.value ? password.value : undefined,
      expireDays: expireDays.value
    }

    const firstItem = props.selectedItems[0]
    if (shareType.value === ShareType.SINGLE && firstItem) {
      // 单文件
      params.fileId = firstItem.id
    } else if (shareType.value === ShareType.FOLDER && firstItem) {
      // 目录
      params.folderId = firstItem.id
    } else {
      // 批量
      params.items = props.selectedItems.map(item => ({
        itemType: item.type === 'folder' ? ItemType.FOLDER : ItemType.FILE,
        fileId: item.type === 'file' ? item.id : undefined,
        folderId: item.type === 'folder' ? item.id : undefined
      } as CreateShareItemParam))
    }

    const result = await createShare(params)
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
    ElMessage.error('复制失败')
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

// 复制全部
async function copyAll() {
  if (!shareResult.value) return
  try {
    let text = `链接: ${shareLink.value}`
    if (shareResult.value.password) {
      text += `\n提取码: ${shareResult.value.password}`
    }
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制链接和提取码')
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

<style scoped lang="scss">
.share-batch-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
  }
  
  :deep(.el-dialog__header) {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #F3F4F6;
  }
  
  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }
}

// 预览区
.share-preview {
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  
  &.type-file {
    background: linear-gradient(135deg, #3B82F6, #60A5FA);
  }
  
  &.type-folder {
    background: linear-gradient(135deg, #F97316, #FB923C);
  }
  
  &.type-batch {
    background: linear-gradient(135deg, #7C3AED, #A78BFA);
  }
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-meta {
  margin: 0;
  font-size: 13px;
  color: #6B7280;
  
  .meta-divider::before {
    content: '·';
    margin: 0 6px;
  }
}

.preview-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  
  .item-name {
    flex: 1;
    color: #374151;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .item-size {
    color: #9CA3AF;
    font-size: 12px;
  }
}

.preview-more {
  padding: 8px 0 0;
  font-size: 12px;
  color: #7C3AED;
}

// 设置区
.share-settings {
  background: #FAFAFA;
  border-radius: 12px;
  padding: 16px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.setting-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.password-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  
  .password-input {
    width: 120px;
    
    :deep(.el-input__inner) {
      letter-spacing: 2px;
      font-weight: 600;
    }
  }
}

.expire-options {
  padding: 8px 0;
}

.expire-radio-group {
  width: 100%;
  
  :deep(.el-radio-button) {
    flex: 1;
  }
  
  :deep(.el-radio-button__inner) {
    width: 100%;
    border-radius: 8px !important;
    border: none !important;
    box-shadow: none !important;
    background: #fff;
    
    &:hover {
      color: #7C3AED;
    }
  }
  
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
    color: #fff;
  }
}

// 结果区
.share-result {
  text-align: center;
  padding: 20px 0;
}

.result-animation {
  margin-bottom: 16px;
}

.success-circle {
  width: 72px;
  height: 72px;
  margin: 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.result-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
}

.result-card {
  background: #F9FAFB;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.result-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  
  &:not(:last-child) {
    border-bottom: 1px solid #E5E7EB;
  }
}

.result-label {
  font-size: 13px;
  color: #6B7280;
}

.result-value {
  display: flex;
  align-items: center;
  gap: 8px;
  
  &.link-value {
    flex: 1;
    justify-content: flex-end;
  }
  
  .link-text {
    font-size: 13px;
    color: #7C3AED;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.password-display {
  font-size: 18px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 3px;
  color: #7C3AED;
}

.copy-all-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
  border: none;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px -5px rgba(124, 58, 237, 0.4);
  }
}

.expire-hint {
  margin: 16px 0 0;
  font-size: 13px;
  color: #9CA3AF;
}

// 底部按钮
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  
  &.center {
    justify-content: center;
  }
}
</style>
