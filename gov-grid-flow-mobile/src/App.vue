<template>
  <div id="app">
    <div v-if="!offlineStore.isOnline" class="offline-banner">
      <van-icon name="warning-o" />
      <span>当前网络不可用，提交的内容将暂存本地</span>
    </div>
    <router-view />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useOfflineStore, useUserStore } from '@/store'

const offlineStore = useOfflineStore()
const userStore = useUserStore()

let deviceId = localStorage.getItem('device_id')
if (!deviceId) {
  deviceId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  localStorage.setItem('device_id', deviceId)
}

onMounted(() => {
  offlineStore.initNetworkListener()
  offlineStore.refresh()

  if (userStore.isLogin) {
    offlineStore.startAutoSync(userStore.userId, deviceId)
  }
})

watch(
  () => userStore.isLogin,
  (isLogin) => {
    if (isLogin) {
      offlineStore.startAutoSync(userStore.userId, deviceId)
    } else {
      offlineStore.stopAutoSync()
    }
  }
)

onUnmounted(() => {
  offlineStore.stopAutoSync()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  background-color: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB',
    'Microsoft Yahei', sans-serif;
}

.offline-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  background: #fff7e6;
  color: #fa8c16;
  font-size: 13px;
  border-bottom: 1px solid #ffd591;
}
</style>
