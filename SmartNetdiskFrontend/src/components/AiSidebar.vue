<template>
  <div class="ai-sidebar-container">
    <!-- Mobile overlay -->
    <Transition name="fade">
      <div v-if="isOpen && isMobile" class="ai-overlay" @click="isOpen = false" />
    </Transition>

    <!-- Floating trigger button -->
    <div class="ai-trigger" :class="{ 'is-open': isOpen }" @click="isOpen = !isOpen">
      <el-icon :size="24" color="white">
        <MagicStick v-if="!isOpen" />
        <Close v-else />
      </el-icon>
    </div>

    <!-- AI Sidebar -->
    <Transition :name="isMobile ? 'slide-up' : 'slide-right'">
      <div v-if="isOpen" class="ai-sidebar" :class="{ 'mobile-fullscreen': isMobile }">
        <!-- Header -->
        <div class="ai-header">
          <div class="ai-header-title">
            <div class="ai-icon-wrapper">
              <el-icon :size="20"><MagicStick /></el-icon>
            </div>
            <span>AI 助手</span>
          </div>
          <div class="ai-header-actions">
            <el-tooltip content="历史会话" placement="bottom">
              <el-button class="header-btn" circle size="small" @click="showHistory = !showHistory">
                <el-icon><Clock /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="新建对话" placement="bottom">
              <el-button class="header-btn" circle size="small" @click="newSession">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button class="header-btn" circle size="small" @click="isOpen = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Mode switch: global / scoped -->
        <div class="ai-mode-bar">
          <div
            class="mode-tab" :class="{ active: mode === 'global' }"
            @click="mode = 'global'"
          >
            <el-icon><ChatDotRound /></el-icon>
            全局问答
          </div>
          <div
            class="mode-tab" :class="{ active: mode === 'scoped' }"
            @click="switchToScoped"
          >
            <el-icon><Folder /></el-icon>
            指定文件
          </div>
        </div>

        <!-- Scoped files bar -->
        <div v-if="mode === 'scoped'" class="scoped-bar">
          <div v-if="showFilePicker" class="file-picker">
            <div class="picker-header">
              <span>选择文件（已向量化）</span>
              <el-button link type="primary" size="small" @click="showFilePicker = false" :disabled="scopedFileIds.length === 0">
                确定 ({{ scopedFileIds.length }})
              </el-button>
            </div>
            <div v-if="loadingFiles" class="picker-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else class="picker-list">
              <div
                v-for="file in vectorizedFiles" :key="file.id"
                class="picker-item" :class="{ selected: scopedFileIds.includes(file.id) }"
                @click="toggleFile(file.id)"
              >
                <el-checkbox :model-value="scopedFileIds.includes(file.id)" />
                <span class="picker-name">{{ file.fileName }}</span>
              </div>
              <div v-if="vectorizedFiles.length === 0" class="picker-empty">
                暂无已向量化的文件
              </div>
            </div>
          </div>
          <div v-else class="scoped-info" @click="showFilePicker = true">
            <el-icon><Folder /></el-icon>
            <span>已选 {{ scopedFileIds.length }} 个文件</span>
            <el-button link size="small">修改</el-button>
          </div>
        </div>

        <!-- History panel -->
        <Transition name="slide-down">
          <div v-if="showHistory" class="history-panel">
            <div class="history-header">
              <span>历史会话</span>
              <el-button link type="danger" size="small" @click="clearAllSessions">清空</el-button>
            </div>
            <div class="history-list">
              <div
                v-for="(session, idx) in sessions" :key="session.id"
                class="history-item" :class="{ active: idx === currentSessionIdx }"
                @click="switchSession(idx)"
              >
                <el-icon><ChatDotRound /></el-icon>
                <span class="history-title">{{ session.title }}</span>
                <span class="history-time">{{ formatSessionTime(session.updatedAt) }}</span>
              </div>
              <div v-if="sessions.length === 0" class="history-empty">暂无历史会话</div>
            </div>
          </div>
        </Transition>

        <!-- Chat area -->
        <div ref="chatAreaRef" class="ai-chat-area">
          <TransitionGroup name="message-list">
            <div
              v-for="(msg, index) in currentMessages"
              :key="index"
              class="message-wrapper" :class="msg.role"
            >
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="32" :src="userStore.avatar || undefined">
                  {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
                </el-avatar>
                <div v-else class="ai-avatar">
                  <el-icon color="white" :size="16"><MagicStick /></el-icon>
                </div>
              </div>
              <div class="message-bubble" :class="msg.role">
                <div v-if="msg.role === 'ai'" class="message-content md-content" v-html="renderMarkdown(msg.content)" />
                <p v-else class="message-content">{{ msg.content }}</p>

                <!-- Search results in message -->
                <div v-if="msg.searchResults?.length" class="search-results">
                  <div
                    v-for="result in msg.searchResults" :key="result.fileId"
                    class="search-result-item" @click="goToFile(result.fileId)"
                  >
                    <el-icon class="result-icon"><Document /></el-icon>
                    <div class="result-info">
                      <span class="result-name">{{ result.fileName }}</span>
                      <span class="result-match">{{ result.matchedContent }}</span>
                    </div>
                    <span class="result-score">{{ Math.round((result.score || 0) * 100) }}%</span>
                  </div>
                </div>

                <!-- References -->
                <div v-if="msg.references?.length" class="message-references">
                  <div class="references-header">
                    <el-icon :size="12"><Document /></el-icon>
                    <span>引用来源</span>
                  </div>
                  <div class="references-list">
                    <span
                      v-for="ref in msg.references" :key="ref.fileId || ref.fileName"
                      class="reference-tag" @click="goToFile(ref.fileId)"
                    >{{ ref.fileName }}</span>
                  </div>
                </div>
              </div>
            </div>
          </TransitionGroup>

          <!-- Loading -->
          <div v-if="isLoading" class="message-wrapper ai">
            <div class="message-avatar">
              <div class="ai-avatar loading">
                <el-icon color="white" :size="16" class="spin"><Loading /></el-icon>
              </div>
            </div>
            <div class="message-bubble ai loading-bubble">
              <span class="loading-dot" /><span class="loading-dot" /><span class="loading-dot" />
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="ai-input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              :placeholder="mode === 'scoped' ? '基于选中的文件提问...' : '提问或搜索文件内容...'"
              :autosize="{ minRows: 1, maxRows: 4 }"
              type="textarea" resize="none"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-button
              class="send-btn" type="primary"
              :disabled="!inputText.trim() || isLoading"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  MagicStick, Close, ChatDotRound, Promotion, Loading,
  Document, Folder, Clock, Plus
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { semanticSearch, chat, type SearchResult, type FileReference } from '@/api/ai'
import { getFileList, type FileInfo } from '@/api/file'
import {
  listSessions, createSession as apiCreateSession,
  updateSession as apiUpdateSession, deleteAllSessions as apiDeleteAll,
  type ChatSessionData
} from '@/api/chatSession'
import { useIsMobile } from '@/composables'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const isMobile = useIsMobile()
const userStore = useUserStore()
const isOpen = ref(false)
const mode = ref<'global' | 'scoped'>('global')
const inputText = ref('')
const isLoading = ref(false)
const chatAreaRef = ref<HTMLElement>()
const showHistory = ref(false)

// === Message types ===
interface Message {
  role: 'user' | 'ai'
  content: string
  references?: { fileId?: number; fileName: string; matchedContent?: string }[]
  searchResults?: SearchResult[]
}

interface Session {
  id: string
  title: string
  mode: 'global' | 'scoped'
  messages: Message[]
  scopedFileIds: number[]
  updatedAt: number
}

// === Session management with PostgreSQL persistence ===
const sessions = ref<Session[]>([])
const currentSessionIdx = ref(0)
let saveTimer: ReturnType<typeof setTimeout> | null = null

function makeWelcomeMsg(sessionMode: string): Message {
  return {
    role: 'ai',
    content: sessionMode === 'scoped'
      ? '请选择文件，然后基于这些文件向我提问。'
      : '你好！我可以回答关于你文件的问题，也能帮你搜索文件。直接提问即可。'
  }
}

function dbToSession(data: ChatSessionData): Session {
  let msgs: Message[] = []
  try { msgs = JSON.parse(data.messages) } catch { /* */ }
  if (msgs.length === 0) msgs = [makeWelcomeMsg(data.mode)]
  let fileIds: number[] = []
  try { fileIds = JSON.parse(data.scopedFileIds || '[]') } catch { /* */ }
  return {
    id: String(data.id),
    title: data.title,
    mode: data.mode as 'global' | 'scoped',
    messages: msgs,
    scopedFileIds: fileIds,
    updatedAt: new Date(data.updateTime || data.createTime).getTime()
  }
}

async function loadSessionsFromDB() {
  try {
    const list = await listSessions()
    sessions.value = list.map(dbToSession)
  } catch { /* first load may fail if not logged in */ }
  if (sessions.value.length === 0) {
    await createNewSessionInDB('global')
  }
  if (currentSessionIdx.value >= sessions.value.length) {
    currentSessionIdx.value = 0
  }
}

async function createNewSessionInDB(sessionMode: 'global' | 'scoped' = 'global') {
  try {
    const welcomeMsg = [makeWelcomeMsg(sessionMode)]
    const data = await apiCreateSession({
      title: '新对话',
      mode: sessionMode,
      messages: JSON.stringify(welcomeMsg),
      scopedFileIds: '[]'
    })
    sessions.value.unshift(dbToSession(data))
    currentSessionIdx.value = 0
  } catch {
    // Fallback: create local-only session
    sessions.value.unshift({
      id: Date.now().toString(),
      title: '新对话',
      mode: sessionMode,
      messages: [makeWelcomeMsg(sessionMode)],
      scopedFileIds: [],
      updatedAt: Date.now()
    })
    currentSessionIdx.value = 0
  }
}

// Debounced save to DB
function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    const s = currentSession.value
    if (!s) return
    const numId = Number(s.id)
    if (isNaN(numId)) return // local-only session
    apiUpdateSession(numId, {
      title: s.title,
      messages: JSON.stringify(s.messages),
      scopedFileIds: JSON.stringify(s.scopedFileIds),
      mode: s.mode
    }).catch(() => { /* silent */ })
  }, 1000)
}

