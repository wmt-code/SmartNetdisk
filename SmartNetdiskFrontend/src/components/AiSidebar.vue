<template>
  <div class="ai-sidebar-container">
    <!-- Mobile overlay -->
    <Transition name="fade">
      <div
        v-if="isOpen && isMobile"
        class="ai-overlay"
        @click="isOpen = false"
      />
    </Transition>

    <!-- Floating trigger button -->
    <div
      class="ai-trigger hover-glow"
      :class="{ 'is-open': isOpen }"
      @click="isOpen = !isOpen"
    >
      <el-icon :size="24" color="white">
        <MagicStick v-if="!isOpen" />
        <Close v-else />
      </el-icon>
    </div>

    <!-- AI Sidebar -->
    <Transition :name="isMobile ? 'slide-up' : 'slide-right'">
      <div
        v-if="isOpen"
        class="ai-sidebar"
        :class="{ 'mobile-fullscreen': isMobile }"
      >
        <!-- Header -->
        <div class="ai-header">
          <div class="ai-header-title">
            <div class="ai-icon-wrapper">
              <el-icon :size="20"><MagicStick /></el-icon>
            </div>
            <span>AI 智能助手</span>
          </div>
          <div class="ai-header-actions">
            <el-tooltip content="清空对话" placement="bottom">
              <el-button
                class="header-btn"
                circle
                size="small"
                @click="clearChat"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button
              class="header-btn"
              circle
              size="small"
              @click="isOpen = false"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Mode toggle -->
        <div class="ai-mode-toggle">
          <el-radio-group v-model="mode" class="mode-group">
            <el-radio-button value="chat" class="mode-btn">
              <el-icon><ChatDotRound /></el-icon>
              <span>智能问答</span>
            </el-radio-button>
            <el-radio-button value="search" class="mode-btn">
              <el-icon><Search /></el-icon>
              <span>语义搜索</span>
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- Chat area -->
        <div ref="chatAreaRef" class="ai-chat-area">
          <TransitionGroup name="message-list">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="msg.role"
            >
              <div class="message-avatar">
                <el-avatar
                  v-if="msg.role === 'user'"
                  :size="36"
                  src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
                />
                <div v-else class="ai-avatar">
                  <el-icon color="white" :size="18"><MagicStick /></el-icon>
                </div>
              </div>

              <div class="message-bubble" :class="msg.role">
                <p class="message-content">{{ msg.content }}</p>

                <!-- Search results -->
                <div v-if="msg.searchResults?.length" class="search-results">
                  <div
                    v-for="result in msg.searchResults"
                    :key="result.fileId"
                    class="search-result-item"
                    @click="goToFile(result.fileId)"
                  >
                    <el-icon class="result-icon"><Document /></el-icon>
                    <div class="result-info">
                      <span class="result-name">{{ result.fileName }}</span>
                      <span class="result-match">{{ result.matchedContent }}</span>
                    </div>
                    <span class="result-score">{{ Math.round((result.score || 0) * 100) }}%</span>
                  </div>
                </div>

                <!-- Reference files -->
                <div v-if="msg.references?.length" class="message-references">
                  <div class="references-header">
                    <el-icon :size="12"><Document /></el-icon>
                    <span>引用来源</span>
                  </div>
                  <div class="references-list">
                    <span
                      v-for="ref in msg.references"
                      :key="ref.fileId || ref.fileName"
                      class="reference-tag"
                      @click="goToFile(ref.fileId)"
                    >
                      {{ ref.fileName }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </TransitionGroup>

          <!-- Loading state -->
          <div v-if="isLoading" class="message-wrapper ai">
            <div class="message-avatar">
              <div class="ai-avatar loading">
                <el-icon color="white" :size="18" class="spin"><Loading /></el-icon>
              </div>
            </div>
            <div class="message-bubble ai loading-bubble">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
          </div>
        </div>

        <!-- Input area -->
        <div class="ai-input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              :placeholder="mode === 'chat' ? '输入问题，如：这个项目的主要功能是什么？' : '描述你要查找的文件内容...'"
              :autosize="{ minRows: 1, maxRows: 4 }"
              type="textarea"
              resize="none"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-button
              class="send-btn"
              type="primary"
              :disabled="!inputText.trim() || isLoading"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
          <p class="input-hint">
            {{ mode === 'chat' ? 'AI 将基于您的文件内容回答问题' : '使用自然语言描述，AI 将帮您找到相关文件' }}
          </p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  MagicStick, Close, ChatDotRound, Search, Promotion, Loading,
  Refresh, Document
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { semanticSearch, chat, type SearchResult, type FileReference } from '@/api/ai'
import { useIsMobile } from '@/composables'

const router = useRouter()
const isMobile = useIsMobile()
const isOpen = ref(false)
const mode = ref<'chat' | 'search'>('chat')
const inputText = ref('')
const isLoading = ref(false)
const chatAreaRef = ref<HTMLElement>()

interface Message {
  role: 'user' | 'ai'
  content: string
  references?: { fileId?: number; fileName: string; matchedContent?: string }[]
  searchResults?: SearchResult[]
}

const messages = ref<Message[]>([
  {
    role: 'ai',
    content: '你好！我是 SmartNetdisk 的 AI 助手。我可以帮你分析文档内容、回答问题，或者通过语义搜索找到你需要的文件。请问有什么可以帮您的吗？'
  }
])

// Scroll to bottom
const scrollToBottom = () => {
  nextTick(() => {
    if (chatAreaRef.value) {
      chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
    }
  })
}

// Send message
const sendMessage = async () => {
  if (!inputText.value.trim() || isLoading.value) return

  const userMessage = inputText.value.trim()
  messages.value.push({ role: 'user', content: userMessage })
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    if (mode.value === 'search') {
      const results = await semanticSearch(userMessage, 10)
      if (results.length > 0) {
        messages.value.push({
          role: 'ai',
          content: `找到 ${results.length} 个与您描述相关的文件：`,
          searchResults: results
        })
      } else {
        messages.value.push({
          role: 'ai',
          content: '抱歉，没有找到与您描述相关的文件。请尝试使用不同的关键词或描述方式。'
        })
      }
    } else {
      const history = messages.value
        .filter(m => m.role === 'user' || m.role === 'ai')
        .slice(-10)
        .map(m => ({
          role: m.role === 'user' ? 'user' as const : 'assistant' as const,
          content: m.content
        }))

      const response = await chat({ question: userMessage, history })

      messages.value.push({
        role: 'ai',
        content: response.answer,
        references: response.references.map((ref: FileReference) => ({
          fileId: ref.fileId,
          fileName: ref.fileName,
          matchedContent: ref.matchedContent
        }))
      })
    }
  } catch (error) {
    console.error('AI 请求失败:', error)
    messages.value.push({
      role: 'ai',
      content: '抱歉，AI 服务暂时不可用。请稍后重试。'
    })
    ElMessage.error('AI 服务请求失败')
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

// Clear chat
const clearChat = () => {
  messages.value = [{
    role: 'ai',
    content: '对话已清空。有什么可以帮您的吗？'
  }]
}

// Go to file
const goToFile = (fileId?: number) => {
  if (fileId) {
    router.push('/files')
    isOpen.value = false
  }
}

// Close on escape key
watch(isOpen, (open) => {
  if (open) {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        isOpen.value = false
      }
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }
})

// Expose methods
defineExpose({
  open: () => { isOpen.value = true },
  close: () => { isOpen.value = false },
  toggle: () => { isOpen.value = !isOpen.value }
})
</script>

<style scoped lang="scss">
.ai-sidebar-container {
  position: relative;
}

// Overlay for mobile
.ai-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay-bg);
  z-index: calc(var(--z-modal) - 1);
}

