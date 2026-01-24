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
            <div class="preview-header">
                <span class="file-name">{{ fileName }}</span>
                <div class="header-actions">
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
        </template>

        <div v-loading="loading" class="preview-content" :style="contentStyle">
            <!-- 图片预览 -->
            <template v-if="previewType === 'image'">
                <div class="image-preview">
                    <img
                        :src="previewUrl"
                        :alt="fileName"
                        :style="imageStyle"
                        @load="onImageLoad"
                    />
                    <div class="image-controls">
                        <el-button-group>
                            <el-button size="small" @click="zoomOut">
                                <el-icon><ZoomOut /></el-icon>
                            </el-button>
                            <el-button size="small" @click="resetZoom">
                                {{ Math.round(imageScale * 100) }}%
                            </el-button>
                            <el-button size="small" @click="zoomIn">
                                <el-icon><ZoomIn /></el-icon>
                            </el-button>
                            <el-button size="small" @click="rotateLeft">
                                <el-icon><RefreshLeft /></el-icon>
                            </el-button>
                            <el-button size="small" @click="rotateRight">
                                <el-icon><RefreshRight /></el-icon>
                            </el-button>
                        </el-button-group>
                    </div>
                </div>
            </template>

            <!-- 视频预览 -->
            <template v-else-if="previewType === 'video'">
                <video
                    ref="videoRef"
                    :src="previewUrl"
                    controls
                    class="video-player"
                >
                    您的浏览器不支持视频播放
                </video>
            </template>

            <!-- 音频预览 -->
            <template v-else-if="previewType === 'audio'">
                <div class="audio-preview">
                    <div class="audio-icon">
                        <el-icon :size="80"><Headset /></el-icon>
                    </div>
                    <audio ref="audioRef" :src="previewUrl" controls class="audio-player">
                        您的浏览器不支持音频播放
                    </audio>
                </div>
            </template>

            <!-- PDF 预览 -->
            <template v-else-if="previewType === 'pdf'">
                <iframe
                    :src="previewUrl"
                    class="pdf-viewer"
                    frameborder="0"
                ></iframe>
            </template>

            <!-- 文本/代码预览 -->
            <template v-else-if="previewType === 'text'">
                <div class="text-preview">
                    <pre><code>{{ textContent }}</code></pre>
                </div>
            </template>

            <!-- Office 文档预览 -->
            <template v-else-if="previewType === 'office'">
                <div class="office-preview">
                    <iframe
                        :src="officePreviewUrl"
                        class="office-viewer"
                        frameborder="0"
                    ></iframe>
                </div>
            </template>

            <!-- 不支持的文件类型 -->
            <template v-else>
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
    ZoomIn,
    ZoomOut,
    RefreshLeft,
    RefreshRight,
    Document,
    Headset
} from '@element-plus/icons-vue'
import { getPreviewUrl, getFileContent, downloadFileStream, type FileInfo } from '@/api/file'

const props = defineProps<{
    modelValue: boolean
    file: FileInfo | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'edit', file: FileInfo): void
}>()

const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const previewUrl = ref('')
const textContent = ref('')
const isFullscreen = ref(false)
const imageScale = ref(1)
const imageRotation = ref(0)

// 文件名和大小
const fileName = computed(() => props.file?.fileName || '')
const fileSizeStr = computed(() => props.file?.fileSizeStr || '')
const fileExt = computed(() => props.file?.fileExt?.toLowerCase() || '')

// 判断预览类型
const previewType = computed(() => {
    const ext = fileExt.value
    if (!ext) return 'unsupported'

    // 图片
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico'].includes(ext)) {
        return 'image'
    }
    // 视频
    if (['mp4', 'webm', 'ogg', 'mov', 'avi', 'mkv'].includes(ext)) {
        return 'video'
    }
    // 音频
    if (['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac'].includes(ext)) {
        return 'audio'
    }
    // PDF
    if (ext === 'pdf') {
        return 'pdf'
    }
    // 文本/代码
    if (EDITABLE_EXTENSIONS.includes(ext)) {
        return 'text'
    }
    // Office 文档
    if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) {
        return 'office'
    }

    return 'unsupported'
})