onMounted(async () => { await loadSessionsFromDB(); scrollToBottom() })

const currentSession = computed(() => sessions.value[currentSessionIdx.value] || sessions.value[0]!)
const currentMessages = computed(() => currentSession.value.messages)

async function newSession() {
  await createNewSessionInDB(mode.value)
  showHistory.value = false
  scopedFileIds.value = []
}

function switchSession(idx: number) {
  currentSessionIdx.value = idx
  mode.value = sessions.value[idx]!.mode
  scopedFileIds.value = [...(sessions.value[idx]!.scopedFileIds || [])]
  showHistory.value = false
  scrollToBottom()
}

async function clearAllSessions() {
  try { await apiDeleteAll() } catch { /* */ }
  sessions.value = []
  await createNewSessionInDB('global')
  scopedFileIds.value = []
}

function updateSessionTitle(msg: string) {
  const s = currentSession.value
  if (s.title === '新对话') {
    s.title = msg.length > 20 ? msg.substring(0, 20) + '...' : msg
  }
  s.updatedAt = Date.now()
}

function formatSessionTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0')
  }
  return (d.getMonth() + 1) + '/' + d.getDate()
}

// === Scoped files ===
const showFilePicker = ref(false)
const scopedFileIds = ref<number[]>([])
const vectorizedFiles = ref<FileInfo[]>([])
const loadingFiles = ref(false)

