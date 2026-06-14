<template>
  <div class="command-screen" :class="{ 'split-mode': isSplitMode }">
    <div class="screen-header">
      <div class="header-left">
        <span class="header-time">{{ currentTime }}</span>
      </div>
      <div class="header-center">
        <h1 class="header-title">街道指挥调度中心</h1>
      </div>
      <div class="header-right">
        <span class="ws-status" :class="{ connected: wsConnected }">
          <span class="dot"></span>
          {{ wsConnected ? '实时' : '离线' }}
        </span>
        <button class="screen-btn" @click="toggleSplitMode" :title="isSplitMode ? '退出分屏' : '分屏显示'">
          <el-icon><Grid /></el-icon>
        </button>
        <button class="screen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <el-icon><FullScreen /></el-icon>
        </button>
      </div>
    </div>

    <div class="screen-body">
      <div class="panel-left">
        <div class="panel overview-panel">
          <div class="panel-header">
            <span class="panel-title">今日概览</span>
          </div>
          <div class="panel-body">
            <div class="stat-grid">
              <div class="stat-item" v-for="item in overviewStats" :key="item.label">
                <span class="stat-value" :style="{ color: item.color }">{{ item.value }}</span>
                <span class="stat-label">{{ item.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel trend-panel">
          <div class="panel-header">
            <span class="panel-title">今日上报趋势</span>
          </div>
          <div class="panel-body">
            <div ref="trendChartRef" class="chart-box"></div>
          </div>
        </div>

        <div class="panel rank-panel">
          <div class="panel-header">
            <span class="panel-title">社区结案率排名</span>
          </div>
          <div class="panel-body">
            <div ref="rankChartRef" class="chart-box"></div>
          </div>
        </div>
      </div>

      <div class="panel-center">
        <div class="panel map-panel">
          <div class="panel-header">
            <span class="panel-title">事件热力图</span>
            <div class="panel-actions">
              <button
                v-for="mode in mapModes"
                :key="mode.value"
                :class="['map-mode-btn', { active: currentMapMode === mode.value }]"
                @click="currentMapMode = mode.value"
              >{{ mode.label }}</button>
            </div>
          </div>
          <div class="panel-body map-body">
            <div ref="mapContainerRef" class="map-container"></div>
          </div>
        </div>
      </div>

      <div class="panel-right">
        <div class="panel member-panel">
          <div class="panel-header">
            <span class="panel-title">网格员在线状态</span>
          </div>
          <div class="panel-body">
            <div class="member-summary">
              <div class="member-stat online">
                <span class="member-count">{{ onlineMemberCount }}</span>
                <span class="member-label">在线</span>
              </div>
              <div class="member-stat offline">
                <span class="member-count">{{ offlineMemberCount }}</span>
                <span class="member-label">离线</span>
              </div>
              <div class="member-rate">
                <span class="rate-value">{{ memberOnlineRate }}%</span>
                <span class="rate-label">在线率</span>
              </div>
            </div>
            <div class="member-list">
              <div
                v-for="member in memberList"
                :key="member.userId"
                class="member-item"
                @click="locateMember(member)"
              >
                <span class="member-dot" :class="member.onDuty === 1 ? 'online' : 'offline'"></span>
                <span class="member-name">{{ member.userName }}</span>
                <span class="member-grid">{{ member.gridName }}</span>
                <span v-if="member.battery != null" class="member-battery" :class="{ low: member.battery < 20 }">
                  {{ member.battery }}%
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel pending-panel">
          <div class="panel-header">
            <span class="panel-title">待处置事件</span>
            <span class="event-count">{{ pendingEvents.length }}</span>
          </div>
          <div class="panel-body">
            <div class="event-list">
              <div
                v-for="event in pendingEvents.slice(0, 8)"
                :key="event.eventId"
                class="event-item"
                @click="showEventDetail(event)"
              >
                <span class="event-priority" :class="getPriorityClass(event.priority)"></span>
                <div class="event-info">
                  <span class="event-title">{{ event.title }}</span>
                  <span class="event-meta">{{ event.eventType }} · {{ event.reportTime }}</span>
                </div>
                <button class="dispatch-btn" @click.stop="handleDispatch(event)">派单</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="eventDialogVisible"
      :title="currentEvent ? currentEvent.title : ''"
      width="520px"
      custom-class="event-detail-dialog"
      :append-to-body="true"
    >
      <div v-if="currentEvent" class="event-detail">
        <div class="detail-row">
          <span class="detail-label">事件编号</span>
          <span class="detail-value">{{ currentEvent.eventNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">事件类型</span>
          <span class="detail-value">{{ currentEvent.eventType }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">优先级</span>
          <span class="detail-value">
            <el-tag :type="priorityTagType(currentEvent.priority)" size="small">{{ priorityLabel(currentEvent.priority) }}</el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">{{ statusLabel(currentEvent.status) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">地址</span>
          <span class="detail-value">{{ currentEvent.address }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">上报人</span>
          <span class="detail-value">{{ currentEvent.reporterName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">上报时间</span>
          <span class="detail-value">{{ currentEvent.reportTime }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="eventDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDispatch(currentEvent)">一键派单</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dispatchDialogVisible"
      title="一键派单"
      width="440px"
      custom-class="dispatch-dialog"
      :append-to-body="true"
    >
      <el-form :model="dispatchForm" label-width="80px">
        <el-form-item label="事件">
          <span>{{ dispatchForm.eventTitle }}</span>
        </el-form-item>
        <el-form-item label="派给">
          <el-select v-model="dispatchForm.assigneeId" placeholder="选择网格员" filterable style="width: 100%">
            <el-option
              v-for="m in onlineMembers"
              :key="m.userId"
              :label="m.userName + ' (' + m.gridName + ')'"
              :value="m.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispatch" :loading="dispatching">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { FullScreen, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createDashboardWS } from '@/utils/websocket'
import { getDashboardAll, getEventDetail, dispatchEvent } from '@/api/screen'

const currentTime = ref('')
const wsConnected = ref(false)
const isFullscreen = ref(false)
const isSplitMode = ref(false)
const eventDialogVisible = ref(false)
const dispatchDialogVisible = ref(false)
const dispatching = ref(false)
const currentEvent = ref(null)
const currentMapMode = ref('heatmap')

const dispatchForm = ref({
  eventId: null,
  eventTitle: '',
  assigneeId: null,
  taskId: null
})

const mapModes = [
  { label: '热力图', value: 'heatmap' },
  { label: '标记图', value: 'marker' }
]

const trendChartRef = ref()
const rankChartRef = ref()
const mapContainerRef = ref()

let trendChart = null
let rankChart = null
let mapChart = null
let ws = null
let timeTimer = null

const overview = ref({
  todayReported: 0,
  todayCompleted: 0,
  pendingCount: 0,
  processingCount: 0,
  completedCount: 0,
  avgHandleTime: 0,
  onlineRate: 0,
  hourlyTrend: []
})

const eventMarkers = ref([])
const heatmapData = ref([])
const communityRank = ref([])
const memberStatus = ref([])

const overviewStats = computed(() => [
  { label: '今日上报', value: overview.value.todayReported, color: '#00d4ff' },
  { label: '今日办结', value: overview.value.todayCompleted, color: '#00ff88' },
  { label: '待处置', value: overview.value.pendingCount, color: '#ff6b6b' },
  { label: '处理中', value: overview.value.processingCount, color: '#ffd93d' },
  { label: '已办结', value: overview.value.completedCount, color: '#6bcb77' },
  { label: '平均时长(h)', value: overview.value.avgHandleTime, color: '#a29bfe' }
])

const onlineMemberCount = computed(() => memberStatus.value.filter(m => m.onDuty === 1).length)
const offlineMemberCount = computed(() => memberStatus.value.filter(m => m.onDuty !== 1).length)
const memberOnlineRate = computed(() => {
  if (memberStatus.value.length === 0) return 0
  return ((onlineMemberCount.value / memberStatus.value.length) * 100).toFixed(1)
})
const memberList = computed(() => memberStatus.value.slice(0, 20))
const onlineMembers = computed(() => memberStatus.value.filter(m => m.onDuty === 1))
const pendingEvents = computed(() => eventMarkers.value.filter(e => e.status === 'PENDING' || e.status === 'APPROVED'))

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  })
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function toggleSplitMode() {
  isSplitMode.value = !isSplitMode.value
  nextTick(() => {
    resizeCharts()
  })
}

function getPriorityClass(priority) {
  if (priority === 'HIGH' || priority === 'URGENT') return 'high'
  if (priority === 'MEDIUM') return 'medium'
  return 'low'
}

function priorityTagType(priority) {
  if (priority === 'HIGH' || priority === 'URGENT') return 'danger'
  if (priority === 'MEDIUM') return 'warning'
  return 'info'
}

function priorityLabel(priority) {
  const map = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[priority] || priority || '未知'
}

function statusLabel(status) {
  const map = { PENDING: '待受理', APPROVED: '已受理', DISPATCHED: '已分派', HANDLED: '已处置', COMPLETED: '已办结', REJECTED: '已驳回' }
  return map[status] || status || '未知'
}

function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const hourlyTrend = overview.value.hourlyTrend || []
  const hours = hourlyTrend.map(h => `${h.hour}:00`)
  const counts = hourlyTrend.map(h => h.count)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 20, 40, 0.85)',
      borderColor: '#00d4ff',
      borderWidth: 1,
      textStyle: { color: '#e0f0ff' }
    },
    grid: { left: '8%', right: '4%', top: '12%', bottom: '14%' },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: '#1a4a6e' } },
      axisLabel: { color: '#7eb8da', fontSize: 10 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#7eb8da', fontSize: 10 },
      splitLine: { lineStyle: { color: '#0d2a45', type: 'dashed' } }
    },
    series: [{
      type: 'line',
      smooth: true,
      data: counts,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#00d4ff' },
      itemStyle: { color: '#00d4ff', borderWidth: 2, borderColor: '#001525' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 212, 255, 0.4)' },
          { offset: 1, color: 'rgba(0, 212, 255, 0.02)' }
        ])
      }
    }]
  })
}

