<script setup lang="ts">
import { FolderOpened, Upload, Search, Delete } from '@element-plus/icons-vue'

interface Props {
  type?: 'empty' | 'search' | 'recycle'
  title?: string
  description?: string
  showAction?: boolean
  actionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'empty',
  title: '',
  description: '',
  showAction: true,
  actionText: '上传文件'
})

const emit = defineEmits<{
  action: []
}>()

const defaultContent = {
  empty: {
    title: '这里还没有文件',
    description: '上传文件或创建文件夹开始使用',
    icon: FolderOpened
  },
  search: {
    title: '未找到匹配结果',
    description: '尝试使用其他关键词搜索',
    icon: Search
  },
  recycle: {
    title: '回收站是空的',
    description: '删除的文件会出现在这里',
    icon: Delete
  }
}

const content = defaultContent[props.type]
</script>

<template>
  <div class="empty-state">
    <div class="empty-state-glow" />

    <div class="empty-state-icon animate-float">
      <el-icon :size="80">
        <component :is="content.icon" />
      </el-icon>
    </div>

    <h3 class="empty-state-title">
      {{ title || content.title }}
    </h3>

    <p class="empty-state-description">
      {{ description || content.description }}
    </p>

    <el-button
      v-if="showAction && type === 'empty'"
      type="primary"
      size="large"
      :icon="Upload"
      class="empty-state-action hover-glow"
      @click="emit('action')"
    >
      {{ actionText }}
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-2xl);
  min-height: 400px;
  text-align: center;
  position: relative;
}

.empty-state-glow {
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(
    circle,
    rgba(124, 58, 237, 0.15) 0%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

.empty-state-icon {
  position: relative;
  z-index: 1;
  color: var(--color-primary);
  opacity: 0.7;
  margin-bottom: var(--space-lg);

  :deep(.el-icon) {
    filter: drop-shadow(0 4px 20px rgba(124, 58, 237, 0.3));
  }
}

.empty-state-title {
  position: relative;
  z-index: 1;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-sm);
}

.empty-state-description {
  position: relative;
  z-index: 1;
  font-size: 1rem;
  color: var(--color-text-muted);
  margin: 0 0 var(--space-xl);
  max-width: 300px;
}

.empty-state-action {
  position: relative;
  z-index: 1;
  padding: 12px 32px;
  font-size: 1rem;
}
</style>
