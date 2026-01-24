<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="90%"
        :fullscreen="isFullscreen"
        class="file-editor-dialog"
        destroy-on-close
        :close-on-click-modal="false"
        @close="handleClose"
    >
        <template #header>
            <div class="editor-header">
                <div class="header-left">
                    <span class="file-name">{{ fileName }}</span>
                    <el-tag v-if="isModified" type="warning" size="small">未保存</el-tag>
                </div>
                <div class="header-actions">
                    <el-button
                        type="primary"
                        size="small"
                        :loading="saving"
                        :disabled="!isModified"
                        @click="handleSave"
                    >
                        保存
                    </el-button>
                    <el-button size="small" @click="handleFormat">
                        格式化
                    </el-button>
                    <el-select v-model="theme" size="small" style="width: 120px">
                        <el-option label="深色主题" value="vs-dark" />
                        <el-option label="浅色主题" value="vs" />
                        <el-option label="高对比度" value="hc-black" />
                    </el-select>
                    <el-button
                        :icon="isFullscreen ? 'Minus' : 'FullScreen'"
                        size="small"
                        circle
                        @click="toggleFullscreen"
                    />
                </div>
            </div>
        </template>

        <div v-loading="loading" class="editor-container" :style="containerStyle">
            <vue-monaco-editor
                v-if="visible && !loading"
                v-model:value="content"
                :language="language"
                :theme="theme"
                :options="editorOptions"
                @mount="handleEditorMount"
                @change="handleContentChange"
            />
        </div>

        <template #footer>
            <div class="editor-footer">
                <div class="footer-info">
                    <span>{{ language.toUpperCase() }}</span>
                    <span>|</span>
                    <span>{{ lineCount }} 行</span>
                    <span>|</span>
                    <span>{{ formatSize(content.length) }}</span>
                </div>
                <div class="footer-actions">
                    <el-button @click="handleClose">取消</el-button>
                    <el-button
                        type="primary"
                        :loading="saving"
                        :disabled="!isModified"
                        @click="handleSave"
                    >
                        保存
                    </el-button>
                </div>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { getFileContent, saveFileContent, type FileInfo } from '@/api/file'

const props = defineProps<{
    modelValue: boolean
    file: FileInfo | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'saved', file: FileInfo): void
}>()

const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const content = ref('')
const originalContent = ref('')
const isFullscreen = ref(false)
const theme = ref('vs-dark')
const editorRef = shallowRef<any>(null)

// 文件信息
const fileName = computed(() => props.file?.fileName || '')
const fileExt = computed(() => props.file?.fileExt?.toLowerCase() || '')
const dialogTitle = computed(() => `编辑 - ${fileName.value}`)

// 是否已修改
const isModified = computed(() => content.value !== originalContent.value)

// 行数
const lineCount = computed(() => content.value.split('\n').length)

// 编辑器选项
const editorOptions = computed(() => ({
    minimap: { enabled: true },
    fontSize: 14,
    lineNumbers: 'on' as const,
    wordWrap: 'on' as const,
    automaticLayout: true,
    scrollBeyondLastLine: false,
    tabSize: 4,
    insertSpaces: true,
    renderWhitespace: 'selection' as const,
    quickSuggestions: true,
    folding: true,
    foldingStrategy: 'indentation' as const,
    formatOnPaste: true,
    formatOnType: true
}))

// 容器样式
const containerStyle = computed(() => ({
    height: isFullscreen.value ? 'calc(100vh - 160px)' : '65vh'
}))