function initRankChart() {
  if (!rankChartRef.value) return
  if (rankChart) rankChart.dispose()
  rankChart = echarts.init(rankChartRef.value)

  const data = communityRank.value.slice(0, 10).reverse()
  const names = data.map(d => d.gridName)
  const rates = data.map(d => d.completionRate)

  rankChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(0, 20, 40, 0.85)',
      borderColor: '#00ff88',
      borderWidth: 1,
      textStyle: { color: '#e0f0ff' }
    },
    grid: { left: '30%', right: '8%', top: '6%', bottom: '6%' },
    xAxis: {
      type: 'value',
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#7eb8da', fontSize: 10, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#0d2a45', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: '#1a4a6e' } },
      axisTick: { show: false },
      axisLabel: { color: '#b8d8ea', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: rates,
      barWidth: 14,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#006b4e' },
          { offset: 1, color: '#00ff88' }
        ])
      },
      label: {
        show: true,
        position: 'right',
        color: '#00ff88',
        fontSize: 10,
        formatter: '{c}%'
      }
    }]
  })
}

function initMapChart() {
  if (!mapContainerRef.value) return
  if (mapChart) mapChart.dispose()
  mapChart = echarts.init(mapContainerRef.value)

  mapChart.on('click', (params) => {
    if (params.componentType === 'series' && params.data) {
      const eventData = params.data.eventData
      if (eventData) {
        showEventDetail(eventData)
      }
    }
  })

  updateMapData()
}

