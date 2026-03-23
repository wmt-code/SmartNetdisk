import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFileList, getRecycleList, getRecentFiles, type FileInfo } from '@/api/file'
import { ElMessage } from 'element-plus'

export function useFileList() {
  const route = useRoute()

  const loading = ref(false)
  const fileList = ref<FileInfo[]>([])
  const currentPage = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const currentFolderId = ref(0)
  const sortField = ref('time')
  const sortOrder = ref<'asc' | 'desc'>('desc')
  const searchKeyword = ref('')
  const folderPath = ref<{ id: number; name: string }[]>([])

  const isRecycleBin = computed(() => route.meta?.isRecycle === true)
  const isRecentView = computed(() => route.meta?.isRecent === true)
  const routeFileType = computed(() => (route.meta?.fileType as string) || '')

  const loadFileList = async () => {
    loading.value = true
    try {
      const params: any = {
        folderId: currentFolderId.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        orderBy:
          sortField.value === 'name'
            ? 'file_name'
            : sortField.value === 'size'
              ? 'file_size'
              : 'create_time',
        isAsc: sortOrder.value === 'asc',
        keyword: searchKeyword.value,
      }

      // 相册等按文件类型筛选的页面
      if (routeFileType.value) {
        params.fileType = routeFileType.value
      }

      if (searchKeyword.value) {
        delete params.folderId
      }

      let res
      if (isRecycleBin.value) {
        res = await getRecycleList(params)
      } else if (isRecentView.value) {
        res = await getRecentFiles(params)
      } else {
        res = await getFileList(params)
      }

      fileList.value = res.records
      total.value = res.total
    } catch (error) {
      console.error('加载文件列表失败:', error)
      ElMessage.error('加载文件列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSort = (command: string) => {
    if (command === 'toggleOrder') {
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortField.value = command
    }
    loadFileList()
  }

  const navigateToRoot = () => {
    folderPath.value = []
    currentFolderId.value = 0
    currentPage.value = 1
    loadFileList()
  }

  const navigateToFolder = (folderId: number, index: number) => {
    folderPath.value = folderPath.value.slice(0, index + 1)
    currentFolderId.value = folderId
    currentPage.value = 1
    loadFileList()
  }

  const enterFolder = (folder: FileInfo) => {
    folderPath.value.push({ id: folder.id, name: folder.fileName })
    currentFolderId.value = folder.id
    currentPage.value = 1
    loadFileList()
  }

  // Watch route keyword changes
  watch(
    () => route.query.keyword,
    (newVal) => {
      searchKeyword.value = (newVal as string) || ''
      loadFileList()
    },
    { immediate: true },
  )

  // Watch route path changes
  watch(
    () => route.path,
    () => {
      currentPage.value = 1
      loadFileList()
    },
    { immediate: false },
  )

  return {
    loading,
    fileList,
    currentPage,
    pageSize,
    total,
    currentFolderId,
    sortField,
    sortOrder,
    searchKeyword,
    folderPath,
    isRecycleBin,
    isRecentView,
    loadFileList,
    handleSort,
    navigateToRoot,
    navigateToFolder,
    enterFolder,
  }
}