// Floating trigger button
.ai-trigger {
  position: fixed;
  bottom: var(--space-lg);
  right: var(--space-lg);
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-primary);
  z-index: var(--z-modal);
  transition: all var(--transition-base) var(--ease-spring);

  &:hover {
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }

  &.is-open {
    background: var(--color-error);
    box-shadow: 0 10px 40px rgba(239, 68, 68, 0.3);
  }

  @media (max-width: 767px) {
    bottom: var(--space-md);
    right: var(--space-md);
    width: 48px;
    height: 48px;
  }
}

// Sidebar
.ai-sidebar {
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 400px;
  max-width: 100vw;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-left: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  z-index: var(--z-modal);
  box-shadow: var(--shadow-2xl);

  &.mobile-fullscreen {
    width: 100vw;
    height: 100vh;
    border-left: none;
    border-radius: 0;
  }
}

// Header
.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.ai-header-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-weight: 600;
  font-size: 1.125rem;
  color: var(--color-text);
}

.ai-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.ai-header-actions {
  display: flex;
  gap: var(--space-xs);
}

.header-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  color: var(--color-text-muted);

  &:hover {
    color: var(--color-text);
  }
}

// Mode toggle
.ai-mode-toggle {
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border-light);
}

.mode-group {
  width: 100%;
  display: flex;

  :deep(.el-radio-button) {
    flex: 1;

    .el-radio-button__inner {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-xs);
      background: var(--color-surface);
      border-color: var(--color-border);
      color: var(--color-text-secondary);
      transition: all var(--transition-fast);
    }

    &.is-active .el-radio-button__inner {
      background: var(--color-primary);
      border-color: var(--color-primary);
      color: white;
    }
  }
}

