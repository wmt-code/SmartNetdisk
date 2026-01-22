<template>
  <div>
    <!-- 触发按钮 -->
    <div
      class="ai-trigger"
      @click="isOpen = !isOpen"
    >
      <el-icon :size="24" color="white">
        <MagicStick v-if="!isOpen" />
        <Close v-else />
      </el-icon>
    </div>

    <!-- AI 侧边栏 -->
    <Transition name="slide">
      <div
        v-if="isOpen"
        class="ai-sidebar fixed right-0 top-0 h-screen w-96 glass-panel z-40 flex flex-col"
      >
        <!-- 头部 -->
        <div class="header flex items-center justify-between p-4 border-b border-gray-100">
          <div class="flex items-center gap-2">
            <el-icon :size="24" color="#7C3AED"><MagicStick /></el-icon>
            <span class="font-bold text-lg text-gray-800">AI 智能助手</span>
          </div>
          <el-button circle size="small" @click="isOpen = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- 功能切换 -->
        <div class="p-4">
          <el-radio-group v-model="mode" class="w-full">
            <el-radio-button value="chat" class="flex-1">
              <el-icon><ChatDotRound /></el-icon>
              智能问答
            </el-radio-button>
            <el-radio-button value="search" class="flex-1">
              <el-icon><Search /></el-icon>
              语义搜索
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- 聊天区域 -->
        <div class="chat-area flex-1 overflow-auto p-4 space-y-4">
          <div v-for="(msg, index) in messages" :key="index" 
               :class="['message', msg.role === 'user' ? 'user-msg' : 'ai-msg']">
            <div class="avatar">
              <el-avatar v-if="msg.role === 'user'" :size="32" 
                         src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <div v-else class="ai-avatar w-8 h-8 rounded-full bg-gradient-to-br from-[#7C3AED] to-[#A78BFA] 
                              flex items-center justify-center">
                <el-icon color="white" :size="16"><MagicStick /></el-icon>
              </div>
            </div>
            <div class="content glass-card p-3 max-w-xs">
              <p class="text-sm text-gray-700">{{ msg.content }}</p>
              <!-- 引用的文件 -->
              <div v-if="msg.references?.length" class="mt-2 pt-2 border-t border-gray-100">
                <p class="text-xs text-gray-500 mb-1">引用文件:</p>
                <div v-for="ref in msg.references" :key="ref" 
                     class="text-xs text-[#7C3AED] cursor-pointer hover:underline">
                  📄 {{ ref }}
                </div>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="message ai-msg">
            <div class="ai-avatar w-8 h-8 rounded-full bg-gradient-to-br from-[#7C3AED] to-[#A78BFA] 
                        flex items-center justify-center">
              <el-icon color="white" :size="16" class="animate-spin"><Loading /></el-icon>
            </div>
            <div class="content glass-card p-3">
              <p class="text-sm text-gray-500">思考中...</p>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area p-4 border-t border-gray-100">
          <div class="flex gap-2">
            <el-input
              v-model="inputText"
              :placeholder="mode === 'chat' ? '输入问题，如：这个项目的主要功能是什么？' : '描述你要查找的文件内容...'"
              @keyup.enter="sendMessage"
            />
            <el-button type="primary" :disabled="!inputText.trim() || isLoading" @click="sendMessage">
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
          <p class="text-xs text-gray-400 mt-2">
            {{ mode === 'chat' ? 'AI 将基于您的文件内容回答问题' : '使用自然语言描述，AI 将帮您找到相关文件' }}
          </p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Close, ChatDotRound, Search, Promotion, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { semanticSearch, chat, type SearchResult, type FileReference } from '@/api/ai'

const router = useRouter()
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

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatAreaRef.value) {
      chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
    }
  })
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim() || isLoading.value) return

  const userMessage = inputText.value.trim()
  messages.value.push({ role: 'user', content: userMessage })
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    if (mode.value === 'search') {
      // 语义搜索
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
      // 智能问答
      const history = messages.value
        .filter(m => m.role === 'user' || m.role === 'ai')
        .slice(-10) // 保留最近10条历史
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

// 清空对话
const clearChat = () => {
  messages.value = [{
    role: 'ai',
    content: '对话已清空。有什么可以帮您的吗？'
  }]
}

// 跳转到文件
const goToFile = (fileId?: number) => {
  if (fileId) {
    // TODO: 实现跳转到具体文件
    router.push('/files')
  }
}

// 暴露方法供外部调用
defineExpose({
  open: () => { isOpen.value = true },
  close: () => { isOpen.value = false },
  toggle: () => { isOpen.value = !isOpen.value }
})
</script>

<style scoped>
.fixed { position: fixed; }
.bottom-6 { bottom: 1.5rem; }
.right-6 { right: 1.5rem; }
.right-0 { right: 0; }
.top-0 { top: 0; }
.w-14 { width: 3.5rem; }
.h-14 { height: 3.5rem; }
.w-8 { width: 2rem; }
.h-8 { height: 2rem; }
.w-96 { width: 24rem; }
.h-screen { height: 100vh; }
.rounded-full { border-radius: 9999px; }
.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.justify-center { justify-content: center; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 0.5rem; }
.p-3 { padding: 0.75rem; }
.p-4 { padding: 1rem; }
.mt-2 { margin-top: 0.5rem; }
.pt-2 { padding-top: 0.5rem; }
.mb-1 { margin-bottom: 0.25rem; }
.max-w-xs { max-width: 20rem; }
.overflow-auto { overflow: auto; }
.space-y-4 > * + * { margin-top: 1rem; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-lg { font-size: 1.125rem; }
.font-bold { font-weight: 700; }
.cursor-pointer { cursor: pointer; }
.shadow-xl { box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04); }
.z-40 { z-index: 40; }
.z-50 { z-index: 50; }

/* AI 浮动触发按钮 */
.ai-trigger {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, #7C3AED 0%, #A78BFA 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 20px 25px -5px rgba(124, 58, 237, 0.3), 0 10px 10px -5px rgba(124, 58, 237, 0.2);
  z-index: 50;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ai-trigger:hover {
  transform: scale(1.1);
  box-shadow: 0 25px 30px -5px rgba(124, 58, 237, 0.4), 0 15px 15px -5px rgba(124, 58, 237, 0.3);
}

/* AI 聊天头像 */
.ai-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, #7C3AED 0%, #A78BFA 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.w-full { width: 100%; }

.transition-transform { transition: transform 0.2s ease; }
.hover\:scale-105:hover { transform: scale(1.05); }
.hover\:underline:hover { text-decoration: underline; }

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.animate-spin { animation: spin 1s linear infinite; }

.message {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}
.user-msg {
  flex-direction: row-reverse;
}
.user-msg .content {
  background: linear-gradient(135deg, #7C3AED 0%, #A78BFA 100%);
}
.user-msg .content p {
  color: white;
}

/* Slide transition */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
