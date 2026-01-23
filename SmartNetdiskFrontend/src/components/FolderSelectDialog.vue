<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="500px"
    @update:model-value="emit('update:visible', $event)"
    append-to-body
  >
    <div class="folder-tree-container" v-loading="loading">
      <el-tree
        ref="treeRef"
        :data="folderTree"
        :props="defaultProps"
        node-key="id"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        @current-change="handleCurrentChange"
      >
        <template #default="{ node, data }">
          <div class="custom-tree-node">
            <el-icon class="mr-2"><FolderOpened /></el-icon>
            <span>{{ node.label }}</span>
            <span v-if="data.id === currentFolderId" class="current-badge">当前位置</span>
          </div>
        </template>
      </el-tree>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :disabled="!selectedFolder" @click="handleConfirm">
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { FolderOpened } from '@element-plus/icons-vue'
import { getFolderTree } from '@/api/file'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
  title?: string
  currentFolderId?: number // 当前所在的文件夹ID，用于禁用移动到自己或子目录（暂简单实现）
  excludeIds?: number[] // 需要排除的文件夹ID（移动文件夹时不能移动到自己内部）
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'confirm', folderId: number): void
}>()

interface FolderNode {
  id: number
  folderName: string
  children?: FolderNode[]
  disabled?: boolean
}

const loading = ref(false)
const folderTree = ref<FolderNode[]>([])
const selectedFolder = ref<FolderNode | null>(null)

const defaultProps = {
  children: 'children',
  label: 'folderName',
  disabled: 'disabled'
}

const loadTree = async () => {
  loading.value = true
  try {
    // 获取文件夹树，根目录手动构造
    const subFolders = await getFolderTree()
    
    // 构造根节点
    const rootNode: FolderNode = {
      id: 0,
      folderName: '全部文件',
      children: subFolders,
    }
    
    folderTree.value = [rootNode]
  } catch (error) {
    console.error('加载文件夹树失败', error)
    ElMessage.error('加载文件夹列表失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    selectedFolder.value = null
    loadTree()
  }
})

const handleCurrentChange = (data: FolderNode) => {
  selectedFolder.value = data
}

const handleConfirm = () => {
  if (selectedFolder.value) {
    emit('confirm', selectedFolder.value.id)
    emit('update:visible', false)
  }
}
</script>

<style scoped>
.folder-tree-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 10px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.mr-2 {
  margin-right: 8px;
}

.current-badge {
  margin-left: 8px;
  font-size: 12px;
  color: #9ca3af;
  background-color: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