function updateMapData() {
  if (!mapChart) return

  const centerLng = 116.4
  const centerLat = 39.9

  if (currentMapMode.value === 'heatmap') {
    const points = heatmapData.value.length > 0
      ? heatmapData.value.map(p => [p.lng, p.lat, p.count])
      : generateMockHeatmapData()

    mapChart.setOption({
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(0, 20, 40, 0.85)',
        borderColor: '#00d4ff',
        borderWidth: 1,
        textStyle: { color: '#e0f0ff' }
      },
      geo: {
        map: '',
        roam: true,
        itemStyle: { areaColor: '#0a1e33', borderColor: '#1a4a6e' },
        emphasis: { itemStyle: { areaColor: '#0d2a45' } },
        center: [centerLng, centerLat],
        zoom: 12
      },
      visualMap: {
        min: 0,
        max: 20,
        show: false,
        inRange: { color: ['#0a1e33', '#006b4e', '#00ff88', '#ffd93d', '#ff6b6b'] }
      },
      series: [{
        type: 'heatmap',
        coordinateSystem: 'cartesian2d',
        data: points,
        pointSize: 18,
        blurSize: 24
      }]
    }, true)
  } else {
    const markers = eventMarkers.value.length > 0
      ? eventMarkers.value.map(e => ({
          name: e.title,
          value: [e.lng, e.lat, 1],
          eventData: e,
          itemStyle: {
            color: e.priority === 'HIGH' || e.priority === 'URGENT' ? '#ff6b6b' :
                   e.priority === 'MEDIUM' ? '#ffd93d' : '#00d4ff'
          }
        }))
      : generateMockMarkerData()

    mapChart.setOption({
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(0, 20, 40, 0.85)',
        borderColor: '#00d4ff',
        borderWidth: 1,
        textStyle: { color: '#e0f0ff' },
        formatter: (params) => {
          const e = params.data.eventData
          if (e) {
            return `<b>${e.title}</b><br/>类型: ${e.eventType || ''}<br/>优先级: ${priorityLabel(e.priority)}<br/>状态: ${statusLabel(e.status)}<br/><span style="color:#00d4ff">点击查看详情</span>`
          }
          return params.name
        }
      },
      geo: {
        map: '',
        roam: true,
        itemStyle: { areaColor: '#0a1e33', borderColor: '#1a4a6e' },
        emphasis: { itemStyle: { areaColor: '#0d2a45' } },
        center: [centerLng, centerLat],
        zoom: 12
      },
      series: [{
        type: 'scatter',
        coordinateSystem: 'geo',
        data: markers,
        symbolSize: 16,
        label: { show: false },
        emphasis: {
          label: { show: true, formatter: '{b}', color: '#fff', fontSize: 12 },
          itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,212,255,0.6)' }
        }
      }, {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: markers.filter(m => m.itemStyle && m.itemStyle.color === '#ff6b6b').slice(0, 5),
        symbolSize: 12,
        rippleEffect: { brushType: 'stroke', scale: 4, period: 4 },
        itemStyle: { color: '#ff6b6b' },
        label: { show: false }
      }]
    }, true)
  }
}

