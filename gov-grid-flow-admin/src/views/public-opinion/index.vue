<template>
  <div class="public-opinion-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="网格/社区">
          <el-tree-select
            v-model="queryForm.gridId"
            :data="gridTree"
            :props="{ label: 'gridName', value: 'id', children: 'children' }"
            placeholder="全部网格"
            clearable
            check-strictly
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="统计周期">
          <el-select v-model="queryForm.trendDays" style="width: 140px">
            <el-option :value="7" label="近7天" />
            <el-option :value="14" label="近14天" />
            <el-option :value="30" label="近30天" />
            <el-option :value="90" label="近90天" />
          </el-select>
        </el-form-item>
        <el-form-item label="词云数量">
          <el-select v-model="queryForm.wordCloudSize" style="width: 120px">
            <el-option :value="30" label="30个词" />
            <el-option :value="50" label="50个词" />
            <el-option :value="100" label="100个词" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="stats-row">
      <el-card shadow="hover" class="stat-card opinion-index" :class="opinionLevelClass">
        <div class="stat-icon">
          <el-icon :size="40"><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ opinionIndexDisplay }}</div>
          <div class="stat-label">舆情指数</div>
          <div class="stat-level">{{ opinionLevelText }}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card positive">
        <div class="stat-icon">
          <el-icon :size="40"><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ dashboardData.positiveCount || 0 }}</div>
          <div class="stat-label">正向评价</div>
          <div class="stat-sub">占比 {{ positiveRateText }}%</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card negative">
        <div class="stat-icon">
          <el-icon :size="40"><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ dashboardData.negativeCount || 0 }}</div>
          <div class="stat-label">负向评价</div>
          <div class="stat-sub">占比 {{ negativeRateText }}%</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card warning" v-if="dashboardData.warningCount > 0">
        <div class="stat-icon">
          <el-icon :size="40"><Bell /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ dashboardData.warningCount || 0 }}</div>
          <div class="stat-label">预警事件</div>
          <div class="stat-sub">需重点关注</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card total">
        <div class="stat-icon">
          <el-icon :size="40"><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ dashboardData.totalEvaluations || 0 }}</div>
          <div class="stat-label">评价总数</div>
          <div class="stat-sub">
            均速 {{ avgSpeedText }} · 均效 {{ avgEffectText }}
          </div>
        </div>
      </el-card>
    </div>

    <div class="chart-row">
      <el-card shadow="never" class="chart-card trend-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">舆情指数趋势</span>
            <el-radio-group v-model="trendChartType" size="small" @change="renderTrendChart">
              <el-radio-button label="index">舆情指数</el-radio-button>
              <el-radio-button label="sentiment">情感得分</el-radio-button>
              <el-radio-button label="count">评价数量</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card wordcloud-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">高频词云</span>
            <el-tag type="info" size="small">TOP {{ queryForm.wordCloudSize }}</el-tag>
          </div>
        </template>
        <div ref="wordCloudRef" class="wordcloud-container"></div>
      </el-card>
    </div>

    <div class="bottom-row">
      <el-card shadow="never" class="bottom-card hot-event-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon><WarningFilled /></el-icon>
              近期负面舆情热点事件
            </span>
            <el-button text type="primary" size="small" @click="fetchData">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </template>

        <div class="hot-event-list" v-loading="loading">
          <div
            v-for="(event, index) in dashboardData.hotNegativeEvents || []"
            :key="event.eventId"
            class="hot-event-item"
          >
            <div class="event-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
            <div class="event-info">
              <div class="event-title" @click="viewEvent(event)">
                {{ event.eventTitle || '无标题' }}
                <el-tag size="small" type="warning" v-if="event.warningLevel === 'critical'">
                  严重预警
                </el-tag>
                <el-tag size="small" type="danger" v-else-if="event.warningLevel === 'warning'">
                  预警
                </el-tag>
              </div>
              <div class="event-meta">
                <span class="event-type">
                  <el-icon><Collection /></el-icon>{{ event.eventType || '-' }}
                </span>
                <span class="event-grid">
                  <el-icon><Location /></el-icon>{{ event.gridName || '-' }}
                </span>
                <span class="event-time">
                  <el-icon><Clock /></el-icon>{{ event.createdAt || '-' }}
                </span>
              </div>
              <div class="event-content" v-if="event.content">
                "{{ event.content }}"
              </div>
              <div class="event-scores">
                <el-rate
                  v-model="event.speedScore"
                  disabled
                  size="small"
                  :max="5"
                  show-text
                  text-color="#ff9900"
                >
                  <template #text>
                    <span class="score-text">速度 {{ event.speedScore }} 分</span>
                  </template>
                </el-rate>
                <el-rate
                  v-model="event.effectScore"
                  disabled
                  size="small"
                  :max="5"
                  show-text
                  text-color="#ff9900"
                >
                  <template #text>
                    <span class="score-text">效果 {{ event.effectScore }} 分</span>
                  </template>
                </el-rate>
                <el-tag size="small" :type="getSentimentTagType(event.sentimentLabel)">
                  情感: {{ getSentimentLabelText(event.sentimentLabel) }}
                  ({{ event.sentimentScore ? event.sentimentScore.toFixed(2) : '0.00' }})
                </el-tag>
              </div>
            </div>
          </div>

          <div v-if="!dashboardData.hotNegativeEvents || dashboardData.hotNegativeEvents.length === 0" class="empty-state">
            <el-empty description="暂无负面舆情事件" :image-size="100" />
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="bottom-card grid-rank-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon><Rank /></el-icon>
              各网格舆情指数排行
            </span>
          </div>
        </template>
        <div class="grid-rank-list">
          <div
            v-for="(grid, index) in dashboardData.gridOpinions || []"
            :key="grid.gridId"
            class="grid-rank-item"
          >
            <div class="grid-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
            <div class="grid-info">
              <div class="grid-name">{{ grid.gridName || '未知' }}</div>
              <div class="grid-stats">
                评价 {{ grid.evaluationCount || 0 }} 条
                <span v-if="grid.warningCount > 0" class="warning-count">
                  预警 {{ grid.warningCount }} 条
                </span>
              </div>
            </div>
            <div class="grid-score">
              <div class="score-bar">
                <div
                  class="score-bar-fill"
                  :style="{
                    width: (grid.opinionIndex * 100) + '%',
                    background: getIndexColor(grid.opinionIndex)
                  }"
                ></div>
              </div>
              <span class="score-text" :style="{ color: getIndexColor(grid.opinionIndex) }">
                {{ grid.opinionIndex ? (grid.opinionIndex * 100).toFixed(1) : 0 }}
              </span>
            </div>
          </div>

          <div v-if="!dashboardData.gridOpinions || dashboardData.gridOpinions.length === 0" class="empty-state">
            <el-empty description="暂无网格数据" :image-size="80" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import {
  getOpinionDashboard
} from '@/api/publicOpinion'
import { getGridTree } from '@/api/grid'

