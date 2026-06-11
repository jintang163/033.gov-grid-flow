<template>
  <div class="statistics-page">
    <el-tabs v-model="activeTab" type="card" class="stat-tabs" :tab-bar-style="{ marginBottom: '20px' }">
      <el-tab-pane label="综合分析" name="overview">
        <el-card shadow="hover" class="filter-card" :body-style="{ padding: '16px 20px' }">
          <div class="filter-content">
            <span class="filter-label">时间范围：</span>
            <el-radio-group v-model="dayRange" size="default" @change="handleDayRangeChange">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="30">近30天</el-radio-button>
              <el-radio-button :value="90">近90天</el-radio-button>
            </el-radio-group>
          </div>
        </el-card>

        <el-row :gutter="20" class="charts-row">
          <el-col :xs="24" :sm="24" :md="14">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">事件趋势分析</span>
                </div>
              </template>
              <div ref="overviewTrendChartRef" class="chart-container-lg"></div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="24" :md="10">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">事件类型分布</span>
                </div>
              </template>
              <div ref="overviewPieChartRef" class="chart-container-lg"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="部门对比" name="dept">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">各部门处置情况对比</span>
                </div>
              </template>
              <div ref="deptChartRef" class="chart-container-xl"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="网格对比" name="grid">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">各网格事件情况对比</span>
                </div>
              </template>
              <div ref="gridChartRef" class="chart-container-xl"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="满意度分析" name="satisfaction">
        <el-row :gutter="20" class="stat-cards-row">
          <el-col :xs="12" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
              <div class="card-content">
                <div class="card-info">
                  <p class="card-title">平均速度评分</p>
                  <p class="card-value">{{ satisfactionStats.avgSpeedScore }}</p>
                  <el-rate v-model="satisfactionStats.avgSpeedScore" disabled size="small" :max="5" />
                </div>
                <div class="card-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
                  <el-icon :size="28" color="#fff">
                    <Timer />
                  </el-icon>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
              <div class="card-content">
                <div class="card-info">
                  <p class="card-title">平均效果评分</p>
                  <p class="card-value">{{ satisfactionStats.avgEffectScore }}</p>
                  <el-rate v-model="satisfactionStats.avgEffectScore" disabled size="small" :max="5" />
                </div>
                <div class="card-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
                  <el-icon :size="28" color="#fff">
                    <CircleCheck />
                  </el-icon>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
              <div class="card-content">
                <div class="card-info">
                  <p class="card-title">综合满意度</p>
                  <p class="card-value">{{ satisfactionStats.overallScore }}</p>
                  <el-rate v-model="satisfactionStats.overallScore" disabled size="small" :max="5" />
                </div>
                <div class="card-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
                  <el-icon :size="28" color="#fff">
                    <Star />
                  </el-icon>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
              <div class="card-content">
                <div class="card-info">
                  <p class="card-title">评价总数</p>
                  <p class="card-value">{{ satisfactionStats.totalRatings }}</p>
                  <p class="card-sub">较上月 <span class="trend-up">+12.5%</span></p>
                </div>
                <div class="card-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)">
                  <el-icon :size="28" color="#fff">
                    <DataAnalysis />
                  </el-icon>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="charts-row">
          <el-col :xs="24" :sm="24" :md="12">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">速度评分分布</span>
                </div>
              </template>
              <div ref="speedRatingChartRef" class="chart-container-lg"></div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">效果评分分布</span>
                </div>
              </template>
              <div ref="effectRatingChartRef" class="chart-container-lg"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import {
  Timer,
  CircleCheck,
  Star,
  DataAnalysis
} from '@element-plus/icons-vue'
import {
  getEventTrend,
  getEventTypeStats,
  getDeptStats,
  getGridStats,
  getSatisfactionStats
} from '@/api/statistics'

const activeTab = ref('overview')
const dayRange = ref(30)

const overviewTrendChartRef = ref()
const overviewPieChartRef = ref()
const deptChartRef = ref()
const gridChartRef = ref()
const speedRatingChartRef = ref()
const effectRatingChartRef = ref()

let overviewTrendChart = null
let overviewPieChart = null
let deptChart = null
let gridChart = null
let speedRatingChart = null
let effectRatingChart = null

const satisfactionStats = reactive({
  avgSpeedScore: 4.3,
  avgEffectScore: 4.5,
  overallScore: 4.4,
  totalRatings: 2586
})