function generateMockHeatmapData() {
  const data = []
  for (let i = 0; i < 50; i++) {
    data.push([
      116.3 + Math.random() * 0.2,
      39.85 + Math.random() * 0.1,
      Math.floor(Math.random() * 15) + 1
    ])
  }
  return data
}

function generateMockMarkerData() {
  const types = ['环境卫生', '市政设施', '治安隐患', '噪音扰民', '民生服务']
  const typeCodes = ['environment', 'facility', 'security', 'noise', 'service']
  const priorities = ['HIGH', 'MEDIUM', 'LOW']
  const data = []
  for (let i = 0; i < 15; i++) {
    const priority = priorities[i % 3]
    const eventData = {
      eventId: 1000 + i,
      eventNo: 'EVT202406' + String(i + 1).padStart(3, '0'),
      title: types[i % 5] + '事件' + (i + 1),
      eventType: typeCodes[i % 5],
      status: i < 5 ? 'PENDING' : 'APPROVED',
      priority,
      lng: 116.3 + Math.random() * 0.2,
      lat: 39.85 + Math.random() * 0.1,
      address: '某街道某社区某路' + (i + 1) + '号',
      reporterName: '居民' + (i + 1),
      reportTime: '2024-06-14 ' + String(8 + i).padStart(2, '0') + ':00:00'
    }
    data.push({
      name: eventData.title,
      value: [eventData.lng, eventData.lat, 1],
      eventData,
      itemStyle: {
        color: priority === 'HIGH' ? '#ff6b6b' : priority === 'MEDIUM' ? '#ffd93d' : '#00d4ff'
      }
    })
  }
  return data
}

function showEventDetail(event) {
  currentEvent.value = event
  eventDialogVisible.value = true
}

function handleDispatch(event) {
  if (!event) return
  dispatchForm.value = {
    eventId: event.eventId,
    eventTitle: event.title,
    assigneeId: null,
    taskId: null
  }
  eventDialogVisible.value = false
  dispatchDialogVisible.value = true
}

