<template>
  <div class="file-main h-full w-full flex flex-col">
    <!-- 工具栏 -->
    <div class="toolbar flex items-center justify-between p-4 border-b border-gray-100">
      <div class="left flex items-center gap-4">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>
            <span class="cursor-pointer" @click="navigateToRoot">
              <el-icon><HomeFilled /></el-icon>
            </span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="(segment, index) in folderPath" :key="segment.id">
            <span class="cursor-pointer" @click="navigateToFolder(segment.id, index)">{{ segment.name }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      
      <div class="right flex items-center gap-2">
        <!-- 新建文件夹 -->
        <el-button @click="showNewFolderDialog = true">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>
        
        <!-- 上传按钮 -->
        <el-dropdown split-button type="primary" @click="triggerUpload">
          <el-icon><Upload /></el-icon>
          上传文件
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="triggerUpload">
                <el-icon><Document /></el-icon>
                上传文件
              </el-dropdown-item>
              <el-dropdown-item @click="triggerFolderUpload">
                <el-icon><Folder /></el-icon>
                上传文件夹
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-divider direction="vertical" />

        <!-- 批量分享按钮 -->
        <el-button 
          v-if="selectedFiles.length > 0"
          type="success"
          @click="handleBatchShare"
        >
          <el-icon><Share /></el-icon>
          分享 ({{ selectedFiles.length }})
        </el-button>

        <el-divider direction="vertical" />

        <!-- 视图切换 -->
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button value="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 文件列表区域 -->
    <div class="file-content flex-1 overflow-auto p-4">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 列表视图 -->
      <el-table
        v-else-if="viewMode === 'list' && fileList.length > 0"
        :data="fileList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @row-dblclick="handleRowDblClick"
        @row-contextmenu="handleContextMenu"
        class="file-table"
        row-class-name="cursor-pointer"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="fileName" label="文件名" min-width="300">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon :size="24" :color="getFileIconColor(row.fileType)">
                <component :is="getFileIcon(row.fileType)" />
              </el-icon>
              <span class="font-medium">{{ row.fileName }}</span>
              <el-tag v-if="row.isVectorized" size="small" type="success">已向量化</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSizeStr" label="大小" width="120">
          <template #default="{ row }">
            {{ row.fileSizeStr }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isRecycleBin ? 140 : 180" fixed="right">
          <template #default="{ row }">
            <!-- 回收站操作 -->
            <template v-if="isRecycleBin">
              <el-button link type="primary" @click.stop="handleRestore(row)">
                <el-icon><RefreshLeft /></el-icon>
                恢复
              </el-button>
              <el-button link type="danger" @click.stop="handlePermanentDelete(row)">
                <el-icon><Delete /></el-icon>
                彻底删除
              </el-button>
            </template>
            <!-- 正常操作 -->
            <template v-else>
              <el-button link type="primary" @click.stop="handleDownload(row)">
                <el-icon><Download /></el-icon>
              </el-button>
              <el-button link type="primary" @click.stop="handleShare(row)">
                <el-icon><Share /></el-icon>
              </el-button>
              <el-button link type="primary" @click.stop="showRenameDialog(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button link type="danger" @click.stop="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格视图 -->
      <div v-else-if="viewMode === 'grid' && fileList.length > 0" class="grid-view grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-4">
        <div
          v-for="file in fileList"
          :key="file.id"
          class="file-card glass-card p-4 flex flex-col items-center cursor-pointer"
          @dblclick="handleRowDblClick(file)"
          @contextmenu.prevent="handleContextMenu($event, file)"
        >
          <el-icon :size="48" :color="getFileIconColor(file.fileType)">
            <component :is="getFileIcon(file.fileType)" />
          </el-icon>
          <span class="mt-2 text-sm text-center truncate w-full">{{ file.fileName }}</span>
          <span class="text-xs text-gray-400 mt-1">{{ file.fileSizeStr }}</span>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="!loading && fileList.length === 0" description="暂无文件" class="mt-20">
        <el-button type="primary" @click="triggerUpload">上传文件</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-container p-4 border-t border-gray-100 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadFileList"
      />
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" width="400px">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreateFolder" />
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名" width="400px">
      <el-input v-model="renameNewName" placeholder="请输入新名称" @keyup.enter="handleRename" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 隐藏的上传 input -->
    <input ref="fileInput" type="file" multiple hidden @change="handleFileChange" />
    <input ref="folderInput" type="file" webkitdirectory hidden @change="handleFolderChange" />

    <!-- 上传进度对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px" :close-on-click-modal="false">
      <div v-for="(item, index) in uploadQueue" :key="index" class="upload-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm truncate" style="max-width: 300px;">{{ item.name }}</span>
          <span class="text-xs text-gray-400">{{ item.status }}</span>
        </div>
        <el-progress :percentage="item.progress" :status="item.error ? 'exception' : undefined" />
      </div>
    </el-dialog>

    <!-- 分享对话框（支持单文件/文件夹/批量） -->
    <ShareBatchDialog
      v-model="shareDialogVisible"
      :selected-items="shareSelectedItems"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, FolderAdd, Upload, Document, Folder, List, Grid,
  Download, Share, Delete, Picture, VideoPlay, Headset, FolderOpened,
  Loading, Edit, RefreshLeft
} from '@element-plus/icons-vue'
import ShareBatchDialog from '@/components/ShareBatchDialog.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFileList, uploadFile, deleteFile, renameFile, downloadFileStream,
  createFolder, restoreFile, permanentDeleteFile, getRecycleList,
  type FileInfo
} from '@/api/file'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 视图模式
const viewMode = ref<'list' | 'grid'>('list')