function generateDates(days) {
  const dates = []
  const now = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')}`)
  }
  return dates
}

function generateRandomData(days, min, max) {
  return Array.from({ length: days }, () => Math.floor(Math.random() * (max - min + 1)) + min)
}

function initOverviewTrendChart(data = [], days = 30) {
  if (!overviewTrendChartRef.value) return
  if (overviewTrendChart) overviewTrendChart.dispose()
  overviewTrendChart = echarts.init(overviewTrendChartRef.value)

  const dates = generateDates(days)
  let chartData
  if (data && data.length > 0) {
    chartData = {
      dates: data.map(d => d.date || d.days),
      newData: data.map(d => d.newCount ?? d.new ?? d.added ?? 0),
      doneData: data.map(d => d.doneCount ?? d.done ?? d.completed ?? 0)
    }
  } else {
    chartData = {
      dates,
      newData: generateRandomData(days, 80, 200),
      doneData: generateRandomData(days, 60, 180)
    }
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' },
      axisPointer: { type: 'cross', crossStyle: { color: '#999' } }
    },
    legend: {
      data: ['新增数量', '办结数量'],
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      }
    ],
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.dates,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [
      {
        name: '新增数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: days <= 15,
        data: chartData.newData,
        itemStyle: { color: '#409EFF', borderWidth: 2, borderColor: '#fff' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.35)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
          ])
        }
      },
      {
        name: '办结数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: days <= 15,
        data: chartData.doneData,
        itemStyle: { color: '#67C23A', borderWidth: 2, borderColor: '#fff' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.35)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
          ])
        }
      }
    ]
  }
  overviewTrendChart.setOption(option)
}

function initOverviewPieChart(data = []) {
  if (!overviewPieChartRef.value) return
  if (overviewPieChart) overviewPieChart.dispose()
  overviewPieChart = echarts.init(overviewPieChartRef.value)

  const defaultData = [
    { value: 428, name: '环境卫生' },
    { value: 356, name: '市政设施' },
    { value: 298, name: '治安隐患' },
    { value: 245, name: '噪音扰民' },
    { value: 192, name: '民生服务' },
    { value: 148, name: '其他' }
  ]

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      left: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#606266', fontSize: 12 }
    },
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6'],
    series: [
      {
        name: '事件类型',
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 11,
          color: '#606266',
          lineHeight: 16
        },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 15, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.2)' }
        },
        labelLine: { show: true, length: 10, length2: 8, smooth: true },
        data: data.length > 0 ? data : defaultData
      }
    ]
  }
  overviewPieChart.setOption(option)
}

function initDeptChart(data = []) {
  if (!deptChartRef.value) return
  if (deptChart) deptChart.dispose()
  deptChart = echarts.init(deptChartRef.value)

  const defaultDepts = ['城管执法局', '生态环境局', '公安局', '住建局', '交通局', '市场监管局', '卫健委', '街道办事处']

  let chartData
  if (Array.isArray(data) && data.length > 0) {
    chartData = {
      depts: data.map(d => d.deptName ?? d.name ?? d.department ?? '未知'),
      pending: data.map(d => d.pendingCount ?? d.pending ?? Math.floor(Math.random() * 30 + 5)),
      processing: data.map(d => d.processingCount ?? d.processing ?? Math.floor(Math.random() * 50 + 10)),
      done: data.map(d => d.doneCount ?? d.done ?? d.completed ?? d.handleCount ?? Math.floor(Math.random() * 200 + 50))
    }
  } else {
    chartData = {
      depts: defaultDepts,
      pending: [18, 22, 12, 32, 15, 28, 10, 8],
      processing: [42, 38, 28, 56, 35, 44, 22, 15],
      done: [268, 196, 178, 107, 118, 70, 93, 75]
    }
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['待办', '处理中', '已办结'],
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.depts,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', interval: 0, rotate: chartData.depts.length > 6 ? 20 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [
      {
        name: '待办',
        type: 'bar',
        stack: 'total',
        barWidth: '45%',
        emphasis: { focus: 'series' },
        itemStyle: {
          color: '#E6A23C',
          borderRadius: [0, 0, 0, 0]
        },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.pending
      },
      {
        name: '处理中',
        type: 'bar',
        stack: 'total',
        emphasis: { focus: 'series' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#66b1ff' }
          ])
        },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.processing
      },
      {
        name: '已办结',
        type: 'bar',
        stack: 'total',
        emphasis: { focus: 'series' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#85ce61' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.done
      }
    ]
  }
  deptChart.setOption(option)
}