async function submitDispatch() {
  if (!dispatchForm.value.assigneeId) {
    ElMessage.warning('请选择派单人员')
    return
  }
  dispatching.value = true
  try {
    await dispatchEvent({
      eventId: dispatchForm.value.eventId,
      assigneeId: dispatchForm.value.assigneeId,
      taskId: dispatchForm.value.taskId || null
    })
    ElMessage.success('派单成功')
    dispatchDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('派单失败')
  } finally {
    dispatching.value = false
  }
}

function locateMember(member) {
  if (member.lng && member.lat && mapChart) {
    mapChart.setOption({
      geo: { center: [member.lng, member.lat], zoom: 14 }
    })
  }
}

function applyData(data) {
  if (data.overview) overview.value = data.overview
  if (data.eventMarkers) eventMarkers.value = data.eventMarkers
  if (data.heatmap) heatmapData.value = data.heatmap
  if (data.communityRank) communityRank.value = data.communityRank
  if (data.memberStatus) memberStatus.value = data.memberStatus

  nextTick(() => {
    initTrendChart()
    initRankChart()
    updateMapData()
  })
}

async function loadData() {
  try {
    const res = await getDashboardAll()
    if (res.data) {
      applyData(res.data)
    }
  } catch (e) {
    applyData(getMockData())
  }
}

function initWebSocket() {
  ws = createDashboardWS(
    (data) => {
      if (data && data.data) {
        applyData(data.data)
      }
    },
    () => { wsConnected.value = true },
    () => { wsConnected.value = false }
  )
  ws.connect()
}

function resizeCharts() {
  trendChart && trendChart.resize()
  rankChart && rankChart.resize()
  mapChart && mapChart.resize()
}

function getMockData() {
  return {
    overview: {
      todayReported: 86,
      todayCompleted: 52,
      pendingCount: 23,
      processingCount: 45,
      completedCount: 942,
      avgHandleTime: 18.5,
      onlineRate: 78.5,
      hourlyTrend: Array.from({ length: new Date().getHours() + 1 }, (_, i) => ({
        hour: i,
        count: Math.floor(Math.random() * 15) + 2
      }))
    },
    eventMarkers: Array.from({ length: 12 }, (_, i) => ({
      eventId: 1000 + i,
      eventNo: 'EVT202406' + String(i + 1).padStart(3, '0'),
      title: ['环境卫生', '市政设施', '治安隐患', '噪音扰民', '民生服务'][i % 5] + '事件' + (i + 1),
      eventType: ['environment', 'facility', 'security', 'noise', 'service'][i % 5],
      status: i < 5 ? 'PENDING' : 'APPROVED',
      priority: ['HIGH', 'MEDIUM', 'LOW'][i % 3],
      lng: 116.3 + Math.random() * 0.2,
      lat: 39.85 + Math.random() * 0.1,
      address: '某街道某社区某路' + (i + 1) + '号',
      reporterName: '居民' + (i + 1),
      reportTime: '2024-06-14 ' + String(8 + i).padStart(2, '0') + ':00:00'
    })),
    heatmap: Array.from({ length: 30 }, () => ({
      lng: 116.3 + Math.random() * 0.2,
      lat: 39.85 + Math.random() * 0.1,
      count: Math.floor(Math.random() * 10) + 1
    })),
    communityRank: [
      { gridId: 1, gridName: '东城社区', totalCount: 356, completedCount: 318, completionRate: 89.3 },
      { gridId: 2, gridName: '西城社区', totalCount: 312, completedCount: 270, completionRate: 86.5 },
      { gridId: 3, gridName: '南城社区', totalCount: 288, completedCount: 244, completionRate: 84.7 },
      { gridId: 4, gridName: '北城社区', totalCount: 265, completedCount: 220, completionRate: 83.0 },
      { gridId: 5, gridName: '高新社区', totalCount: 218, completedCount: 185, completionRate: 84.9 },
      { gridId: 6, gridName: '经开社区', totalCount: 196, completedCount: 158, completionRate: 80.6 },
      { gridId: 7, gridName: '郊区社区', totalCount: 152, completedCount: 116, completionRate: 76.3 }
    ],
    memberStatus: Array.from({ length: 16 }, (_, i) => ({
      userId: 100 + i,
      userName: '网格员' + (i + 1),
      phone: '138****' + String(1000 + i),
      gridId: (i % 7) + 1,
      gridName: ['东城社区', '西城社区', '南城社区', '北城社区', '高新社区', '经开社区', '郊区社区'][i % 7],
      lng: 116.3 + Math.random() * 0.2,
      lat: 39.85 + Math.random() * 0.1,
      onDuty: i < 12 ? 1 : 0,
      lastReportTime: '2024-06-14 ' + String(9 + i % 10).padStart(2, '0') + ':30:00',
      battery: 30 + Math.floor(Math.random() * 70)
    }))
  }
}

