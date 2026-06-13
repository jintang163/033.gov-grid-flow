<template>
  <div class="profile-container">
    <van-nav-bar title="我的" fixed placeholder />

    <div class="profile-content">
      <div class="user-card">
        <van-image
          round
          width="64"
          height="64"
          :src="userAvatar"
          class="user-avatar"
        />
        <div class="user-info">
          <div class="user-name">{{ userName }}</div>
          <div class="user-role-grid">
            <van-tag plain type="primary" class="role-tag">{{ roleText }}</van-tag>
            <span class="grid-text" v-if="gridName">
              <van-icon name="location-o" size="12" /> {{ gridName }}
            </span>
          </div>
        </div>
        <van-icon name="arrow" class="arrow-icon" @click="goEditProfile" />
      </div>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell title="我的上报" icon="edit" is-link @click="goMyReport">
          <template #right-icon>
            <span class="cell-count">{{ reportCount }}</span>
          </template>
        </van-cell>
        <van-cell title="我的待办" icon="todo-list-o" is-link @click="goTodo">
          <template #right-icon>
            <span class="cell-count warn">{{ todoCount }}</span>
          </template>
        </van-cell>
        <van-cell title="我的已办" icon="passed" is-link @click="goMyDone">
          <template #right-icon>
            <span class="cell-count success">{{ doneCount }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell title="离线事件同步" icon="exchange" @click="onManualSync">
          <template #right-icon>
            <span v-if="offlineStore.syncing" class="sync-status syncing">
              <van-loading size="16px" /> 同步中
            </span>
            <span v-else-if="offlineStore.pendingCount > 0" class="sync-status pending">
              {{ offlineStore.pendingCount }}条待同步
            </span>
            <span v-else class="sync-status synced">
              已同步
            </span>
          </template>
        </van-cell>
        <van-cell title="已同步缓存" icon="description" is-link @click="onClearSynced">
          <template #right-icon>
            <span class="cell-count success">{{ offlineStore.stats.synced }}条</span>
          </template>
        </van-cell>
        <van-cell title="网络状态" icon="wifi">
          <template #right-icon>
            <span :class="offlineStore.isOnline ? 'net-online' : 'net-offline'">
              <i :class="['dot', offlineStore.isOnline ? 'online' : 'offline']"></i>
              {{ offlineStore.isOnline ? '在线' : '离线' }}
            </span>
          </template>
        </van-cell>
        <van-cell title="修改密码" icon="lock" is-link @click="goChangePassword" />
      </van-cell-group>

      <div class="logout-btn">
        <van-button block round type="danger" @click="handleLogout">
          退出登录
        </van-button>
      </div>
    </div>

    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/report" icon="add-o">上报</van-tabbar-item>
      <van-tabbar-item to="/todo" icon="todo-list-o">待办</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>

    <van-dialog
      v-model:show="showPasswordDialog"
      title="修改密码"
      show-cancel-button
      @confirm="onPasswordConfirm"
    >
      <van-form>
        <van-field
          v-model="passwordForm.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
        />
        <van-field
          v-model="passwordForm.newPassword"
          type="password"
          label="新密码"
          placeholder="请输入新密码"
          :rules="[{ required: true, message: '请输入新密码' }]"
        />
        <van-field
          v-model="passwordForm.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          :rules="[{ required: true, message: '请再次输入新密码' }]"
        />
      </van-form>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore, useOfflineStore } from '@/store'
import { getMyTodo, getMyReport, getMyDone, changePassword } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const offlineStore = useOfflineStore()
const active = ref(3)
const showPasswordDialog = ref(false)

let deviceId = localStorage.getItem('device_id')
if (!deviceId) {
  deviceId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  localStorage.setItem('device_id', deviceId)
}

const onManualSync = async () => {
  if (!offlineStore.isOnline) {
    showToast('当前网络不可用，无法同步')
    return
  }
  try {
    const result = await offlineStore.processQueue(userStore.userId, deviceId)
    if (result) {
      showToast(
        `同步完成：成功${result.successCount}条，失败${result.failedCount}条`
      )
    }
  } catch (e) {
    showToast('同步失败：' + (e.message || '未知错误'))
  }
}