function initGridChart(data = []) {
  if (!gridChartRef.value) return
  if (gridChart) gridChart.dispose()
  gridChart = echarts.init(gridChartRef.value)

  const defaultGrids = ['第一网格', '第二网格', '第三网格', '第四网格', '第五网格', '第六网格', '第七网格']

  let chartData
  if (Array.isArray(data) && data.length > 0) {
    chartData = {
      grids: data.map(d => d.gridName ?? d.name ?? d.grid ?? '未知'),
      pending: data.map(d => d.pendingCount ?? d.pending ?? d.todo ?? Math.floor(Math.random() * 25 + 3)),
      processing: data.map(d => d.processingCount ?? d.processing ?? Math.floor(Math.random() * 40 + 8)),
      done: data.map(d => d.doneCount ?? d.done ?? d.completed ?? Math.floor(Math.random() * 180 + 40))
    }
  } else {
    chartData = {
      grids: defaultGrids,
      pending: [14, 10, 18, 8, 12, 9, 6],
      processing: [32, 26, 22, 28, 18, 14, 12],
      done: [140, 126, 108, 99, 82, 75, 58]
    }
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['待办', '处理中', '已办结'],
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.grids,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', interval: 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [
      {
        name: '待办',
        type: 'bar',
        stack: 'total',
        barWidth: '45%',
        emphasis: { focus: 'series' },
        itemStyle: { color: '#E6A23C' },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.pending
      },
      {
        name: '处理中',
        type: 'bar',
        stack: 'total',
        emphasis: { focus: 'series' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#66b1ff' }
          ])
        },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.processing
      },
      {
        name: '已办结',
        type: 'bar',
        stack: 'total',
        emphasis: { focus: 'series' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#85ce61' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 11,
          formatter: (params) => params.value > 0 ? params.value : ''
        },
        data: chartData.done
      }
    ]
  }
  gridChart.setOption(option)
}