async function loadVectorizedFiles() {
  loadingFiles.value = true
  try {
    const res = await getFileList({ pageSize: 100 })
    vectorizedFiles.value = res.records.filter(f => f.isVectorized)
  } catch { /* silent */ }
  finally { loadingFiles.value = false }
}

function toggleFile(id: number) {
  const idx = scopedFileIds.value.indexOf(id)
  if (idx >= 0) scopedFileIds.value.splice(idx, 1)
  else scopedFileIds.value.push(id)
  // Sync to session
  currentSession.value.scopedFileIds = [...scopedFileIds.value]
}

function switchToScoped() {
  mode.value = 'scoped'
  loadVectorizedFiles()
  if (scopedFileIds.value.length === 0) showFilePicker.value = true
}

// === Chat ===
const scrollToBottom = () => {
  nextTick(() => {
    if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
  })
}

const sendMessage = async () => {
  if (!inputText.value.trim() || isLoading.value) return

  const userMessage = inputText.value.trim()
  const msgs = currentSession.value.messages
  msgs.push({ role: 'user', content: userMessage })
  updateSessionTitle(userMessage)
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    if (mode.value === 'scoped' && scopedFileIds.value.length === 0) {
      msgs.push({ role: 'ai', content: '请先选择文件后再提问。' })
    } else {
      // Build history
      const history = msgs
        .filter(m => m.role === 'user' || m.role === 'ai')
        .slice(-10)
        .map(m => ({
          role: m.role === 'user' ? 'user' as const : 'assistant' as const,
          content: m.content
        }))

      const chatParams: any = { question: userMessage, history }
      if (mode.value === 'scoped' && scopedFileIds.value.length > 0) {
        chatParams.fileIds = scopedFileIds.value
      }

      const response = await chat(chatParams)

      msgs.push({
        role: 'ai',
        content: response.answer,
        references: response.references?.map((ref: FileReference) => ({
          fileId: ref.fileId,
          fileName: ref.fileName,
          matchedContent: ref.matchedContent
        }))
      })
    }
  } catch (error) {
    console.error('AI 请求失败:', error)
    msgs.push({ role: 'ai', content: '抱歉，AI 服务暂时不可用。请稍后重试。' })
  } finally {
    isLoading.value = false
    scrollToBottom()
    scheduleSave()
  }
}

