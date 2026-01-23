<template>
  <div class="shares-main h-full w-full flex flex-col">
    <!-- 工具栏 -->
    <div class="toolbar flex items-center justify-between p-4 border-b border-gray-100">
      <h2 class="text-lg font-medium text-gray-800">我的分享</h2>
      <div class="flex items-center gap-2">
        <el-radio-group v-model="statusFilter" size="small" @change="loadShares">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="1">有效</el-radio-button>
          <el-radio-button :value="2">已过期</el-radio-button>
          <el-radio-button :value="0">已取消</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 分享列表 -->
    <div class="shares-content flex-1 overflow-auto p-4">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 列表 -->
      <el-table
        v-else-if="shareList.length > 0"
        :data="shareList"
        style="width: 100%"
        class="shares-table"
      >
        <el-table-column prop="fileName" label="分享内容" min-width="280">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon :size="20" :color="getShareTypeColor(row.shareType)">
                <component :is="getShareTypeIcon(row.shareType)" />
              </el-icon>
              <div class="file-info">
                <span class="font-medium">{{ getShareDisplayName(row) }}</span>
                <div class="share-meta">
                  <el-tag size="small" :type="getShareTypeTagType(row.shareType)" class="type-tag">
                    {{ formatShareType(row.shareType) }}
                  </el-tag>
                  <span v-if="row.fileCount > 1" class="file-count">{{ row.fileCount }} 个文件</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分享链接" min-width="200">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span class="text-primary text-sm">{{ getShortLink(row.shareCode) }}</span>
              <el-button link size="small" @click="copyLink(row.shareCode)">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提取码" width="100">
          <template #default="{ row }">
            <span v-if="row.password" class="font-mono text-sm">{{ row.password }}</span>
            <span v-else class="text-gray-400">无</span>
          </template>
        </el-table-column>
        <el-table-column label="访问/下载" width="100">
          <template #default="{ row }">
            <span class="text-sm text-gray-600">{{ row.viewCount }} / {{ row.downloadCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="160">
          <template #default="{ row }">
            <span v-if="!row.expireTime" class="text-green-600">永久有效</span>
            <span v-else-if="isExpired(row.expireTime)" class="text-red-500">已过期</span>
            <span v-else class="text-gray-600">{{ formatExpireTime(row.expireTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">有效</el-tag>
            <el-tag v-else-if="row.status === 2" type="info" size="small">已过期</el-tag>
            <el-tag v-else type="danger" size="small">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-sm text-gray-500">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 1" 
              link 
              type="danger" 
              @click="handleCancel(row)"
            >
              取消分享
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-else-if="!loading" description="暂无分享记录" class="mt-20" />
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-container p-4 border-t border-gray-100 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadShares"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Document, DocumentCopy, Loading, FolderOpened, Folder } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getMyShares, 
  cancelShare, 
  generateShareLink, 
  formatShareType,
  ShareType,
  type ShareInfo 
} from '@/api/share'

// 分页
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

// 状态
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const shareList = ref<ShareInfo[]>([])

// 加载分享列表
async function loadShares() {
  loading.value = true
  try {
    const result = await getMyShares({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value
    })
    shareList.value = result.records
    total.value = result.total
  } catch (error) {
    console.error('加载分享列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 取消分享
async function handleCancel(share: ShareInfo) {
  try {
    await ElMessageBox.confirm(
      '确定要取消此分享吗？取消后分享链接将失效。',
      '取消分享',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await cancelShare(share.id)
    ElMessage.success('分享已取消')
    await loadShares()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消分享失败:', error)
    }
  }
}

// 复制链接
async function copyLink(code: string) {
  try {
    await navigator.clipboard.writeText(generateShareLink(code))
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 获取短链接显示
function getShortLink(code: string) {
  return `/s/${code}`
}

// 格式化时间
function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

// 格式化过期时间
function formatExpireTime(time: string) {
  if (!time) return '永久'
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 判断是否过期
function isExpired(time: string | null) {
  if (!time) return false
  return new Date(time) < new Date()
}

// 获取分享显示名称
function getShareDisplayName(row: ShareInfo) {
  if (row.shareType === ShareType.FOLDER && row.folderName) {
    return row.folderName
  }
  if (row.shareType === ShareType.BATCH && row.shareTitle) {
    return row.shareTitle
  }
  return row.fileName || '未命名分享'
}

// 获取分享类型图标
function getShareTypeIcon(shareType: number) {
  switch (shareType) {
    case ShareType.FOLDER: return FolderOpened
    case ShareType.BATCH: return Folder
    default: return Document
  }
}

// 获取分享类型颜色
function getShareTypeColor(shareType: number) {
  switch (shareType) {
    case ShareType.FOLDER: return '#F97316'
    case ShareType.BATCH: return '#7C3AED'
    default: return '#3B82F6'
  }
}

// 获取分享类型标签类型
function getShareTypeTagType(shareType: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (shareType) {
    case ShareType.FOLDER: return 'warning'
    case ShareType.BATCH: return 'primary'
    default: return 'info'
  }
}

onMounted(() => {
  loadShares()
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #7C3AED;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.text-primary {
  color: #7C3AED;
}

.font-mono {
  font-family: 'Courier New', Courier, monospace;
}

.shares-table :deep(.el-table__row) {
  transition: background-color 0.2s;
}

.shares-table :deep(.el-table__row:hover) {
  background-color: rgba(124, 58, 237, 0.05) !important;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.share-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-tag {
  font-size: 11px;
}

.file-count {
  font-size: 12px;
  color: #9CA3AF;
}
</style>
