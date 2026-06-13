import NProgress from 'nprogress'
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'

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
        meta: { title: '首页', icon: 'HomeFilled', roles: ['admin', 'street_manager', 'grid_leader', 'worker', 'handler', 'supervisor'] }
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
        meta: { title: '事件管理', icon: 'Document', roles: ['admin', 'street_manager', 'grid_leader', 'worker', 'handler', 'supervisor'] }
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
        meta: { title: '网格管理', icon: 'Grid', roles: ['admin', 'street_manager', 'grid_leader'] }
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
        meta: { title: '用户管理', icon: 'User', roles: ['admin', 'street_manager'] }
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
        meta: { title: '统计分析', icon: 'DataAnalysis', roles: ['admin', 'street_manager', 'grid_leader', 'supervisor'] }
      }
    ]
  },
  {
    path: '/urge',
    component: () => import('@/layout/index.vue'),
    redirect: '/urge/rule',
    meta: { title: '催办管理', icon: 'AlarmClock', roles: ['admin', 'street_manager', 'grid_leader'] },
    children: [
      {
        path: 'rule',
        name: 'UrgeRule',
        component: () => import('@/views/urge/rule.vue'),
        meta: { title: '催办规则', icon: 'Setting', roles: ['admin', 'street_manager'] }
      },
      {
        path: 'template',
        name: 'UrgeTemplate',
        component: () => import('@/views/urge/template.vue'),
        meta: { title: '催办模板', icon: 'Document', roles: ['admin', 'street_manager'] }
      },
      {
        path: 'record',
        name: 'UrgeRecord',
        component: () => import('@/views/urge/record.vue'),
        meta: { title: '催办记录', icon: 'Bell', roles: ['admin', 'street_manager', 'grid_leader'] }
      }
    ]
  },
  {
    path: '/analysis',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'EventAnalysis',
        component: () => import('@/views/analysis/index.vue'),
        meta: { title: '关联分析报告', icon: 'DataLine', roles: ['admin', 'street_manager', 'grid_leader', 'supervisor'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const whiteList = ['/login']

function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  }
  return true
}

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const hasToken = getToken()
  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      const userStore = useUserStore()
      if (!userStore.role) {
        try {
          await userStore.getUserInfoAction()
        } catch (e) {
          userStore.resetState()
          const tokenKey = (await import('@/utils/auth')).getTokenKey ? '' : 'token'
          ;(await import('@/utils/auth')).removeToken()
          next(`/login?redirect=${to.path}`)
          NProgress.done()
          return
        }
      }
      const currentRole = userStore.role
      if (!currentRole) {
        next()
        return
      }
      const roles = [currentRole]
      const targetRoute = to.matched.length > 0 ? to.matched[to.matched.length - 1] : null
      if (targetRoute && targetRoute.meta && targetRoute.meta.roles) {
        if (hasPermission(roles, targetRoute)) {
          next()
        } else {
          next({ path: '/dashboard' })
          NProgress.done()
        }
      } else {
        next()
      }
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
