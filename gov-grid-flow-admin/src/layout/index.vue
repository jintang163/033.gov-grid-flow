<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapse">政务网格系统</span>
        <span v-else>政务</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :default-openeds="defaultOpeneds"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-sub-menu v-if="route.children.length > 1" :index="route.path">
            <template #title>
              <el-icon>
                <component :is="route.meta?.icon || route.children[0].meta.icon" />
              </el-icon>
              <span>{{ route.meta?.title || route.children[0].meta.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="route.path + '/' + child.path"
            >
              <el-icon>
                <component :is="child.meta.icon" />
              </el-icon>
              <template #title>{{ child.meta.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="resolvePath(route)">
            <el-icon>
              <component :is="route.children[0].meta.icon" />
            </el-icon>
            <template #title>{{ route.children[0].meta.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const isCollapse = computed(() => !appStore.sidebar.opened)

const activeMenu = computed(() => route.path)

const defaultOpeneds = computed(() => {
  const matched = route.matched
  if (matched.length >= 2 && matched[0].path !== '/') {
    return [matched[0].path]
  }
  return []
})

const hasAnyPermission = (children, roles) => {
  return children.some(child => {
    if (child.meta && child.meta.roles) {
      return roles.some(role => child.meta.roles.includes(role))
    }
    return true
  })
}

const menuRoutes = computed(() => {
  const currentRole = userStore.role
  if (!currentRole) return []
  const roles = [currentRole]
  return router.options.routes.filter(r => {
    if (!r.children || r.children.length === 0 || r.path === '/login') return false
    if (r.meta && r.meta.roles) {
      if (!roles.some(role => r.meta.roles.includes(role))) {
        return false
      }
    }
    return hasAnyPermission(r.children, roles)
  }).map(r => {
    if (r.children.length <= 1) return r
    const filteredChildren = r.children.filter(child => {
      if (child.meta && child.meta.roles) {
        return roles.some(role => child.meta.roles.includes(role))
      }
      return true
    })
    return { ...r, children: filteredChildren }
  })
})

const breadcrumbs = computed(() => {
  return route.matched.filter(r => r.meta && r.meta.title)
})

function resolvePath(route) {
  return route.path + '/' + route.children[0].path
}

function toggleSidebar() {
  appStore.toggleSidebar()
}

async function handleCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      ElMessage.success('退出成功')
      router.push('/login')
    } catch (e) {
    }
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    background-color: #2b2f3a;
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
      }
    }
  }
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