const loading = ref(false)
const dashboardData = ref({})
const gridTree = ref([])
const trendChartRef = ref(null)
const wordCloudRef = ref(null)
let trendChartInstance = null
let wordCloudInstance = null
const trendChartType = ref('index')

const queryForm = reactive({
  gridId: null,
  trendDays: 7,
  wordCloudSize: 50,
  hotEventSize: 10
})

const opinionIndexDisplay = computed(() => {
  if (!dashboardData.value.opinionIndex) return '0.00'
  return (dashboardData.value.opinionIndex * 100).toFixed(1)
})

const opinionLevelText = computed(() => {
  const level = dashboardData.value.opinionLevel
  const map = {
    excellent: '优秀',
    good: '良好',
    normal: '正常',
    attention: '关注',
    warning: '预警',
    critical: '严重'
  }
  return map[level] || '正常'
})

const opinionLevelClass = computed(() => {
  return `level-${dashboardData.value.opinionLevel || 'normal'}`
})

const positiveRateText = computed(() => {
  if (!dashboardData.value.positiveRate) return '0'
  return (dashboardData.value.positiveRate * 100).toFixed(1)
})

const negativeRateText = computed(() => {
  if (!dashboardData.value.negativeRate) return '0'
  return (dashboardData.value.negativeRate * 100).toFixed(1)
})

const avgSpeedText = computed(() => {
  return dashboardData.value.avgSpeedScore || '-'
})

const avgEffectText = computed(() => {
  return dashboardData.value.avgEffectScore || '-'
})

function getIndexColor(index) {
  if (index >= 0.8) return '#67c23a'
  if (index >= 0.7) return '#409eff'
  if (index >= 0.6) return '#e6a23c'
  if (index >= 0.4) return '#f56c6c'
  return '#f5222d'
}

function getSentimentTagType(label) {
  if (label === 'positive') return 'success'
  if (label === 'negative') return 'danger'
  return 'info'
}

function getSentimentLabelText(label) {
  if (label === 'positive') return '正向'
  if (label === 'negative') return '负向'
  return '中性'
}

