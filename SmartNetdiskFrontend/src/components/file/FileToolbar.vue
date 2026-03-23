<template>
  <div class="file-toolbar" :class="{ 'is-mobile': isMobile }">
    <div class="toolbar-left">
      <!-- Breadcrumb navigation -->
      <el-breadcrumb v-if="!isRecycle" separator="/" class="breadcrumb">
        <el-breadcrumb-item>
          <span class="breadcrumb-root" @click="emit('navigate-root')">
            <el-icon><HomeFilled /></el-icon>
            <span v-if="!isMobile" class="root-text">文件</span>
          </span>
        </el-breadcrumb-item>
        <el-breadcrumb-item v-for="(segment, index) in folderPath" :key="segment.id">
          <span class="breadcrumb-item" @click="emit('navigate-folder', segment.id, index)">
            {{ segment.name }}
          </span>
        </el-breadcrumb-item>
      </el-breadcrumb>

      <!-- Recycle bin title -->
      <div v-if="isRecycle" class="page-title">
        <el-icon :size="18"><Delete /></el-icon>
        <span>回收站</span>
      </div>
    </div>

    <div class="toolbar-right">
      <!-- Recycle: empty -->
      <el-button v-if="isRecycle" type="danger" plain size="small" @click="emit('empty-recycle')">
        <el-icon><Delete /></el-icon>
        <span class="btn-label">清空</span>
      </el-button>

      <!-- Normal: new folder -->
      <el-button v-if="!isRecycle" size="small" @click="emit('new-folder')" class="action-btn">
        <el-icon><FolderAdd /></el-icon>
        <span class="btn-label">新建</span>
      </el-button>

      <!-- Normal: upload -->
      <el-dropdown v-if="!isRecycle" split-button type="primary" size="small" @click="emit('upload')">
        <el-icon><Upload /></el-icon>
        <span class="btn-label">上传</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="emit('upload')">
              <el-icon><Document /></el-icon> 上传文件
            </el-dropdown-item>
            <el-dropdown-item @click="emit('upload-folder')">
              <el-icon><Folder /></el-icon> 上传文件夹
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- Batch actions (desktop only) -->
      <template v-if="!isMobile && selectedCount > 0">
        <div class="toolbar-divider" />
        <div class="batch-actions">
          <el-button v-if="!isRecycle" type="primary" plain size="small" @click="emit('batch-share')">
            <el-icon><Share /></el-icon>
            分享 ({{ selectedCount }})
          </el-button>
          <el-button v-if="!isRecycle" size="small" plain @click="emit('batch-download')">
            <el-icon><Download /></el-icon>
            下载
          </el-button>
          <template v-if="!isRecycle">
            <el-button size="small" plain @click="emit('batch-move')">移动</el-button>
            <el-button size="small" plain @click="emit('batch-copy')">复制</el-button>
            <el-button size="small" type="danger" plain @click="emit('batch-delete')">删除</el-button>
          </template>
          <template v-if="isRecycle">
            <el-button size="small" type="primary" plain @click="emit('batch-restore')">
              恢复 ({{ selectedCount }})
            </el-button>
            <el-button size="small" type="danger" plain @click="emit('batch-permanent-delete')">彻底删除</el-button>
          </template>
        </div>
      </template>

      <div class="toolbar-divider" />

      <!-- View toggle -->
      <el-radio-group v-model="localViewMode" size="small" class="view-toggle">
        <el-radio-button value="list">
          <el-icon><List /></el-icon>
        </el-radio-button>
        <el-radio-button value="grid">
          <el-icon><Grid /></el-icon>
        </el-radio-button>
      </el-radio-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  HomeFilled, Delete, FolderAdd, Upload, Document,
  Folder, Share, List, Grid, Download
} from '@element-plus/icons-vue'

const props = defineProps<{
  folderPath: { id: number; name: string }[]
  isRecycle: boolean
  isMobile: boolean
  selectedCount: number
  viewMode: 'list' | 'grid'
}>()

const emit = defineEmits<{
  (e: 'navigate-root'): void
  (e: 'navigate-folder', folderId: number, index: number): void
  (e: 'empty-recycle'): void
  (e: 'new-folder'): void
  (e: 'upload'): void
  (e: 'upload-folder'): void
  (e: 'batch-share'): void
  (e: 'batch-download'): void
  (e: 'batch-move'): void
  (e: 'batch-copy'): void
  (e: 'batch-delete'): void
  (e: 'batch-restore'): void
  (e: 'batch-permanent-delete'): void
  (e: 'update:viewMode', mode: 'list' | 'grid'): void
}>()

const localViewMode = computed({
  get: () => props.viewMode,
  set: (val) => emit('update:viewMode', val)
})
</script>

<style scoped lang="scss">
.file-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md);
  border-bottom: 1px solid var(--toolbar-border, var(--color-border-light));
  flex-wrap: wrap;
  gap: var(--space-sm);
  min-height: 50px;
  background: var(--toolbar-bg, transparent);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
  flex-shrink: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-shrink: 0;
}

.breadcrumb {
  min-width: 0;
  overflow: hidden;
}

.breadcrumb-root {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--transition-fast);

  &:hover { color: var(--color-primary); }

  .root-text {
    font-weight: 500;
  }
}

.breadcrumb-item {
  cursor: pointer;
  color: var(--color-text-secondary);
  font-weight: 450;
  transition: color var(--transition-fast);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;

  &:hover { color: var(--color-primary); }
}

.page-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: var(--font-size-md);
  color: var(--color-text);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--color-border);
  margin: 0 4px;
  flex-shrink: 0;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.view-toggle {
  flex-shrink: 0;
}

.action-btn {
  font-weight: 500;
}

// Mobile
.file-toolbar.is-mobile {
  padding: var(--space-sm);
  min-height: 44px;

  .btn-label { display: none; }

  .toolbar-right {
    gap: 4px;
  }

  .breadcrumb-item {
    max-width: 60px;
  }
}
</style>