function initRatingChart(ref, data, title) {
  if (!ref.value) return
  let chart = echarts.getInstanceByDom(ref.value)
  if (chart) chart.dispose()
  chart = echarts.init(ref.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}星: ${p.value} 条评价 (${((p.value / data.reduce((a, b) => a + b, 0)) * 100).toFixed(1)}%)`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '12%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['1星', '2星', '3星', '4星', '5星'],
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 13 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [
      {
        type: 'bar',
        data: data.map((val, idx) => ({
          value: val,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: ['#F56C6C', '#F89898', '#E6A23C', '#85ce61', '#67C23A'][idx] },
              { offset: 1, color: ['#fde2e2', '#fef0f0', '#faecd8', '#f0f9eb', '#e1f3d8'][idx] }
            ])
          }
        })),
        barWidth: '45%',
        label: {
          show: true,
          position: 'top',
          color: '#606266',
          fontWeight: 600
        }
      }
    ]
  }
  chart.setOption(option)
  return chart
}

async function loadOverviewTrendChart() {
  try {
    const res = await getEventTrend(dayRange.value)
    initOverviewTrendChart(res.data || [], dayRange.value)
  } catch (e) {
    initOverviewTrendChart([], dayRange.value)
  }
}

async function loadOverviewPieChart() {
  try {
    const res = await getEventTypeStats()
    const data = res.data || []
    const formattedData = Array.isArray(data)
      ? data.map(item => ({
          value: item.value ?? item.count ?? item.num ?? 0,
          name: item.name ?? item.type ?? item.label ?? '未知'
        }))
      : []
    initOverviewPieChart(formattedData)
  } catch (e) {
    initOverviewPieChart([])
  }
}

async function loadDeptChart() {
  try {
    const res = await getDeptStats()
    initDeptChart(res.data || [])
  } catch (e) {
    initDeptChart([])
  }
}

async function loadGridChart() {
  try {
    const res = await getGridStats()
    initGridChart(res.data || [])
  } catch (e) {
    initGridChart([])
  }
}

async function loadSatisfactionData() {
  try {
    const res = await getSatisfactionStats()
    const data = res.data || {}
    satisfactionStats.avgSpeedScore = data.avgSpeedScore ?? 4.3
    satisfactionStats.avgEffectScore = data.avgEffectScore ?? 4.5
    satisfactionStats.overallScore = data.overallScore ?? 4.4
    satisfactionStats.totalRatings = data.totalRatings ?? 2586

    const speedDist = data.speedDistribution || [28, 68, 186, 682, 856]
    const effectDist = data.effectDistribution || [18, 52, 168, 712, 896]

    nextTick(() => {
      speedRatingChart = initRatingChart(speedRatingChartRef, speedDist, '速度评分')
      effectRatingChart = initRatingChart(effectRatingChartRef, effectDist, '效果评分')
    })
  } catch (e) {
    satisfactionStats.avgSpeedScore = 4.3
    satisfactionStats.avgEffectScore = 4.5
    satisfactionStats.overallScore = 4.4
    satisfactionStats.totalRatings = 2586

    nextTick(() => {
      speedRatingChart = initRatingChart(speedRatingChartRef, [28, 68, 186, 682, 856], '速度评分')
      effectRatingChart = initRatingChart(effectRatingChartRef, [18, 52, 168, 712, 896], '效果评分')
    })
  }
}

function handleDayRangeChange() {
  loadOverviewTrendChart()
}

function handleResize() {
  overviewTrendChart && overviewTrendChart.resize()
  overviewPieChart && overviewPieChart.resize()
  deptChart && deptChart.resize()
  gridChart && gridChart.resize()
  speedRatingChart && speedRatingChart.resize()
  effectRatingChart && effectRatingChart.resize()
}

watch(activeTab, (newVal) => {
  nextTick(() => {
    if (newVal === 'dept') {
      loadDeptChart()
    } else if (newVal === 'grid') {
      loadGridChart()
    } else if (newVal === 'satisfaction') {
      loadSatisfactionData()
    }
    setTimeout(() => handleResize(), 100)
  })
})

onMounted(() => {
  nextTick(() => {
    loadOverviewTrendChart()
    loadOverviewPieChart()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  overviewTrendChart && overviewTrendChart.dispose()
  overviewPieChart && overviewPieChart.dispose()
  deptChart && deptChart.dispose()
  gridChart && gridChart.dispose()
  speedRatingChart && speedRatingChart.dispose()
  effectRatingChart && effectRatingChart.dispose()
})
</script>

<style lang="scss" scoped>
.statistics-page {
  padding: 0;
  background: #f5f7fa;
  min-height: 100%;
  box-sizing: border-box;

  :deep(.el-tabs--card > .el-tabs__header .el-tabs__item) {
    border-radius: 8px 8px 0 0;
    margin-right: 4px;
    font-weight: 500;
  }

  :deep(.el-tabs--card > .el-tabs__header .el-tabs__item.is-active) {
    background: #fff;
    color: #409EFF;
    border-bottom-color: #fff;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  .filter-card {
    :deep(.el-card) {
      border-radius: 8px;
      border: none;
    }
  }

  .filter-content {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .filter-label {
      color: #606266;
      font-weight: 500;
      font-size: 14px;
    }
  }

  .stat-cards-row {
    margin-bottom: 20px;
  }

  .stat-cards-row,
  .charts-row {
    margin-bottom: 20px;
  }

  :deep(.el-card) {
    border-radius: 8px;
    border: none;
  }

  .stat-card {
    transition: transform 0.3s ease;

    &:hover {
      transform: translateY(-4px);
    }

    .card-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-info {
        flex: 1;
        min-width: 0;

        .card-title {
          margin: 0 0 6px 0;
          color: #909399;
          font-size: 13px;
          font-weight: 500;
        }

        .card-value {
          margin: 0 0 6px 0;
          color: #303133;
          font-size: 26px;
          font-weight: 700;
          line-height: 1.2;
          font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
        }

        .card-sub {
          margin: 6px 0 0 0;
          color: #909399;
          font-size: 12px;

          .trend-up {
            color: #67C23A;
            font-weight: 600;
          }
        }
      }

      .card-icon {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-left: 12px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      position: relative;
      padding-left: 10px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 14px;
        background: linear-gradient(180deg, #409EFF 0%, #667eea 100%);
        border-radius: 2px;
      }
    }
  }

  .chart-container-lg {
    height: 360px;
    width: 100%;
  }

  .chart-container-xl {
    height: 420px;
    width: 100%;
  }
}
</style>
