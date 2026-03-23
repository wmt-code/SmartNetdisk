<template>
  <div class="admin-page">
    <div class="admin-container">
      <div class="page-header">
        <h2>管理后台</h2>
        <el-button size="small" plain @click="$router.push('/files')">
          <el-icon><Back /></el-icon> 返回网盘
        </el-button>
      </div>

      <!-- Tab navigation -->
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="数据概览" name="overview">
          <div v-loading="loadingStats">
            <!-- Stat cards -->
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-icon users"><el-icon :size="24"><User /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalUsers }}</span>
                  <span class="stat-label">总用户数</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon files"><el-icon :size="24"><Document /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalFiles }}</span>
                  <span class="stat-label">总文件数</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon storage"><el-icon :size="24"><Coin /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ formatSize(stats.totalUsedSpace) }}</span>
                  <span class="stat-label">已用存储</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon ai"><el-icon :size="24"><MagicStick /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.vectorizedFiles }}</span>
                  <span class="stat-label">已向量化</span>
                </div>
              </div>
            </div>

            <!-- Charts row -->
            <div class="charts-row">
              <div class="chart-card">
                <h4>近 7 天上传趋势</h4>
                <div ref="uploadTrendRef" class="chart-container" />
              </div>
              <div class="chart-card">
                <h4>文件类型分布</h4>
                <div ref="fileTypeRef" class="chart-container" />
              </div>
            </div>

            <!-- User storage rank -->
            <div class="chart-card full-width">
              <h4>用户存储排行</h4>
              <div ref="storageRankRef" class="chart-container" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="用户管理" name="users">
          <div class="tab-toolbar">
            <el-input v-model="userKeyword" placeholder="搜索用户名或邮箱" clearable style="width: 260px"
              @keyup.enter="loadUsers" @clear="loadUsers" />
            <el-button type="primary" @click="loadUsers">搜索</el-button>
          </div>

          <el-table :data="users" v-loading="loadingUsers" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="role" label="角色" width="90">
              <template #default="{ row }">
                <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
                  {{ row.role === 'admin' ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="存储" width="150">
              <template #default="{ row }">
                <span>{{ formatSize(row.usedSpace || 0) }} / {{ formatSize(row.totalSpace || 0) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link size="small" :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleUserStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button link size="small" @click="showSpaceDialog(row)">配额</el-button>
                <el-button link size="small" @click="toggleRole(row)">
                  {{ row.role === 'admin' ? '取消管理员' : '设为管理员' }}
                </el-button>
                <el-button link size="small" type="danger" @click="handleDeleteUser(row)"
                  :disabled="row.role === 'admin'">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination v-model:current-page="userPage" :page-size="20" :total="userTotal"
              layout="prev, pager, next" @current-change="loadUsers" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="文件管理" name="files">
          <div class="tab-toolbar file-filters">
            <el-input v-model="fileKeyword" placeholder="搜索文件名" clearable style="width: 200px"
              @keyup.enter="loadFiles" @clear="loadFiles" />
            <el-input v-model="fileOwner" placeholder="所属用户" clearable style="width: 130px"
              @keyup.enter="loadFiles" @clear="loadFiles" />
            <el-select v-model="fileTypeFilter" placeholder="文件类型" clearable style="width: 120px">
              <el-option label="文档" value="document" />
              <el-option label="图片" value="image" />
              <el-option label="视频" value="video" />
              <el-option label="音频" value="audio" />
              <el-option label="其他" value="other" />
            </el-select>
            <el-date-picker v-model="fileDateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="width: 260px" />
            <el-button type="primary" @click="loadFiles">搜索</el-button>
            <el-button @click="resetFileFilters">重置</el-button>
          </div>

          <el-table :data="files" v-loading="loadingFiles" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip />
            <el-table-column prop="ownerName" label="所属用户" width="100" />
            <el-table-column label="大小" width="100">
              <template #default="{ row }">{{ formatSize(row.fileSize || 0) }}</template>
            </el-table-column>
            <el-table-column prop="fileType" label="类型" width="80" />
            <el-table-column prop="createTime" label="上传时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link size="small" type="danger" @click="handleDeleteFile(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination v-model:current-page="filePage" :page-size="20" :total="fileTotal"
              layout="prev, pager, next" @current-change="loadFiles" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Space dialog -->
    <el-dialog v-model="spaceDialogVisible" title="修改存储配额" width="400px">
      <el-form label-position="top">
        <el-form-item label="存储配额 (GB)">
          <el-input-number v-model="spaceGB" :min="1" :max="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spaceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpdateSpace">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, PieChart, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])
import { Back, User, Document, Coin, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminStats, getAdminUsers, getAdminFiles,
  updateUserStatus, updateUserSpace, updateUserRole,
  deleteAdminUser, deleteAdminFile,
  formatSize, type AdminStats, type AdminUser, type AdminFile
} from '@/api/admin'

const activeTab = ref('overview')

// Chart refs
const uploadTrendRef = ref<HTMLElement>()
const fileTypeRef = ref<HTMLElement>()
const storageRankRef = ref<HTMLElement>()
let chartInstances: echarts.ECharts[] = []

function renderCharts() {
  // 延迟确保 DOM 和 Tab 面板完全渲染
  setTimeout(() => {
    // Dispose old instances
    chartInstances.forEach(c => c.dispose())
    chartInstances = []

    // Upload trend (line chart)
    if (uploadTrendRef.value && stats.value.uploadTrend) {
      const chart = echarts.init(uploadTrendRef.value)
      chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 20, bottom: 30 },
        xAxis: { type: 'category', data: stats.value.uploadTrend.map((d: any) => d.date) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'line',
          data: stats.value.uploadTrend.map((d: any) => d.count),
          smooth: true,
          areaStyle: { color: 'rgba(124,58,237,0.1)' },
          lineStyle: { color: '#7C3AED', width: 2 },
          itemStyle: { color: '#7C3AED' }
        }]
      })
    }

    // File type distribution (pie chart)
    if (fileTypeRef.value && stats.value.fileTypeDistribution) {
      const chart = echarts.init(fileTypeRef.value)
      chartInstances.push(chart)
      const typeNames: Record<string, string> = { document: '文档', image: '图片', video: '视频', audio: '音频', other: '其他' }
      const colors = ['#3B82F6', '#8B5CF6', '#EC4899', '#14B8A6', '#F59E0B']
      const data = Object.entries(stats.value.fileTypeDistribution).map(([k, v]) => ({
        name: typeNames[k] || k, value: v
      }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        color: colors,
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '55%'],
          label: { fontSize: 12 },
          data
        }]
      })
    }

    // User storage rank (bar chart)
    if (storageRankRef.value && stats.value.userStorageRank) {
      const chart = echarts.init(storageRankRef.value)
      chartInstances.push(chart)
      const rank = stats.value.userStorageRank as any[]
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (p: any) => {
            const d = p[0]
            return `${d.name}<br/>已用: ${formatSize(d.value)}`
          }
        },
        grid: { left: 80, right: 20, top: 10, bottom: 30 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: rank.map((u: any) => u.username).reverse(), axisTick: { show: false } },
        series: [{
          type: 'bar',
          data: rank.map((u: any) => u.usedSpace).reverse(),
          barWidth: 20,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#7C3AED' },
              { offset: 1, color: '#A78BFA' }
            ]),
            borderRadius: [0, 4, 4, 0]
          }
        }]
      })
    }
  }, 300)
}

