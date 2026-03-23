<template>
    <el-dialog
        v-model="visible"
        :title="fileName"
        :width="dialogWidth"
        :fullscreen="isFullscreen"
        class="file-preview-dialog"
        destroy-on-close
        @close="handleClose"
    >
        <template #header>
            <div class="preview-header-wrapper">
            <div class="preview-header">
                <span class="file-name">{{ fileName }}</span>
                <div class="header-actions">
                    <!-- 图片画廊导航 -->
                    <template v-if="isImage && galleryFiles.length > 1">
                        <el-button size="small" :disabled="galleryIndex <= 0" @click="prevImage">
                            <el-icon><ArrowLeft /></el-icon>
                        </el-button>
                        <span class="gallery-counter">{{ galleryIndex + 1 }} / {{ galleryFiles.length }}</span>
                        <el-button size="small" :disabled="galleryIndex >= galleryFiles.length - 1" @click="nextImage">
                            <el-icon><ArrowRight /></el-icon>
                        </el-button>
                    </template>
                    <el-button
                        v-if="canEdit"
                        type="primary"
                        size="small"
                        @click="handleEdit"
                    >
                        编辑
                    </el-button>
                    <el-button size="small" @click="handleDownload">
                        下载
                    </el-button>
                    <el-button
                        :icon="isFullscreen ? 'Minus' : 'FullScreen'"
                        size="small"
                        circle
                        @click="toggleFullscreen"
                    />
                </div>
            </div>

            <!-- AI Summary bar -->
            <div v-if="file?.aiSummary" class="summary-bar">
                <div class="summary-toggle" @click="showSummary = !showSummary">
                    <el-icon><MagicStick /></el-icon>
                    <span>AI 摘要</span>
                    <el-icon class="toggle-arrow" :class="{ 'is-expanded': showSummary }"><ArrowDown /></el-icon>
                </div>
                <Transition name="slide-down">
                    <div v-if="showSummary" class="summary-content">
                        {{ file.aiSummary }}
                    </div>
                </Transition>
            </div>
            </div>
        </template>

        <div v-loading="loading" class="preview-content" :style="contentStyle">
            <!-- 图片原生预览 -->
            <template v-if="isImage && imageUrl">
                <div class="image-preview" @wheel.prevent="handleWheel">
                    <img
                        ref="imageRef"
                        :src="imageUrl"
                        :style="imageStyle"
                        class="preview-image"
                        @load="onImageLoad"
                        @mousedown="startDrag"
                        draggable="false"
                    />
                    <div class="image-toolbar">
                        <el-button circle size="small" @click="zoomIn" title="放大">
                            <el-icon><ZoomIn /></el-icon>
                        </el-button>
                        <el-button circle size="small" @click="zoomOut" title="缩小">
                            <el-icon><ZoomOut /></el-icon>
                        </el-button>
                        <el-button circle size="small" @click="resetZoom" title="重置">
                            <el-icon><RefreshRight /></el-icon>
                        </el-button>
                        <el-button circle size="small" @click="rotateImage" title="旋转">
                            <el-icon><RefreshLeft /></el-icon>
                        </el-button>
                    </div>
                </div>
            </template>

            <!-- 视频使用原生播放器 -->
            <template v-else-if="isVideo && mediaUrl">
                <div class="native-media-player video-player">
                    <video
                        ref="videoRef"
                        :src="mediaUrl"
                        controls
                        autoplay
                        preload="metadata"
                        class="media-element"
                    >
                        您的浏览器不支持视频播放
                    </video>
                </div>
            </template>

            <!-- 音频使用原生播放器 -->
            <template v-else-if="isAudio && mediaUrl">
                <div class="native-media-player audio-player">
                    <div class="audio-cover">
                        <el-icon :size="80" color="#667eea"><Headset /></el-icon>
                    </div>
                    <div class="audio-info">
                        <h3 class="audio-title">{{ fileName }}</h3>
                        <p class="audio-size">{{ fileSizeStr }}</p>
                    </div>
                    <audio
                        ref="audioRef"
                        :src="mediaUrl"
                        controls
                        autoplay
                        preload="metadata"
                        class="audio-element"
                    >
                        您的浏览器不支持音频播放
                    </audio>
                </div>
            </template>

            <!-- PDF 使用浏览器内置预览 -->
            <template v-else-if="isPdf && pdfUrl">
                <div class="pdf-preview">
                    <iframe
                        :src="pdfUrl"
                        class="pdf-viewer"
                        frameborder="0"
                    ></iframe>
                </div>
            </template>

            <!-- 文本/代码只读预览 -->
            <template v-else-if="isTextPreview && textContent !== null">
                <div class="text-preview">
                    <div v-if="isMarkdown" class="markdown-body" v-html="renderedMarkdown"></div>
                    <pre v-else class="code-preview"><code>{{ textContent }}</code></pre>
                </div>
            </template>

            <!-- Office 等文件使用 kkFileView 预览 -->
            <template v-else-if="previewUrl">
                <div class="kkfileview-preview">
                    <iframe
                        :src="previewUrl"
                        class="kkfileview-viewer"
                        frameborder="0"
                        @error="onKkfileviewError"
                    ></iframe>
                </div>
            </template>

            <!-- 加载中或无预览 -->
            <template v-else-if="!loading">
                <div class="unsupported-preview">
                    <el-icon :size="64"><Document /></el-icon>
                    <p>暂不支持预览此类型文件</p>
                    <p class="file-info">{{ fileName }} ({{ fileSizeStr }})</p>
                    <el-button type="primary" @click="handleDownload">
                        下载文件
                    </el-button>
                </div>
            </template>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
    Document, Headset, ArrowLeft, ArrowRight,
    ZoomIn, ZoomOut, RefreshRight, RefreshLeft,
    MagicStick, ArrowDown
} from '@element-plus/icons-vue'
import { getPreviewUrl, getStreamUrl, downloadFileStream, getFileContent, type FileInfo } from '@/api/file'