watch(currentMapMode, () => {
  updateMapData()
})

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)

  loadData()
  initWebSocket()

  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })

  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (ws) ws.disconnect()
  trendChart && trendChart.dispose()
  rankChart && rankChart.dispose()
  mapChart && mapChart.dispose()
  window.removeEventListener('resize', resizeCharts)
})
</script>

<style lang="scss" scoped>
.command-screen {
  width: 100vw;
  height: 100vh;
  background: #001525;
  color: #e0f0ff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 2000;
}

.screen-header {
  height: 64px;
  background: linear-gradient(180deg, rgba(0, 40, 80, 0.95) 0%, rgba(0, 20, 45, 0.9) 100%);
  border-bottom: 1px solid rgba(0, 212, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;

  .header-center {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }

  .header-title {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    background: linear-gradient(90deg, #00d4ff, #00ff88);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 6px;
  }

  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .header-time {
    font-size: 16px;
    color: #7eb8da;
    font-family: 'DIN Alternate', monospace;
    letter-spacing: 2px;
  }

  .ws-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #7eb8da;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #7eb8da;
    }

    &.connected .dot {
      background: #00ff88;
      box-shadow: 0 0 8px rgba(0, 255, 136, 0.6);
      animation: pulse 2s infinite;
    }
  }

  .screen-btn {
    width: 32px;
    height: 32px;
    border: 1px solid rgba(0, 212, 255, 0.3);
    background: rgba(0, 40, 80, 0.5);
    border-radius: 4px;
    color: #7eb8da;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;

    &:hover {
      border-color: #00d4ff;
      color: #00d4ff;
      background: rgba(0, 80, 160, 0.3);
    }
  }
}

.screen-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
  min-height: 0;
}

.panel-left, .panel-right {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-center {
  flex: 1;
  min-width: 0;
}

.panel {
  background: rgba(0, 30, 55, 0.85);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-header {
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    background: rgba(0, 50, 90, 0.5);
    border-bottom: 1px solid rgba(0, 212, 255, 0.1);
    flex-shrink: 0;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
      color: #b8d8ea;
      position: relative;
      padding-left: 12px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 14px;
        background: linear-gradient(180deg, #00d4ff, #00ff88);
        border-radius: 2px;
      }
    }

    .panel-actions {
      display: flex;
      gap: 4px;
    }

    .event-count {
      background: #ff6b6b;
      color: #fff;
      font-size: 12px;
      padding: 1px 8px;
      border-radius: 10px;
      font-weight: 600;
    }
  }

  .panel-body {
    flex: 1;
    padding: 12px 16px;
    overflow-y: auto;
    min-height: 0;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(0, 212, 255, 0.2);
      border-radius: 2px;
    }
  }
}

.map-mode-btn {
  padding: 2px 10px;
  font-size: 11px;
  border: 1px solid rgba(0, 212, 255, 0.3);
  background: transparent;
  color: #7eb8da;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.3s;

  &.active {
    background: rgba(0, 212, 255, 0.15);
    border-color: #00d4ff;
    color: #00d4ff;
  }
}

.overview-panel .stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 8px 0;

  .stat-value {
    display: block;
    font-size: 22px;
    font-weight: 700;
    font-family: 'DIN Alternate', 'Helvetica Neue', monospace;
    line-height: 1.3;
  }

  .stat-label {
    display: block;
    font-size: 11px;
    color: #7eb8da;
    margin-top: 4px;
  }
}

