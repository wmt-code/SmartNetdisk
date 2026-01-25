<template>
  <div class="uppy-uploader">
    <!-- Uppy Dashboard 区域 -->
    <div ref="uppyContainer" class="uppy-dashboard-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import Uppy from '@uppy/core'
import Dashboard from '@uppy/dashboard'
import ThumbnailGenerator from '@uppy/thumbnail-generator'
import { SmartUploadPlugin } from '@/utils/uppySmartUpload'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables'

// 导入 Uppy 样式
import '@uppy/core/css/style.css'
import '@uppy/dashboard/css/style.css'

const props = defineProps<{
  folderId: number
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success', files: { id: number; name: string }[]): void
}>()

const userStore = useUserStore()
const isMobile = useIsMobile()
const uppyContainer = ref<HTMLElement>()

let uppy: Uppy | null = null
let smartUploadPlugin: SmartUploadPlugin | null = null

// 初始化 Uppy
function initUppy() {
  uppy = new Uppy({
    id: 'smartUppy',
    autoProceed: false,
    restrictions: {
      maxFileSize: 10 * 1024 * 1024 * 1024, // 10GB
      maxNumberOfFiles: 100,
    },
  })

  // 安装智能上传插件（手动创建并调用 install 方法来注册预处理器和上传器）
  smartUploadPlugin = new SmartUploadPlugin(uppy, { folderId: props.folderId })
  smartUploadPlugin.install()

  // 安装缩略图生成器（用于图片/视频预览）
  uppy.use(ThumbnailGenerator, {
    thumbnailWidth: 280,
    thumbnailHeight: 280,
    thumbnailType: 'image/jpeg',
    waitForThumbnailsBeforeUpload: false
  })

  // 安装 Dashboard
  uppy.use(Dashboard, {
    inline: true,
    target: uppyContainer.value,
    width: '100%',
    height: isMobile.value ? 'calc(100vh - 120px)' : 450,
    hideProgressDetails: false, // 启用详细进度显示（上传速度、剩余时间）
    proudlyDisplayPoweredByUppy: false,
    note: isMobile.value ? '支持秒传、断点续传' : '支持拖拽上传文件或文件夹、秒传、断点续传，单文件最大 10GB',
    hideUploadButton: false,
    showRemoveButtonAfterComplete: true,
    singleFileFullScreen: false, // 移动端不使用全屏预览
    showSelectedFiles: true, // 显示已选文件列表
    // 启用文件夹上传
    fileManagerSelectionType: 'both', // 允许选择文件和文件夹
    doneButtonHandler: () => {
      emit('close')
    },
    locale: {
      strings: {
        // 主界面
        dropPasteFiles: '将文件拖放到此处，或 %{browseFiles}',
        dropPasteFolders: '将文件拖放到此处，或 %{browseFolders}',
        dropPasteBoth: '将文件拖放到此处，%{browseFiles} 或 %{browseFolders}',
        dropPasteImportFiles: '将文件拖放到此处，粘贴，或 %{browseFiles}',
        dropPasteImportFolders: '将文件拖放到此处，粘贴，或 %{browseFolders}',
        dropPasteImportBoth: '将文件拖放到此处，粘贴，%{browseFiles} 或 %{browseFolders}',
        dropHint: '将文件拖放到此处',
        browseFiles: '选择文件',
        browseFolders: '选择文件夹',
        // 文件操作（复数形式）
        uploadXFiles: { 0: '上传文件', 1: '上传 %{smart_count} 个文件' },
        uploadXNewFiles: { 0: '上传新文件', 1: '上传 %{smart_count} 个新文件' },
        upload: '开始上传',
        cancel: '取消',
        retryUpload: '重试上传',
        pause: '暂停',
        resume: '继续',
        done: '完成',
        // 进度状态（复数形式）
        filesUploadedOfTotal: { 0: '已上传 %{complete} / %{smart_count} 个文件', 1: '已上传 %{complete} / %{smart_count} 个文件' },
        dataUploadedOfTotal: '%{complete} / %{total}',
        xTimeLeft: '剩余 %{time}',
        uploadComplete: '上传完成',
        uploadPaused: '上传已暂停',
        resumeUpload: '继续上传',
        pauseUpload: '暂停上传',
        cancelUpload: '取消上传',
        // 文件选择（复数形式）
        xFilesSelected: { 0: '已选择文件', 1: '已选择 %{smart_count} 个文件' },
        uploadingXFiles: { 0: '正在上传文件', 1: '正在上传 %{smart_count} 个文件' },
        processingXFiles: { 0: '正在处理文件', 1: '正在处理 %{smart_count} 个文件' },
        // 进度详情
        uploading: '正在上传',
        complete: '完成',
        uploadFailed: '上传失败',
        paused: '已暂停',
        retry: '重试',
        // 文件信息
        xMoreFilesAdded: { 0: '又添加了 %{smart_count} 个文件', 1: '又添加了 %{smart_count} 个文件' },
        // 其他
        addMore: '添加更多',
        addMoreFiles: '添加更多文件',
        editFile: '编辑文件',
        editing: '正在编辑 %{file}',
        finishEditingFile: '完成编辑',
        removeFile: '移除文件 %{file}',
        myDevice: '本地文件',
        poweredBy: '',
      },
    },
  })

  // 监听上传成功事件
  uppy.on('upload-success', (file, response) => {
    if (file && response?.body) {
      const result = response.body as unknown as { fileId: number; fileName: string; fileSize?: number }
      // 更新已用空间
      if (file.size) {
        userStore.updateUsedSpace(file.size)
      }
    }
  })

  // 监听全部上传完成
  uppy.on('complete', (result) => {
    const successFiles = result.successful?.map(file => {
      const body = file.response?.body as unknown as { fileId: number; fileName: string } | undefined
      return {
        id: body?.fileId ?? 0,
        name: file.name
      }
    }) || []

    if (successFiles.length > 0) {
      emit('success', successFiles)
    }
  })
}

