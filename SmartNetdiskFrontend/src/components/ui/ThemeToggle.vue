<script setup lang="ts">
import { useTheme } from '@/composables'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'

const { theme, isDark, cycleTheme } = useTheme()

const themeIcons = {
  light: Sunny,
  dark: Moon,
  system: Monitor
}

const themeLabels = {
  light: '浅色模式',
  dark: '深色模式',
  system: '跟随系统'
}
</script>

<template>
  <el-tooltip :content="themeLabels[theme]" placement="bottom">
    <el-button
      class="theme-toggle"
      :icon="themeIcons[theme]"
      circle
      @click="cycleTheme"
    />
  </el-tooltip>
</template>

<style scoped lang="scss">
.theme-toggle {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  --el-button-hover-border-color: transparent;
  color: var(--color-text);
  transition: all var(--transition-base) var(--ease-spring);

  &:hover {
    transform: rotate(15deg) scale(1.1);
    color: var(--color-primary);
  }

  &:active {
    transform: rotate(15deg) scale(0.95);
  }

  :deep(.el-icon) {
    font-size: 18px;
  }
}
</style>