const props = defineProps<{
    modelValue: boolean
    file: FileInfo | null
    /** 同目录下的图片文件列表，用于画廊模式左右切换 */
    imageFiles?: FileInfo[]
    /** 分享模式：传入后使用分享接口而非登录接口 */
    shareMode?: {
        code: string
        password?: string
    }
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'edit', file: FileInfo): void
    (e: 'switch', file: FileInfo): void
}>()

const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const showSummary = ref(false)
const previewUrl = ref('')
const mediaUrl = ref('')
const imageUrl = ref('')
const pdfUrl = ref('')
const textContent = ref<string | null>(null)
const isFullscreen = ref(false)

// 图片缩放/拖拽/旋转状态
const imageScale = ref(1)
const imageRotation = ref(0)
const imageX = ref(0)
const imageY = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)

// 文件名和大小
const fileName = computed(() => props.file?.fileName || '')
const fileSizeStr = computed(() => props.file?.fileSizeStr || '')

// 扩展名分类
const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico']
const VIDEO_EXTENSIONS = ['mp4', 'webm', 'ogg', 'mov', 'avi', 'mkv', 'm4v', 'flv', 'wmv']
const AUDIO_EXTENSIONS = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a', 'wma', 'ape']
const TEXT_PREVIEW_EXTENSIONS = [
    'txt', 'md', 'markdown', 'log',
    'json', 'xml', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg', 'properties',
    'html', 'htm', 'css', 'scss', 'sass', 'less', 'js', 'ts', 'jsx', 'tsx', 'vue', 'svelte',
    'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'rb', 'php', 'swift', 'kt', 'kts',
    'scala', 'groovy', 'r', 'lua', 'pl', 'pm', 'sh', 'bash', 'zsh', 'fish', 'bat', 'cmd', 'ps1',
    'sql', 'gitignore', 'dockerignore', 'editorconfig', 'env'
]
const EDITABLE_EXTENSIONS = TEXT_PREVIEW_EXTENSIONS

// PDF 扩展名
const PDF_EXTENSIONS = ['pdf']
// Office 文件（需要 kkFileView）
const OFFICE_EXTENSIONS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'csv', 'rtf']

const fileExt = computed(() => props.file?.fileExt?.toLowerCase() || '')
const canEdit = computed(() => EDITABLE_EXTENSIONS.includes(fileExt.value) && !props.shareMode)
const isImage = computed(() => IMAGE_EXTENSIONS.includes(fileExt.value))
const isVideo = computed(() => VIDEO_EXTENSIONS.includes(fileExt.value))
const isAudio = computed(() => AUDIO_EXTENSIONS.includes(fileExt.value))
const isPdf = computed(() => PDF_EXTENSIONS.includes(fileExt.value))
const isTextPreview = computed(() => TEXT_PREVIEW_EXTENSIONS.includes(fileExt.value))
const isMarkdown = computed(() => ['md', 'markdown'].includes(fileExt.value))
const useNativePlayer = computed(() => isVideo.value || isAudio.value)

