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
          default-active="1"
          class="flex-1 bg-transparent border-r-0 p-2"
          text-color="#475569"
          active-text-color="#7C3AED"
        >
          <el-menu-item index="1" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Folder /></el-icon>
            <span>全部文件</span>
          </el-menu-item>
          <el-menu-item index="2" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Clock /></el-icon>
            <span>最近访问</span>
          </el-menu-item>
          <el-menu-item index="3" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Picture /></el-icon>
            <span>相册</span>
          </el-menu-item>
          <el-menu-item index="4" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Share /></el-icon>
            <span>我的分享</span>
          </el-menu-item>
          <el-divider />
          <el-menu-item index="5" class="rounded-lg mb-1 hover:bg-white/50">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
        </el-menu>

        <div class="p-4 border-t border-gray-100">
          <div class="bg-white/60 p-3 rounded-lg">
            <div class="flex justify-between text-xs text-gray-500 mb-1">
              <span>存储空间</span>
              <span>10GB / 100GB</span>
            </div>
            <el-progress :percentage="10" :color="'#7C3AED'" :show-text="false" stroke-width="6" />
          </div>
        </div>
      </el-aside>

      <el-container class="py-4 pr-4 pl-0">
        <!-- Header -->
        <el-header class="glass-panel rounded-xl h-16 mb-4 flex items-center justify-between px-6">
          <!-- Search Bar -->
          <div class="flex-1 max-w-xl">
            <el-input
              placeholder="搜索文件、或提问 AI..."
              prefix-icon="Search"
              class="w-full"
            >
              <template #append>
                <el-button>
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
            <div class="flex items-center gap-2 cursor-pointer hover:bg-white/50 p-1.5 rounded-full transition-colors">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="text-sm font-medium text-gray-700">Admin</span>
            </div>
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
    <AiSidebar />
  </div>
</template>

<script setup lang="ts">
import { Cloudy, Folder, Clock, Picture, Share, Delete, Search, MagicStick, Bell, Setting } from '@element-plus/icons-vue'
import AiSidebar from '@/components/AiSidebar.vue'
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
</style>
