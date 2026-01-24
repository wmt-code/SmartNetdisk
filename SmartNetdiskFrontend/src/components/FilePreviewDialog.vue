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
            <!-- 视频使用原生播放器 -->
            <template v-if="isVideo && mediaUrl">
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

            <!-- 其他文件使用 kkFileView 预览 -->
            <template v-else-if="previewUrl">
                <div class="kkfileview-preview">
                    <iframe
                        :src="previewUrl"
                        class="kkfileview-viewer"
                        frameborder="0"
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
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Headset } from '@element-plus/icons-vue'
import { getPreviewUrl, getStreamUrl, downloadFileStream, type FileInfo } from '@/api/file'

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
const mediaUrl = ref('')
const isFullscreen = ref(false)

// 文件名和大小
const fileName = computed(() => props.file?.fileName || '')
const fileSizeStr = computed(() => props.file?.fileSizeStr || '')

// 视频/音频扩展名（使用原生播放器）
const VIDEO_EXTENSIONS = ['mp4', 'webm', 'ogg', 'mov', 'avi', 'mkv', 'm4v', 'flv', 'wmv']
const AUDIO_EXTENSIONS = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a', 'wma', 'ape']

// 是否可以编辑（文本文件支持在线编辑）
const EDITABLE_EXTENSIONS = [
    'txt', 'md', 'markdown', 'log',
    'json', 'xml', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg', 'properties',
    'html', 'htm', 'css', 'scss', 'sass', 'less', 'js', 'ts', 'jsx', 'tsx', 'vue', 'svelte',
    'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'rb', 'php', 'swift', 'kt', 'kts',
    'scala', 'groovy', 'r', 'lua', 'pl', 'pm', 'sh', 'bash', 'zsh', 'fish', 'bat', 'cmd', 'ps1',
    'sql', 'gitignore', 'dockerignore', 'editorconfig', 'env'
]

const fileExt = computed(() => props.file?.fileExt?.toLowerCase() || '')
const canEdit = computed(() => EDITABLE_EXTENSIONS.includes(fileExt.value))
const isVideo = computed(() => VIDEO_EXTENSIONS.includes(fileExt.value))
const isAudio = computed(() => AUDIO_EXTENSIONS.includes(fileExt.value))
const useNativePlayer = computed(() => isVideo.value || isAudio.value)

// 对话框宽度（kkFileView 使用 90%）
const dialogWidth = computed(() => {
    if (isFullscreen.value) return '100%'
    return '90%'
})

// 内容区域样式
const contentStyle = computed(() => {
    if (isFullscreen.value) {
        return { height: 'calc(100vh - 120px)' }
    }
    // 使用 80vh 确保 kkFileView 有足够空间
    return { height: '80vh' }
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
        // 视频/音频使用原生播放器，直接获取流式 URL
        if (useNativePlayer.value) {
            mediaUrl.value = getStreamUrl(props.file.id)
        } else {
            // 其他文件使用 kkFileView 预览
            previewUrl.value = await getPreviewUrl(props.file.id)
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
    overflow: hidden;
    min-height: 300px;
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
            
            // 自定义播放器控件样式
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

// kkFileView 预览（用于文档等其他文件）
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
