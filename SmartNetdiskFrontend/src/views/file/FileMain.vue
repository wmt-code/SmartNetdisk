<template>
  <div class="file-main" :class="{ 'is-mobile': isMobile }">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 面包屑导航 - 非回收站 -->
        <el-breadcrumb v-if="!isRecycleBin" separator="/" class="breadcrumb">
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
        <div v-if="isRecycleBin" class="recycle-title">
          <el-icon :size="20"><Delete /></el-icon>
          <span>回收站</span>
        </div>
      </div>

      <div class="toolbar-right">
        <!-- 清空回收站按钮 -->
        <el-button v-if="isRecycleBin" type="danger" plain size="small" @click="handleEmptyRecycleBin">
          <el-icon><Delete /></el-icon>
          <span class="btn-text">清空</span>
        </el-button>

        <!-- 新建文件夹 - 移动端只显示图标 -->
        <el-button v-if="!isRecycleBin && !isMobile" @click="showNewFolderDialog = true">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>
        <el-tooltip v-if="!isRecycleBin && isMobile" content="新建文件夹" placement="bottom">
          <el-button @click="showNewFolderDialog = true" circle>
            <el-icon><FolderAdd /></el-icon>
          </el-button>
        </el-tooltip>

        <!-- 上传按钮 -->
        <el-dropdown v-if="!isRecycleBin" split-button type="primary" size="default" @click="triggerUpload">
          <el-icon><Upload /></el-icon>
          <span class="btn-text">上传文件</span>
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

        <!-- 桌面端显示的批量操作按钮 -->
        <template v-if="!isMobile">
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
            <el-button @click="handleBatchMove">移动</el-button>
            <el-button @click="handleBatchCopy">复制</el-button>
            <el-button type="danger" @click="handleBatchDelete">删除</el-button>
          </el-button-group>

          <!-- 批量操作按钮组 - 回收站 -->
          <el-button-group v-if="selectedFiles.length > 0 && isRecycleBin" class="ml-2">
            <el-button type="primary" @click="handleBatchRestore">
              恢复 ({{ selectedFiles.length }})
            </el-button>
            <el-button type="danger" @click="handleBatchPermanentDelete">彻底删除</el-button>
          </el-button-group>

          <el-divider direction="vertical" />
        </template>

        <!-- 视图切换 -->
        <el-radio-group v-model="viewMode" size="small" class="view-toggle">
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
    <div class="file-content">
      <!-- 加载骨架屏 -->
      <FileSkeleton v-if="loading" :count="12" :type="viewMode" />

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
        :table-layout="isMobile ? 'auto' : 'fixed'"
      >
        <el-table-column type="selection" :width="isMobile ? 32 : 40" />
        <el-table-column prop="fileName" label="文件名" :min-width="isMobile ? 150 : 300">
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="isMobile ? 20 : 24" :color="getFileIconColor(row.fileType)">
                <component :is="getFileIcon(row.fileType)" />
              </el-icon>
              <span class="file-name-text">{{ row.fileName }}</span>
              <el-tag v-if="row.isVectorized" size="small" type="success">已向量化</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" prop="fileSizeStr" label="大小" width="100">
          <template #default="{ row }">
            {{ row.fileSizeStr }}
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" prop="updateTime" label="修改时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 100 : (isRecycleBin ? 200 : 160)" fixed="right">
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
      <div v-else-if="viewMode === 'grid' && fileList.length > 0" class="grid-view">
        <div
          v-for="(file, index) in fileList"
          :key="file.id"
          class="file-card-wrapper stagger-item"
          :style="{ animationDelay: `${index * 30}ms` }"
        >
          <div
            class="file-card"
            :class="{
              'is-selected': isFileSelected(file),
              'is-folder': file.fileType === 'folder'
            }"
            @click="toggleFileSelection(file, $event)"
            @dblclick="handleRowDblClick(file)"
            @contextmenu.prevent="handleContextMenu($event, file)"
          >
            <!-- Selection checkbox -->
            <div class="file-card-checkbox" @click.stop>
              <el-checkbox
                :model-value="isFileSelected(file)"
                @change="toggleFileSelection(file)"
              />
            </div>

            <!-- File icon with type color -->
            <div class="file-card-icon" :class="getFileTypeClass(file.fileType)">
              <el-icon :size="48">
                <component :is="getFileIcon(file.fileType)" />
              </el-icon>
            </div>

            <!-- File info -->
            <div class="file-card-info">
              <span class="file-card-name" :title="file.fileName">{{ file.fileName }}</span>
              <span class="file-card-meta">{{ file.fileSizeStr }}</span>
            </div>

            <!-- Vectorized badge -->
            <el-tag v-if="file.isVectorized" class="file-card-badge" size="small" type="success">
              AI
            </el-tag>

            <!-- Hover actions -->
            <div class="file-card-actions">
              <el-button
                v-if="!isRecycleBin && file.fileType !== 'folder'"
                size="small"
                circle
                @click.stop="handleDownload(file)"
              >
                <el-icon><Download /></el-icon>
              </el-button>
              <el-button
                v-if="!isRecycleBin"
                size="small"
                circle
                @click.stop="handleShare(file)"
              >
                <el-icon><Share /></el-icon>
              </el-button>
              <el-button
                size="small"
                circle
                type="danger"
                @click.stop="isRecycleBin ? handlePermanentDelete(file) : handleDelete(file)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!loading && fileList.length === 0"
        :type="isRecycleBin ? 'recycle' : (searchKeyword ? 'search' : 'empty')"
        :show-action="!isRecycleBin && !searchKeyword"
        @action="triggerUpload"
      />
    </div>

    <!-- Mobile action bar for batch operations -->
    <MobileActionBar
      v-if="isMobile"
      :selected-count="selectedFiles.length"
      :is-recycle="isRecycleBin"
      @clear="clearSelection"
      @download="handleBatchDownload"
      @delete="handleBatchDelete"
      @move="handleBatchMove"
      @copy="handleBatchCopy"
      @share="handleBatchShare"
      @restore="handleBatchRestore"
      @permanent-delete="handleBatchPermanentDelete"
    />

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
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" :width="isMobile ? '90%' : '400px'">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreateFolder" />
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名" :width="isMobile ? '90%' : '400px'">
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
      :width="isMobile ? '95%' : '700px'"
      :fullscreen="isMobile"
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

    <!-- 批量操作进度对话框 -->
    <BatchProgressDialog
      v-model="progressVisible"
      title="正在处理"
      :percentage="progressPercentage"
      :status-text="progressStatus"
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
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, FolderAdd, Upload, Document, Folder, List, Grid,
  Download, Share, Delete, Picture, VideoPlay, Headset, FolderOpened,
  Loading, Edit, RefreshLeft, Scissor, DocumentCopy, MagicStick, View, EditPen,
  Sort, Check, Files, UploadFilled, CollectionTag
} from '@element-plus/icons-vue'
import ShareBatchDialog from '@/components/ShareBatchDialog.vue'
import FolderSelectDialog from '@/components/FolderSelectDialog.vue'
import UppyUploader from '@/components/UppyUploader.vue'
import FilePreviewDialog from '@/components/FilePreviewDialog.vue'
import FileEditorDialog from '@/components/FileEditorDialog.vue'
import { EmptyState, FileSkeleton } from '@/components/ui'
import MobileActionBar from '@/components/file/MobileActionBar.vue'
import { useIsMobile } from '@/composables'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFileList, deleteFile, renameFile, downloadFileStream,
  createFolder, restoreFile, permanentDeleteFile, getRecycleList, deleteFolder,
  type FileInfo
} from '@/api/file'
import { vectorizeFile } from '@/api/ai'


