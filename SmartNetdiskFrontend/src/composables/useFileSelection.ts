import { ref } from 'vue'
import type { FileInfo } from '@/api/file'

export function useFileSelection(fileList: import('vue').Ref<FileInfo[]>) {
  const selectedFiles = ref<FileInfo[]>([])

  const isFileSelected = (file: FileInfo) => {
    return selectedFiles.value.some(f => f.id === file.id)
  }

  const toggleFileSelection = (file: FileInfo, event?: MouseEvent) => {
    const index = selectedFiles.value.findIndex(f => f.id === file.id)
    if (index >= 0) {
      selectedFiles.value.splice(index, 1)
    } else {
      if (event?.shiftKey && selectedFiles.value.length > 0) {
        const lastSelected = selectedFiles.value[selectedFiles.value.length - 1]
        const lastIndex = fileList.value.findIndex(f => f.id === lastSelected?.id)
        const currentIndex = fileList.value.findIndex(f => f.id === file.id)
        const start = Math.min(lastIndex, currentIndex)
        const end = Math.max(lastIndex, currentIndex)
        for (let i = start; i <= end; i++) {
          const f = fileList.value[i]
          if (f && !selectedFiles.value.some(s => s.id === f.id)) {
            selectedFiles.value.push(f)
          }
        }
      } else {
        selectedFiles.value.push(file)
      }
    }
  }

  const clearSelection = () => {
    selectedFiles.value = []
  }

  const handleSelectionChange = (selection: FileInfo[]) => {
    selectedFiles.value = selection
  }

  return {
    selectedFiles,
    isFileSelected,
    toggleFileSelection,
    clearSelection,
    handleSelectionChange
  }
}