// Resize charts on window resize
function handleResize() { chartInstances.forEach(c => c.resize()) }
onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(c => c.dispose())
})

// Stats
const loadingStats = ref(false)
const stats = ref<any>({ totalUsers: 0, activeUsers: 0, totalFiles: 0, totalUsedSpace: 0, totalAllocatedSpace: 0, vectorizedFiles: 0 })

async function loadStats() {
  loadingStats.value = true
  try {
    stats.value = await getAdminStats()
    renderCharts()
  } catch { /* */ }
  finally { loadingStats.value = false }
}

// Users
const loadingUsers = ref(false)
const users = ref<AdminUser[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')

async function loadUsers() {
  loadingUsers.value = true
  try {
    const res = await getAdminUsers({ pageNum: userPage.value, pageSize: 20, keyword: userKeyword.value })
    users.value = res.records
    userTotal.value = res.total
  } catch { /* */ }
  finally { loadingUsers.value = false }
}

async function toggleUserStatus(row: AdminUser) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  loadUsers()
}

async function toggleRole(row: AdminUser) {
  const newRole = row.role === 'admin' ? 'user' : 'admin'
  await updateUserRole(row.id, newRole)
  ElMessage.success('角色已更新')
  loadUsers()
}

async function handleDeleteUser(row: AdminUser) {
  await ElMessageBox.confirm(`确定删除用户 ${row.username}？`, '删除确认', { type: 'warning' })
  await deleteAdminUser(row.id)
  ElMessage.success('已删除')
  loadUsers()
}