async function fetchGridTree() {
  try {
    const res = await getGridTree()
    if (res.code === 200) {
      gridTree.value = res.data || []
    }
  } catch (e) {
    console.error('获取网格树失败', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      gridId: queryForm.gridId || null,
      trendDays: queryForm.trendDays,
      wordCloudSize: queryForm.wordCloudSize,
      hotEventSize: queryForm.hotEventSize
    }
    const res = await getOpinionDashboard(params)
    if (res.code === 200) {
      dashboardData.value = res.data
      nextTick(() => {
        renderTrendChart()
        renderWordCloud()
      })
    }
  } catch (e) {
    ElMessage.error('获取舆情数据失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleReset() {
  queryForm.gridId = null
  queryForm.trendDays = 7
  queryForm.wordCloudSize = 50
  queryForm.hotEventSize = 10
  fetchData()
}

function viewEvent(event) {
  console.log('查看事件:', event.eventId)
}

function renderTrendChart() {
  if (!trendChartRef.value) return

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }

  const trendData = dashboardData.value.trendData || []
  const dates = trendData.map(item => item.date)

  let seriesData = []
  let yAxisName = ''
  let color = ''

  if (trendChartType.value === 'index') {
    seriesData = trendData.map(item => item.opinionIndex ? (item.opinionIndex * 100).toFixed(1) : 0)
    yAxisName = '舆情指数'
    color = '#409eff'
  } else if (trendChartType.value === 'sentiment') {
    seriesData = trendData.map(item => item.avgSentimentScore ? (item.avgSentimentScore * 100).toFixed(1) : 50)
    yAxisName = '情感得分'
    color = '#67c23a'
  } else {
    seriesData = trendData.map(item => item.evaluationCount || 0)
    yAxisName = '评价数量'
    color = '#e6a23c'
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f6fc' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    series: [
      {
        name: yAxisName,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: seriesData,
        lineStyle: { width: 3, color: color },
        itemStyle: { color: color, borderWidth: 2, borderColor: '#fff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: color + '40' },
            { offset: 1, color: color + '05' }
          ])
        }
      }
    ]
  }

  trendChartInstance.setOption(option, true)
}

function renderWordCloud() {
  if (!wordCloudRef.value) return

  if (!wordCloudInstance) {
    wordCloudInstance = echarts.init(wordCloudRef.value)
  }

  const wordData = dashboardData.value.wordCloud || []
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b8c5']

  const data = wordData.map((item, index) => ({
    name: item.name,
    value: item.value,
    textStyle: {
      color: colors[index % colors.length],
      fontWeight: 'bold'
    }
  }))

  const option = {
    tooltip: {
      show: true,
      formatter: function(params) {
        return `${params.name}: ${params.value}次`
      }
    },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      left: 'center',
      top: 'center',
      width: '90%',
      height: '80%',
      sizeRange: [14, 60],
      rotationRange: [-45, 45],
      rotationStep: 15,
      gridSize: 8,
      drawOutOfBound: false,
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold'
      },
      emphasis: {
        focus: 'self',
        textStyle: {
          textShadowBlur: 10,
          textShadowColor: 'rgba(0,0,0,0.3)'
        }
      },
      data: data
    }]
  }

  wordCloudInstance.setOption(option, true)
}

function handleResize() {
  trendChartInstance && trendChartInstance.resize()
  wordCloudInstance && wordCloudInstance.resize()
}

onMounted(() => {
  fetchGridTree()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance && trendChartInstance.dispose()
  wordCloudInstance && wordCloudInstance.dispose()
})
</script>

