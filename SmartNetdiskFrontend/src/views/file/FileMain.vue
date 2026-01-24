<template>
  <div class="file-main h-full w-full flex flex-col">
    <!-- 工具栏 -->
    <div class="toolbar flex items-center justify-between p-4 border-b border-gray-100">
      <div class="left flex items-center gap-4">
        <!-- 面包屑导航 - 非回收站 -->
        <el-breadcrumb v-if="!isRecycleBin" separator="/">
          <el-breadcrumb-item>
            <span class="cursor-pointer" @click="navigateToRoot">
              <el-icon><HomeFilled /></el-icon>
            </span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="(segment, index) in folderPath" :key="segment.id">
            <span class="cursor-pointer" @click="navigateToFolder(segment.id, index)">{{ segment.name }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 回收站标题 -->
        <div v-if="isRecycleBin" class="flex items-center gap-2">
          <el-icon :size="20"><Delete /></el-icon>
          <span class="font-medium text-lg">回收站</span>
        </div>
      </div>

      <div class="right flex items-center gap-2">
        <!-- 新建文件夹 -->
        <el-button v-if="!isRecycleBin" @click="showNewFolderDialog = true">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>

        <!-- 上传按钮 -->
        <el-dropdown v-if="!isRecycleBin" split-button type="primary" @click="triggerUpload">
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

        <el-divider v-if="!isRecycleBin" direction="vertical" />

        <!-- 批量分享按钮 -->
        <el-button
          v-if="selectedFiles.length > 0 && !isRecycleBin"
          type="success"
          @click="handleBatchShare"
        >
          <el-icon><Share /></el-icon>
          分享 ({{ selectedFiles.length }})
        </el-button>

        <!-- 批量操作按钮组 - 普通文件列表 -->
        <el-button-group v-if="selectedFiles.length > 0 && !isRecycleBin" class="ml-2">
          <el-button @click="handleBatchMove">
            移动
          </el-button>
          <el-button @click="handleBatchCopy">
            复制
          </el-button>
          <el-button type="danger" @click="handleBatchDelete">
            删除
          </el-button>
        </el-button-group>

        <!-- 批量操作按钮组 - 回收站 -->
        <el-button-group v-if="selectedFiles.length > 0 && isRecycleBin" class="ml-2">
          <el-button type="primary" @click="handleBatchRestore">
            恢复 ({{ selectedFiles.length }})
          </el-button>
          <el-button type="danger" @click="handleBatchPermanentDelete">
            彻底删除
          </el-button>
        </el-button-group>

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
        <el-table-column label="操作" :width="isRecycleBin ? 200 : 180" fixed="right">
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

    <!-- Uppy 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="handleUploadClose"
    >
      <UppyUploader
        :folder-id="currentFolderId"
        :visible="uploadDialogVisible"
        @close="handleUploadClose"
        @success="handleUploadSuccess"
      />
    </el-dialog>

    <!-- 分享对话框（支持单文件/文件夹/批量） -->
    <ShareBatchDialog
      v-model="shareDialogVisible"
      :selected-items="shareSelectedItems"
      @success="handleShareSuccess"
    />

    <!-- 移动文件夹对话框 -->
    <FolderSelectDialog
      v-model:visible="moveDialogVisible"
      title="移动到"
      :current-folder-id="currentFolderId"
      :exclude-ids="operationTargetFiles.map(f => f.id)"
      @confirm="confirmMove"
    />

    <!-- 复制文件夹对话框 -->
    <FolderSelectDialog
      v-model:visible="copyDialogVisible"
      title="复制到"
      :current-folder-id="currentFolderId"
      @confirm="confirmCopy"
    />

    <!-- 文件预览对话框 -->
    <FilePreviewDialog
      v-model="previewDialogVisible"
      :file="previewTargetFile"
      @edit="handleOpenEditor"
    />

    <!-- 文件编辑器对话框 -->
    <FileEditorDialog
      v-model="editorDialogVisible"
      :file="editorTargetFile"
      @saved="handleEditorSaved"
    />

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div
        v-show="contextMenuVisible"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
      >
        <div
          v-if="contextMenuTarget?.fileType === 'folder'"
          class="menu-item"
          @click="handleContextMenuAction('open')"
        >
          <el-icon><FolderOpened /></el-icon> 打开
        </div>
        <div
          v-if="contextMenuTarget?.fileType !== 'folder'"
          class="menu-item"
          @click="handleContextMenuAction('preview')"
        >
          <el-icon><View /></el-icon> 预览
        </div>
        <div
          v-if="isEditableFile(contextMenuTarget)"
          class="menu-item"
          @click="handleContextMenuAction('edit')"
        >
          <el-icon><EditPen /></el-icon> 编辑
        </div>
        <div class="menu-item" @click="handleContextMenuAction('download')">
          <el-icon><Download /></el-icon> 下载
        </div>
        <div
          v-if="['pdf', 'doc', 'docx', 'txt', 'md'].includes(contextMenuTarget?.fileExt || '')"
          class="menu-item"
          @click="handleContextMenuAction('vectorize')"
        >
          <el-icon><MagicStick /></el-icon> 智能分析
        </div>
        <div class="menu-item" @click="handleContextMenuAction('share')">
          <el-icon><Share /></el-icon> 分享
        </div>
        <el-divider class="menu-divider" />
        <div class="menu-item" @click="handleContextMenuAction('move')">
          <el-icon><Scissor /></el-icon> 移动
        </div>
        <div class="menu-item" @click="handleContextMenuAction('copy')">
          <el-icon><DocumentCopy /></el-icon> 复制
        </div>
        <div class="menu-item" @click="handleContextMenuAction('rename')">
          <el-icon><Edit /></el-icon> 重命名
        </div>
        <div class="menu-item danger" @click="handleContextMenuAction('delete')">
          <el-icon><Delete /></el-icon> 删除
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  HomeFilled, FolderAdd, Upload, Document, Folder, List, Grid,
  Download, Share, Delete, Picture, VideoPlay, Headset, FolderOpened,
  Loading, Edit, RefreshLeft, Scissor, DocumentCopy, MagicStick, View, EditPen
} from '@element-plus/icons-vue'
import ShareBatchDialog from '@/components/ShareBatchDialog.vue'
import FolderSelectDialog from '@/components/FolderSelectDialog.vue'
import UppyUploader from '@/components/UppyUploader.vue'
import FilePreviewDialog from '@/components/FilePreviewDialog.vue'
import FileEditorDialog from '@/components/FileEditorDialog.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFileList, deleteFile, renameFile, downloadFileStream,
  createFolder, restoreFile, permanentDeleteFile, getRecycleList, deleteFolder,
  type FileInfo
} from '@/api/file'
import { vectorizeFile } from '@/api/ai'