// Markdown rendering
marked.setOptions({ breaks: true, gfm: true })

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text) as string
  } catch {
    return text
  }
}

const goToFile = (fileId?: number) => {
  if (fileId) { router.push('/files'); isOpen.value = false }
}

// Open: scroll to bottom + escape to close
watch(isOpen, (open) => {
  if (open) {
    scrollToBottom()
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') isOpen.value = false }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }
})

defineExpose({
  open: () => { isOpen.value = true },
  close: () => { isOpen.value = false },
  toggle: () => { isOpen.value = !isOpen.value }
})
</script>

<style scoped lang="scss">
.ai-sidebar-container { position: relative; }

.ai-overlay {
  position: fixed; inset: 0;
  background: var(--overlay-bg);
  z-index: calc(var(--z-modal) - 1);
}

.ai-trigger {
  position: fixed; bottom: var(--space-lg); right: var(--space-lg);
  width: 56px; height: 56px; border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: var(--shadow-primary); z-index: var(--z-modal);
  transition: all var(--transition-base) var(--ease-spring);
  &:hover { transform: scale(1.1); }
  &.is-open { background: var(--color-error); box-shadow: 0 10px 40px rgba(239, 68, 68, 0.3); }
  @media (max-width: 767px) { bottom: var(--space-md); right: var(--space-md); width: 48px; height: 48px; }
}

.ai-sidebar {
  position: fixed; right: 0; top: 0; height: 100vh; width: 400px; max-width: 100vw;
  background: var(--color-surface, #fff);
  border-left: 1px solid var(--color-border);
  display: flex; flex-direction: column; z-index: var(--z-modal);
  box-shadow: var(--shadow-2xl);
  &.mobile-fullscreen { width: 100vw; border-left: none; }
}

// Header
.ai-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--color-border-light);
}
.ai-header-title {
  display: flex; align-items: center; gap: 8px;
  font-weight: 600; font-size: 1rem; color: var(--color-text);
}
.ai-icon-wrapper {
  width: 32px; height: 32px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex; align-items: center; justify-content: center; color: white;
}
.ai-header-actions { display: flex; gap: 4px; }
.header-btn {
  --el-button-bg-color: transparent; --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  color: var(--color-text-muted); &:hover { color: var(--color-text); }
}

// Mode bar
.ai-mode-bar {
  display: flex; border-bottom: 1px solid var(--color-border-light);
}
.mode-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 0; font-size: 13px; font-weight: 500; cursor: pointer;
  color: var(--color-text-muted); transition: all var(--transition-fast);
  border-bottom: 2px solid transparent;
  &:hover { color: var(--color-text); background: var(--sidebar-item-hover); }
  &.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
}