const onClearSynced = async () => {
  try {
    await showConfirmDialog({
      title: '提示',
      message: `确定清除 ${offlineStore.stats.synced} 条已同步的本地缓存吗？`,
      confirmButtonText: '清除',
      cancelButtonText: '取消'
    })
    offlineStore.clearSynced()
    showToast({ type: 'success', message: '已清除缓存' })
  } catch {}
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const todoCount = ref(0)
const reportCount = ref(0)
const doneCount = ref(0)

const roleMap = {
  admin: '系统管理员',
  grid_leader: '网格长',
  worker: '网格员',
  handler: '处置员'
}

const userName = computed(() => userStore.userName || '未登录用户')
const userAvatar = computed(() => userStore.userAvatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg')
const userRole = computed(() => userStore.userRole || '')
const roleText = computed(() => roleMap[userRole.value] || userRole.value || '未设置角色')
const gridName = computed(() => userStore.userInfo?.gridName || userStore.userInfo?.grid?.gridName || '')

const fetchCounts = async () => {
  try {
    const [todoRes, reportRes, doneRes] = await Promise.all([
      getMyTodo({ page: 1, size: 1 }),
      getMyReport({ page: 1, size: 1 }),
      getMyDone({ page: 1, size: 1 })
    ])
    todoCount.value = todoRes.data?.total || 0
    reportCount.value = reportRes.data?.total || 0
    doneCount.value = doneRes.data?.total || 0
  } catch {
    todoCount.value = 5
    reportCount.value = 3
    doneCount.value = 12
  }
}

const goMyReport = () => router.push('/todo')
const goTodo = () => router.push('/todo')
const goMyDone = () => router.push('/todo')
const goEditProfile = () => showToast('个人资料功能')

const goChangePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordDialog.value = true
}

const onPasswordConfirm = async () => {
  if (!passwordForm.oldPassword) {
    showToast('请输入原密码')
    return
  }
  if (!passwordForm.newPassword) {
    showToast('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    showToast('新密码长度不能少于6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    showToast({ type: 'success', message: '密码修改成功' })
  } catch (e) {
    showToast(e.message || '密码修改失败，请重试')
    return Promise.reject()
  }
}

const handleLogout = async () => {
  try {
    await showConfirmDialog({
      title: '提示',
      message: '确定要退出登录吗？',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
    await userStore.logout()
    showToast('已退出登录')
    router.replace('/login')
  } catch {}
}

onMounted(() => {
  offlineStore.refresh()
  fetchCounts()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  padding-bottom: 50px;
  background: #f7f8fa;
}

.profile-content {
  padding: 12px 0;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 24px 16px;
  background: linear-gradient(135deg, #1989fa 0%, #5fb7ff 100%);
  color: #fff;
  margin: 0 12px;
  border-radius: 12px;
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  margin-left: 16px;
  overflow: hidden;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
  line-height: 1.2;
}

.user-role-grid {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
}

.grid-text {
  font-size: 12px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 2px;
}

.arrow-icon {
  color: rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

.cell-count {
  font-size: 13px;
  color: #969799;
  margin-right: 4px;
}

.cell-count.warn {
  color: #ff976a;
  font-weight: 500;
}

.cell-count.success {
  color: #07c160;
  font-weight: 500;
}

.logout-btn {
  margin: 32px 16px 0;
}

:deep(.van-field__label) {
  width: 90px;
}

.sync-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  margin-right: 4px;

  &.syncing {
    color: #1989fa;
  }

  &.pending {
    color: #ff976a;
    font-weight: 500;
  }

  &.synced {
    color: #07c160;
  }
}

.net-online,
.net-offline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-right: 4px;

  .dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.online {
      background: #07c160;
      box-shadow: 0 0 4px #07c160;
    }

    &.offline {
      background: #969799;
    }
  }
}

.net-online {
  color: #07c160;
}

.net-offline {
  color: #969799;
}
</style>
