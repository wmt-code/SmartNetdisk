<template>
  <Teleport to="body">
    <Transition name="context-menu">
      <div
        v-show="visible"
        ref="menuRef"
        class="context-menu"
        :style="menuStyle"
        @click.stop
      >
        <div v-if="target?.fileType === 'folder'" class="menu-item" @click="emit('action', 'open')">
          <el-icon><FolderOpened /></el-icon>
          <span>打开</span>
        </div>
        <div v-if="target?.fileType !== 'folder'" class="menu-item" @click="emit('action', 'preview')">
          <el-icon><View /></el-icon>
          <span>预览</span>
        </div>
        <div v-if="isEditable" class="menu-item" @click="emit('action', 'edit')">
          <el-icon><EditPen /></el-icon>
          <span>编辑</span>
        </div>
        <div v-if="target?.fileType !== 'folder'" class="menu-item" @click="emit('action', 'download')">
          <el-icon><Download /></el-icon>
          <span>下载</span>
        </div>
        <div v-if="canVectorize" class="menu-item" @click="emit('action', 'vectorize')">
          <el-icon><MagicStick /></el-icon>
          <span>智能分析</span>
        </div>
        <div v-if="canSummarize" class="menu-item" @click="emit('action', 'summary')">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 摘要</span>
        </div>
        <div class="menu-item" @click="emit('action', 'share')">
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item" @click="emit('action', 'move')">
          <el-icon><Rank /></el-icon>
          <span>移动</span>
        </div>
        <div class="menu-item" @click="emit('action', 'copy')">
          <el-icon><DocumentCopy /></el-icon>
          <span>复制</span>
        </div>
        <div class="menu-item" @click="emit('action', 'rename')">
          <el-icon><Edit /></el-icon>
          <span>重命名</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item danger" @click="emit('action', 'delete')">
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import {
  FolderOpened, View, EditPen, Download, MagicStick,
  Share, Rank, DocumentCopy, Edit, Delete, ChatDotRound
} from '@element-plus/icons-vue'
import type { FileInfo } from '@/api/file'

const props = defineProps<{
  visible: boolean
  position: { x: number; y: number }
  target: FileInfo | null
}>()

const emit = defineEmits<{
  (e: 'action', action: string): void
}>()

const menuRef = ref<HTMLElement>()
const adjustedPos = ref({ x: 0, y: 0 })

const menuStyle = computed(() => ({
  left: adjustedPos.value.x + 'px',
  top: adjustedPos.value.y + 'px'
}))

// 菜单显示后根据实际 DOM 尺寸调整位置
watch(() => props.visible, async (val) => {
  if (val) {
    // 先放到点击位置
    adjustedPos.value = { ...props.position }
    await nextTick()
    if (!menuRef.value) return
    const rect = menuRef.value.getBoundingClientRect()
    let { x, y } = props.position
    // 超出右边
    if (x + rect.width > window.innerWidth - 8) {
      x = window.innerWidth - rect.width - 8
    }
    // 超出底部
    if (y + rect.height > window.innerHeight - 8) {
      y = window.innerHeight - rect.height - 8
    }
    if (x < 8) x = 8
    if (y < 8) y = 8
    adjustedPos.value = { x, y }
  }
})

const EDITABLE_EXTENSIONS = [
  'txt', 'md', 'markdown', 'log',
  'json', 'xml', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg', 'properties',
  'html', 'htm', 'css', 'scss', 'sass', 'less', 'js', 'ts', 'jsx', 'tsx', 'vue', 'svelte',
  'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'rb', 'php', 'swift', 'kt', 'kts',
  'sql', 'sh', 'bash', 'bat', 'cmd', 'ps1'
]

const VECTORIZE_EXTENSIONS = ['pdf', 'doc', 'docx', 'txt', 'md']

const isEditable = computed(() => {
  if (!props.target) return false
  return EDITABLE_EXTENSIONS.includes(props.target.fileExt?.toLowerCase() || '')
})

const canVectorize = computed(() => {
  if (!props.target) return false
  return VECTORIZE_EXTENSIONS.includes(props.target.fileExt?.toLowerCase() || '')
})

const SUMMARIZE_EXTENSIONS = ['pdf', 'doc', 'docx', 'txt', 'md']

const canSummarize = computed(() => {
  if (!props.target) return false
  return SUMMARIZE_EXTENSIONS.includes(props.target.fileExt?.toLowerCase() || '')
})
</script>

<style scoped lang="scss">
.context-menu {
  position: fixed;
  z-index: 2000;
  background: var(--color-surface-elevated, var(--color-surface));
  min-width: 180px;
  max-height: calc(100vh - 16px);
  overflow-y: auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
  padding: 6px;
  font-size: var(--font-size-sm);
}

.context-menu-enter-active {
  transition: all var(--transition-fast) var(--ease-spring);
}
.context-menu-leave-active {
  transition: all var(--transition-fast) var(--ease-in);
}
.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.92);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--color-text);
  transition: all var(--transition-fast);
  font-weight: 450;

  .el-icon {
    font-size: 16px;
    color: var(--color-text-muted);
    transition: color var(--transition-fast);
  }

  &:hover {
    background: var(--sidebar-item-hover);
    color: var(--color-primary);

    .el-icon {
      color: var(--color-primary);
    }
  }

  &.danger {
    color: var(--color-error);

    .el-icon {
      color: var(--color-error);
    }

    &:hover {
      background: rgba(239, 68, 68, 0.08);
    }
  }
}

.menu-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 4px 8px;
}
</style>
