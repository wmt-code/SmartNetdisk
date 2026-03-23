<template>
  <div class="grid-view" :class="{ 'is-mobile': isMobile }">
    <div
      v-for="(file, index) in files"
      :key="file.id"
      class="file-card-wrapper"
      :style="{ animationDelay: `${Math.min(index * 25, 300)}ms` }"
    >
      <div
        class="file-card"
        :class="{
          'is-selected': isSelected(file),
          'is-folder': file.fileType === 'folder'
        }"
        @click="emit('select', file, $event)"
        @dblclick="emit('dblclick', file)"
        @contextmenu.prevent="emit('contextmenu', $event, file)"
      >
        <!-- Selection checkbox -->
        <div class="file-card-checkbox" @click.stop>
          <el-checkbox
            :model-value="isSelected(file)"
            @change="emit('select', file)"
          />
        </div>

        <!-- File icon -->
        <div class="file-card-icon" :class="getFileTypeClass(file.fileType)">
          <el-icon :size="isMobile ? 36 : 44">
            <component :is="getFileIcon(file.fileType)" />
          </el-icon>
        </div>

        <!-- File info -->
        <el-tooltip
          :content="file.aiSummary"
          :disabled="!file.aiSummary"
          placement="bottom"
          :show-after="500"
          :max-width="300"
          effect="dark"
        >
          <div class="file-card-info">
            <span class="file-card-name" :title="file.fileName">{{ file.fileName }}</span>
            <span class="file-card-meta">{{ file.fileSizeStr }}</span>
          </div>
        </el-tooltip>

        <!-- AI badges -->
        <div class="file-card-badges">
          <span v-if="file.isVectorized" class="file-card-badge badge-vectorized">已向量化</span>
          <span v-if="file.aiSummary" class="file-card-badge badge-summary">摘要</span>
        </div>

        <!-- Hover actions -->
        <div class="file-card-actions">
          <el-button
            v-if="!isRecycle && file.fileType !== 'folder'"
            size="small"
            circle
            @click.stop="emit('download', file)"
          >
            <el-icon><Download /></el-icon>
          </el-button>
          <el-button
            v-if="!isRecycle"
            size="small"
            circle
            @click.stop="emit('share', file)"
          >
            <el-icon><Share /></el-icon>
          </el-button>
          <el-button
            size="small"
            circle
            @click.stop="emit('delete', file)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download, Share, Delete, MagicStick } from '@element-plus/icons-vue'
import type { FileInfo } from '@/api/file'
import { getFileIcon, getFileTypeClass } from '@/constants/fileTypes'

defineProps<{
  files: FileInfo[]
  isSelected: (file: FileInfo) => boolean
  isRecycle?: boolean
  isMobile?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', file: FileInfo, event?: MouseEvent): void
  (e: 'dblclick', file: FileInfo): void
  (e: 'contextmenu', event: MouseEvent, file: FileInfo): void
  (e: 'download', file: FileInfo): void
  (e: 'share', file: FileInfo): void
  (e: 'delete', file: FileInfo): void
}>()
</script>

<style scoped lang="scss">
.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-md);

  @media (min-width: 768px) {
    grid-template-columns: repeat(auto-fill, minmax(175px, 1fr));
  }

  @media (min-width: 1280px) {
    grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  }

  &.is-mobile {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-sm);
  }
}

.file-card-wrapper {
  opacity: 0;
  animation: cardFadeIn var(--transition-slow) var(--ease-out) forwards;
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(8px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.file-card {
  position: relative;
  background: var(--file-card-bg, var(--card-bg));
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--card-border);
  padding: var(--space-md) var(--space-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all var(--transition-base) var(--ease-out);
  overflow: hidden;
  min-height: 150px;

  &:hover {
    background: var(--file-card-hover-bg, var(--card-bg-hover));
    border-color: var(--color-primary-200, rgba(124, 58, 237, 0.2));
    box-shadow: var(--card-shadow-hover, var(--shadow-lg));
    transform: translateY(-2px);

    .file-card-checkbox { opacity: 1; }
    .file-card-actions { opacity: 1; transform: translateY(0); }
  }

  &.is-selected {
    border-color: var(--file-card-selected-border, var(--color-primary));
    background: var(--file-card-selected-bg, rgba(124, 58, 237, 0.05));
    box-shadow: 0 0 0 1px var(--color-primary-200, rgba(124, 58, 237, 0.15));

    .file-card-checkbox { opacity: 1; }
  }
}

.file-card-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  opacity: 0;
  transition: opacity var(--transition-fast);
  z-index: 2;
}

.file-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-sm);
  border-radius: var(--radius-md);
  transition: all var(--transition-base) var(--ease-out);

  &.type-folder { color: var(--color-folder); background: rgba(245, 158, 11, 0.08); }
  &.type-document { color: var(--color-document); background: rgba(59, 130, 246, 0.08); }
  &.type-image { color: var(--color-image); background: rgba(139, 92, 246, 0.08); }
  &.type-video { color: var(--color-video); background: rgba(236, 72, 153, 0.08); }
  &.type-audio { color: var(--color-audio); background: rgba(20, 184, 166, 0.08); }
  &.type-archive { color: var(--color-archive); background: rgba(249, 115, 22, 0.08); }
  &.type-code { color: var(--color-code); background: rgba(16, 185, 129, 0.08); }

  .file-card:hover & {
    transform: scale(1.08);
  }
}

.file-card-info {
  width: 100%;
  text-align: center;
  padding: 0 4px;
}

.file-card-name {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.file-card-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.file-card-badges {
  position: absolute;
  top: 6px;
  right: 6px;
  display: flex;
  gap: 3px;
  flex-direction: column;
  align-items: flex-end;
}

.file-card-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  color: white;
  font-size: 9px;
  font-weight: 600;

  &.badge-vectorized {
    background: var(--color-success, #10B981);
  }
  &.badge-summary {
    background: var(--color-primary, #7C3AED);
  }
}

.file-card-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 8px 12px;
  background: linear-gradient(transparent 0%, rgba(0, 0, 0, 0.65) 50%, rgba(0, 0, 0, 0.8) 100%);
  display: flex;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transform: translateY(6px);
  transition: all var(--transition-base) var(--ease-out);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);

  .el-button {
    --el-button-size: 30px;
    background: rgba(255, 255, 255, 0.95);
    border: none;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
    color: #374151;

    &:hover {
      background: var(--color-primary);
      color: white;
      transform: scale(1.12);
    }
  }
}

// Mobile adjustments
.is-mobile .file-card {
  padding: var(--space-sm);
  min-height: 120px;

  .file-card-checkbox { opacity: 1; }
  .file-card-icon {
    width: 48px;
    height: 48px;
  }
  .file-card-name { font-size: var(--font-size-xs); }
  .file-card-meta { font-size: 10px; }
}
</style>
