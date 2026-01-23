<template>
  <div class="common-layout">
    <el-container class="h-screen w-screen bg-[#FAF5FF]">
      <!-- Sidebar -->
      <el-aside width="240px" class="glass-panel m-4 rounded-xl flex flex-col border-0">
        <div class="h-16 flex items-center justify-center border-b border-gray-100">
          <span class="text-xl font-bold text-[#7C3AED] flex items-center gap-2">
            <el-icon><Cloudy /></el-icon>
            SmartNetdisk
          </span>
        </div>
        
        <el-menu
          :default-active="activeMenu"
          class="flex-1 bg-transparent border-r-0 p-2"
          text-color="#475569"
          active-text-color="#7C3AED"
          router
        >
          <el-menu-item index="/files" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Folder /></el-icon>
            <span>全部文件</span>
          </el-menu-item>
          <el-menu-item index="/recent" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Clock /></el-icon>
            <span>最近访问</span>
          </el-menu-item>
          <el-menu-item index="/photos" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Picture /></el-icon>
            <span>相册</span>
          </el-menu-item>
          <el-menu-item index="/shares" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Share /></el-icon>
            <span>我的分享</span>
          </el-menu-item>
          <el-divider />
          <el-menu-item index="/recycle" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
        </el-menu>

        <div class="p-4 border-t border-gray-100">
          <div class="bg-white/60 p-3 rounded-lg">
            <div class="flex justify-between text-xs text-gray-500 mb-1">
              <span>存储空间</span>
              <span>{{ userStore.usedSpaceStr }} / {{ userStore.totalSpaceStr }}</span>
            </div>
            <el-progress :percentage="userStore.usedPercent" :color="'#7C3AED'" :show-text="false" stroke-width="6" />
          </div>
        </div>
      </el-aside>

      <el-container class="py-4 pr-4 pl-0">
        <!-- Header -->
        <el-header class="glass-panel rounded-xl h-16 mb-4 flex items-center justify-between px-6">
          <!-- Search Bar -->
          <div class="flex-1 max-w-xl">
            <el-input
              v-model="searchQuery"
              placeholder="搜索文件、或提问 AI..."
              prefix-icon="Search"
              class="w-full"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="toggleAiSidebar">
                   <el-icon color="#7C3AED"><MagicStick /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>

          <!-- User Profile -->
          <div class="flex items-center gap-4">
             <el-button circle plain>
              <el-icon><Bell /></el-icon>
            </el-button>
            <el-button circle plain>
              <el-icon><Setting /></el-icon>
            </el-button>
            <el-dropdown @command="handleUserCommand">
              <div class="flex items-center gap-2 cursor-pointer hover:bg-white/50 p-1.5 rounded-full transition-colors">
                <el-avatar :size="32" :src="userStore.avatar || undefined">
                  {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
                </el-avatar>
                <span class="text-sm font-medium text-gray-700">{{ userStore.username || '用户' }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    系统设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- Main Content -->
        <el-main class="glass-panel rounded-xl p-0 relative overflow-hidden">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view> 
        </el-main>
      </el-container>
    </el-container>
    
    <!-- AI 助手侧边栏 -->
    <AiSidebar ref="aiSidebarRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Cloudy, Folder, Clock, Picture, Share, Delete, 
  MagicStick, Bell, Setting, User, SwitchButton 
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import AiSidebar from '@/components/AiSidebar.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchQuery = ref('')
const aiSidebarRef = ref()

// 当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/files')) return '/files'
  if (path.startsWith('/recent')) return '/recent'
  if (path.startsWith('/photos')) return '/photos'
  if (path.startsWith('/shares')) return '/shares'
  if (path.startsWith('/recycle')) return '/recycle'
  return '/files'
})

// 搜索处理
function handleSearch() {
  if (searchQuery.value.trim()) {
    console.log('搜索:', searchQuery.value)
    // TODO: 实现搜索功能
  }
}

// 切换 AI 侧边栏
function toggleAiSidebar() {
  aiSidebarRef.value?.toggle()
}

// 用户菜单命令
async function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      // TODO: 跳转个人中心
      break
    case 'settings':
      // TODO: 跳转设置页
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
        // 用户取消
      }
      break
  }
}

// 组件挂载时获取用户信息
onMounted(async () => {
  try {
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
})
</script>

<style scoped>
.h-screen {
  height: 100vh;
}
.w-screen {
  width: 100vw;
}
.flex-col {
    flex-direction: column;
}
.flex-1 {
    flex: 1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
