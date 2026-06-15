import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('@/views/report/index.vue'),
    meta: { title: '上报', requiresAuth: true }
  },
  {
    path: '/todo',
    name: 'Todo',
    component: () => import('@/views/todo/index.vue'),
    meta: { title: '待办', requiresAuth: true }
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: () => import('@/views/detail/index.vue'),
    meta: { title: '详情', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '我的', requiresAuth: true }
  },
  {
    path: '/voice-settings',
    name: 'VoiceSettings',
    component: () => import('@/views/profile/voice.vue'),
    meta: { title: '语音播报设置', requiresAuth: true }
  },
  {
    path: '/warning',
    name: 'Warning',
    component: () => import('@/views/warning/index.vue'),
    meta: { title: '预警消息', requiresAuth: true }
  },
  {
    path: '/warning-detail',
    name: 'WarningDetail',
    component: () => import('@/views/warning/detail.vue'),
    meta: { title: '预警详情', requiresAuth: true }
  },
  {
    path: '/warning-grid',
    name: 'WarningGrid',
    component: () => import('@/views/warning/grid.vue'),
    meta: { title: '网格预警', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '政务网格流转系统'
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
