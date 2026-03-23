<template>
  <div class="file-main" :class="{ 'is-mobile': isMobile }">
    <!-- Toolbar -->
    <FileToolbar
      :folder-path="folderPath"
      :is-recycle="isRecycleBin"
      :is-mobile="isMobile"
      :selected-count="selectedFiles.length"
      v-model:view-mode="viewMode"
      @navigate-root="navigateToRoot"
      @navigate-folder="navigateToFolder"
      @empty-recycle="handleEmptyRecycleBin"
      @new-folder="showNewFolderDialog = true"
      @upload="uploadDialogVisible = true"
      @upload-folder="uploadDialogVisible = true"
      @batch-download="handleBatchDownloadZip"
      @batch-share="handleBatchShare(selectedFiles)"
      @batch-move="handleBatchMove(selectedFiles)"
      @batch-copy="handleBatchCopy(selectedFiles)"
      @batch-delete="handleBatchDelete(selectedFiles)"
      @batch-restore="handleBatchRestore(selectedFiles)"
      @batch-permanent-delete="handleBatchPermanentDelete(selectedFiles)"
    />

    <!-- File content area -->
    <div class="file-content">
      <!-- Loading skeleton -->
      <FileSkeleton v-if="loading" :count="12" :type="viewMode" />

      <!-- List view -->
      <el-table
        v-else-if="viewMode === 'list' && fileList.length > 0"
        :data="fileList"
        style="width: 100%"
        class="file-table"
        row-class-name="file-table-row"
        :table-layout="isMobile ? 'auto' : 'fixed'"
        @selection-change="handleSelectionChange"
        @row-dblclick="handleRowDblClick"
        @row-contextmenu="(row: any, _col: any, e: MouseEvent) => contextMenu.show(e, row)"
      >
        <el-table-column type="selection" :width="isMobile ? 32 : 40" />
        <el-table-column prop="fileName" label="文件名" :min-width="isMobile ? 150 : 300">
          <template #default="{ row }">
            <el-tooltip
              :content="truncateSummary(row.aiSummary)"
              :disabled="!row.aiSummary"
              placement="bottom-start"
              :show-after="400"
              :max-width="320"
              effect="dark"
            >
              <div class="file-name-cell">
                <div class="file-icon-wrapper" :class="getFileTypeClass(row.fileType)">
                  <el-icon :size="isMobile ? 18 : 20">
                    <component :is="getFileIcon(row.fileType)" />
                  </el-icon>
                </div>
                <span class="file-name-text">{{ row.fileName }}</span>
                <el-tag v-if="isFileProcessing(row.id)" size="small" type="warning" effect="plain" class="ai-tag">
                  <el-icon class="is-loading" :size="12"><Loading /></el-icon> AI 处理中
                </el-tag>
                <template v-else>
                  <el-tag v-if="row.isVectorized" size="small" type="success" effect="plain" class="ai-tag">已向量化</el-tag>
                  <el-tag v-if="row.aiSummary" size="small" type="primary" effect="plain" class="ai-tag">AI 摘要</el-tag>
                </template>
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" prop="fileSizeStr" label="大小" width="100">
          <template #default="{ row }">
            <span class="meta-text">{{ row.fileSizeStr }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" prop="updateTime" label="修改时间" width="160">
          <template #default="{ row }">
            <span class="meta-text">{{ formatTime(row.updateTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 100 : (isRecycleBin ? 180 : 150)" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <template v-if="isRecycleBin">
                <el-button link type="primary" size="small" @click.stop="handleRestore(row)">恢复</el-button>
                <el-button link type="danger" size="small" @click.stop="handlePermanentDelete(row)">删除</el-button>
              </template>
              <template v-else>
                <el-button v-if="row.fileType !== 'folder'" link type="primary" size="small" @click.stop="handleDownload(row)">
                  <el-icon><Download /></el-icon>
                </el-button>
                <el-button link type="primary" size="small" @click.stop="handleShare(row)">
                  <el-icon><Share /></el-icon>
                </el-button>
                <el-button link size="small" @click.stop="showRenameDialog(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button link type="danger" size="small" @click.stop="handleDelete(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Grid view -->
      <FileGridView
        v-else-if="viewMode === 'grid' && fileList.length > 0"
        :files="fileList"
        :is-selected="isFileSelected"
        :is-recycle="isRecycleBin"
        :is-mobile="isMobile"
        @select="toggleFileSelection"
        @dblclick="handleRowDblClick"
        @contextmenu="contextMenu.show"
        @download="handleDownload"
        @share="handleShare"
        @delete="(f) => isRecycleBin ? handlePermanentDelete(f) : handleDelete(f)"
      />

      <!-- Empty state -->
      <EmptyState
        v-else-if="!loading && fileList.length === 0"
        :type="isRecycleBin ? 'recycle' : (searchKeyword ? 'search' : 'empty')"
        :show-action="!isRecycleBin && !searchKeyword"
        @action="uploadDialogVisible = true"
      />
    </div>

    <!-- Mobile action bar -->
    <MobileActionBar
      v-if="isMobile"
      :selected-count="selectedFiles.length"
      :is-recycle="isRecycleBin"
      @clear="clearSelection"
      @download="selectedFiles.forEach(f => f.fileType !== 'folder' && handleDownload(f))"
      @delete="handleBatchDelete(selectedFiles)"
      @move="handleBatchMove(selectedFiles)"
      @copy="handleBatchCopy(selectedFiles)"
      @share="handleBatchShare(selectedFiles)"
      @restore="handleBatchRestore(selectedFiles)"
      @permanent-delete="handleBatchPermanentDelete(selectedFiles)"
    />

    <!-- Pagination -->
    <div v-if="total > pageSize" class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadFileList"
      />
    </div>

    <!-- Context menu -->
    <FileContextMenu
      :visible="contextMenu.visible.value"
      :position="contextMenu.position.value"
      :target="contextMenu.target.value"
      @action="handleContextAction"
    />

    <!-- Dialogs -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" :width="isMobile ? '90%' : '400px'" class="app-dialog">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreateFolder(currentFolderId)" />
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateFolder(currentFolderId)">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameDialogVisible" title="重命名" :width="isMobile ? '90%' : '400px'" class="app-dialog">
      <el-input v-model="renameNewName" placeholder="请输入新名称" @keyup.enter="handleRename" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      :width="isMobile ? '95%' : '700px'"
      :fullscreen="isMobile"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="uploadDialogVisible = false"
    >
      <UppyUploader
        :folder-id="currentFolderId"
        :visible="uploadDialogVisible"
        @close="uploadDialogVisible = false"
        @success="handleUploadSuccess"
      />
    </el-dialog>

    <ShareBatchDialog
      v-model="shareDialogVisible"
      :selected-items="shareSelectedItems"
      @success="handleShareSuccess"
    />

    <FolderSelectDialog
      v-model:visible="moveDialogVisible"
      title="移动到"
      :current-folder-id="currentFolderId"
      :exclude-ids="operationTargetFiles.map(f => f.id)"
      @confirm="confirmMove"
    />

    <FolderSelectDialog
      v-model:visible="copyDialogVisible"
      title="复制到"
      :current-folder-id="currentFolderId"
      @confirm="confirmCopy"
    />

    <FilePreviewDialog
      v-model="previewDialogVisible"
      :file="previewTargetFile"
      @edit="handleOpenEditor"
    />

    <FileEditorDialog
      v-model="editorDialogVisible"
      :file="editorTargetFile"
      @saved="handleEditorSaved"
    />

    <BatchProgressDialog
      v-model="progressVisible"
      title="正在处理"
      :percentage="progressPercentage"
      :status-text="progressStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Download, Share, Edit, Delete, MagicStick, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { batchDownloadZip } from '@/api/file'
import { useIsMobile } from '@/composables'
import { useFileList } from '@/composables/useFileList'
import { useFileSelection } from '@/composables/useFileSelection'
import { useFileOperations } from '@/composables/useFileOperations'
import { useContextMenu } from '@/composables/useContextMenu'
import { getFileIcon, getFileTypeClass, formatTime } from '@/constants/fileTypes'

// Components
import FileToolbar from '@/components/file/FileToolbar.vue'
import FileGridView from '@/components/file/FileGridView.vue'
import FileContextMenu from '@/components/file/FileContextMenu.vue'
import MobileActionBar from '@/components/file/MobileActionBar.vue'
import ShareBatchDialog from '@/components/ShareBatchDialog.vue'
import FolderSelectDialog from '@/components/FolderSelectDialog.vue'
import UppyUploader from '@/components/UppyUploader.vue'
import FilePreviewDialog from '@/components/FilePreviewDialog.vue'
import FileEditorDialog from '@/components/FileEditorDialog.vue'
import BatchProgressDialog from '@/components/BatchProgressDialog.vue'
import { EmptyState, FileSkeleton } from '@/components/ui'

const isMobile = useIsMobile()
const viewMode = ref<'list' | 'grid'>('list')

// Composables
const {
  loading, fileList, currentPage, pageSize, total,
  currentFolderId, searchKeyword, folderPath, isRecycleBin,
  loadFileList, navigateToRoot, navigateToFolder, enterFolder
} = useFileList()

const {
  selectedFiles, isFileSelected, toggleFileSelection,
  clearSelection, handleSelectionChange
} = useFileSelection(fileList)

const {
  showNewFolderDialog, newFolderName, creating,
  renameDialogVisible, renameNewName, renaming,
  uploadDialogVisible, shareDialogVisible,
  moveDialogVisible, copyDialogVisible,
  previewDialogVisible, previewTargetFile,
  editorDialogVisible, editorTargetFile,
  progressVisible, progressPercentage, progressStatus,
  operationTargetFiles, shareSelectedItems,
  isEditableFile, isFileProcessing,
  handleCreateFolder, handleDownload, handleShare, handleBatchShare,
  showRenameDialog, handleRename, handleDelete,
  handleRestore, handlePermanentDelete,
  handlePreview, handleOpenEditor, handleVectorize, handleGenerateSummary,
  handleMove, handleCopy, handleBatchMove, handleBatchCopy,
  confirmMove, confirmCopy,
  handleBatchDelete, handleBatchRestore, handleBatchPermanentDelete,
  handleEmptyRecycleBin,
  handleUploadSuccess, handleEditorSaved, handleShareSuccess
} = useFileOperations(loadFileList, clearSelection)

const contextMenu = useContextMenu()

// Truncate AI summary for tooltip
function truncateSummary(text: string | null | undefined): string {
  if (!text) return ''
  return text.length > 80 ? text.substring(0, 80) + '...' : text
}

// Batch download as ZIP
function handleBatchDownloadZip() {
  const fileIds = selectedFiles.value.filter((f: any) => f.fileType !== 'folder').map((f: any) => f.id)
  if (fileIds.length === 0) {
    ElMessage.warning('请选择要下载的文件')
    return
  }
  batchDownloadZip(fileIds)
  ElMessage.success('正在打包下载...')
}

// Row double click
const handleRowDblClick = (row: any) => {
  if (row.fileType === 'folder') {
    enterFolder(row)
  } else {
    handlePreview(row)
  }
}

// Context menu action dispatch
const handleContextAction = (action: string) => {
  const row = contextMenu.target.value
  if (!row) return
  contextMenu.hide()

  const actions: Record<string, () => void> = {
    open: () => handleRowDblClick(row),
    preview: () => handlePreview(row),
    edit: () => handleOpenEditor(row),
    download: () => handleDownload(row),
    vectorize: () => handleVectorize(row),
    summary: () => handleGenerateSummary(row),
    share: () => handleShare(row),
    move: () => handleMove(row),
    copy: () => handleCopy(row),
    rename: () => showRenameDialog(row),
    delete: () => handleDelete(row)
  }
  actions[action]?.()
}

onMounted(() => loadFileList())
</script>

<style scoped lang="scss">
.file-main {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.file-content {
  flex: 1;
  overflow: auto;
  padding: var(--space-md);
}

// Table styles
.file-table {
  :deep(.file-table-row) {
    cursor: pointer;
    transition: background-color var(--transition-fast);
  }

  :deep(.el-table__row:hover) {
    background-color: var(--sidebar-item-hover) !important;
  }

  :deep(.el-table__header-wrapper th) {
    background: var(--color-surface-secondary);
    color: var(--color-text-secondary);
    font-weight: 500;
    font-size: var(--font-size-sm);
  }
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  .file-icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--radius-sm);
    flex-shrink: 0;

    &.type-folder { color: var(--color-folder); background: rgba(245, 158, 11, 0.1); }
    &.type-document { color: var(--color-document); background: rgba(59, 130, 246, 0.1); }
    &.type-image { color: var(--color-image); background: rgba(139, 92, 246, 0.1); }
    &.type-video { color: var(--color-video); background: rgba(236, 72, 153, 0.1); }
    &.type-audio { color: var(--color-audio); background: rgba(20, 184, 166, 0.1); }
    &.type-archive { color: var(--color-archive); background: rgba(249, 115, 22, 0.1); }
    &.type-code { color: var(--color-code); background: rgba(16, 185, 129, 0.1); }
  }

  .file-name-text {
    font-weight: 500;
    font-size: var(--font-size-base);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
    color: var(--color-text);
  }

  .ai-tag {
    flex-shrink: 0;
    font-size: 11px;
    border-radius: var(--radius-full);
    display: inline-flex;
    align-items: center;
    gap: 3px;
    white-space: nowrap;
    padding: 0 8px;
    height: 22px;
    line-height: 22px;
  }
}

.meta-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.pagination-bar {
  padding: var(--space-sm) var(--space-md);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: center;
}

// Mobile
.file-main.is-mobile {
  .file-content {
    padding: var(--space-sm);
    padding-bottom: calc(var(--space-sm) + 100px);
  }

  .file-name-cell {
    gap: 6px;

    .file-icon-wrapper {
      width: 28px;
      height: 28px;
    }

    .file-name-text {
      font-size: var(--font-size-sm);
      max-width: 120px;
    }
  }

  .file-table :deep(.el-table__body td) {
    padding: 8px 4px;
  }
}
</style>
