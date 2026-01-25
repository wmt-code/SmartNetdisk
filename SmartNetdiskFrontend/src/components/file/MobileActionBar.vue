<script setup lang="ts">
import {
  Download,
  Delete,
  Rank,
  Share,
  Close,
  DocumentCopy
} from '@element-plus/icons-vue'

interface Props {
  selectedCount: number
  canDownload?: boolean
  canDelete?: boolean
  canMove?: boolean
  canShare?: boolean
  canCopy?: boolean
  isRecycle?: boolean
}

withDefaults(defineProps<Props>(), {
  canDownload: true,
  canDelete: true,
  canMove: true,
  canShare: true,
  canCopy: true,
  isRecycle: false
})

const emit = defineEmits<{
  clear: []
  download: []
  delete: []
  move: []
  copy: []
  share: []
  restore: []
  permanentDelete: []
}>()
</script>

<template>
  <Transition name="slide-up">
    <div v-if="selectedCount > 0" class="mobile-action-bar">
      <div class="action-bar-header">
        <el-button
          class="clear-btn"
          :icon="Close"
          circle
          size="small"
          @click="emit('clear')"
        />
        <span class="selected-text">已选择 {{ selectedCount }} 项</span>
      </div>

      <div class="action-bar-actions">
        <template v-if="!isRecycle">
          <button
            v-if="canDownload"
            class="action-btn"
            @click="emit('download')"
          >
            <el-icon :size="20"><Download /></el-icon>
            <span>下载</span>
          </button>

          <button
            v-if="canMove"
            class="action-btn"
            @click="emit('move')"
          >
            <el-icon :size="20"><Rank /></el-icon>
            <span>移动</span>
          </button>

          <button
            v-if="canCopy"
            class="action-btn"
            @click="emit('copy')"
          >
            <el-icon :size="20"><DocumentCopy /></el-icon>
            <span>复制</span>
          </button>

          <button
            v-if="canShare"
            class="action-btn"
            @click="emit('share')"
          >
            <el-icon :size="20"><Share /></el-icon>
            <span>分享</span>
          </button>

          <button
            v-if="canDelete"
            class="action-btn action-btn--danger"
            @click="emit('delete')"
          >
            <el-icon :size="20"><Delete /></el-icon>
            <span>删除</span>
          </button>
        </template>

        <template v-else>
          <button
            class="action-btn action-btn--success"
            @click="emit('restore')"
          >
            <el-icon :size="20"><Rank /></el-icon>
            <span>还原</span>
          </button>

          <button
            class="action-btn action-btn--danger"
            @click="emit('permanentDelete')"
          >
            <el-icon :size="20"><Delete /></el-icon>
            <span>彻底删除</span>
          </button>
        </template>
      </div>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.mobile-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid var(--glass-border);
  padding: var(--space-sm) var(--space-md);
  padding-bottom: calc(var(--space-sm) + env(safe-area-inset-bottom, 0));
  z-index: var(--z-fixed);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
}

.action-bar-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.clear-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--color-border);
  --el-button-hover-bg-color: var(--color-error);
  --el-button-hover-border-color: var(--color-error);
  --el-button-hover-text-color: white;
}

.selected-text {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
}

.action-bar-actions {
  display: flex;
  justify-content: space-around;
  gap: var(--space-xs);
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: var(--space-sm);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  color: var(--color-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-width: 56px;

  &:hover,
  &:active {
    background: var(--sidebar-item-hover);
  }

  &:active {
    transform: scale(0.95);
  }

  span {
    font-size: 0.75rem;
  }

  &--danger {
    color: var(--color-error);

    &:hover,
    &:active {
      background: rgba(239, 68, 68, 0.1);
    }
  }

  &--success {
    color: var(--color-success);

    &:hover,
    &:active {
      background: rgba(34, 197, 94, 0.1);
    }
  }
}

// Slide up transition
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all var(--transition-base) var(--ease-out);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
