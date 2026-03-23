import { ref } from 'vue'
import {
  deleteFile,
  renameFile,
  downloadFileStream,
  createFolder,
  restoreFile,
  permanentDeleteFile,
  deleteFolder,
  moveFile,
  batchMoveFiles,
  copyFile,
  batchCopyFiles,
  batchDeleteFiles,
  restoreFolder,
  permanentDeleteFolder,
  clearRecycleBin,
  type FileInfo,
} from '@/api/file'
import { vectorizeFile, generateSummary } from '@/api/ai'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useFileOperations(loadFileList: () => Promise<void>, clearSelection: () => void) {
  const userStore = useUserStore()

  // Dialog states
  const showNewFolderDialog = ref(false)
  const newFolderName = ref('')
  const creating = ref(false)
  const renameDialogVisible = ref(false)
  const renameNewName = ref('')
  const renameTargetFile = ref<FileInfo | null>(null)
  const renaming = ref(false)
  const uploadDialogVisible = ref(false)
  const shareDialogVisible = ref(false)
  const moveDialogVisible = ref(false)
  const copyDialogVisible = ref(false)
  const previewDialogVisible = ref(false)
  const previewTargetFile = ref<FileInfo | null>(null)
  const editorDialogVisible = ref(false)
  const editorTargetFile = ref<FileInfo | null>(null)
  const progressVisible = ref(false)
  const progressPercentage = ref(0)
  const progressStatus = ref('')
  const operationTargetFiles = ref<FileInfo[]>([])
  const shareSelectedItems = ref<
    {
      type: 'file' | 'folder'
      id: number
      name: string
      size: number
      sizeStr: string
      fileType?: string
    }[]
  >([])

  // EDITABLE_EXTENSIONS
  const EDITABLE_EXTENSIONS = [
    'txt',
    'md',
    'markdown',
    'log',
    'json',
    'xml',
    'yml',
    'yaml',
    'toml',
    'ini',
    'conf',
    'cfg',
    'properties',
    'html',
    'htm',
    'css',
    'scss',
    'sass',
    'less',
    'js',
    'ts',
    'jsx',
    'tsx',
    'vue',
    'svelte',
    'java',
    'py',
    'go',
    'rs',
    'c',
    'cpp',
    'h',
    'hpp',
    'cs',
    'rb',
    'php',
    'swift',
    'kt',
    'kts',
    'scala',
    'groovy',
    'r',
    'lua',
    'pl',
    'pm',
    'sh',
    'bash',
    'zsh',
    'fish',
    'bat',
    'cmd',
    'ps1',
    'sql',
    'gitignore',
    'dockerignore',
    'editorconfig',
    'env',
  ]

  const isEditableFile = (file: FileInfo | null): boolean => {
    if (!file) return false
    return EDITABLE_EXTENSIONS.includes(file.fileExt?.toLowerCase() || '')
  }

  const handleCreateFolder = async (currentFolderId: number) => {
    if (!newFolderName.value.trim()) {
      ElMessage.warning('请输入文件夹名称')
      return
    }
    creating.value = true
    try {
      await createFolder(newFolderName.value, currentFolderId)
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
    downloadFileStream(row.id)
  }

  const handleShare = (row: FileInfo) => {
    shareSelectedItems.value = [
      {
        type: row.fileType === 'folder' ? 'folder' : 'file',
        id: row.id,
        name: row.fileName,
        size: row.fileSize,
        sizeStr: row.fileSizeStr || '',
        fileType: row.fileType,
      },
    ]
    shareDialogVisible.value = true
  }

  const handleBatchShare = (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) {
      ElMessage.warning('请先选择要分享的文件')
      return
    }
    shareSelectedItems.value = selectedFiles.map((file) => ({
      type: file.fileType === 'folder' ? 'folder' : 'file',
      id: file.id,
      name: file.fileName,
      size: file.fileSize,
      sizeStr: file.fileSizeStr || '',
      fileType: file.fileType,
    }))
    shareDialogVisible.value = true
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
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
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
      if (error !== 'cancel') console.error('删除失败:', error)
    }
  }

  const handleRestore = async (row: FileInfo) => {
    try {
      if (row.fileType === 'folder') {
        await restoreFolder(row.id)
      } else {
        await restoreFile(row.id)
      }
      ElMessage.success(`${row.fileType === 'folder' ? '文件夹' : '文件'}已恢复`)
      await loadFileList()
    } catch (error) {
      console.error('恢复失败:', error)
    }
  }

  const handlePermanentDelete = async (row: FileInfo) => {
    try {
      const isFolder = row.fileType === 'folder'
      await ElMessageBox.confirm(
        `确定要彻底删除此${isFolder ? '文件夹' : '文件'}吗？此操作不可恢复！`,
        '彻底删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' },
      )
      if (isFolder) {
        await permanentDeleteFolder(row.id)
      } else {
        await permanentDeleteFile(row.id)
        userStore.updateUsedSpace(-row.fileSize)
      }
      ElMessage.success('已彻底删除')
      await loadFileList()
    } catch (error) {
      if (error !== 'cancel') console.error('彻底删除失败:', error)
    }
  }

  const handlePreview = (row: FileInfo) => {
    previewTargetFile.value = row
    previewDialogVisible.value = true
  }

  const handleOpenEditor = (row: FileInfo) => {
    editorTargetFile.value = row
    editorDialogVisible.value = true
    previewDialogVisible.value = false
  }

  // AI 处理中的文件 ID 集合
  const processingFileIds = ref<Set<number>>(new Set())

  const isFileProcessing = (fileId: number) => processingFileIds.value.has(fileId)

  const handleVectorize = async (row: FileInfo) => {
    processingFileIds.value.add(row.id)
    ElMessage({
      message: `「${row.fileName}」正在进行智能分析，完成后将通知您`,
      type: 'info',
      duration: 3000,
    })
    try {
      await vectorizeFile(row.id)
      ElMessage.success(`「${row.fileName}」智能分析完成`)
      await loadFileList()
    } catch (error) {
      ElMessage.error(`「${row.fileName}」智能分析失败`)
      console.error('触发向量化失败:', error)
    } finally {
      processingFileIds.value.delete(row.id)
    }
  }

  const handleGenerateSummary = async (row: FileInfo) => {
    processingFileIds.value.add(row.id)
    ElMessage({
      message: `「${row.fileName}」正在生成 AI 摘要，请稍候...`,
      type: 'info',
      duration: 3000,
    })
    try {
      await generateSummary(row.id)
      ElMessage.success(`「${row.fileName}」AI 摘要已生成`)
      await loadFileList()
    } catch (error) {
      ElMessage.error(`「${row.fileName}」摘要生成失败`)
      console.error('生成摘要失败:', error)
    } finally {
      processingFileIds.value.delete(row.id)
    }
  }

  const handleMove = (row: FileInfo) => {
    operationTargetFiles.value = [row]
    moveDialogVisible.value = true
  }

  const handleCopy = (row: FileInfo) => {
    operationTargetFiles.value = [row]
    copyDialogVisible.value = true
  }

  const handleBatchMove = (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) return
    operationTargetFiles.value = selectedFiles
    moveDialogVisible.value = true
  }

  const handleBatchCopy = (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) return
    operationTargetFiles.value = selectedFiles
    copyDialogVisible.value = true
  }

  const confirmMove = async (targetFolderId: number) => {
    if (operationTargetFiles.value.length === 0) return
    try {
      if (operationTargetFiles.value.length === 1) {
        await moveFile(operationTargetFiles.value[0]!.id, targetFolderId)
      } else {
        await batchMoveFiles(
          operationTargetFiles.value.map((f) => f.id),
          targetFolderId,
        )
      }
      ElMessage.success('移动成功')
      await loadFileList()
    } catch (error) {
      console.error('移动失败:', error)
    }
  }

  const confirmCopy = async (targetFolderId: number) => {
    if (operationTargetFiles.value.length === 0) return
    try {
      if (operationTargetFiles.value.length === 1) {
        await copyFile(operationTargetFiles.value[0]!.id, targetFolderId)
      } else {
        await batchCopyFiles(
          operationTargetFiles.value.map((f) => f.id),
          targetFolderId,
        )
      }
      ElMessage.success('复制成功')
      await loadFileList()
    } catch (error) {
      console.error('复制失败:', error)
    }
  }

  const processBatchOperation = async (
    items: FileInfo[],
    operationName: string,
    processItem: (item: FileInfo) => Promise<void>,
  ) => {
    if (items.length === 0) return
    progressVisible.value = true
    progressPercentage.value = 0
    progressStatus.value = `正在${operationName} 0/${items.length}`
    let successCount = 0,
      failCount = 0
    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (!item) continue
      try {
        await processItem(item)
        successCount++
      } catch {
        failCount++
      }
      progressPercentage.value = Math.round(((i + 1) / items.length) * 100)
      progressStatus.value = `正在${operationName} ${i + 1}/${items.length}`
    }
    setTimeout(() => {
      progressVisible.value = false
      if (failCount > 0) {
        ElMessage.warning(`${operationName}完成: ${successCount} 个成功, ${failCount} 个失败`)
      } else {
        ElMessage.success(`${operationName}成功`)
      }
      loadFileList()
      clearSelection()
    }, 500)
  }

  const handleBatchDelete = async (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) return
    try {
      await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedFiles.length} 个文件/文件夹吗？`,
        '批量删除',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
      )
      const files = selectedFiles.filter((f) => f.fileType !== 'folder')
      const folders = selectedFiles.filter((f) => f.fileType === 'folder')
      if (files.length > 0) {
        await batchDeleteFiles(files.map((f) => f.id))
      }
      for (const folder of folders) {
        await deleteFolder(folder.id)
      }
      ElMessage.success('批量删除成功')
      files.forEach((f) => userStore.updateUsedSpace(-f.fileSize))
      await loadFileList()
      clearSelection()
    } catch (error) {
      if (error !== 'cancel') console.error('批量删除失败:', error)
    }
  }

  const handleBatchRestore = async (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) return
    try {
      await ElMessageBox.confirm(
        `确定要恢复选中的 ${selectedFiles.length} 个文件/文件夹吗？`,
        '批量恢复',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' },
      )
      await processBatchOperation(selectedFiles, '恢复', async (item) => {
        if (item.fileType === 'folder') {
          await restoreFolder(item.id)
        } else {
          await restoreFile(item.id)
        }
      })
    } catch (error) {
      if (error !== 'cancel') console.error('批量恢复失败')
    }
  }

  const handleBatchPermanentDelete = async (selectedFiles: FileInfo[]) => {
    if (selectedFiles.length === 0) return
    try {
      await ElMessageBox.confirm(
        `确定要彻底删除选中的 ${selectedFiles.length} 个文件/文件夹吗？此操作不可恢复！`,
        '批量彻底删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' },
      )
      await processBatchOperation(selectedFiles, '删除', async (item) => {
        if (item.fileType === 'folder') {
          await permanentDeleteFolder(item.id)
        } else {
          await permanentDeleteFile(item.id)
          userStore.updateUsedSpace(-item.fileSize)
        }
      })
    } catch (error) {
      if (error !== 'cancel') console.error('批量彻底删除失败')
    }
  }

  const handleEmptyRecycleBin = async () => {
    try {
      await ElMessageBox.confirm('确定要清空回收站吗？所有文件将无法恢复！', '清空回收站', {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await clearRecycleBin()
      ElMessage.success('回收站已清空')
      await loadFileList()
    } catch (error) {
      if (error !== 'cancel') console.error('清空回收站失败:', error)
    }
  }

  const handleUploadSuccess = (files: { id: number; name: string }[]) => {
    ElMessage.success(`成功上传 ${files.length} 个文件`)
    loadFileList()
  }

  const handleEditorSaved = () => {
    loadFileList()
  }
  const handleShareSuccess = () => {
    console.log('分享创建成功')
  }

  return {
    // Dialog states
    showNewFolderDialog,
    newFolderName,
    creating,
    renameDialogVisible,
    renameNewName,
    renameTargetFile,
    renaming,
    uploadDialogVisible,
    shareDialogVisible,
    moveDialogVisible,
    copyDialogVisible,
    previewDialogVisible,
    previewTargetFile,
    editorDialogVisible,
    editorTargetFile,
    progressVisible,
    progressPercentage,
    progressStatus,
    operationTargetFiles,
    shareSelectedItems,
    // AI processing state
    processingFileIds,
    isFileProcessing,
    // Methods
    isEditableFile,
    handleCreateFolder,
    handleDownload,
    handleShare,
    handleBatchShare,
    showRenameDialog,
    handleRename,
    handleDelete,
    handleRestore,
    handlePermanentDelete,
    handlePreview,
    handleOpenEditor,
    handleVectorize,
    handleGenerateSummary,
    handleMove,
    handleCopy,
    handleBatchMove,
    handleBatchCopy,
    confirmMove,
    confirmCopy,
    handleBatchDelete,
    handleBatchRestore,
    handleBatchPermanentDelete,
    handleEmptyRecycleBin,
    handleUploadSuccess,
    handleEditorSaved,
    handleShareSuccess,
  }
}