// 可编辑的文件扩展名
const EDITABLE_EXTENSIONS = [
    'txt', 'md', 'markdown', 'log',
    'json', 'xml', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg', 'properties',
    'html', 'htm', 'css', 'scss', 'sass', 'less', 'js', 'ts', 'jsx', 'tsx', 'vue', 'svelte',
    'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'rb', 'php', 'swift', 'kt', 'kts',
    'scala', 'groovy', 'r', 'lua', 'pl', 'pm', 'sh', 'bash', 'zsh', 'fish', 'bat', 'cmd', 'ps1',
    'sql', 'gitignore', 'dockerignore', 'editorconfig', 'env'
]

// 是否可以编辑
const canEdit = computed(() => {
    return EDITABLE_EXTENSIONS.includes(fileExt.value)
})

// 对话框宽度
const dialogWidth = computed(() => {
    if (isFullscreen.value) return '100%'
    const type = previewType.value
    if (type === 'image') return '80%'
    if (type === 'video') return '80%'
    if (type === 'audio') return '500px'
    if (type === 'pdf' || type === 'office') return '90%'
    if (type === 'text') return '80%'
    return '600px'
})

// 内容区域样式
const contentStyle = computed(() => {
    if (isFullscreen.value) {
        return { height: 'calc(100vh - 120px)' }
    }
    const type = previewType.value
    if (type === 'pdf' || type === 'office' || type === 'text') {
        return { height: '70vh' }
    }
    return {}
})

// 图片样式
const imageStyle = computed(() => ({
    transform: `scale(${imageScale.value}) rotate(${imageRotation.value}deg)`,
    transition: 'transform 0.3s ease'
}))

// Office 在线预览 URL
const officePreviewUrl = computed(() => {
    if (!previewUrl.value) return ''
    // 使用 Microsoft Office Online 预览
    return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(previewUrl.value)}`
})

// 监听文件变化,加载预览
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
        const type = previewType.value

        if (type === 'text') {
            // 获取文本内容
            const res = await getFileContent(props.file.id)
            textContent.value = res.content
        } else if (type !== 'unsupported') {
            // 获取预览 URL
            previewUrl.value = await getPreviewUrl(props.file.id)
        }
    } catch (error: any) {
        ElMessage.error(error.message || '加载预览失败')
    } finally {
        loading.value = false
    }
}

function resetState() {
    previewUrl.value = ''
    textContent.value = ''
    imageScale.value = 1
    imageRotation.value = 0
}

function handleClose() {
    visible.value = false
}

function handleDownload() {
    if (props.file) {
        downloadFileStream(props.file.id)
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

// 图片操作
function onImageLoad() {
    // 图片加载完成
}

function zoomIn() {
    imageScale.value = Math.min(imageScale.value + 0.25, 5)
}

function zoomOut() {
    imageScale.value = Math.max(imageScale.value - 0.25, 0.25)
}

function resetZoom() {
    imageScale.value = 1
    imageRotation.value = 0
}

function rotateLeft() {
    imageRotation.value -= 90
}

function rotateRight() {
    imageRotation.value += 90
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

.preview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-right: 40px;

    .file-name {
        font-size: 16px;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 60%;
    }

    .header-actions {
        display: flex;
        gap: 8px;
    }
}

.preview-content {
    overflow: auto;
    min-height: 300px;
}

// 图片预览
.image-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    background-color: #f5f5f5;
    min-height: 400px;

    img {
        max-width: 100%;
        max-height: 60vh;
        object-fit: contain;
    }

    .image-controls {
        margin-top: 16px;
        padding: 8px;
        background: white;
        border-radius: 4px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
}

// 视频预览
.video-player {
    width: 100%;
    max-height: 70vh;
    background: #000;
}

// 音频预览
.audio-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    min-height: 200px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

    .audio-icon {
        color: white;
        margin-bottom: 24px;
    }

    .audio-player {
        width: 100%;
        max-width: 400px;
    }
}

// PDF 预览
.pdf-viewer {
    width: 100%;
    height: 100%;
    min-height: 70vh;
}

// 文本预览
.text-preview {
    padding: 16px;
    background: #1e1e1e;
    height: 100%;
    overflow: auto;

    pre {
        margin: 0;
        white-space: pre-wrap;
        word-wrap: break-word;

        code {
            color: #d4d4d4;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 14px;
            line-height: 1.5;
        }
    }
}

// Office 预览
.office-preview {
    height: 100%;

    .office-viewer {
        width: 100%;
        height: 100%;
        min-height: 70vh;
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
