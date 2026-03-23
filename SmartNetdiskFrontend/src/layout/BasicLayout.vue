<template>
  <div class="common-layout" :class="{ 'sidebar-collapsed': collapsed && !isMobile }">
    <el-container class="layout-container">
      <!-- Mobile sidebar overlay -->
      <Transition name="fade">
        <div v-if="isMobile && mobileOpen" class="sidebar-overlay" @click="closeSidebar" />
      </Transition>

      <!-- Sidebar -->
      <Transition :name="isMobile ? 'slide-left' : ''">
        <el-aside v-show="!isMobile || mobileOpen" :width="sidebarWidth" class="sidebar"
          :class="{ 'mobile-drawer': isMobile }">
          <!-- Logo -->
          <div class="sidebar-header">
            <div class="logo-container">
              <el-icon class="logo-icon" :size="28">
                <Cloudy />
              </el-icon>
              <Transition name="fade">
                <span v-if="!collapsed || isMobile" class="logo-text">SmartNetdisk</span>
              </Transition>
            </div>
            <!-- Collapse toggle (desktop only) -->
            <el-button v-if="!isMobile" class="collapse-btn" @click="toggleSidebar" circle size="small">
              <el-icon>
                <Fold v-if="!collapsed" />
                <Expand v-else />
              </el-icon>
            </el-button>
          </div>

          <!-- Navigation menu -->
          <el-menu :default-active="activeMenu" class="sidebar-menu" :collapse="collapsed && !isMobile"
            :collapse-transition="false" router @select="handleMenuSelect">
            <el-menu-item index="/files" class="menu-item">
              <el-icon>
                <Folder />
              </el-icon>
              <template #title><span>全部文件</span></template>
            </el-menu-item>
            <el-menu-item index="/recent" class="menu-item">
              <el-icon>
                <Clock />
              </el-icon>
              <template #title><span>最近访问</span></template>
            </el-menu-item>
            <el-menu-item index="/photos" class="menu-item">
              <el-icon>
                <Picture />
              </el-icon>
              <template #title><span>相册</span></template>
            </el-menu-item>
            <el-menu-item index="/shares" class="menu-item">
              <el-icon>
                <Share />
              </el-icon>
              <template #title><span>我的分享</span></template>
            </el-menu-item>
            <el-divider />
            <el-menu-item index="/recycle" class="menu-item">
              <el-icon>
                <Delete />
              </el-icon>
              <template #title><span>回收站</span></template>
            </el-menu-item>
            <!-- Admin entry (only for admin users) -->
            <el-menu-item v-if="userStore.userInfo?.role === 'admin'" index="/admin" class="menu-item">
              <el-icon><Setting /></el-icon>
              <template #title><span>管理后台</span></template>
            </el-menu-item>
          </el-menu>

          <!-- Storage info -->
          <div class="sidebar-footer" :class="{ 'collapsed': collapsed && !isMobile }">
            <div class="storage-card">
              <Transition name="fade" mode="out-in">
                <div v-if="!collapsed || isMobile" class="storage-info">
                  <div class="storage-header">
                    <span>存储空间</span>
                    <span>{{ userStore.usedSpaceStr }} / {{ userStore.totalSpaceStr }}</span>
                  </div>
                  <el-progress :percentage="userStore.usedPercent" :color="'#7C3AED'" :show-text="false"
                    stroke-width="6" />
                </div>
                <div v-else class="storage-icon">
                  <el-icon :size="20">
                    <Coin />
                  </el-icon>
                </div>
              </Transition>
            </div>
          </div>
        </el-aside>
      </Transition>

      <!-- Main container -->
      <el-container class="main-container">
        <!-- Mobile header -->
        <el-header v-if="isMobile" class="mobile-header">
          <el-button class="menu-btn" @click="toggleSidebar" circle>
            <el-icon :size="20">
              <Menu />
            </el-icon>
          </el-button>
          <span class="mobile-title">SmartNetdisk</span>
          <el-button class="ai-btn" @click="toggleAiSidebar" circle>
            <el-icon :size="20" color="#7C3AED">
              <MagicStick />
            </el-icon>
          </el-button>
        </el-header>

        <!-- Desktop header -->
        <el-header v-else class="desktop-header glass-panel">
          <!-- Search Bar -->
          <div class="search-container">
            <el-input v-model="searchQuery" placeholder="搜索文件、或提问 AI..." prefix-icon="Search" class="search-input"
              clearable @keyup.enter="handleSearch" @clear="handleSearch">
              <template #append>
                <el-button @click="toggleAiSidebar" class="ai-search-btn">
                  <el-icon :size="18" color="#7C3AED">
                    <MagicStick />
                  </el-icon>
                </el-button>
              </template>
            </el-input>
          </div>

          <!-- Right actions -->
          <div class="header-actions">
            <ThemeToggle />
            <NotificationDropdown />
            <el-button circle class="action-btn" @click="router.push('/settings')">
              <el-icon>
                <Setting />
              </el-icon>
            </el-button>
            <el-dropdown @command="handleUserCommand" trigger="click">
              <div class="user-profile">
                <el-avatar :size="32" :src="userStore.avatar || undefined">
                  {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
                </el-avatar>
                <span class="username">{{ userStore.username || '用户' }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon>
                      <User />
                    </el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon>
                      <Setting />
                    </el-icon>
                    系统设置
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" command="admin">
                    <el-icon>
                      <Monitor />
                    </el-icon>
                    后台管理
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon>
                      <SwitchButton />
                    </el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- Mobile search bar -->
        <div v-if="isMobile" class="mobile-search">
          <el-input v-model="searchQuery" placeholder="搜索文件..." prefix-icon="Search" clearable
            @keyup.enter="handleSearch" @clear="handleSearch" />
        </div>

        <!-- Main Content -->
        <el-main class="main-content glass-panel">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- AI Assistant Sidebar -->
    <AiSidebar ref="aiSidebarRef" />
  </div>
</template>

<script setup lang="ts">
import AiSidebar from '@/components/AiSidebar.vue'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import { ThemeToggle } from '@/components/ui'
import { useSidebar, useTheme } from '@/composables'
import { useUserStore } from '@/stores/user'
import {
  Clock,
  Cloudy,
  Coin,
  Delete,
  Expand,
  Fold,
  Folder,
  MagicStick,
  Menu,
  Picture,
  Setting,
  Share,
  SwitchButton,
  User,
  Monitor
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// Initialize theme
useTheme()

// Sidebar state
const { collapsed, mobileOpen, isMobile, sidebarWidth, toggle: toggleSidebar } = useSidebar()

const searchQuery = ref('')
const aiSidebarRef = ref()

// Current active menu
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/files')) return '/files'
  if (path.startsWith('/recent')) return '/recent'
  if (path.startsWith('/photos')) return '/photos'
  if (path.startsWith('/shares')) return '/shares'
  if (path.startsWith('/recycle')) return '/recycle'
  if (path.startsWith('/admin')) return '/admin'
  return '/files'
})

// Close sidebar on mobile
function closeSidebar() {
  if (isMobile.value) {
    mobileOpen.value = false
  }
}

// Handle menu selection (close drawer on mobile)
function handleMenuSelect() {
  if (isMobile.value) {
    mobileOpen.value = false
  }
}

// Search handler
function handleSearch() {
  const keyword = searchQuery.value.trim()
  if (keyword) {
    router.push({ path: '/files', query: { keyword } })
  } else {
    router.push({ path: '/files' })
  }
}

// Toggle AI sidebar
function toggleAiSidebar() {
  aiSidebarRef.value?.toggle()
}

// User menu commands
async function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        router.push('/login')
      } catch {
        // User cancelled
      }
      break
  }
}