import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// Mobile detection
const isMobile = useIsMobile()

// 视图模式
const viewMode = ref<'list' | 'grid'>('list')

// 加载状态
const loading = ref(false)
const creating = ref(false)
const renaming = ref(false)

// 拖拽上传状态
const isDragging = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0) // 总条数 (注意：后端可能没返回准确的 total，需要确认 PageResult 结构)

// 当前文件夹 ID
const currentFolderId = ref(0)

// 排序和搜索
const sortField = ref('time')
const sortOrder = ref<'asc' | 'desc'>('desc')
const searchKeyword = ref('')

// 判断是否是回收站 (must be defined before loadFileList)
const isRecycleBin = computed(() => route.meta?.isRecycle === true)

// 排序处理
const handleSort = (command: string) => {
  if (command === 'toggleOrder') {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = command
  }
  loadFileList()
}

// 搜索清除
const clearSearch = () => {
  router.push({ query: {} })
}

// 高亮搜索关键词
const highlightKeyword = (text: string) => {
  if (!searchKeyword.value) return text
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return text.replace(regex, '<span class="text-red-500 font-bold">$1</span>')
}

// 加载文件列表
const loadFileList = async () => {
  loading.value = true
  try {
    const params: any = {
      folderId: currentFolderId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      orderBy: sortField.value === 'name' ? 'file_name' : (sortField.value === 'size' ? 'file_size' : 'create_time'),
      isAsc: sortOrder.value === 'asc',
      keyword: searchKeyword.value
    }
    
    // 如果有搜索关键词，则不传 folderId (全局搜索)
    if (searchKeyword.value) {
        delete params.folderId
    }

    let res
    if (isRecycleBin.value) {
      res = await getRecycleList(params)
    } else {
      res = await getFileList(params)
    }

    fileList.value = res.records
    total.value = res.total
    
    if (!searchKeyword.value && !isRecycleBin.value && route.params.path) {
         // TODO: 真正的面包屑加载逻辑
         // 这里简单因为没有 updateBreadcrumb 函数体，保持原有逻辑或忽略
    }
  } catch (error) {
    console.error('加载文件列表失败:', error)
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// 监听路由变化（搜索关键词）
watch(() => route.query.keyword, (newVal) => {
  searchKeyword.value = (newVal as string) || ''
  loadFileList()
}, { immediate: true })

// 新建文件夹
const showNewFolderDialog = ref(false)
const newFolderName = ref('')

// 重命名
const renameDialogVisible = ref(false)
const renameNewName = ref('')
const renameTargetFile = ref<FileInfo | null>(null)

// 上传相关
const uploadDialogVisible = ref(false)
const uppyRef = ref()

// 选中的文件
const selectedFiles = ref<FileInfo[]>([])

// 文件列表
const fileList = ref<FileInfo[]>([])

// 文件夹路径（用于面包屑导航）
const folderPath = ref<{ id: number; name: string }[]>([])

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

// 文件类型 CSS 类
const getFileTypeClass = (type: string) => {
  const classMap: Record<string, string> = {
    folder: 'type-folder',
    document: 'type-document',
    image: 'type-image',
    video: 'type-video',
    audio: 'type-audio',
    archive: 'type-archive',
    code: 'type-code'
  }
  return classMap[type] || 'type-document'
}

// 检查文件是否被选中
const isFileSelected = (file: FileInfo) => {
  return selectedFiles.value.some(f => f.id === file.id)
}

// 切换文件选中状态
const toggleFileSelection = (file: FileInfo, event?: MouseEvent) => {
  const index = selectedFiles.value.findIndex(f => f.id === file.id)
  if (index >= 0) {
    selectedFiles.value.splice(index, 1)
  } else {
    if (event?.shiftKey && selectedFiles.value.length > 0) {
      // Shift 选择范围
      const lastSelected = selectedFiles.value[selectedFiles.value.length - 1]
      const lastIndex = fileList.value.findIndex(f => f.id === lastSelected?.id)
      const currentIndex = fileList.value.findIndex(f => f.id === file.id)
      const start = Math.min(lastIndex, currentIndex)
      const end = Math.max(lastIndex, currentIndex)
      for (let i = start; i <= end; i++) {
        const f = fileList.value[i]
        if (f && !selectedFiles.value.some(s => s.id === f.id)) {
          selectedFiles.value.push(f)
        }
      }
    } else {
      selectedFiles.value.push(file)
    }
  }
}

// 清空选中
const clearSelection = () => {
  selectedFiles.value = []
}

// 批量下载（移动端）
const handleBatchDownload = () => {
  if (selectedFiles.value.length === 0) return
  selectedFiles.value.forEach(file => {
    if (file.fileType !== 'folder') {
      handleDownload(file)
    }
  })
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}



// 拖拽处理
const handleDragOver = (e: DragEvent) => {
  isDragging.value = true
}

const handleDragLeave = (e: DragEvent) => {
  // 防止在子元素上触发 leave 导致闪烁，检查 relatedTarget
  const relatedTarget = e.relatedTarget as HTMLElement
  if (!relatedTarget || !e.currentTarget || !(e.currentTarget as HTMLElement).contains(relatedTarget)) {
    isDragging.value = false
  }
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    if (!uppyRef.value) {
      // 如果 uppy ref 不存在，先打开对话框
      uploadDialogVisible.value = true
      // 等待 DOM 更新后添加文件
      setTimeout(() => {
        uppyRef.value?.addFiles(files)
      }, 100)
    } else {
      uploadDialogVisible.value = true
      uppyRef.value.addFiles(files)
    }
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

// 批量进度相关
import BatchProgressDialog from '@/components/BatchProgressDialog.vue'
const progressVisible = ref(false)
const progressPercentage = ref(0)
const progressStatus = ref('')

// 清空回收站
const handleEmptyRecycleBin = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空回收站吗？所有文件将无法恢复！',
      '清空回收站',
      {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const { clearRecycleBin } = await import('@/api/file')
    await clearRecycleBin()
    ElMessage.success('回收站已清空')
    await loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空回收站失败:', error)
      ElMessage.error('清空回收站失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 带进度的批量操作处理函数
const processBatchOperation = async (
  items: FileInfo[], 
  operationName: string, 
  processItem: (item: FileInfo) => Promise<void>
) => {
  if (items.length === 0) return

  progressVisible.value = true
  progressPercentage.value = 0
  progressStatus.value = `正在${operationName} 0/${items.length}`
  
  let successCount = 0
  let failCount = 0

  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (!item) continue; // TS safety check

    try {
      await processItem(item)
      successCount++
    } catch (error) {
      console.error(`${operationName}失败:`, item.fileName, error)
      failCount++
    }
    
    progressPercentage.value = Math.round(((i + 1) / items.length) * 100)
    progressStatus.value = `正在${operationName} ${i + 1}/${items.length}`
  }

  // 稍微延迟关闭以展示 100% 状态
  setTimeout(() => {
    progressVisible.value = false
    if (failCount > 0) {
      ElMessage.warning(`${operationName}完成: ${successCount} 个成功, ${failCount} 个失败`)
    } else {
      ElMessage.success(`${operationName}成功`)
    }
    loadFileList()
    selectedFiles.value = [] // 清空选中
  }, 500)
}

// 批量恢复（回收站） - 使用进度条
const handleBatchRestore = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要恢复选中的 ${selectedFiles.value.length} 个文件/文件夹吗？`,
      '批量恢复',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )

    const { restoreFile, restoreFolder } = await import('@/api/file')

    await processBatchOperation(selectedFiles.value, '恢复', async (item) => {
      if (item.fileType === 'folder') {
        await restoreFolder(item.id)
      } else {
        await restoreFile(item.id)
      }
    })
  } catch (error) {
    if (error !== 'cancel') console.error('取消批量恢复')
  }
}

// 批量彻底删除（回收站） - 使用进度条
const handleBatchPermanentDelete = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${selectedFiles.value.length} 个文件/文件夹吗？此操作不可恢复！`,
      '批量彻底删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )

    const { permanentDeleteFile, permanentDeleteFolder } = await import('@/api/file')

    await processBatchOperation(selectedFiles.value, '删除', async (item) => {
      if (item.fileType === 'folder') {
        await permanentDeleteFolder(item.id)
      } else {
        await permanentDeleteFile(item.id)
        userStore.updateUsedSpace(-item.fileSize)
      }
    })
  } catch (error) {
    if (error !== 'cancel') console.error('取消批量彻底删除')
  }
}
</script>

<style scoped lang="scss">
.file-main {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

// Toolbar
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border-light);
  flex-wrap: wrap;
  gap: var(--space-sm);
  min-height: 56px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-shrink: 0;
  min-width: 0;

  .breadcrumb {
    flex-shrink: 1;
    min-width: 0;
    overflow: hidden;
  }

  .recycle-title {
    display: flex;
    align-items: center;
    gap: var(--space-xs);
    font-weight: 500;
    font-size: 1.125rem;
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-shrink: 0;

  .btn-text {
    margin-left: 4px;
  }

  .view-toggle {
    flex-shrink: 0;
  }
}

// File content area
.file-content {
  flex: 1;
  overflow: auto;
  padding: var(--space-md);
}

// Grid view
.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: var(--space-md);

  @media (min-width: 640px) {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

// File card
.file-card-wrapper {
  opacity: 0;
  animation: fadeInUp var(--transition-base) var(--ease-out) forwards;
}

.file-card {
  position: relative;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  border: 2px solid transparent;
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all var(--transition-base) var(--ease-out);
  overflow: hidden;

  &:hover {
    background: var(--card-bg-hover);
    transform: translateY(-4px);
    box-shadow: var(--shadow-xl);

    .file-card-checkbox {
      opacity: 1;
    }

    .file-card-actions {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &.is-selected {
    border-color: var(--color-primary);
    background: var(--sidebar-item-active);

    .file-card-checkbox {
      opacity: 1;
    }
  }

  &.is-folder {
    .file-card-icon {
      color: var(--color-folder);
    }
  }
}

.file-card-checkbox {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  opacity: 0;
  transition: opacity var(--transition-fast);
  z-index: 2;
}

.file-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: var(--space-sm);
  transition: transform var(--transition-base) var(--ease-spring);

  &.type-folder { color: var(--color-folder); }
  &.type-document { color: var(--color-document); }
  &.type-image { color: var(--color-image); }
  &.type-video { color: var(--color-video); }
  &.type-audio { color: var(--color-audio); }
  &.type-archive { color: var(--color-archive); }
  &.type-code { color: var(--color-code); }

  .file-card:hover & {
    transform: scale(1.1);
  }
}

.file-card-info {
  width: 100%;
  text-align: center;
}

.file-card-name {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.file-card-meta {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.file-card-badge {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
}

.file-card-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-sm);
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  display: flex;
  justify-content: center;
  gap: var(--space-xs);
  opacity: 0;
  transform: translateY(8px);
  transition: all var(--transition-base) var(--ease-out);

  .el-button {
    --el-button-size: 28px;
    background: rgba(255, 255, 255, 0.9);
    border: none;

    &:hover {
      background: white;
      transform: scale(1.1);
    }
  }
}

// Stagger animation
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stagger-item {
  opacity: 0;
  animation: fadeInUp var(--transition-base) var(--ease-out) forwards;
}

// File table
.file-table {
  :deep(.el-table__row) {
    transition: background-color var(--transition-fast);
  }

  :deep(.el-table__row:hover) {
    background-color: var(--sidebar-item-hover) !important;
  }

  :deep(.el-table__header-wrapper) {
    background: var(--color-surface-secondary);
  }

  :deep(.el-table__body-wrapper) {
    background: transparent;
  }
}

// Pagination
.pagination-container {
  padding: var(--space-md);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: center;
}

// Context menu
.context-menu {
  position: fixed;
  z-index: 2000;
  background: var(--color-surface);
  min-width: 160px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
  padding: var(--space-xs) 0;
  font-size: 14px;
  animation: scaleIn var(--transition-fast) var(--ease-spring);
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  cursor: pointer;
  color: var(--color-text);
  transition: all var(--transition-fast);

  &:hover {
    background-color: var(--sidebar-item-hover);
    color: var(--color-primary);
  }

  &.danger {
    color: var(--color-error);

    &:hover {
      background-color: rgba(239, 68, 68, 0.1);
    }
  }
}

.menu-divider {
  margin: var(--space-xs) 0;
  border-color: var(--color-border-light);
}

// Mobile styles
.file-main.is-mobile {
  .toolbar {
    padding: var(--space-sm);
    gap: var(--space-xs);
  }

  .toolbar-left {
    gap: var(--space-sm);
    max-width: 40%;

    .breadcrumb {
      :deep(.el-breadcrumb__item) {
        .el-breadcrumb__inner {
          max-width: 60px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          display: inline-block;
        }
      }
    }

    .recycle-title {
      font-size: 1rem;
      span {
        display: none;
      }
    }
  }

  .toolbar-right {
    gap: var(--space-xs);

    .btn-text {
      display: none;
    }

    // Hide text in split button
    :deep(.el-dropdown__caret-button) {
      padding-left: 8px;
      padding-right: 8px;
    }
  }

  .file-content {
    padding: var(--space-sm);
    padding-bottom: calc(var(--space-sm) + 100px); // Space for mobile action bar
  }

  .grid-view {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-sm);
    width: 100%;
  }

  .file-card-wrapper {
    width: 100%;
    min-width: 0;
  }

  .file-card {
    padding: var(--space-sm);
    width: 100%;
    min-width: 0;
    min-height: 120px;

    .file-card-checkbox {
      opacity: 1;
      top: 4px;
      left: 4px;
    }
  }

  .file-card-icon {
    width: 44px;
    height: 44px;

    .el-icon {
      font-size: 32px !important;
    }
  }

  .file-card-name {
    font-size: 0.75rem;
    max-width: 100%;
    word-break: break-all;
  }

  .file-card-meta {
    font-size: 0.625rem;
  }

  .file-card-badge {
    top: 4px;
    right: 4px;
    font-size: 0.625rem;
    padding: 0 4px;
  }

  .file-card-actions {
    padding: 4px;
    gap: 2px;

    .el-button {
      --el-button-size: 24px;
    }
  }

  // Table mobile styles
  .file-table {
    :deep(.el-table__header) {
      th {
        padding: 8px 4px;
        font-size: 0.75rem;
      }
    }

    :deep(.el-table__body) {
      td {
        padding: 8px 4px;
      }
    }
  }

  .file-name-cell {
    .file-name-text {
      font-size: 0.8125rem;
      max-width: 120px;
    }
  }
}

// File name cell styles
.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  min-width: 0;

  .file-name-text {
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }
}

// Utility classes
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
.ml-2 { margin-left: 0.5rem; }
.overflow-auto { overflow: auto; }
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.text-center { text-align: center; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-lg { font-size: 1.125rem; }
.font-medium { font-weight: 500; }
.cursor-pointer { cursor: pointer; }
.border-b { border-bottom-width: 1px; }
.border-gray-100 { border-color: #F3F4F6; }
</style>
