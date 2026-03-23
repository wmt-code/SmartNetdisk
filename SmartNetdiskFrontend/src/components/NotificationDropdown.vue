<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="notification-popover"
    @show="loadNotifications"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
        <el-button circle class="action-btn">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="header-title">通知</span>
        <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="handleMarkAllRead">
          全部已读
        </el-button>
      </div>

      <div v-if="loading" class="notification-loading">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>

      <div v-else-if="notifications.length === 0" class="notification-empty">
        <el-icon :size="40" color="#ccc"><BellFilled /></el-icon>
        <p>暂无通知</p>
      </div>

      <div v-else class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'is-unread': item.isRead === 0 }"
          @click="handleClickItem(item)"
        >
          <div class="item-icon" :class="'type-' + item.type">
            <el-icon :size="16">
              <Upload v-if="item.type === 'upload'" />
              <Share v-else-if="item.type === 'share'" />
              <MagicStick v-else-if="item.type === 'ai'" />
              <Bell v-else />
            </el-icon>
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div v-if="item.content" class="item-desc">{{ item.content }}</div>
            <div class="item-time">{{ formatRelativeTime(item.createTime) }}</div>
          </div>
          <div v-if="item.isRead === 0" class="unread-dot" />
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, BellFilled, Loading, Upload, Share, MagicStick } from '@element-plus/icons-vue'
import {
  getNotifications, getUnreadCount, markAsRead, markAllAsRead,
  type NotificationItem
} from '@/api/notification'

const popoverVisible = ref(false)
const loading = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadUnreadCount() {
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    // silent
  }
}

async function loadNotifications() {
  loading.value = true
  try {
    notifications.value = await getNotifications()
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

async function handleClickItem(item: NotificationItem) {
  if (item.isRead === 0) {
    await markAsRead(item.id)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

async function handleMarkAllRead() {
  await markAllAsRead()
  notifications.value.forEach(n => n.isRead = 1)
  unreadCount.value = 0
}

function formatRelativeTime(time: string): string {
  if (!time) return ''
  const date = new Date(time.replace(' ', 'T'))
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + ' 天前'
  return time.substring(0, 10)
}

onMounted(() => {
  loadUnreadCount()
  // Poll every 30 seconds
  pollTimer = setInterval(loadUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style lang="scss">
.notification-popover {
  padding: 0 !important;
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
}
</style>

<style scoped lang="scss">
.notification-badge {
  :deep(.el-badge__content) {
    background: var(--color-error);
    border: none;
  }
}

.notification-panel {
  max-height: 480px;
  display: flex;
  flex-direction: column;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light);

  .header-title {
    font-weight: 600;
    font-size: var(--font-size-md, 1rem);
    color: var(--color-text);
  }
}

.notification-loading,
.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--color-text-muted);

  p {
    margin: 12px 0 0;
    font-size: var(--font-size-sm);
  }
}

.notification-list {
  overflow-y: auto;
  max-height: 420px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
  position: relative;

  &:hover {
    background: var(--sidebar-item-hover, rgba(0, 0, 0, 0.03));
  }

  &.is-unread {
    background: rgba(124, 58, 237, 0.03);
  }

  & + & {
    border-top: 1px solid var(--color-border-light);
  }
}

.item-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.type-upload { background: rgba(16, 185, 129, 0.1); color: var(--color-success); }
  &.type-share { background: rgba(59, 130, 246, 0.1); color: var(--color-info); }
  &.type-ai { background: rgba(124, 58, 237, 0.1); color: var(--color-primary); }
  &.type-system { background: rgba(245, 158, 11, 0.1); color: var(--color-warning); }
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.item-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-time {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
