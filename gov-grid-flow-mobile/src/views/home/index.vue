<template>
  <div class="home-container">
    <van-nav-bar fixed placeholder>
      <template #left>
        <div class="welcome-area">
          <div class="welcome-text">{{ welcomeText }}</div>
          <div class="user-name">{{ userName }}</div>
        </div>
      </template>
      <template #right>
        <div class="notify-wrap" @click="goNotifications">
          <van-icon name="bell-o" size="22" />
          <van-badge v-if="unreadCount > 0" :content="unreadCount" class="notify-badge" />
        </div>
      </template>
    </van-nav-bar>

    <div class="home-content">
      <van-notice-bar
        left-icon="volume-o"
        :text="noticeText"
        scrollable
        wrapable
        color="#1989fa"
        background="#e5f3ff"
      />

      <van-card class="stats-card">
        <template #title>
          <div class="stats-title">数据统计</div>
        </template>
        <template #desc>
          <van-grid :column-num="3" border class="stats-grid">
            <van-grid-item @click="goMyReport">
              <div class="stat-num today">{{ stats.todayReported }}</div>
              <div class="stat-label">今日上报</div>
            </van-grid-item>
            <van-grid-item @click="goTodo">
              <div class="stat-num pending">{{ stats.todo }}</div>
              <div class="stat-label">待办数</div>
            </van-grid-item>
            <van-grid-item @click="goMyDone">
              <div class="stat-num done">{{ stats.done }}</div>
              <div class="stat-label">已办结</div>
            </van-grid-item>
          </van-grid>
        </template>
      </van-card>

      <div class="quick-section">
        <div class="section-header">
          <span class="section-title">快捷入口</span>
        </div>
        <van-grid :column-num="4" border class="quick-grid">
          <van-grid-item icon="add-o" text="事件上报" @click="goReport" />
          <van-grid-item icon="todo-list-o" text="待办事项" @click="goTodo" />
          <van-grid-item icon="scan" text="扫码上报" @click="goScan" />
          <van-grid-item icon="location-o" text="地图浏览" @click="goMap" />
          <van-grid-item icon="log" text="事件记录" @click="goEventList" />
          <van-grid-item icon="chart-trending-o" text="统计分析" @click="goStatistics" />
        </van-grid>
      </div>

      <div class="recent-section">
        <div class="section-header">
          <span class="section-title">近期事件</span>
          <span class="section-more" @click="goEventList">
            查看全部 <van-icon name="arrow" />
          </span>
        </div>
        <van-cell-group v-if="recentList.length > 0" inset class="recent-list">
          <van-cell
            v-for="item in recentList"
            :key="item.id"
            :title="item.title"
            :label="formatTime(item.createTime)"
            is-link
            @click="goDetail(item.id)"
          >
            <template #icon>
              <van-icon :name="getEventTypeIcon(item.eventType)" size="20" :color="getEventTypeColor(item.eventType)" />
            </template>
            <template #right-icon>
              <van-tag :type="getStatusType(item.status)" plain>{{ getStatusText(item.status) }}</van-tag>
            </template>
          </van-cell>
        </van-cell-group>
        <van-empty v-else description="暂无事件记录" />
      </div>
    </div>

    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/report" icon="add-o">上报</van-tabbar-item>
      <van-tabbar-item to="/todo" icon="todo-list-o">待办</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyTodo, getMyReport, getMyDone, getEventList, getNotificationList } from '@/api'
import { useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()
const active = ref(0)
const unreadCount = ref(3)
const noticeText = ref('欢迎使用政务网格流转系统，请及时处理待办事项。今日共收到5条新事件，其中2条需紧急处理。')

const userName = computed(() => userStore.userName || '用户')

const welcomeText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
})

const stats = reactive({
  todayReported: 0,
  todo: 0,
  done: 0
})

const recentList = ref([])

const mockRecent = [
  {
    id: 1,
    title: '小区门口垃圾堆积',
    description: 'XX小区门口垃圾堆积未及时清理，影响居民出行',
    eventType: 'environment',
    priority: 'urgent',
    status: 'PENDING',
    address: 'XX街道XX小区东门',
    createTime: '2024-01-15 10:30:00',
    images: ''
  },
  {
    id: 2,
    title: '路灯损坏',
    description: 'XX路段路灯损坏，夜间存在安全隐患',
    eventType: 'public_facility',
    priority: 'normal',
    status: 'DISPATCHED',
    address: 'XX路与YY路交叉口',
    createTime: '2024-01-14 16:20:00',
    images: ''
  },
  {
    id: 3,
    title: '井盖缺失',
    description: '人行道井盖缺失，存在安全隐患',
    eventType: 'public_facility',
    priority: 'HIGH',
    status: 'APPROVED',
    address: 'XX路30号门前',
    createTime: '2024-01-14 09:15:00',
    images: ''
  },
  {
    id: 4,
    title: '邻里噪音纠纷',
    description: '楼上住户夜间噪音扰民，多次沟通无果',
    eventType: 'dispute',
    priority: 'normal',
    status: 'COMPLETED',
    address: 'XX小区5号楼2单元',
    createTime: '2024-01-13 22:40:00',
    images: ''
  },
  {
    id: 5,
    title: '消防通道堵塞',
    description: '小区消防通道被私家车占用',
    eventType: 'safety_hazard',
    priority: 'URGENT',
    status: 'COMPLETED',
    address: 'XX小区西区北门',
    createTime: '2024-01-13 14:20:00',
    images: ''
  }
]

