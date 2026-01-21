<template>
  <div class="file-main h-full w-full flex flex-col">
    <!-- 工具栏 -->
    <div class="toolbar flex items-center justify-between p-4 border-b border-gray-100">
      <div class="left flex items-center gap-4">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/files' }">
            <el-icon><HomeFilled /></el-icon>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="(segment, index) in pathSegments" :key="index">
            {{ segment }}
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
      <!-- 列表视图 -->
      <el-table
        v-if="viewMode === 'list'"
        :data="fileList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @row-dblclick="handleRowDblClick"
        class="file-table"
        row-class-name="cursor-pointer"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="name" label="文件名" min-width="300">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon :size="24" :color="getFileIconColor(row.type)">
                <component :is="getFileIcon(row.type)" />
              </el-icon>
              <span class="font-medium">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120">
          <template #default="{ row }">
            {{ row.type === 'folder' ? '-' : formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="modifiedTime" label="修改时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleDownload(row)">
              <el-icon><Download /></el-icon>
            </el-button>
            <el-button link type="primary" @click.stop="handleShare(row)">
              <el-icon><Share /></el-icon>
            </el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格视图 -->
      <div v-else class="grid-view grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-4">
        <div
          v-for="file in fileList"
          :key="file.id"
          class="file-card glass-card p-4 flex flex-col items-center cursor-pointer"
          @dblclick="handleRowDblClick(file)"
        >
          <el-icon :size="48" :color="getFileIconColor(file.type)">
            <component :is="getFileIcon(file.type)" />
          </el-icon>
          <span class="mt-2 text-sm text-center truncate w-full">{{ file.name }}</span>
          <span class="text-xs text-gray-400 mt-1">{{ file.type === 'folder' ? '' : formatSize(file.size) }}</span>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="fileList.length === 0" description="暂无文件" class="mt-20" />
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" width="400px">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" />
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" @click="createFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 隐藏的上传 input -->
    <input ref="fileInput" type="file" multiple hidden @change="handleFileChange" />
    <input ref="folderInput" type="file" webkitdirectory hidden @change="handleFolderChange" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, FolderAdd, Upload, Document, Folder, List, Grid,
  Download, Share, Delete, Picture, VideoPlay, Headset, FolderOpened
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 视图模式
const viewMode = ref<'list' | 'grid'>('list')

// 新建文件夹
const showNewFolderDialog = ref(false)
const newFolderName = ref('')

// 上传相关
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)

// 选中的文件
const selectedFiles = ref<any[]>([])

// 模拟文件列表数据
const fileList = ref([
  { id: 1, name: '项目文档', type: 'folder', size: 0, modifiedTime: '2026-01-20 10:30' },
  { id: 2, name: '产品设计.pdf', type: 'pdf', size: 2048000, modifiedTime: '2026-01-19 14:22' },
  { id: 3, name: '会议记录.docx', type: 'word', size: 512000, modifiedTime: '2026-01-18 09:15' },
  { id: 4, name: '数据分析.xlsx', type: 'excel', size: 1024000, modifiedTime: '2026-01-17 16:45' },
  { id: 5, name: '演示视频.mp4', type: 'video', size: 52428800, modifiedTime: '2026-01-16 11:00' },
  { id: 6, name: '背景音乐.mp3', type: 'audio', size: 5242880, modifiedTime: '2026-01-15 08:30' },
  { id: 7, name: '封面图片.png', type: 'image', size: 1048576, modifiedTime: '2026-01-14 13:20' },
])

// 面包屑路径
const pathSegments = computed(() => {
  const path = route.params.path
  if (!path) return []
  if (Array.isArray(path)) return path
  return path.split('/')
})

// 文件图标
const getFileIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    folder: FolderOpened,
    pdf: Document,
    word: Document,
    excel: Document,
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
    pdf: '#EF4444',
    word: '#3B82F6',
    excel: '#22C55E',
    image: '#8B5CF6',
    video: '#EC4899',
    audio: '#06B6D4',
  }
  return colorMap[type] || '#6B7280'
}

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 事件处理
const handleSelectionChange = (selection: any[]) => {
  selectedFiles.value = selection
}

const handleRowDblClick = (row: any) => {
  if (row.type === 'folder') {
    const currentPath = route.params.path || ''
    const newPath = currentPath ? `${currentPath}/${row.name}` : row.name
    router.push(`/files/${newPath}`)
  }
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const triggerFolderUpload = () => {
  folderInput.value?.click()
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files) {
    console.log('上传文件:', files)
    // TODO: 调用上传 API
  }
}

const handleFolderChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files) {
    console.log('上传文件夹:', files)
    // TODO: 调用上传 API
  }
}

const createFolder = () => {
  if (newFolderName.value.trim()) {
    console.log('创建文件夹:', newFolderName.value)
    // TODO: 调用创建文件夹 API
    showNewFolderDialog.value = false
    newFolderName.value = ''
  }
}

const handleDownload = (row: any) => {
  console.log('下载:', row.name)
}

const handleShare = (row: any) => {
  console.log('分享:', row.name)
}

const handleDelete = (row: any) => {
  console.log('删除:', row.name)
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
.gap-2 { gap: 0.5rem; }
.gap-4 { gap: 1rem; }
.p-4 { padding: 1rem; }
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
</style>