// Fetch user info on mount
onMounted(async () => {
  try {
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
})

// Close mobile drawer on route change
watch(() => route.path, () => {
  if (isMobile.value) {
    mobileOpen.value = false
  }
})
</script>

<style scoped lang="scss">
.common-layout {
  --sidebar-transition: var(--transition-base);
}

.layout-container {
  height: 100vh;
  width: 100vw;
  background-color: var(--color-background);
  transition: background-color var(--transition-base);
}

// Sidebar overlay for mobile
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay-bg);
  z-index: calc(var(--z-modal-backdrop) - 1);
}

// Sidebar
.sidebar {
  background: var(--sidebar-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  margin: var(--space-md);
  margin-right: 0;
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: width var(--sidebar-transition) var(--ease-out),
    background var(--transition-base);
  z-index: var(--z-fixed);

  &.mobile-drawer {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    margin: 0;
    border-radius: 0;
    border-right: 1px solid var(--sidebar-border);
    z-index: var(--z-modal);
    box-shadow: var(--shadow-2xl);
  }
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-md);
  border-bottom: 1px solid var(--color-border-light);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  overflow: hidden;
}

.logo-icon {
  color: var(--color-primary);
  flex-shrink: 0;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.collapse-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  --el-button-hover-border-color: transparent;
  color: var(--color-text-muted);
  flex-shrink: 0;

  &:hover {
    color: var(--color-primary);
  }
}