// Space dialog
const spaceDialogVisible = ref(false)
const spaceGB = ref(10)
const spaceTargetUserId = ref(0)

function showSpaceDialog(row: AdminUser) {
  spaceTargetUserId.value = row.id
  spaceGB.value = Math.round((row.totalSpace || 0) / (1024 * 1024 * 1024))
  spaceDialogVisible.value = true
}

async function confirmUpdateSpace() {
  await updateUserSpace(spaceTargetUserId.value, spaceGB.value * 1024 * 1024 * 1024)
  ElMessage.success('配额已更新')
  spaceDialogVisible.value = false
  loadUsers()
}

// Files
const loadingFiles = ref(false)
const files = ref<AdminFile[]>([])
const fileTotal = ref(0)
const filePage = ref(1)
const fileKeyword = ref('')
const fileOwner = ref('')
const fileTypeFilter = ref('')
const fileDateRange = ref<string[] | null>(null)

async function loadFiles() {
  loadingFiles.value = true
  try {
    const params: any = { pageNum: filePage.value, pageSize: 20 }
    if (fileKeyword.value) params.keyword = fileKeyword.value
    if (fileOwner.value) params.ownerName = fileOwner.value
    if (fileTypeFilter.value) params.fileType = fileTypeFilter.value
    if (fileDateRange.value && fileDateRange.value.length === 2) {
      params.startDate = fileDateRange.value[0]
      params.endDate = fileDateRange.value[1]
    }
    console.log('[Admin] loadFiles params:', JSON.stringify(params))
    const res = await getAdminFiles(params)
    files.value = res.records
    fileTotal.value = res.total
  } catch { /* */ }
  finally { loadingFiles.value = false }
}

// 筛选条件变化时自动搜索
watch([fileTypeFilter, fileDateRange], () => {
  filePage.value = 1
  loadFiles()
})

function resetFileFilters() {
  fileKeyword.value = ''
  fileOwner.value = ''
  fileTypeFilter.value = ''
  fileDateRange.value = null
  filePage.value = 1
  loadFiles()
}

async function handleDeleteFile(row: AdminFile) {
  await ElMessageBox.confirm(`确定删除文件 ${row.fileName}？`, '删除确认', { type: 'warning' })
  await deleteAdminFile(row.id)
  ElMessage.success('已删除')
  loadFiles()
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadStats()
  loadUsers()
  loadFiles()
})
</script>

<style scoped lang="scss">
.admin-page {
  height: 100%;
  overflow: auto;
  padding: var(--space-lg);
  background: var(--color-background);
}

.admin-container {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);

  h2 {
    margin: 0;
    font-size: var(--font-size-2xl, 1.5rem);
    font-weight: 700;
    color: var(--color-text);
  }
}

.admin-tabs {
  :deep(.el-tabs__content) {
    padding-top: var(--space-md);
  }
}

// Stats grid
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
  margin-bottom: var(--space-lg);

  @media (max-width: 767px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: var(--card-bg, var(--color-surface));
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border, var(--color-border));
  padding: var(--space-lg);
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.users { background: rgba(59, 130, 246, 0.1); color: #3B82F6; }
  &.files { background: rgba(16, 185, 129, 0.1); color: #10B981; }
  &.storage { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
  &.ai { background: rgba(124, 58, 237, 0.1); color: #7C3AED; }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

// Tab toolbar
.tab-toolbar {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
  align-items: center;
}

// Charts
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
  margin-top: var(--space-md);

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--card-bg, var(--color-surface));
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border, var(--color-border));
  padding: var(--space-md);

  &.full-width {
    margin-top: var(--space-md);
  }

  h4 {
    margin: 0 0 var(--space-sm);
    font-size: var(--font-size-sm);
    font-weight: 600;
    color: var(--color-text);
  }
}

.chart-container {
  width: 100%;
  height: 260px;
}

// Pagination
.pagination-bar {
  display: flex;
  justify-content: center;
  padding: var(--space-md) 0;
}

@media (max-width: 767px) {
  .admin-page { padding: var(--space-md); }
}
</style>