// 加载状态
const loading = ref(false)
const creating = ref(false)
const renaming = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 当前文件夹 ID
const currentFolderId = ref(0)

// 新建文件夹
const showNewFolderDialog = ref(false)
const newFolderName = ref('')

// 重命名
const renameDialogVisible = ref(false)
const renameNewName = ref('')
const renameTargetFile = ref<FileInfo | null>(null)

// 上传相关
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)
const uploadDialogVisible = ref(false)
const uploadQueue = ref<{ name: string; progress: number; status: string; error: boolean }[]>([])

// 选中的文件
const selectedFiles = ref<FileInfo[]>([])

// 文件列表
const fileList = ref<FileInfo[]>([])

// 文件夹路径（用于面包屑导航）
const folderPath = ref<{ id: number; name: string }[]>([])

// 判断是否是回收站
const isRecycleBin = computed(() => route.meta?.isRecycle === true)

// 文件图标
const getFileIcon = (type: string) => {
  const iconMap: Record<string, unknown> = {
    folder: FolderOpened,
    image: Picture,
    video: VideoPlay,
    audio: Headset,
  }
  return iconMap[type] || Document
}

// 文件图标颜色
const getFileIconColor = (type: string) => {
  const colorMap: Record<string, string> = {
    folder: '#F97316',
    document: '#3B82F6',
    image: '#8B5CF6',
    video: '#EC4899',
    audio: '#06B6D4',
  }
  return colorMap[type] || '#6B7280'
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

// 加载文件列表
async function loadFileList() {
  loading.value = true
  try {
    // 根据是否是回收站选择不同的 API
    const result = isRecycleBin.value
      ? await getRecycleList({
          pageNum: currentPage.value,
          pageSize: pageSize.value
        })
      : await getFileList({
          folderId: currentFolderId.value,
          pageNum: currentPage.value,
          pageSize: pageSize.value,
          fileType: route.meta?.fileType as string | undefined
        })
    fileList.value = result.records
    total.value = result.total
  } catch (error) {
    console.error('加载文件列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 事件处理
const handleSelectionChange = (selection: FileInfo[]) => {
  selectedFiles.value = selection
}

const handleRowDblClick = (row: FileInfo) => {
  if (row.fileType === 'folder') {
    // 更新当前文件夹 ID
    folderPath.value.push({ id: row.id, name: row.fileName })
    currentFolderId.value = row.id
    currentPage.value = 1
    loadFileList()
  } else {
    // 预览文件
    handlePreview(row)
  }
}

// 导航到根目录
const navigateToRoot = () => {
  folderPath.value = []
  currentFolderId.value = 0
  currentPage.value = 1
  loadFileList()
}

// 导航到指定文件夹
const navigateToFolder = (folderId: number, index: number) => {
  // 截取路径到指定位置
  folderPath.value = folderPath.value.slice(0, index + 1)
  currentFolderId.value = folderId
  currentPage.value = 1
  loadFileList()
}

const handleContextMenu = (event: MouseEvent, row: FileInfo) => {
  event.preventDefault()
  console.log('右键菜单:', row)
  // TODO: 实现右键菜单
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const triggerFolderUpload = () => {
  folderInput.value?.click()
}

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return

  uploadDialogVisible.value = true
  uploadQueue.value = Array.from(files).map(f => ({
    name: f.name,
    progress: 0,
    status: '等待上传',
    error: false
  }))

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    const queueItem = uploadQueue.value[i]
    if (!queueItem || !file) continue
    
    queueItem.status = '上传中...'
    
    try {
      await uploadFile(file, currentFolderId.value, (percent) => {
        const item = uploadQueue.value[i]
        if (item) {
          item.progress = percent
        }
      })
      queueItem.status = '上传成功'
      queueItem.progress = 100
      userStore.updateUsedSpace(file.size)
    } catch {
      queueItem.status = '上传失败'
      queueItem.error = true
    }
  }

  // 刷新列表
  await loadFileList()
  
  // 重置 input
  target.value = ''
  
  // 延迟关闭对话框
  setTimeout(() => {
    uploadDialogVisible.value = false
    uploadQueue.value = []
  }, 1500)
}

const handleFolderChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files) {
    console.log('上传文件夹:', files)
    // TODO: 实现文件夹上传
  }
}

const handleCreateFolder = async () => {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  creating.value = true
  try {
    await createFolder(newFolderName.value, currentFolderId.value)
    ElMessage.success('创建成功')
    showNewFolderDialog.value = false
    newFolderName.value = ''
    await loadFileList()
  } catch (error) {
    console.error('创建文件夹失败:', error)
  } finally {
    creating.value = false
  }
}

const handleDownload = (row: FileInfo) => {
  // 直接调用下载，浏览器会根据响应头处理文件名
  downloadFileStream(row.id)
}

const handlePreview = async (row: FileInfo) => {
  try {
    const { getPreviewUrl } = await import('@/api/file')
    const url = await getPreviewUrl(row.id)
    window.open(url, '_blank')
  } catch (error) {
    console.error('获取预览链接失败:', error)
  }
}

// 分享相关
const shareDialogVisible = ref(false)
const shareSelectedItems = ref<{
  type: 'file' | 'folder'
  id: number
  name: string
  size: number
  sizeStr: string
  fileType?: string
}[]>([])

// 单文件/文件夹分享
const handleShare = (row: FileInfo) => {
  shareSelectedItems.value = [{
    type: row.fileType === 'folder' ? 'folder' : 'file',
    id: row.id,
    name: row.fileName,
    size: row.fileSize,
    sizeStr: row.fileSizeStr || '',
    fileType: row.fileType
  }]
  shareDialogVisible.value = true
}

// 批量分享
const handleBatchShare = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择要分享的文件')
    return
  }
  shareSelectedItems.value = selectedFiles.value.map(file => ({
    type: file.fileType === 'folder' ? 'folder' : 'file',
    id: file.id,
    name: file.fileName,
    size: file.fileSize,
    sizeStr: file.fileSizeStr || '',
    fileType: file.fileType
  }))
  shareDialogVisible.value = true
}