.sidebar-collapsed .collapse-btn {
  margin-left: auto;
  margin-right: auto;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  padding: var(--space-sm);
  background: transparent !important;
  --el-menu-text-color: var(--color-text-secondary);
  --el-menu-hover-text-color: var(--color-primary);
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--sidebar-item-hover);
  --el-menu-active-color: var(--color-primary);
}

.menu-item {
  border-radius: var(--radius-lg) !important;
  margin-bottom: 4px;
  transition: all var(--transition-fast);

  &:hover {
    background-color: var(--sidebar-item-hover) !important;
  }

  &.is-active {
    background-color: var(--sidebar-item-active) !important;
    color: var(--color-primary) !important;
  }
}

.sidebar-footer {
  padding: var(--space-md);
  border-top: 1px solid var(--color-border-light);

  &.collapsed {
    padding: var(--space-sm);
  }
}

.storage-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-sm) var(--space-md);
  transition: all var(--transition-base);
}

.storage-info {
  .storage-header {
    display: flex;
    justify-content: space-between;
    font-size: 0.75rem;
    color: var(--color-text-muted);
    margin-bottom: var(--space-xs);
  }
}

.storage-icon {
  display: flex;
  justify-content: center;
  color: var(--color-primary);
}

// Main container
.main-container {
  padding: var(--space-md);
  padding-left: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-width: 0;
}

// Mobile header
.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--mobile-header-height);
  padding: 0 var(--space-sm);
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-xl);

  .mobile-title {
    font-weight: 600;
    color: var(--color-primary);
  }

  .menu-btn,
  .ai-btn {
    --el-button-bg-color: transparent;
    --el-button-border-color: transparent;
    --el-button-hover-bg-color: var(--sidebar-item-hover);
    color: var(--color-text);
  }
}

.mobile-search {
  padding: 0 var(--space-sm);

  :deep(.el-input__wrapper) {
    background-color: var(--input-bg);
    box-shadow: 0 0 0 1px var(--input-border) inset;
    border-radius: var(--radius-lg);
  }
}

// Desktop header
.desktop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 var(--space-lg);
  border-radius: var(--radius-xl);
  gap: var(--space-lg);
}

.search-container {
  flex: 1;
  max-width: 600px;
}

.search-input {
  :deep(.el-input__wrapper) {
    background-color: var(--input-bg);
    box-shadow: 0 0 0 1px var(--input-border) inset;
    border-radius: var(--radius-lg);
    transition: all var(--transition-fast);

    &:hover,
    &:focus-within {
      box-shadow: 0 0 0 1px var(--color-primary) inset;
    }
  }

  :deep(.el-input-group__append) {
    background-color: transparent;
    border: none;
    padding: 0 10px;
  }
}

.ai-search-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  min-width: 40px;
  padding: 8px 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.action-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: var(--sidebar-item-hover);
  --el-button-hover-border-color: transparent;
  color: var(--color-text);

  &:hover {
    color: var(--color-primary);
  }
}

.user-profile {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background var(--transition-fast);

  &:hover {
    background: var(--sidebar-item-hover);
  }

  .username {
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--color-text);
  }
}

// Main content
.main-content {
  flex: 1;
  border-radius: var(--radius-xl);
  padding: 0 !important;
  overflow: hidden;
  position: relative;
}

// Page transitions
.page-enter-active {
  transition: all var(--transition-slow) var(--ease-out);
}

.page-leave-active {
  transition: all var(--transition-base) var(--ease-in);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
}

// Fade transition
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// Slide left transition
.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform var(--transition-base) var(--ease-out);
}

.slide-left-enter-from,
.slide-left-leave-to {
  transform: translateX(-100%);
}

// Mobile responsive
@media (max-width: 767px) {
  .main-container {
    padding: var(--space-sm);
    gap: var(--space-sm);
  }

  .desktop-header {
    display: none;
  }

  .main-content {
    border-radius: var(--radius-lg);
  }
}

// Collapsed sidebar adjustments
.sidebar-collapsed {
  .sidebar:not(.mobile-drawer) {
    .sidebar-header {
      justify-content: center;
      padding: 0 var(--space-sm);
    }

    .logo-container {
      justify-content: center;
    }
  }
}
</style>
