<template>
  <div class="settings-page">
    <div class="settings-container">
      <h2 class="page-title">系统设置</h2>

      <!-- 外观设置 -->
      <div class="settings-card">
        <h3 class="card-title">
          <el-icon><Brush /></el-icon>
          外观设置
        </h3>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">主题模式</span>
            <span class="setting-desc">选择界面主题风格</span>
          </div>
          <el-segmented v-model="currentTheme" :options="themeOptions" @change="handleThemeChange" />
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">默认视图</span>
            <span class="setting-desc">文件浏览的默认显示方式</span>
          </div>
          <el-radio-group v-model="defaultView" size="small" @change="savePreference('defaultView', defaultView)">
            <el-radio-button value="list">
              <el-icon><List /></el-icon> 列表
            </el-radio-button>
            <el-radio-button value="grid">
              <el-icon><Grid /></el-icon> 网格
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 上传设置 -->
      <div class="settings-card">
        <h3 class="card-title">
          <el-icon><Upload /></el-icon>
          上传设置
        </h3>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">分片大小</span>
            <span class="setting-desc">大文件分片上传的每片大小</span>
          </div>
          <el-select v-model="chunkSize" style="width: 140px" @change="savePreference('chunkSize', chunkSize)">
            <el-option label="2 MB" :value="2" />
            <el-option label="5 MB" :value="5" />
            <el-option label="10 MB" :value="10" />
            <el-option label="20 MB" :value="20" />
          </el-select>
        </div>
      </div>

      <!-- 关于 -->
      <div class="settings-card">
        <h3 class="card-title">
          <el-icon><InfoFilled /></el-icon>
          关于
        </h3>

        <div class="about-info">
          <div class="about-row">
            <span class="about-label">应用名称</span>
            <span class="about-value">SmartNetdisk</span>
          </div>
          <div class="about-row">
            <span class="about-label">版本</span>
            <span class="about-value">2.0.0</span>
          </div>
          <div class="about-row">
            <span class="about-label">技术栈</span>
            <span class="about-value">Spring Boot + Vue 3 + PostgreSQL</span>
          </div>
          <div class="about-row">
            <span class="about-label">AI 能力</span>
            <span class="about-value">语义搜索 · RAG 问答 · 文档向量化</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Brush, List, Grid, Upload, InfoFilled } from '@element-plus/icons-vue'
import { useTheme } from '@/composables'

const { theme, setTheme } = useTheme()

const currentTheme = ref(theme.value)
const defaultView = ref(localStorage.getItem('defaultView') || 'list')
const chunkSize = ref(Number(localStorage.getItem('chunkSize')) || 5)

const themeOptions = [
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' },
  { label: '跟随系统', value: 'system' }
]

function handleThemeChange(val: string | number) {
  setTheme(val as 'light' | 'dark' | 'system')
}

function savePreference(key: string, value: any) {
  localStorage.setItem(key, String(value))
}

onMounted(() => {
  currentTheme.value = theme.value
})
</script>

<style scoped lang="scss">
.settings-page {
  height: 100%;
  overflow: auto;
  padding: var(--space-lg);
}

.settings-container {
  max-width: 640px;
  margin: 0 auto;
}

.page-title {
  font-size: var(--font-size-2xl, 1.5rem);
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: var(--space-lg);
}

.settings-card {
  background: var(--card-bg, var(--color-surface));
  border-radius: var(--radius-xl);
  border: 1px solid var(--card-border, var(--color-border));
  padding: var(--space-lg);
  margin-bottom: var(--space-md);
  box-shadow: var(--shadow-xs);

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 var(--space-lg);
    font-size: var(--font-size-md, 1rem);
    font-weight: 600;
    color: var(--color-text);

    .el-icon {
      color: var(--color-primary);
    }
  }
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border-light);

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &:first-of-type {
    padding-top: 0;
  }
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .setting-label {
    font-weight: 500;
    font-size: var(--font-size-base);
    color: var(--color-text);
  }

  .setting-desc {
    font-size: var(--font-size-xs);
    color: var(--color-text-muted);
  }
}

.about-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.about-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) 0;

  .about-label {
    font-size: var(--font-size-sm);
    color: var(--color-text-muted);
  }

  .about-value {
    font-size: var(--font-size-sm);
    font-weight: 500;
    color: var(--color-text);
  }
}

@media (max-width: 767px) {
  .settings-page {
    padding: var(--space-md);
  }

  .settings-card {
    padding: var(--space-md);
  }

  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-sm);
  }
}
</style>