// 语言映射
const languageMap: Record<string, string> = {
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    py: 'python',
    rb: 'ruby',
    java: 'java',
    go: 'go',
    rs: 'rust',
    c: 'c',
    cpp: 'cpp',
    h: 'c',
    hpp: 'cpp',
    cs: 'csharp',
    php: 'php',
    swift: 'swift',
    kt: 'kotlin',
    kts: 'kotlin',
    scala: 'scala',
    groovy: 'groovy',
    r: 'r',
    lua: 'lua',
    pl: 'perl',
    pm: 'perl',
    sh: 'shell',
    bash: 'shell',
    zsh: 'shell',
    fish: 'shell',
    bat: 'bat',
    cmd: 'bat',
    ps1: 'powershell',
    sql: 'sql',
    html: 'html',
    htm: 'html',
    css: 'css',
    scss: 'scss',
    sass: 'scss',
    less: 'less',
    json: 'json',
    xml: 'xml',
    yml: 'yaml',
    yaml: 'yaml',
    toml: 'toml',
    ini: 'ini',
    conf: 'ini',
    cfg: 'ini',
    properties: 'properties',
    md: 'markdown',
    markdown: 'markdown',
    txt: 'plaintext',
    log: 'plaintext',
    vue: 'vue',
    svelte: 'svelte',
    gitignore: 'plaintext',
    dockerignore: 'plaintext',
    editorconfig: 'ini',
    env: 'plaintext'
}

// 获取语言
const language = computed(() => {
    return languageMap[fileExt.value] || 'plaintext'
})

// 监听文件变化
watch(
    () => props.file,
    async (file) => {
        if (file && visible.value) {
            await loadContent()
        }
    }
)

watch(visible, async (val) => {
    if (val && props.file) {
        await loadContent()
    } else {
        content.value = ''
        originalContent.value = ''
    }
})

// 加载文件内容
async function loadContent() {
    if (!props.file) return

    loading.value = true
    try {
        const res = await getFileContent(props.file.id)
        content.value = res.content
        originalContent.value = res.content
    } catch (error: any) {
        ElMessage.error(error.message || '加载文件内容失败')
        visible.value = false
    } finally {
        loading.value = false
    }
}

// 编辑器挂载
function handleEditorMount(editor: any) {
    editorRef.value = editor

    // 添加快捷键
    editor.addCommand(
        // Ctrl+S 保存
        // monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS,
        2048 | 49, // CtrlCmd + S
        () => {
            if (isModified.value) {
                handleSave()
            }
        }
    )
}

// 内容变化
function handleContentChange() {
    // 可以在这里添加自动保存逻辑
}

// 保存
async function handleSave() {
    if (!props.file || !isModified.value) return

    saving.value = true
    try {
        await saveFileContent(props.file.id, content.value)
        originalContent.value = content.value
        ElMessage.success('保存成功')
        emit('saved', props.file)
    } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
    } finally {
        saving.value = false
    }
}

// 格式化
function handleFormat() {
    if (editorRef.value) {
        editorRef.value.getAction('editor.action.formatDocument')?.run()
    }
}

// 关闭
async function handleClose() {
    if (isModified.value) {
        try {
            await ElMessageBox.confirm(
                '文件已修改但未保存，确定要关闭吗？',
                '提示',
                {
                    confirmButtonText: '保存并关闭',
                    cancelButtonText: '不保存',
                    distinguishCancelAndClose: true,
                    type: 'warning'
                }
            )
            // 点击"保存并关闭"
            await handleSave()
            visible.value = false
        } catch (action) {
            if (action === 'cancel') {
                // 点击"不保存"
                visible.value = false
            }
            // 点击关闭按钮，不做任何操作
        }
    } else {
        visible.value = false
    }
}

// 全屏切换
function toggleFullscreen() {
    isFullscreen.value = !isFullscreen.value
}

// 格式化文件大小
function formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
</script>

<style lang="scss" scoped>
.file-editor-dialog {
    :deep(.el-dialog__header) {
        padding: 12px 20px;
        border-bottom: 1px solid var(--el-border-color-lighter);
        margin-right: 0;
    }

    :deep(.el-dialog__body) {
        padding: 0;
    }

    :deep(.el-dialog__footer) {
        padding: 12px 20px;
        border-top: 1px solid var(--el-border-color-lighter);
    }
}

.editor-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-right: 40px;

    .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .file-name {
            font-size: 15px;
            font-weight: 500;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 400px;
        }
    }

    .header-actions {
        display: flex;
        align-items: center;
        gap: 8px;
    }
}

.editor-container {
    width: 100%;
    background: #1e1e1e;
}

.editor-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .footer-info {
        display: flex;
        align-items: center;
        gap: 12px;
        color: #909399;
        font-size: 13px;
    }

    .footer-actions {
        display: flex;
        gap: 8px;
    }
}
</style>
