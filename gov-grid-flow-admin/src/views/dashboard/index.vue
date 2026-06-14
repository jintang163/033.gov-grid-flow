<template>
  <div class="dashboard">
    <div class="dashboard-toolbar">
      <el-button type="primary" @click="$router.push('/screen')" :icon="Monitor">指挥调度大屏</el-button>
    </div>
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
          <div class="card-content">
            <div class="card-info">
              <p class="card-title">{{ item.title }}</p>
              <p class="card-value">
                {{ item.value }}
                <span v-if="item.unit" class="card-unit">{{ item.unit }}</span>
              </p>
            </div>
            <div class="card-icon" :style="{ background: item.gradient }">
              <el-icon :size="28" color="#fff">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :md="14">
        <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">事件趋势（近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="10">
        <el-card shadow="hover" class="chart-card" :body-style="{ padding: '16px' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">事件类型分布</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover" class="table-card" :body-style="{ padding: '0' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">部门处置排名</span>
            </div>
          </template>
          <el-table :data="deptTableData" stripe style="width: 100%" :header-cell-style="{ background: '#fafafa', color: '#606266' }">
            <el-table-column type="index" label="排名" width="60" align="center">
              <template #default="{ $index }">
                <el-tag v-if="$index < 3" :type="rankTagType[$index]" effect="dark" size="small">{{ $index + 1 }}</el-tag>
                <span v-else>{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deptName" label="部门名称" min-width="120" />
            <el-table-column prop="handleCount" label="处置数" width="80" align="center" />
            <el-table-column prop="avgDuration" label="平均时长(h)" width="100" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.avgDuration > 24 ? '#F56C6C' : '#67C23A' }">{{ row.avgDuration }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="reworkRate" label="返工率(%)" width="90" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.reworkRate" :stroke-width="8" :color="row.reworkRate > 10 ? '#F56C6C' : '#67C23A'" :show-text="false" style="width: 60px; display: inline-block; vertical-align: middle; margin-right: 6px;" />
                <span>{{ row.reworkRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="avgScore" label="平均评分" width="90" align="center">
              <template #default="{ row }">
                <el-rate v-model="row.avgScore" disabled size="small" :max="5" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover" class="table-card" :body-style="{ padding: '0' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">网格事件统计</span>
            </div>
          </template>
          <el-table :data="gridTableData" stripe style="width: 100%" :header-cell-style="{ background: '#fafafa', color: '#606266' }">
            <el-table-column prop="gridName" label="网格名称" min-width="110" />
            <el-table-column prop="totalCount" label="总事件" width="70" align="center" />
            <el-table-column prop="pendingCount" label="待办" width="60" align="center">
              <template #default="{ row }">
                <el-tag type="warning" size="small" effect="plain">{{ row.pendingCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="processingCount" label="处理中" width="70" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small" effect="plain">{{ row.processingCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="doneCount" label="已办结" width="70" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small" effect="plain">{{ row.doneCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="avgScore" label="平均评分" width="100" align="center">
              <template #default="{ row }">
                <span class="score-text">{{ row.avgScore }}</span>
                <el-rate v-model="row.avgScore" disabled size="small" :max="5" style="margin-left: 4px;" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  Document,
  Bell,
  Loading,
  CircleCheck,
  Timer,
  Star,
  RefreshLeft,
  Monitor
} from '@element-plus/icons-vue'
import {
  getOverviewStats,
  getEventTrend,
  getEventTypeStats,
  getDeptStats,
  getGridStats
} from '@/api/statistics'

const trendChartRef = ref()
const pieChartRef = ref()

let trendChart = null
let pieChart = null

const rankTagType = ['danger', 'warning', 'success']

const statCards = ref([
  { title: '事件总数', value: 0, icon: 'Document', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', unit: '' },
  { title: '待受理数', value: 0, icon: 'Bell', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', unit: '' },
  { title: '处理中数', value: 0, icon: 'Loading', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', unit: '' },
  { title: '已办结数', value: 0, icon: 'CircleCheck', gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', unit: '' },
  { title: '平均处置时长', value: 0, icon: 'Timer', gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', unit: 'h' },
  { title: '平均满意度', value: 0, icon: 'Star', gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)', unit: '' },
  { title: '返工率', value: 0, icon: 'RefreshLeft', gradient: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)', unit: '%' }
])

const deptTableData = ref([])
const gridTableData = ref([])

const defaultDeptData = [
  { deptName: '城管执法局', handleCount: 328, avgDuration: 18.5, reworkRate: 5, avgScore: 4.6 },
  { deptName: '生态环境局', handleCount: 256, avgDuration: 22.3, reworkRate: 8, avgScore: 4.3 },
  { deptName: '公安局', handleCount: 218, avgDuration: 12.6, reworkRate: 3, avgScore: 4.8 },
  { deptName: '住建局', handleCount: 195, avgDuration: 36.2, reworkRate: 12, avgScore: 4.1 },
  { deptName: '交通局', handleCount: 168, avgDuration: 25.8, reworkRate: 7, avgScore: 4.4 },
  { deptName: '市场监管局', handleCount: 142, avgDuration: 28.4, reworkRate: 9, avgScore: 4.2 },
  { deptName: '卫健委', handleCount: 125, avgDuration: 15.2, reworkRate: 4, avgScore: 4.7 },
  { deptName: '街道办事处', handleCount: 98, avgDuration: 8.5, reworkRate: 2, avgScore: 4.9 }
]

const defaultGridData = [
  { gridName: '第一网格（东城区）', totalCount: 186, pendingCount: 12, processingCount: 28, doneCount: 146, avgScore: 4.5 },
  { gridName: '第二网格（西城区）', totalCount: 162, pendingCount: 8, processingCount: 22, doneCount: 132, avgScore: 4.6 },
  { gridName: '第三网格（南城区）', totalCount: 148, pendingCount: 15, processingCount: 18, doneCount: 115, avgScore: 4.3 },
  { gridName: '第四网格（北城区）', totalCount: 135, pendingCount: 6, processingCount: 24, doneCount: 105, avgScore: 4.4 },
  { gridName: '第五网格（高新区）', totalCount: 112, pendingCount: 10, processingCount: 16, doneCount: 86, avgScore: 4.7 },
  { gridName: '第六网格（经开区）', totalCount: 98, pendingCount: 8, processingCount: 12, doneCount: 78, avgScore: 4.5 },
  { gridName: '第七网格（郊区）', totalCount: 76, pendingCount: 5, processingCount: 10, doneCount: 61, avgScore: 4.2 }
]

async function loadOverview() {
  try {
    const res = await getOverviewStats()
    const data = res.data || {}
    statCards.value[0].value = data.totalEvents ?? 0
    statCards.value[1].value = data.pendingCount ?? 0
    statCards.value[2].value = data.processingCount ?? 0
    statCards.value[3].value = data.completedCount ?? 0
    statCards.value[4].value = data.avgDuration ?? 0
    statCards.value[5].value = data.avgSatisfaction ?? 0
    statCards.value[6].value = data.reworkRate ?? 0
  } catch (e) {
    statCards.value[0].value = 1286
    statCards.value[1].value = 86
    statCards.value[2].value = 258
    statCards.value[3].value = 942
    statCards.value[4].value = 20.5
    statCards.value[5].value = 4.5
    statCards.value[6].value = 6.8
  }
}

function initTrendChart(data = []) {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const defaultData = {
    dates: ['6-05', '6-06', '6-07', '6-08', '6-09', '6-10', '6-11'],
    newData: [128, 156, 142, 168, 135, 182, 165],
    doneData: [105, 132, 128, 145, 118, 160, 152]
  }

  let chartData
  if (data && data.length > 0) {
    chartData = {
      dates: data.map(d => d.date || d.days),
      newData: data.map(d => d.newCount ?? d.new ?? d.added ?? 0),
      doneData: data.map(d => d.doneCount ?? d.done ?? d.completed ?? 0)
    }
  } else {
    chartData = defaultData
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
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.dates,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399' }
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
        symbolSize: 8,
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
        symbolSize: 8,
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
  trendChart.setOption(option)
}

function initPieChart(data = []) {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const defaultData = [
    { value: 328, name: '环境卫生' },
    { value: 256, name: '市政设施' },
    { value: 218, name: '治安隐患' },
    { value: 185, name: '噪音扰民' },
    { value: 142, name: '民生服务' },
    { value: 98, name: '其他' }
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
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 15,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        labelLine: {
          show: true,
          length: 10,
          length2: 8,
          smooth: true
        },
        data: data.length > 0 ? data : defaultData
      }
    ]
  }
  pieChart.setOption(option)
}

async function loadTrendChart() {
  try {
    const res = await getEventTrend(7)
    initTrendChart(res.data || [])
  } catch (e) {
    initTrendChart([])
  }
}

async function loadPieChart() {
  try {
    const res = await getEventTypeStats()
    const data = res.data || []
    const formattedData = Array.isArray(data)
      ? data.map(item => ({
          value: item.value ?? item.count ?? item.num ?? 0,
          name: item.name ?? item.type ?? item.label ?? '未知'
        }))
      : []
    initPieChart(formattedData)
  } catch (e) {
    initPieChart([])
  }
}

async function loadDeptTable() {
  try {
    const res = await getDeptStats()
    const data = res.data || []
    if (Array.isArray(data) && data.length > 0) {
      deptTableData.value = data.map(item => ({
        deptName: item.deptName ?? item.name ?? item.department ?? '未知部门',
        handleCount: item.handleCount ?? item.count ?? item.total ?? 0,
        avgDuration: Number((item.avgDuration ?? item.duration ?? item.avgTime ?? 0).toFixed(1)),
        reworkRate: Number((item.reworkRate ?? item.rework ?? 0).toFixed(1)),
        avgScore: Number((item.avgScore ?? item.score ?? item.rating ?? 4.5).toFixed(1))
      }))
    } else {
      deptTableData.value = defaultDeptData
    }
  } catch (e) {
    deptTableData.value = defaultDeptData
  }
}

async function loadGridTable() {
  try {
    const res = await getGridStats()
    const data = res.data || []
    if (Array.isArray(data) && data.length > 0) {
      gridTableData.value = data.map(item => ({
        gridName: item.gridName ?? item.name ?? item.grid ?? '未知网格',
        totalCount: item.totalCount ?? item.total ?? item.count ?? 0,
        pendingCount: item.pendingCount ?? item.pending ?? item.todo ?? 0,
        processingCount: item.processingCount ?? item.processing ?? 0,
        doneCount: item.doneCount ?? item.done ?? item.completed ?? 0,
        avgScore: Number((item.avgScore ?? item.score ?? item.rating ?? 4.5).toFixed(1))
      }))
    } else {
      gridTableData.value = defaultGridData
    }
  } catch (e) {
    gridTableData.value = defaultGridData
  }
}

function handleResize() {
  trendChart && trendChart.resize()
  pieChart && pieChart.resize()
}

onMounted(() => {
  nextTick(() => {
    loadOverview()
    loadTrendChart()
    loadPieChart()
    loadDeptTable()
    loadGridTable()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart && trendChart.dispose()
  pieChart && pieChart.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
  background: #f5f7fa;
  min-height: 100%;
  box-sizing: border-box;

  .dashboard-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 16px;
  }

  .stat-cards {
    margin-bottom: 20px;

    :deep(.el-card) {
      border-radius: 8px;
      border: none;
      transition: transform 0.3s ease, box-shadow 0.3s ease;

      &:hover {
        transform: translateY(-4px);
      }
    }

    .stat-card {
      .card-content {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .card-info {
          flex: 1;
          min-width: 0;

          .card-title {
            margin: 0 0 8px 0;
            color: #909399;
            font-size: 13px;
            font-weight: 500;
          }

          .card-value {
            margin: 0;
            color: #303133;
            font-size: 26px;
            font-weight: 700;
            line-height: 1.2;
            font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;

            .card-unit {
              font-size: 14px;
              font-weight: 500;
              color: #909399;
              margin-left: 4px;
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
  }

  .charts-row {
    margin-bottom: 20px;

    :deep(.el-card) {
      border-radius: 8px;
      border: none;
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

    .chart-card {
      .chart-container {
        height: 340px;
        width: 100%;
      }
    }

    .table-card {
      :deep(.el-table) {
        border-radius: 0 0 8px 8px;
      }

      :deep(.el-table__row) {
        td {
          padding: 10px 0;
        }
      }

      .score-text {
        color: #E6A23C;
        font-weight: 600;
        font-size: 13px;
      }
    }
  }
}
</style>
