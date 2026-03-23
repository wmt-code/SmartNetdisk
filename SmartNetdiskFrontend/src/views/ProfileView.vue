<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- Hero banner -->
      <div class="profile-hero">
        <div class="hero-bg" />
        <div class="hero-content">
          <!-- Avatar with upload -->
          <div class="avatar-wrapper">
            <el-avatar :size="96" :src="userStore.avatar || undefined" class="user-avatar">
              {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <label class="avatar-upload-btn" title="更换头像">
              <el-icon :size="16"><Camera /></el-icon>
              <input type="file" accept="image/*" class="hidden-input" @change="handleAvatarChange" />
            </label>
          </div>
          <div class="hero-info">
            <h2 class="hero-name">{{ userStore.username }}</h2>
            <p class="hero-email">{{ userStore.userInfo?.email }}</p>
          </div>
        </div>
      </div>

      <div class="profile-grid">
        <!-- Left column: Basic info -->
        <div class="profile-card">
          <div class="card-header">
            <el-icon><User /></el-icon>
            <h3>基本信息</h3>
          </div>
          <el-form label-position="top" class="profile-form">
            <el-form-item label="用户名">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input :model-value="userStore.userInfo?.email" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- Right column: Storage + Stats -->
        <div class="side-column">
          <!-- Storage card -->
          <div class="profile-card">
            <div class="card-header">
              <el-icon><Coin /></el-icon>
              <h3>存储空间</h3>
            </div>
            <div class="storage-visual">
              <div class="storage-ring">
                <el-progress
                  type="dashboard"
                  :percentage="userStore.usedPercent"
                  :color="storageColor"
                  :stroke-width="8"
                  :width="120"
                >
                  <template #default="{ percentage }">
                    <span class="storage-percent">{{ percentage }}%</span>
                  </template>
                </el-progress>
              </div>
              <div class="storage-detail">
                <div class="detail-row">
                  <span class="detail-label">已使用</span>
                  <span class="detail-value">{{ userStore.usedSpaceStr }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">总容量</span>
                  <span class="detail-value">{{ userStore.totalSpaceStr }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Account info card -->
          <div class="profile-card">
            <div class="card-header">
              <el-icon><InfoFilled /></el-icon>
              <h3>账户信息</h3>
            </div>
            <div class="info-rows">
              <div class="info-row">
                <span class="info-label">账户状态</span>
                <el-tag type="success" size="small">正常</el-tag>
              </div>
              <div class="info-row">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(userStore.userInfo?.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Password change: full width -->
        <div class="profile-card full-width">
          <div class="card-header">
            <el-icon><Lock /></el-icon>
            <h3>修改密码</h3>
          </div>
          <el-form label-position="top" class="password-form">
            <div class="password-grid">
              <el-form-item label="当前密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-20位新密码" />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
            </div>
            <el-form-item>
              <el-button type="warning" :loading="changingPassword" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Camera, User, Coin, InfoFilled, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile, changePassword } from '@/api/auth'
import api from '@/utils/api'

const userStore = useUserStore()

const saving = ref(false)
const changingPassword = ref(false)

const form = ref({ username: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const storageColor = computed(() => {
  const p = userStore.usedPercent
  if (p > 90) return '#EF4444'
  if (p > 70) return '#F59E0B'
  return '#7C3AED'
})

onMounted(() => {
  form.value.username = userStore.username || ''
})

function formatDate(time: string | undefined): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 10)
}

async function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    // Upload avatar via backend API
    await api.post('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('头像更新成功')
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  }

  // Reset input
  input.value = ''
}

async function handleSaveProfile() {
  if (!form.value.username.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  saving.value = true
  try {
    await updateProfile({ username: form.value.username })
    ElMessage.success('修改成功')
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('更新失败:', error)
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning('请填写所有密码字段')
    return
  }
  if (newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changePassword({ oldPassword, newPassword, confirmPassword })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    changingPassword.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page {
  height: 100%;
  overflow: auto;
  background: var(--color-background);
}

.profile-container {
  max-width: 860px;
  margin: 0 auto;
  padding: var(--space-lg);
}

// Hero banner
.profile-hero {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  margin-bottom: var(--space-lg);
}

.hero-bg {
  height: 140px;
  background: linear-gradient(135deg, #7C3AED 0%, #A78BFA 50%, #C4B5FD 100%);
}

.hero-content {
  display: flex;
  align-items: flex-end;
  gap: var(--space-lg);
  padding: 0 var(--space-xl);
  margin-top: -48px;
  padding-bottom: var(--space-lg);
  position: relative;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;

  .user-avatar {
    border: 4px solid var(--color-surface);
    background: linear-gradient(135deg, #7C3AED, #6D28D9);
    color: white;
    font-size: 2.5rem;
    font-weight: 700;
    box-shadow: var(--shadow-lg);
  }

  .avatar-upload-btn {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--color-primary);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border: 3px solid var(--color-surface);
    transition: all var(--transition-fast);
    box-shadow: var(--shadow-sm);

    &:hover {
      background: var(--color-primary-dark, #5B21B6);
      transform: scale(1.1);
    }

    .hidden-input {
      display: none;
    }
  }
}

.hero-info {
  padding-bottom: 4px;

  .hero-name {
    margin: 0;
    font-size: var(--font-size-xl, 1.25rem);
    font-weight: 700;
    color: var(--color-text);
  }

  .hero-email {
    margin: 2px 0 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-muted);
  }
}

// Grid layout
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--space-md);
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.full-width {
  grid-column: 1 / -1;
}

// Cards
.profile-card {
  background: var(--card-bg, var(--color-surface));
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border, var(--color-border));
  padding: var(--space-lg);
  box-shadow: var(--shadow-xs);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: var(--space-md);

  .el-icon {
    color: var(--color-primary);
    font-size: 18px;
  }

  h3 {
    margin: 0;
    font-size: var(--font-size-md, 1rem);
    font-weight: 600;
    color: var(--color-text);
  }
}

// Storage visual
.storage-visual {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.storage-ring {
  flex-shrink: 0;
}

.storage-percent {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text);
}

.storage-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  flex: 1;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .detail-label {
    font-size: var(--font-size-sm);
    color: var(--color-text-muted);
  }

  .detail-value {
    font-size: var(--font-size-sm);
    font-weight: 600;
    color: var(--color-text);
  }
}

// Info rows
.info-rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-xs) 0;

  .info-label {
    font-size: var(--font-size-sm);
    color: var(--color-text-muted);
  }

  .info-value {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
  }
}

// Password
.password-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
}

// Mobile
@media (max-width: 767px) {
  .profile-container {
    padding: var(--space-md);
  }

  .hero-bg {
    height: 100px;
  }

  .hero-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 var(--space-md);
    margin-top: -40px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .password-grid {
    grid-template-columns: 1fr;
  }
}
</style>