import { useUserStore } from '@/stores/user'

const route = useRoute()
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
const uploadDialogVisible = ref(false)

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
    // 打开文件预览
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
  contextMenuTarget.value = row
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
  
  // 点击其他地方关闭菜单
  const closeMenu = () => {
    contextMenuVisible.value = false
    document.removeEventListener('click', closeMenu)
  }
  document.addEventListener('click', closeMenu)
}

// 右键菜单状态
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const contextMenuTarget = ref<FileInfo | null>(null)

// 处理右键菜单动作
const handleContextMenuAction = (action: string) => {
  if (!contextMenuTarget.value) return
  const row = contextMenuTarget.value

  switch (action) {
    case 'open':
      handleRowDblClick(row)
      break
    case 'preview':
      handlePreview(row)
      break
    case 'edit':
      handleOpenEditor(row)
      break
    case 'download':
      if (row.fileType === 'folder') {
        ElMessage.warning('文件夹暂不支持下载')
      } else {
        handleDownload(row)
      }
      break
    case 'vectorize':
      handleVectorize(row)
      break
    case 'share':
      handleShare(row)
      break
    case 'move':
      handleMove(row)
      break
    case 'copy':
      handleCopy(row)
      break
    case 'rename':
      showRenameDialog(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
  contextMenuVisible.value = false
}

const triggerUpload = () => {
  uploadDialogVisible.value = true
}

const triggerFolderUpload = () => {
  uploadDialogVisible.value = true
}

// Uppy 上传关闭处理（不刷新列表）
const handleUploadClose = () => {
  uploadDialogVisible.value = false
  // 注：不在关闭时刷新列表，只在上传成功时刷新
}

// Uppy 上传成功处理
const handleUploadSuccess = (files: { id: number; name: string }[]) => {
  ElMessage.success(`成功上传 ${files.length} 个文件`)
  loadFileList()
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
    const isFolder = row.fileType === 'folder'
    const typeName = isFolder ? '文件夹' : '文件'
    
    await ElMessageBox.confirm(
      `确定要删除此${typeName}吗？${isFolder ? '文件夹内的所有内容也将被删除。' : '文件将移入回收站。'}`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (isFolder) {
      await deleteFolder(row.id)
    } else {
      await deleteFile(row.id)
      userStore.updateUsedSpace(-row.fileSize)
    }
    
    ElMessage.success('删除成功')
    await loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

// 恢复文件（回收站）
const handleRestore = async (row: FileInfo) => {
  try {
    const isFolder = row.fileType === 'folder'

    if (isFolder) {
      const { restoreFolder } = await import('@/api/file')
      await restoreFolder(row.id)
    } else {
      await restoreFile(row.id)
    }

    ElMessage.success(`${isFolder ? '文件夹' : '文件'}已恢复`)
    await loadFileList()
  } catch (error) {
    console.error('恢复失败:', error)
  }
}

// 彻底删除文件（回收站）
const handlePermanentDelete = async (row: FileInfo) => {
  try {
    const isFolder = row.fileType === 'folder'
    const typeName = isFolder ? '文件夹' : '文件'

    await ElMessageBox.confirm(
      `确定要彻底删除此${typeName}吗？此操作不可恢复！`,
      '彻底删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    if (isFolder) {
      const { permanentDeleteFolder } = await import('@/api/file')
      await permanentDeleteFolder(row.id)
    } else {
      await permanentDeleteFile(row.id)
      userStore.updateUsedSpace(-row.fileSize)
    }

    ElMessage.success(`${typeName}已彻底删除`)
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

const handleVectorize = async (row: FileInfo) => {
  try {
    await vectorizeFile(row.id)
    ElMessage.success('已开始智能分析，请稍后在 AI 助手中查看')
  } catch (error) {
    console.error('触发向量化失败:', error)
  }
}

// 移动/复制相关
const moveDialogVisible = ref(false)
const copyDialogVisible = ref(false)
const operationTargetFiles = ref<FileInfo[]>([]) // 当前操作的目标文件列表

// 预览和编辑相关
const previewDialogVisible = ref(false)
const previewTargetFile = ref<FileInfo | null>(null)
const editorDialogVisible = ref(false)
const editorTargetFile = ref<FileInfo | null>(null)

// 可编辑的文件扩展名
const EDITABLE_EXTENSIONS = [
  'txt', 'md', 'markdown', 'log',
  'json', 'xml', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg', 'properties',
  'html', 'htm', 'css', 'scss', 'sass', 'less', 'js', 'ts', 'jsx', 'tsx', 'vue', 'svelte',
  'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'rb', 'php', 'swift', 'kt', 'kts',
  'scala', 'groovy', 'r', 'lua', 'pl', 'pm', 'sh', 'bash', 'zsh', 'fish', 'bat', 'cmd', 'ps1',
  'sql', 'gitignore', 'dockerignore', 'editorconfig', 'env'
]

// 判断文件是否可编辑
const isEditableFile = (file: FileInfo | null): boolean => {
  if (!file) return false
  const ext = file.fileExt?.toLowerCase() || ''
  return EDITABLE_EXTENSIONS.includes(ext)
}

// 打开预览
const handlePreview = (row: FileInfo) => {
  previewTargetFile.value = row
  previewDialogVisible.value = true
}

// 打开编辑器
const handleOpenEditor = (row: FileInfo) => {
  editorTargetFile.value = row
  editorDialogVisible.value = true
  previewDialogVisible.value = false // 关闭预览（如果从预览打开编辑）
}

// 编辑器保存后的回调
const handleEditorSaved = () => {
  loadFileList() // 刷新文件列表（文件大小可能变化）
}

// 单文件/文件夹 移动/复制
const handleMove = (row: FileInfo) => {
  operationTargetFiles.value = [row]
  moveDialogVisible.value = true
}

const handleCopy = (row: FileInfo) => {
  operationTargetFiles.value = [row]
  copyDialogVisible.value = true
}

// 批量 移动/复制/删除
const handleBatchMove = () => {
  if (selectedFiles.value.length === 0) return
  operationTargetFiles.value = selectedFiles.value
  moveDialogVisible.value = true
}

const handleBatchCopy = () => {
  if (selectedFiles.value.length === 0) return
  operationTargetFiles.value = selectedFiles.value
  copyDialogVisible.value = true
}

const handleBatchDelete = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedFiles.value.length} 个文件/文件夹吗？`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 分离文件和文件夹
    const files = selectedFiles.value.filter(f => f.fileType !== 'folder')
    const folders = selectedFiles.value.filter(f => f.fileType === 'folder')

    // 删除文件
    if (files.length > 0) {
      const fileIds = files.map(f => f.id)
      const { batchDeleteFiles } = await import('@/api/file')
      await batchDeleteFiles(fileIds)
    }

    // 删除文件夹
    if (folders.length > 0) {
      const { deleteFolder } = await import('@/api/file')
      for (const folder of folders) {
        await deleteFolder(folder.id)
      }
    }

    ElMessage.success('批量删除成功')

    // 更新已用空间（只对文件更新）
    files.forEach(f => {
      userStore.updateUsedSpace(-f.fileSize)
    })

    await loadFileList()
    selectedFiles.value = [] // 清空选中
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 批量恢复（回收站）
const handleBatchRestore = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要恢复选中的 ${selectedFiles.value.length} 个文件/文件夹吗？`,
      '批量恢复',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    // 分离文件和文件夹
    const files = selectedFiles.value.filter(f => f.fileType !== 'folder')
    const folders = selectedFiles.value.filter(f => f.fileType === 'folder')

    // 恢复文件
    if (files.length > 0) {
      const { restoreFile } = await import('@/api/file')
      for (const file of files) {
        await restoreFile(file.id)
      }
    }

    // 恢复文件夹
    if (folders.length > 0) {
      const { restoreFolder } = await import('@/api/file')
      for (const folder of folders) {
        await restoreFolder(folder.id)
      }
    }

    ElMessage.success('批量恢复成功')
    await loadFileList()
    selectedFiles.value = [] // 清空选中
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量恢复失败:', error)
      ElMessage.error('批量恢复失败，请稍后重试')
    }
  }
}

// 批量彻底删除（回收站）
const handleBatchPermanentDelete = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${selectedFiles.value.length} 个文件/文件夹吗？此操作不可恢复！`,
      '批量彻底删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    // 分离文件和文件夹
    const files = selectedFiles.value.filter(f => f.fileType !== 'folder')
    const folders = selectedFiles.value.filter(f => f.fileType === 'folder')

    // 彻底删除文件
    if (files.length > 0) {
      const { permanentDeleteFile } = await import('@/api/file')
      for (const file of files) {
        await permanentDeleteFile(file.id)
        userStore.updateUsedSpace(-file.fileSize)
      }
    }

    // 彻底删除文件夹
    if (folders.length > 0) {
      const { permanentDeleteFolder } = await import('@/api/file')
      for (const folder of folders) {
        await permanentDeleteFolder(folder.id)
      }
    }

    ElMessage.success('批量彻底删除成功')
    await loadFileList()
    selectedFiles.value = [] // 清空选中
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量彻底删除失败:', error)
      ElMessage.error('批量彻底删除失败，请稍后重试')
    }
  }
}

// 执行移动
const confirmMove = async (targetFolderId: number) => {
  if (operationTargetFiles.value.length === 0) return
  
  try {
    const { moveFile, batchMoveFiles } = await import('@/api/file')
    
    // 如果是单个文件且操作列表只有1个
    if (operationTargetFiles.value.length === 1) {
      await moveFile(operationTargetFiles.value[0]!.id, targetFolderId)
    } else {
      // 批量移动
      const fileIds = operationTargetFiles.value.map(f => f.id)
      await batchMoveFiles(fileIds, targetFolderId)
    }
    
    ElMessage.success('移动成功')
    await loadFileList()
  } catch (error) {
    console.error('移动失败:', error)
  }
}

// 执行复制
const confirmCopy = async (targetFolderId: number) => {
  if (operationTargetFiles.value.length === 0) return
  
  try {
    const { copyFile, batchCopyFiles } = await import('@/api/file')
    
    // 如果是单个文件且操作列表只有1个
    if (operationTargetFiles.value.length === 1) {
      await copyFile(operationTargetFiles.value[0]!.id, targetFolderId)
    } else {
      // 批量复制
      const fileIds = operationTargetFiles.value.map(f => f.id)
      await batchCopyFiles(fileIds, targetFolderId)
    }
    
    ElMessage.success('复制成功')
    await loadFileList()
  } catch (error) {
    console.error('复制失败:', error)
  }
}
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

.upload-list {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px; /* 防止滚动条遮挡内容 */
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  z-index: 2000;
  background: white;
  min-width: 140px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e5e7eb;
  padding: 4px 0;
  font-size: 14px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  color: #374151;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f3f4f6;
  color: #7C3AED;
}

.menu-item.danger {
  color: #EF4444;
}

.menu-item.danger:hover {
  background-color: #FEF2F2;
}

.menu-divider {
  margin: 4px 0;
  border-color: #e5e7eb;
}
</style>