// Scoped files
.scoped-bar { border-bottom: 1px solid var(--color-border-light); }
.scoped-info {
  display: flex; align-items: center; gap: 6px; padding: 8px 14px;
  font-size: 13px; color: var(--color-primary); font-weight: 500; cursor: pointer;
  background: rgba(124, 58, 237, 0.04);
  &:hover { background: rgba(124, 58, 237, 0.08); }
}
.file-picker { max-height: 240px; display: flex; flex-direction: column; }
.picker-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 14px; font-size: 13px; font-weight: 600; color: var(--color-text);
  border-bottom: 1px solid var(--color-border-light);
}
.picker-loading { padding: 20px; text-align: center; color: var(--color-text-muted); }
.picker-list { overflow-y: auto; flex: 1; }
.picker-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 14px;
  cursor: pointer; font-size: 13px; transition: background var(--transition-fast);
  &:hover { background: var(--sidebar-item-hover); }
  &.selected { background: rgba(124, 58, 237, 0.06); color: var(--color-primary); }
  .picker-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.picker-empty { padding: 20px 14px; text-align: center; font-size: 13px; color: var(--color-text-muted); }

// History panel
.history-panel {
  border-bottom: 1px solid var(--color-border-light);
  max-height: 280px; display: flex; flex-direction: column;
  background: var(--color-surface-secondary, #f8f8f8);
}
.history-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 14px; font-size: 13px; font-weight: 600; color: var(--color-text);
}
.history-list { overflow-y: auto; flex: 1; }
.history-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 14px;
  cursor: pointer; font-size: 13px; transition: background var(--transition-fast);
  &:hover { background: var(--sidebar-item-hover); }
  &.active { background: rgba(124, 58, 237, 0.08); color: var(--color-primary); }
  .el-icon { flex-shrink: 0; color: var(--color-text-muted); }
  .history-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .history-time { flex-shrink: 0; font-size: 11px; color: var(--color-text-muted); }
}
.history-empty { padding: 20px; text-align: center; font-size: 13px; color: var(--color-text-muted); }