// 销毁 Uppy
function destroyUppy() {
  if (uppy) {
    uppy.destroy()
    uppy = null
    smartUploadPlugin = null
  }
}

// 监听 folderId 变化
watch(() => props.folderId, (newFolderId) => {
  if (smartUploadPlugin) {
    smartUploadPlugin.setFolderId(newFolderId)
  }
})

// 监听 visible 变化，重置 Uppy 状态
watch(() => props.visible, (visible) => {
  if (visible && uppy) {
    // 清空之前的文件
    uppy.cancelAll()
  }
})

onMounted(() => {
  initUppy()
})

// 暴露给父组件的方法
const addFiles = (files: FileList | File[]) => {
  if (!uppy) return
  
  Array.from(files).forEach((file) => {
    try {
      uppy?.addFile({
        source: 'DragDrop',
        name: file.name,
        type: file.type,
        data: file,
      })
    } catch (err) {
      // 忽略重复添加文件的错误
      console.warn('File addition skipped:', err)
    }
  })
}

defineExpose({
  addFiles
})

onBeforeUnmount(() => {
  destroyUppy()
})
</script>

<style lang="scss" scoped>
.uppy-uploader {
  width: 100%;
  
  :deep(.uppy-Dashboard-inner) {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(139, 92, 246, 0.2);
    border-radius: 12px;
  }

  :deep(.uppy-Dashboard-AddFiles) {
    border: 2px dashed rgba(139, 92, 246, 0.3);
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: rgba(139, 92, 246, 0.6);
      background: rgba(139, 92, 246, 0.05);
    }
  }

  :deep(.uppy-Dashboard-browse) {
    color: #8B5CF6;
    font-weight: 500;
    
    &:hover {
      color: #7C3AED;
    }
  }

  :deep(.uppy-StatusBar-actionBtn--upload) {
    background: linear-gradient(135deg, #8B5CF6 0%, #F97316 100%);
    border: none;
    
    &:hover {
      background: linear-gradient(135deg, #7C3AED 0%, #EA580C 100%);
    }
  }

  :deep(.uppy-StatusBar-actionBtn--done) {
    background: #10B981;
    border: none;
  }

  :deep(.uppy-Dashboard-Item-progress) {
    .uppy-Dashboard-Item-progressIndicator {
      color: #8B5CF6;
    }
  }

  :deep(.uppy-StatusBar-progress) {
    background: linear-gradient(135deg, #8B5CF6 0%, #F97316 100%);
  }

  :deep(.uppy-StatusBar) {
    background: rgba(255, 255, 255, 0.9);
    border-top: 1px solid rgba(139, 92, 246, 0.1);
  }

  :deep(.uppy-Dashboard-note) {
    color: #6B7280;
    font-size: 12px;
  }

  // Mobile responsive styles
  @media (max-width: 767px) {
    :deep(.uppy-Dashboard-inner) {
      border-radius: 8px;
    }

    :deep(.uppy-Dashboard-AddFiles-title) {
      font-size: 1rem;
    }

    :deep(.uppy-Dashboard-note) {
      font-size: 11px;
      padding: 0 8px;
    }

    :deep(.uppy-Dashboard-AddFiles) {
      padding: 16px;
    }

    :deep(.uppy-Dashboard-Item) {
      padding: 8px;
    }

    :deep(.uppy-Dashboard-Item-name) {
      font-size: 12px;
    }

    :deep(.uppy-StatusBar-content) {
      flex-wrap: wrap;
    }
  }
}
</style>