// 画廊相关
const galleryFiles = computed(() => {
    if (!props.imageFiles || props.imageFiles.length === 0) return []
    return props.imageFiles.filter(f => IMAGE_EXTENSIONS.includes(f.fileExt?.toLowerCase() || ''))
})
const galleryIndex = computed(() => {
    if (!props.file || galleryFiles.value.length === 0) return -1
    return galleryFiles.value.findIndex(f => f.id === props.file!.id)
})

// 简单的 Markdown 渲染（不依赖外部库）
const renderedMarkdown = computed(() => {
    if (!textContent.value) return ''
    return simpleMarkdownRender(textContent.value)
})

// 图片样式
const imageStyle = computed(() => ({
    transform: `translate(${imageX.value}px, ${imageY.value}px) scale(${imageScale.value}) rotate(${imageRotation.value}deg)`,
    cursor: isDragging.value ? 'grabbing' : 'grab',
    transition: isDragging.value ? 'none' : 'transform 0.2s ease'
}))

// 对话框宽度
const dialogWidth = computed(() => {
    if (isFullscreen.value) return '100%'
    if (isImage.value) return '80%'
    return '90%'
})

// 内容区域样式
const contentStyle = computed(() => {
    if (isFullscreen.value) {
        return { height: 'calc(100vh - 120px)' }
    }
    return { height: '80vh' }
})

// 监听文件变化
watch(
    () => props.file,
    async (file) => {
        if (!file || !visible.value) return
        await loadPreview()
    }
)

watch(visible, async (val) => {
    if (val && props.file) {
        await loadPreview()
    } else {
        resetState()
    }
})

async function loadPreview() {
    if (!props.file) return

    loading.value = true
    resetState()

    try {
        if (isImage.value) {
            // 图片使用流式 URL 直接加载
            imageUrl.value = getStreamUrl(props.file.id, props.shareMode)
        } else if (useNativePlayer.value) {
            // 视频/音频使用原生播放器
            mediaUrl.value = getStreamUrl(props.file.id, props.shareMode)
        } else if (isPdf.value) {
            // PDF 使用浏览器内置预览（stream URL + inline 显示）
            pdfUrl.value = getStreamUrl(props.file.id, props.shareMode)
        } else if (isTextPreview.value) {
            // 文本文件加载内容
            if (!props.shareMode) {
                const result = await getFileContent(props.file.id)
                textContent.value = result.content
            } else {
                // 分享模式下使用 kkFileView
                previewUrl.value = await getPreviewUrl(props.file.id, props.shareMode)
            }
        } else {
            // Office 等其他格式使用 kkFileView
            previewUrl.value = await getPreviewUrl(props.file.id, props.shareMode)
        }
        loading.value = false
    } catch (error: any) {
        ElMessage.error(error.message || '加载预览失败')
        loading.value = false
    }
}

function resetState() {
    previewUrl.value = ''
    mediaUrl.value = ''
    imageUrl.value = ''
    pdfUrl.value = ''
    textContent.value = null
    imageScale.value = 1
    imageRotation.value = 0
    imageX.value = 0
    imageY.value = 0
}

function onKkfileviewError() {
    // kkFileView 加载失败时的降级处理
    previewUrl.value = ''
    ElMessage.warning('文档预览服务不可用，请下载后查看')
}

function handleClose() {
    visible.value = false
}

function handleDownload() {
    if (props.file) {
        downloadFileStream(props.file.id, props.shareMode)
    }
}

function handleEdit() {
    if (props.file) {
        emit('edit', props.file)
        visible.value = false
    }
}

function toggleFullscreen() {
    isFullscreen.value = !isFullscreen.value
}

// ========== 图片操作 ==========
function onImageLoad() {
    imageScale.value = 1
    imageX.value = 0
    imageY.value = 0
    imageRotation.value = 0
}

function zoomIn() {
    imageScale.value = Math.min(imageScale.value * 1.25, 5)
}

function zoomOut() {
    imageScale.value = Math.max(imageScale.value / 1.25, 0.1)
}

function resetZoom() {
    imageScale.value = 1
    imageX.value = 0
    imageY.value = 0
    imageRotation.value = 0
}