// Chat area
.ai-chat-area {
  flex: 1; overflow-y: auto; padding: 14px;
  display: flex; flex-direction: column; gap: 12px;
}
.message-wrapper {
  display: flex; gap: 8px; align-items: flex-start;
  &.user { flex-direction: row-reverse; }
}
.ai-avatar {
  width: 32px; height: 32px; border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex; align-items: center; justify-content: center;
  &.loading { animation: pulse 2s infinite; }
}
.message-bubble {
  max-width: 85%; padding: 10px 14px; border-radius: var(--radius-lg);
  animation: fadeInUp var(--transition-base) var(--ease-out);
  &.ai { background: var(--ai-message-bg); border-bottom-left-radius: 4px; }
  &.user { background: var(--user-message-bg); border-bottom-right-radius: 4px; color: white; }
}
.message-content {
  font-size: 14px; line-height: 1.6; color: var(--color-text);
  word-break: break-word; margin: 0;
  .user & { color: white; white-space: pre-wrap; }

  // Markdown rendered content
  &.md-content {
    :deep(p) { margin: 0 0 8px; &:last-child { margin-bottom: 0; } }
    :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
      margin: 12px 0 6px; font-weight: 600; line-height: 1.4;
      &:first-child { margin-top: 0; }
    }
    :deep(h1) { font-size: 1.2em; }
    :deep(h2) { font-size: 1.1em; }
    :deep(h3) { font-size: 1.05em; }
    :deep(strong) { font-weight: 600; }
    :deep(em) { font-style: italic; }
    :deep(ul), :deep(ol) {
      margin: 6px 0; padding-left: 1.5em;
      li { margin: 3px 0; }
    }
    :deep(code) {
      background: rgba(0, 0, 0, 0.06); padding: 1px 5px;
      border-radius: 4px; font-family: var(--font-mono, monospace);
      font-size: 0.9em;
    }
    :deep(pre) {
      background: #1e1e2e; color: #cdd6f4; padding: 12px;
      border-radius: 8px; overflow-x: auto; margin: 8px 0;
      font-size: 13px; line-height: 1.5;
      code { background: none; padding: 0; color: inherit; }
    }
    :deep(blockquote) {
      border-left: 3px solid var(--color-primary); margin: 8px 0;
      padding: 4px 12px; color: var(--color-text-secondary);
      background: rgba(124, 58, 237, 0.04); border-radius: 0 6px 6px 0;
    }
    :deep(a) { color: var(--color-primary); text-decoration: none; &:hover { text-decoration: underline; } }
    :deep(hr) { border: none; border-top: 1px solid var(--color-border-light); margin: 10px 0; }
    :deep(table) {
      width: 100%; border-collapse: collapse; margin: 8px 0; font-size: 13px;
      th, td { border: 1px solid var(--color-border); padding: 6px 10px; text-align: left; }
      th { background: var(--color-surface-secondary, #f5f5f5); font-weight: 600; }
    }
  }
}

// Search results
.search-results { margin-top: 8px; display: flex; flex-direction: column; gap: 4px; }
.search-result-item {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  background: var(--color-surface); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--transition-fast);
  &:hover { background: var(--sidebar-item-hover); }
}
.result-icon { color: var(--color-document); flex-shrink: 0; }
.result-info { flex: 1; min-width: 0; }
.result-name { display: block; font-size: 13px; font-weight: 500; color: var(--color-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result-match { display: block; font-size: 12px; color: var(--color-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result-score { font-size: 12px; font-weight: 600; color: var(--color-primary); flex-shrink: 0; }

// References
.message-references { margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--color-border-light); }
.references-header { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--color-text-muted); margin-bottom: 4px; }
.references-list { display: flex; flex-wrap: wrap; gap: 4px; }
.reference-tag {
  padding: 2px 8px; background: var(--color-primary-100, rgba(124,58,237,0.1));
  color: var(--color-primary); border-radius: var(--radius-full); font-size: 12px;
  cursor: pointer; transition: all var(--transition-fast);
  &:hover { background: var(--color-primary); color: white; }
}

// Loading
.loading-bubble { display: flex; align-items: center; gap: 4px; padding: 14px !important; }
.loading-dot {
  width: 7px; height: 7px; background: var(--color-primary);
  border-radius: var(--radius-full); animation: bounce 1.4s infinite ease-in-out both;
  &:nth-child(1) { animation-delay: -0.32s; }
  &:nth-child(2) { animation-delay: -0.16s; }
}

// Input
.ai-input-area { padding: 12px 14px; border-top: 1px solid var(--color-border-light); background: var(--color-surface); }
.input-wrapper {
  display: flex; gap: 8px; align-items: flex-end;
  :deep(.el-textarea__inner) {
    background: var(--input-bg); border-color: var(--input-border);
    border-radius: var(--radius-lg); padding: 8px 12px; font-size: 14px; resize: none;
    &:focus { border-color: var(--color-primary); }
  }
}
.send-btn { flex-shrink: 0; width: 38px; height: 38px; border-radius: var(--radius-lg); }

// Animations
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

// Transitions
.fade-enter-active, .fade-leave-active { transition: opacity var(--transition-base); }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-right-enter-active, .slide-right-leave-active { transition: transform var(--transition-base) var(--ease-out); }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
.slide-up-enter-active, .slide-up-leave-active { transition: transform var(--transition-base) var(--ease-out); }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); }
.slide-down-enter-active, .slide-down-leave-active { transition: all var(--transition-base); overflow: hidden; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; max-height: 0; }
.slide-down-enter-to, .slide-down-leave-from { opacity: 1; max-height: 300px; }
.message-list-enter-active, .message-list-leave-active { transition: all var(--transition-base); }
.message-list-enter-from { opacity: 0; transform: translateY(10px); }
.message-list-leave-to { opacity: 0; }

// Mobile
@media (max-width: 767px) {
  .ai-sidebar.mobile-fullscreen { top: 0; left: 0; right: 0; bottom: 0; width: 100%; height: 100%; }
  .ai-input-area { padding-bottom: calc(12px + env(safe-area-inset-bottom, 20px)); }
  .input-wrapper :deep(.el-textarea__inner) { font-size: 16px; }
  .send-btn { width: 44px; height: 44px; }
  .message-bubble { max-width: 90%; }
}
</style>
