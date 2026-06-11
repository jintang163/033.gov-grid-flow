import NProgress from 'nprogress'
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/event',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'Event',
        component: () => import('@/views/event/index.vue'),
        meta: { title: '事件管理', icon: 'Document' }
      }
    ]
  },
  {
    path: '/grid',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'Grid',
        component: () => import('@/views/grid/index.vue'),
        meta: { title: '网格管理', icon: 'Grid' }
      }
    ]
  },
  {
    path: '/user',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      }
    ]
  },
  {
    path: '/statistics',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '统计分析', icon: 'DataAnalysis' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  NProgress.start()
  const hasToken = getToken()
  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      next()
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