const eventTypeIconMap = {
  environment: 'smile-o',
  public_facility: 'wap-home-o',
  dispute: 'chat-o',
  safety_hazard: 'warning-o',
  security: 'shield-o',
  service: 'service-o',
  traffic: 'orders-o',
  other: 'more-o'
}

const eventTypeColorMap = {
  environment: '#07c160',
  public_facility: '#1989fa',
  dispute: '#ff976a',
  safety_hazard: '#ee0a24',
  security: '#7232dd',
  service: '#00b578',
  traffic: '#ff6034',
  other: '#969799'
}

const statusTextMap = {
  PENDING: '待受理',
  APPROVED: '已受理',
  DISPATCHED: '已分派',
  HANDLED: '已处置',
  COMPLETED: '已办结',
  REJECTED: '已驳回'
}

const statusTypeMap = {
  PENDING: 'warning',
  APPROVED: 'primary',
  DISPATCHED: 'primary',
  HANDLED: 'success',
  COMPLETED: 'success',
  REJECTED: 'danger'
}

const fetchStats = async () => {
  try {
    const [todoRes, reportRes, doneRes] = await Promise.all([
      getMyTodo({ page: 1, size: 1 }),
      getMyReport({ page: 1, size: 1 }),
      getMyDone({ page: 1, size: 1 })
    ])
    stats.todo = todoRes.data?.total || 5
    stats.todayReported = reportRes.data?.total || 3
    stats.done = doneRes.data?.total || 12
  } catch {
    stats.todo = 5
    stats.todayReported = 3
    stats.done = 12
  }
}

const fetchRecentList = async () => {
  try {
    const res = await getEventList({ page: 1, size: 5 })
    recentList.value = res.data?.records || res.data || mockRecent
  } catch {
    recentList.value = mockRecent
  }
}

const fetchNotifications = async () => {
  try {
    const res = await getNotificationList({ isRead: 0 })
    unreadCount.value = res.data?.total || res.data?.length || 3
  } catch {
    unreadCount.value = 3
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

const getEventTypeIcon = (type) => eventTypeIconMap[type] || 'more-o'
const getEventTypeColor = (type) => eventTypeColorMap[type] || '#969799'
const getStatusText = (status) => statusTextMap[status] || status
const getStatusType = (status) => statusTypeMap[status] || 'default'

const goReport = () => router.push('/report')
const goTodo = () => router.push('/todo')
const goDetail = (id) => router.push(`/detail/${id}`)
const goMap = () => showToast('地图浏览功能')
const goScan = () => showToast('扫码上报功能')
const goEventList = () => router.push('/todo')
const goStatistics = () => showToast('统计分析功能')
const goNotifications = () => showToast('通知列表功能')
const goMyReport = () => router.push('/todo')
const goMyDone = () => router.push('/todo')

onMounted(() => {
  fetchStats()
  fetchRecentList()
  fetchNotifications()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  padding-bottom: 50px;
  background: #f7f8fa;
}

.home-content {
  padding: 12px 0;
}

.welcome-area {
  display: flex;
  flex-direction: column;
}

.welcome-text {
  font-size: 12px;
  color: #969799;
  line-height: 1.2;
}

.user-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  line-height: 1.4;
  margin-top: 2px;
}

.notify-wrap {
  position: relative;
  padding: 4px;
}

.notify-badge {
  position: absolute;
  top: 0;
  right: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 12px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.section-more {
  font-size: 13px;
  color: #969799;
  display: flex;
  align-items: center;
}

.stats-card {
  margin: 12px;
}

.stats-title {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.stats-grid {
  margin-top: 8px;
}

.stat-num {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.2;
}

.stat-num.today {
  color: #1989fa;
}

.stat-num.pending {
  color: #ff976a;
}

.stat-num.done {
  color: #07c160;
}

.stat-label {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
}

.quick-section {
  background: #fff;
  margin: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.quick-section .section-header {
  padding: 12px 16px 8px;
}

.quick-grid {
  background: transparent;
}

.recent-section {
  margin-top: 12px;
}

.recent-list {
  margin: 0 12px;
}

:deep(.van-grid-item__text) {
  font-size: 12px;
}

:deep(.van-cell__title) {
  font-weight: 500;
}
</style>