function rotateImage() {
    imageRotation.value = (imageRotation.value + 90) % 360
}

function handleWheel(e: WheelEvent) {
    if (e.deltaY < 0) {
        zoomIn()
    } else {
        zoomOut()
    }
}

function startDrag(e: MouseEvent) {
    isDragging.value = true
    dragStartX.value = e.clientX - imageX.value
    dragStartY.value = e.clientY - imageY.value

    const onMove = (ev: MouseEvent) => {
        if (!isDragging.value) return
        imageX.value = ev.clientX - dragStartX.value
        imageY.value = ev.clientY - dragStartY.value
    }
    const onUp = () => {
        isDragging.value = false
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
    }
    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
}

// 画廊切换
function prevImage() {
    const idx = galleryIndex.value
    if (idx > 0) {
        const file = galleryFiles.value[idx - 1]
        if (file) emit('switch', file)
    }
}

function nextImage() {
    const idx = galleryIndex.value
    if (idx < galleryFiles.value.length - 1) {
        const file = galleryFiles.value[idx + 1]
        if (file) emit('switch', file)
    }
}

// ========== 简单 Markdown 渲染 ==========
function simpleMarkdownRender(md: string): string {
    let html = md
    // 转义 HTML
    html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    // 代码块
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="md-code-block"><code>$2</code></pre>')
    // 行内代码
    html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
    // 标题
    html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>')
    html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>')
    html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
    html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
    html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
    html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
    // 粗体/斜体
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
    // 链接
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    // 无序列表
    html = html.replace(/^\s*[-*+]\s+(.+)$/gm, '<li>$1</li>')
    // 分割线
    html = html.replace(/^---$/gm, '<hr />')
    // 段落（用双换行分段）
    html = html.replace(/\n\n/g, '</p><p>')
    // 单换行为 <br>
    html = html.replace(/\n/g, '<br />')
    return '<p>' + html + '</p>'
}
</script>

<style lang="scss" scoped>
.file-preview-dialog {
    :deep(.el-dialog__header) {
        padding: 16px 20px;
        border-bottom: 1px solid var(--el-border-color-lighter);
        margin-right: 0;
    }

    :deep(.el-dialog__body) {
        padding: 0;
    }
}

.preview-header-wrapper {
    width: 100%;
    padding-right: 40px;
}

.preview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    .file-name {
        font-size: 16px;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 50%;
    }

    .header-actions {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .gallery-counter {
        font-size: 13px;
        color: #909399;
        min-width: 50px;
        text-align: center;
    }
}

