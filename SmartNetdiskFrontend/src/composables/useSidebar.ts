import { ref, computed, watch, onMounted } from 'vue'
import { useMediaQuery } from './useMediaQuery'

const STORAGE_KEY = 'smartnetdisk-sidebar-collapsed'

// Global reactive state
const collapsed = ref(false)
const mobileOpen = ref(false)

export function useSidebar() {
  const isMobile = useMediaQuery('(max-width: 767px)')

  // Load saved state on mount
  onMounted(() => {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved !== null) {
      collapsed.value = saved === 'true'
    }
  })

  // Save state when collapsed changes (desktop only)
  watch(collapsed, (value) => {
    if (!isMobile.value) {
      localStorage.setItem(STORAGE_KEY, String(value))
    }
  })

  // Auto-close mobile drawer on resize to desktop
  watch(isMobile, (mobile) => {
    if (!mobile) {
      mobileOpen.value = false
    }
  })

  // Computed width based on state
  const sidebarWidth = computed(() => {
    if (isMobile.value) {
      return '240px' // Full width in mobile drawer
    }
    return collapsed.value ? '72px' : '240px'
  })

  // Whether sidebar is visible (for mobile drawer)
  const isOpen = computed(() => {
    if (isMobile.value) {
      return mobileOpen.value
    }
    return true // Always visible on desktop
  })

  function toggle() {
    if (isMobile.value) {
      mobileOpen.value = !mobileOpen.value
    } else {
      collapsed.value = !collapsed.value
    }
  }

  function open() {
    if (isMobile.value) {
      mobileOpen.value = true
    } else {
      collapsed.value = false
    }
  }

  function close() {
    if (isMobile.value) {
      mobileOpen.value = false
    } else {
      collapsed.value = true
    }
  }

  function setCollapsed(value: boolean) {
    collapsed.value = value
  }

  function setMobileOpen(value: boolean) {
    mobileOpen.value = value
  }

  return {
    collapsed,
    mobileOpen,
    isMobile,
    sidebarWidth,
    isOpen,
    toggle,
    open,
    close,
    setCollapsed,
    setMobileOpen
  }
}