<style lang="scss" scoped>
.public-opinion-page {
  .filter-card {
    margin-bottom: 16px;
    .filter-form {
      .el-form-item {
        margin-bottom: 0;
      }
    }
  }

  .stats-row {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 16px;
    margin-bottom: 16px;

    .stat-card {
      .el-card__body {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 20px;
      }

      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }

      .stat-content {
        flex: 1;
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
          line-height: 1.2;
        }
        .stat-label {
          font-size: 13px;
          color: #909399;
          margin-top: 4px;
        }
        .stat-sub {
          font-size: 12px;
          color: #c0c4cc;
          margin-top: 2px;
        }
        .stat-level {
          display: inline-block;
          font-size: 12px;
          padding: 2px 8px;
          border-radius: 4px;
          margin-top: 6px;
        }
      }

      &.opinion-index {
        .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
        .stat-level { background: #ecf5ff; color: #409eff; }
        &.level-excellent .stat-icon { background: linear-gradient(135deg, #11998e, #38ef7d); }
        &.level-good .stat-icon { background: linear-gradient(135deg, #56ccf2, #2f80ed); }
        &.level-normal .stat-icon { background: linear-gradient(135deg, #f7971e, #ffd200); }
        &.level-attention .stat-icon { background: linear-gradient(135deg, #f093fb, #f5576c); }
        &.level-warning .stat-icon { background: linear-gradient(135deg, #ff512f, #dd2476); }
        &.level-critical .stat-icon { background: linear-gradient(135deg, #870000, #190a05); }
      }

      &.positive {
        .stat-icon { background: linear-gradient(135deg, #11998e, #38ef7d); }
      }

      &.negative {
        .stat-icon { background: linear-gradient(135deg, #f5576c, #f093fb); }
      }

      &.warning {
        .stat-icon { background: linear-gradient(135deg, #ff512f, #dd2476); }
        .stat-value { color: #f56c6c; }
      }

      &.total {
        .stat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }
      }
    }
  }

  .chart-row {
    display: grid;
    grid-template-columns: 1.5fr 1fr;
    gap: 16px;
    margin-bottom: 16px;

    .chart-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .card-title {
          font-weight: 600;
          font-size: 15px;
          color: #303133;
        }
      }
    }

    .trend-card .chart-container {
      height: 320px;
    }

    .wordcloud-card .wordcloud-container {
      height: 320px;
    }
  }

  .bottom-row {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 16px;

    .bottom-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .card-title {
          font-weight: 600;
          font-size: 15px;
          color: #303133;
          display: flex;
          align-items: center;
          gap: 6px;
        }
      }
    }

    .hot-event-card {
      .hot-event-list {
        max-height: 480px;
        overflow-y: auto;
      }

      .hot-event-item {
        display: flex;
        gap: 12px;
        padding: 16px;
        border-bottom: 1px solid #f2f6fc;
        transition: background 0.2s;

        &:hover {
          background: #fafbfc;
        }

        &:last-child {
          border-bottom: none;
        }

        .event-rank {
          width: 28px;
          height: 28px;
          border-radius: 6px;
          background: #f2f6fc;
          color: #909399;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
          font-size: 14px;
          flex-shrink: 0;

          &.rank-1 { background: linear-gradient(135deg, #ff512f, #dd2476); color: #fff; }
          &.rank-2 { background: linear-gradient(135deg, #f7971e, #ffd200); color: #fff; }
          &.rank-3 { background: linear-gradient(135deg, #56ccf2, #2f80ed); color: #fff; }
        }

        .event-info {
          flex: 1;
          min-width: 0;

          .event-title {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            &:hover {
              color: #409eff;
            }
          }

          .event-meta {
            display: flex;
            gap: 16px;
            font-size: 12px;
            color: #909399;
            margin-bottom: 8px;

            .event-type, .event-grid, .event-time {
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }

          .event-content {
            font-size: 13px;
            color: #606266;
            line-height: 1.6;
            margin-bottom: 8px;
            font-style: italic;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }

          .event-scores {
            display: flex;
            align-items: center;
            gap: 16px;

            .score-text {
              font-size: 12px;
              color: #909399;
            }
          }
        }
      }
    }

    .grid-rank-card {
      .grid-rank-list {
        max-height: 480px;
        overflow-y: auto;
      }

      .grid-rank-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        border-bottom: 1px solid #f2f6fc;

        &:last-child {
          border-bottom: none;
        }

        .grid-rank {
          width: 24px;
          height: 24px;
          border-radius: 4px;
          background: #f2f6fc;
          color: #909399;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
          font-size: 12px;
          flex-shrink: 0;

          &.rank-1 { background: #f56c6c; color: #fff; }
          &.rank-2 { background: #e6a23c; color: #fff; }
          &.rank-3 { background: #409eff; color: #fff; }
        }

        .grid-info {
          flex: 1;
          min-width: 0;

          .grid-name {
            font-size: 14px;
            color: #303133;
            margin-bottom: 2px;
          }

          .grid-stats {
            font-size: 12px;
            color: #909399;

            .warning-count {
              color: #f56c6c;
              margin-left: 8px;
            }
          }
        }

        .grid-score {
          display: flex;
          align-items: center;
          gap: 8px;

          .score-bar {
            width: 80px;
            height: 6px;
            background: #f2f6fc;
            border-radius: 3px;
            overflow: hidden;

            .score-bar-fill {
              height: 100%;
              border-radius: 3px;
              transition: width 0.3s;
            }
          }

          .score-text {
            font-size: 13px;
            font-weight: bold;
            min-width: 40px;
            text-align: right;
          }
        }
      }
    }

    .empty-state {
      padding: 40px 0;
    }
  }
}

@media (max-width: 1400px) {
  .public-opinion-page {
    .stats-row {
      grid-template-columns: repeat(3, 1fr);
    }
    .chart-row {
      grid-template-columns: 1fr;
    }
    .bottom-row {
      grid-template-columns: 1fr;
    }
  }
}
</style>