// Chat area
.ai-chat-area {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

// Message
.message-wrapper {
  display: flex;
  gap: var(--space-sm);
  align-items: flex-start;

  &.user {
    flex-direction: row-reverse;
  }
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;

  &.loading {
    animation: pulse 2s infinite;
  }
}

.message-bubble {
  max-width: 85%;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-lg);
  animation: fadeInUp var(--transition-base) var(--ease-out);

  &.ai {
    background: var(--ai-message-bg);
    border-bottom-left-radius: var(--radius-sm);
  }

  &.user {
    background: var(--user-message-bg);
    border-bottom-right-radius: var(--radius-sm);
    color: white;
  }
}

.message-content {
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;

  .user & {
    color: white;
  }
}

// Search results
.search-results {
  margin-top: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  background: var(--color-surface);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--sidebar-item-hover);
    transform: translateX(4px);
  }
}

.result-icon {
  color: var(--color-document);
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-name {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-match {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-score {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-primary);
  flex-shrink: 0;
}

// References
.message-references {
  margin-top: var(--space-sm);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border-light);
}

.references-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-bottom: var(--space-xs);
}

.references-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.reference-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background: var(--color-primary-100);
  color: var(--color-primary);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-primary);
    color: white;
  }
}

// Loading bubble
.loading-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: var(--space-md) !important;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: var(--color-primary);
  border-radius: var(--radius-full);
  animation: bounce 1.4s infinite ease-in-out both;

  &:nth-child(1) { animation-delay: -0.32s; }
  &:nth-child(2) { animation-delay: -0.16s; }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

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

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// Input area
.ai-input-area {
  padding: var(--space-md);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-surface);
}

.input-wrapper {
  display: flex;
  gap: var(--space-sm);
  align-items: flex-end;

  :deep(.el-textarea__inner) {
    background: var(--input-bg);
    border-color: var(--input-border);
    border-radius: var(--radius-lg);
    padding: var(--space-sm) var(--space-md);
    font-size: 0.9375rem;
    resize: none;

    &:focus {
      border-color: var(--color-primary);
    }
  }
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
}

.input-hint {
  margin-top: var(--space-xs);
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

// Transitions
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform var(--transition-base) var(--ease-out);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform var(--transition-base) var(--ease-out);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.message-list-enter-active,
.message-list-leave-active {
  transition: all var(--transition-base);
}

.message-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.message-list-leave-to {
  opacity: 0;
}

// Mobile adjustments
@media (max-width: 767px) {
  .ai-header {
    padding: var(--space-sm) var(--space-md);
  }

  .ai-header-title {
    font-size: 1rem;
  }

  .ai-icon-wrapper {
    width: 32px;
    height: 32px;
  }

  .ai-mode-toggle {
    padding: var(--space-sm);
  }

  .ai-chat-area {
    padding: var(--space-sm);
  }

  .message-bubble {
    max-width: 90%;
  }

  .ai-input-area {
    padding: var(--space-sm);
    padding-bottom: calc(var(--space-sm) + env(safe-area-inset-bottom, 0));
  }
}
</style>
