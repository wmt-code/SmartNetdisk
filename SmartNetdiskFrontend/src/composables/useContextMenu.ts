import { ref } from 'vue'
import type { FileInfo } from '@/api/file'

export function useContextMenu() {
  const visible = ref(false)
  const position = ref({ x: 0, y: 0 })
  const target = ref<FileInfo | null>(null)

  const show = (event: MouseEvent, row: FileInfo) => {
    event.preventDefault()
    target.value = row

    // 计算菜单位置，防止超出视口
    const menuWidth = 200
    const menuHeight = 340 // 预估菜单高度
    let x = event.clientX
    let y = event.clientY

    if (x + menuWidth > window.innerWidth) {
      x = window.innerWidth - menuWidth - 8
    }
    if (y + menuHeight > window.innerHeight) {
      y = window.innerHeight - menuHeight - 8
    }
    if (y < 8) y = 8

    position.value = { x, y }
    visible.value = true

    const close = () => {
      visible.value = false
      document.removeEventListener('click', close)
    }
    document.addEventListener('click', close)
  }

  const hide = () => {
    visible.value = false
  }

  return {
    visible,
    position,
    target,
    show,
    hide,
  }
}