.summary-bar {
    border-top: 1px solid var(--el-border-color-lighter);
    background: var(--color-surface-secondary, #f8fafc);
    margin-top: 8px;
    border-radius: 6px;
}

.summary-toggle {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    cursor: pointer;
    font-size: var(--font-size-sm, 13px);
    font-weight: 500;
    color: var(--color-primary, #7C3AED);
    transition: background 0.15s;

    &:hover {
        background: var(--sidebar-item-hover, rgba(0, 0, 0, 0.04));
    }

    .toggle-arrow {
        margin-left: auto;
        transition: transform 0.15s;
        &.is-expanded { transform: rotate(180deg); }
    }
}

.summary-content {
    padding: 12px 16px;
    font-size: var(--font-size-sm, 13px);
    line-height: 1.7;
    color: var(--color-text-secondary, #666);
    border-top: 1px solid var(--el-border-color-lighter);
}

.slide-down-enter-active,
.slide-down-leave-active {
    transition: all 0.2s;
    overflow: hidden;
}
.slide-down-enter-from,
.slide-down-leave-to {
    opacity: 0;
    max-height: 0;
    padding-top: 0;
    padding-bottom: 0;
}
.slide-down-enter-to,
.slide-down-leave-from {
    opacity: 1;
    max-height: 200px;
}

.preview-content {
    overflow: hidden;
    min-height: 300px;
}

// 图片预览
.image-preview {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background: #f0f0f0;
    position: relative;

    // 棋盘格背景（透明图片友好）
    background-image:
        linear-gradient(45deg, #e0e0e0 25%, transparent 25%),
        linear-gradient(-45deg, #e0e0e0 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, #e0e0e0 75%),
        linear-gradient(-45deg, transparent 75%, #e0e0e0 75%);
    background-size: 20px 20px;
    background-position: 0 0, 0 10px, 10px -10px, -10px 0px;

    .preview-image {
        max-width: 90%;
        max-height: 90%;
        object-fit: contain;
        user-select: none;
        border-radius: 4px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    }

    .image-toolbar {
        position: absolute;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        display: flex;
        gap: 8px;
        padding: 8px 16px;
        background: rgba(0, 0, 0, 0.6);
        border-radius: 24px;
        backdrop-filter: blur(8px);

        .el-button {
            background: rgba(255, 255, 255, 0.15);
            border: none;
            color: #fff;

            &:hover {
                background: rgba(255, 255, 255, 0.3);
            }
        }
    }
}

// 文本预览
.text-preview {
    padding: 24px;
    height: 100%;
    overflow: auto;
    background: #fafafa;

    .code-preview {
        margin: 0;
        white-space: pre-wrap;
        word-wrap: break-word;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        font-size: 14px;
        line-height: 1.6;
        color: #333;
        background: #fff;
        padding: 20px;
        border-radius: 8px;
        border: 1px solid #e8e8e8;
    }

    .markdown-body {
        max-width: 860px;
        margin: 0 auto;
        font-size: 15px;
        line-height: 1.7;
        color: #333;

        :deep(h1) { font-size: 2em; margin: 0.5em 0; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        :deep(h2) { font-size: 1.5em; margin: 0.5em 0; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        :deep(h3) { font-size: 1.25em; margin: 0.5em 0; }
        :deep(h4) { font-size: 1em; margin: 0.5em 0; }
        :deep(strong) { font-weight: 600; }
        :deep(a) { color: #7C3AED; text-decoration: none; &:hover { text-decoration: underline; } }
        :deep(hr) { border: none; border-top: 1px solid #ddd; margin: 1.5em 0; }
        :deep(li) { margin: 0.3em 0; margin-left: 1.5em; }
        :deep(.md-code-block) {
            background: #1e1e1e;
            color: #d4d4d4;
            padding: 16px;
            border-radius: 8px;
            overflow-x: auto;
            font-family: 'Consolas', monospace;
            font-size: 14px;
            line-height: 1.5;
        }
        :deep(.md-inline-code) {
            background: #f0f0f0;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'Consolas', monospace;
            font-size: 0.9em;
        }
    }
}

// 原生媒体播放器（视频/音频）
.native-media-player {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
    padding: 24px;

    &.video-player {
        .media-element {
            max-width: 100%;
            max-height: 100%;
            width: auto;
            height: auto;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
            outline: none;

            &::-webkit-media-controls-panel {
                background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
            }
        }
    }

    &.audio-player {
        gap: 24px;

        .audio-cover {
            width: 160px;
            height: 160px;
            border-radius: 50%;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 20px 60px rgba(102, 126, 234, 0.4);
            animation: pulse 2s ease-in-out infinite;
        }

        .audio-info {
            text-align: center;
            color: #fff;

            .audio-title {
                margin: 0 0 8px;
                font-size: 20px;
                font-weight: 600;
                max-width: 400px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .audio-size {
                margin: 0;
                font-size: 14px;
                color: rgba(255, 255, 255, 0.6);
            }
        }

        .audio-element {
            width: 100%;
            max-width: 500px;
            height: 54px;
            border-radius: 27px;
            outline: none;

            &::-webkit-media-controls-panel {
                background: rgba(255, 255, 255, 0.1);
                border-radius: 27px;
            }
        }
    }
}

@keyframes pulse {
    0%, 100% {
        transform: scale(1);
        box-shadow: 0 20px 60px rgba(102, 126, 234, 0.4);
    }
    50% {
        transform: scale(1.05);
        box-shadow: 0 25px 70px rgba(102, 126, 234, 0.5);
    }
}

// PDF 预览（浏览器内置）
.pdf-preview {
    height: 100%;

    .pdf-viewer {
        width: 100%;
        height: 100%;
        border: none;
    }
}

// kkFileView 预览
.kkfileview-preview {
    height: 80vh;

    .kkfileview-viewer {
        width: 100%;
        height: 100%;
        border: none;
    }
}

// 不支持的预览
.unsupported-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    color: #909399;

    p {
        margin: 16px 0 8px;
        font-size: 16px;
    }

    .file-info {
        font-size: 14px;
        color: #c0c4cc;
        margin-bottom: 24px;
    }
}
</style>