const handleShareSuccess = () => {
  // 分享创建成功的回调
  console.log('分享创建成功')
}

const showRenameDialog = (row: FileInfo) => {
  renameTargetFile.value = row
  renameNewName.value = row.fileName
  renameDialogVisible.value = true
}

const handleRename = async () => {
  if (!renameNewName.value.trim() || !renameTargetFile.value) {
    ElMessage.warning('请输入新名称')
    return
  }

  renaming.value = true
  try {
    await renameFile(renameTargetFile.value.id, renameNewName.value)
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    await loadFileList()
  } catch (error) {
    console.error('重命名失败:', error)
  } finally {
    renaming.value = false
  }
}

const handleDelete = async (row: FileInfo) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除此文件吗？文件将移入回收站。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteFile(row.id)
    ElMessage.success('删除成功')
    userStore.updateUsedSpace(-row.fileSize)
    await loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 恢复文件（回收站）
const handleRestore = async (row: FileInfo) => {
  try {
    await restoreFile(row.id)
    ElMessage.success('文件已恢复')
    await loadFileList()
  } catch (error) {
    console.error('恢复失败:', error)
  }
}

// 彻底删除文件（回收站）
const handlePermanentDelete = async (row: FileInfo) => {
  try {
    await ElMessageBox.confirm(
      '确定要彻底删除此文件吗？此操作不可恢复！',
      '彻底删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    await permanentDeleteFile(row.id)
    ElMessage.success('文件已彻底删除')
    userStore.updateUsedSpace(-row.fileSize)
    await loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('彻底删除失败:', error)
    }
  }
}

// 监听路由变化
watch(() => route.path, () => {
  currentPage.value = 1
  loadFileList()
}, { immediate: false })

// 组件挂载时加载数据
onMounted(() => {
  loadFileList()
})
</script>

<style scoped>
.h-full { height: 100%; }
.w-full { width: 100%; }
.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.gap-2 { gap: 0.5rem; }
.gap-4 { gap: 1rem; }
.p-4 { padding: 1rem; }
.mb-1 { margin-bottom: 0.25rem; }
.mb-4 { margin-bottom: 1rem; }
.mt-2 { margin-top: 0.5rem; }
.mt-20 { margin-top: 5rem; }
.overflow-auto { overflow: auto; }
.truncate { 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}
.text-center { text-align: center; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.font-medium { font-weight: 500; }
.cursor-pointer { cursor: pointer; }

.grid { display: grid; }
.grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }

@media (min-width: 640px) {
  .sm\:grid-cols-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
@media (min-width: 768px) {
  .md\:grid-cols-6 { grid-template-columns: repeat(6, minmax(0, 1fr)); }
}
@media (min-width: 1024px) {
  .lg\:grid-cols-8 { grid-template-columns: repeat(8, minmax(0, 1fr)); }
}

.file-table :deep(.el-table__row) {
  transition: background-color 0.2s;
}
.file-table :deep(.el-table__row:hover) {
  background-color: rgba(124, 58, 237, 0.05) !important;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #7C3AED;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.upload-item {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
}
</style>