.chart-box {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

.trend-panel, .rank-panel, .member-panel, .pending-panel {
  flex: 1;
  min-height: 0;
}

.map-panel {
  height: 100%;

  .map-body {
    padding: 0;
  }

  .map-container {
    width: 100%;
    height: 100%;
    min-height: 400px;
  }
}

.member-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px 0;

  .member-stat, .member-rate {
    text-align: center;
    flex: 1;

    .member-count, .rate-value {
      display: block;
      font-size: 20px;
      font-weight: 700;
      font-family: 'DIN Alternate', monospace;
    }

    .member-label, .rate-label {
      display: block;
      font-size: 11px;
      color: #7eb8da;
      margin-top: 2px;
    }
  }

  .online .member-count { color: #00ff88; }
  .offline .member-count { color: #7eb8da; }
  .member-rate .rate-value { color: #00d4ff; }
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 212, 255, 0.08);
  }

  .member-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;

    &.online { background: #00ff88; box-shadow: 0 0 6px rgba(0, 255, 136, 0.5); }
    &.offline { background: #5a6e7f; }
  }

  .member-name {
    font-size: 12px;
    color: #b8d8ea;
    width: 60px;
    flex-shrink: 0;
  }

  .member-grid {
    font-size: 11px;
    color: #7eb8da;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .member-battery {
    font-size: 10px;
    color: #00ff88;
    flex-shrink: 0;

    &.low { color: #ff6b6b; }
  }
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  background: rgba(0, 40, 80, 0.3);
  border: 1px solid rgba(0, 212, 255, 0.08);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(0, 60, 120, 0.4);
    border-color: rgba(0, 212, 255, 0.2);
  }

  .event-priority {
    width: 4px;
    height: 28px;
    border-radius: 2px;
    flex-shrink: 0;

    &.high { background: #ff6b6b; }
    &.medium { background: #ffd93d; }
    &.low { background: #00d4ff; }
  }

  .event-info {
    flex: 1;
    min-width: 0;

    .event-title {
      display: block;
      font-size: 12px;
      color: #e0f0ff;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .event-meta {
      display: block;
      font-size: 10px;
      color: #7eb8da;
      margin-top: 2px;
    }
  }

  .dispatch-btn {
    padding: 3px 10px;
    font-size: 11px;
    border: 1px solid rgba(0, 255, 136, 0.4);
    background: rgba(0, 255, 136, 0.1);
    color: #00ff88;
    border-radius: 3px;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.2s;

    &:hover {
      background: rgba(0, 255, 136, 0.25);
      border-color: #00ff88;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.split-mode {
  .panel-left {
    width: 280px;
  }

  .panel-right {
    width: 280px;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 1200px) {
  .panel-left, .panel-right {
    width: 260px;
  }

  .header-title {
    font-size: 18px !important;
    letter-spacing: 3px !important;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 900px) {
  .screen-body {
    flex-direction: column;
    overflow-y: auto;
  }

  .panel-left, .panel-right {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;

    .panel {
      flex: 1;
      min-width: 280px;
    }
  }

  .panel-center {
    min-height: 350px;
  }
}
</style>

<style>
.event-detail-dialog {
  background: #0a1e33 !important;
  border: 1px solid rgba(0, 212, 255, 0.2) !important;

  .el-dialog__header {
    border-bottom: 1px solid rgba(0, 212, 255, 0.1);
  }

  .el-dialog__title {
    color: #e0f0ff !important;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: #7eb8da !important;
  }

  .el-dialog__body {
    color: #b8d8ea !important;
  }
}

.dispatch-dialog {
  background: #0a1e33 !important;
  border: 1px solid rgba(0, 212, 255, 0.2) !important;

  .el-dialog__title {
    color: #e0f0ff !important;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: #7eb8da !important;
  }

  .el-form-item__label {
    color: #b8d8ea !important;
  }

  .el-input__inner, .el-textarea__inner {
    background: rgba(0, 40, 80, 0.5) !important;
    border-color: rgba(0, 212, 255, 0.2) !important;
    color: #e0f0ff !important;
  }
}

.event-detail {
  .detail-row {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid rgba(0, 212, 255, 0.06);

    .detail-label {
      width: 80px;
      color: #7eb8da;
      font-size: 13px;
      flex-shrink: 0;
    }

    .detail-value {
      flex: 1;
      color: #e0f0ff;
      font-size: 13px;
    }
  }
}
</style>
