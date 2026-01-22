import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layout/BasicLayout.vue'
import { getToken } from '@/utils/api'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      redirect: '/files',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'files/:path*',
          name: 'Files',
          component: () => import('@/views/file/FileMain.vue'),
          meta: { title: '全部文件' }
        },
        {
          path: 'recent',
          name: 'Recent',
          component: () => import('@/views/file/FileMain.vue'),
          meta: { title: '最近访问' }
        },
        {
          path: 'photos',
          name: 'Photos',
          component: () => import('@/views/file/FileMain.vue'),
          meta: { title: '相册', fileType: 'image' }
        },
        {
          path: 'shares',
          name: 'MyShares',
          component: () => import('@/views/file/FileMain.vue'),
          meta: { title: '我的分享' }
        },
        {
          path: 'recycle',
          name: 'Recycle',
          component: () => import('@/views/file/FileMain.vue'),
          meta: { title: '回收站', isRecycle: true }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', guest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册', guest: true }
    },
    {
      path: '/s/:code',
      name: 'ShareAccess',
      component: () => import('@/views/ShareView.vue'),
      meta: { title: '文件分享', public: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      redirect: '/'
    }
  ]
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = getToken()
  const isLoggedIn = !!token

  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - SmartNetdisk`
  } else {
    document.title = 'SmartNetdisk'
  }

  // 公开页面，不需要登录
  if (to.meta?.public) {
    next()
    return
  }

  // 访客页面（登录/注册），已登录则跳转首页
  if (to.meta?.guest) {
    if (isLoggedIn) {
      next('/')
    } else {
      next()
    }
    return
  }

  // 需要登录的页面
  if (to.meta?.requiresAuth) {
    if (isLoggedIn) {
      next()
    } else {
      next('/login')
    }
    return
  }

  next()
})

export default router
