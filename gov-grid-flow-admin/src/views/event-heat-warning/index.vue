<template>
  <div class="event-heat-warning-page">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="6">
        <el-card shadow="hover" class="filter-card" :body-style="{ padding: '16px' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">筛选条件</span>
            </div>
          </template>
          <div class="filter-content">
            <div class="filter-item">
              <label class="filter-label">选择网格</label>
              <el-select
                v-model="selectedGridId"
                placeholder="全部网格"
                clearable
                style="width: 100%"
                @change="handleGridChange"
              >
                <el-option
                  v-for="grid in gridList"
                  :key="grid.id"
                  :label="grid.gridName"
                  :value="grid.id"
                />
              </el-select>
            </div>
            <div class="filter-item">
              <label class="filter-label">预警等级</label>
              <div class="level-legend">
                <div class="legend-item">
                  <span class="legend-color low"></span>
                  <span class="legend-text">低风险</span>
                </div>
                <div class="legend-item">
                  <span class="legend-color medium"></span>
                  <span class="legend-text">中风险</span>
                </div>
                <div class="legend-item">
                  <span class="legend-color high"></span>
                  <span class="legend-text">高风险</span>
                </div>
                <div class="legend-item">
                  <span class="legend-color critical"></span>
                  <span class="legend-text">极高风险</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="high-warning-card" :body-style="{ padding: '16px' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">高风险预警网格</span>
              <el-tag type="danger" size="small">{{ highWarningGrids.length }}</el-tag>
            </div>
          </template>
          <div class="high-warning-list">
            <div
              v-for="item in highWarningGrids"
              :key="item.gridId"
              class="warning-item"
              :class="'level-' + item.heatLevel"
              @click="handleSelectGrid(item.gridId)"
            >
              <div class="warning-grid-name">{{ item.gridName }}</div>
              <div class="warning-info">
                <span class="heat-score">热度: {{ item.heatScore }}</span>
                <el-tag :type="getHeatLevelTagType(item.heatLevel)" size="small">
                  {{ item.heatLevelDesc }}
                </el-tag>
              </div>
              <div class="warning-predicted">
                预计事件: {{ item.predictedEventCount }} 件
              </div>
            </div>
            <div v-if="highWarningGrids.length === 0" class="empty-warning">
              <el-empty description="暂无高风险预警" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="18">
        <el-card shadow="hover" class="calendar-card" :body-style="{ padding: '16px' }">
          <template #header>
            <div class="card-header">
              <span class="header-title">事件热度日历热力图</span>
              <div class="header-actions">
                <el-button size="small" @click="prevMonth">
                  <el-icon><ArrowLeft /></el-icon>
                </el-button>
                <span class="month-label">{{ currentMonthLabel }}</span>
                <el-button size="small" @click="nextMonth">
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          <div ref="calendarChartRef" class="calendar-chart"></div>
        </el-card>

        <el-row :gutter="20" class="detail-row">
          <el-col :xs="24" :sm="24" :md="12">
            <el-card shadow="hover" class="detail-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">选中日期详情</span>
                </div>
              </template>
              <div v-if="selectedDateData" class="date-detail">
                <div class="detail-date">{{ selectedDateData.date }}</div>
                <div class="detail-heat">
                  <span class="heat-label">热度值</span>
                  <span class="heat-value" :class="'heat-' + selectedDateData.heatLevel">
                    {{ selectedDateData.heatValue }}
                  </span>
                </div>
                <div class="detail-count">
                  <span class="count-label">事件数量</span>
                  <span class="count-value">{{ selectedDateData.eventCount || 0 }} 件</span>
                </div>
                <div class="detail-grid">
                  <span class="grid-label">所属网格</span>
                  <span class="grid-value">{{ selectedDateData.gridName || '-' }}</span>
                </div>
                <div v-if="selectedDateData.topEventTypes && selectedDateData.topEventTypes.length > 0" class="detail-types">
                  <div class="types-title">高发事件类型</div>
                  <div class="types-list">
                    <div
                      v-for="(type, index) in selectedDateData.topEventTypes"
                      :key="type.eventType"
                      class="type-item"
                    >
                      <span class="type-rank">{{ index + 1 }}</span>
                      <span class="type-name">{{ type.eventTypeName }}</span>
                      <span class="type-count">{{ type.predictedCount || 0 }}件</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-detail">
                <el-empty description="请在日历上选择日期查看详情" :image-size="80" />
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12">
            <el-card shadow="hover" class="detail-card" :body-style="{ padding: '16px' }">
              <template #header>
                <div class="card-header">
                  <span class="header-title">未来24小时预警预测</span>
                  <el-button
                    v-if="selectedGridId"
                    type="primary"
                    size="small"
                    @click="handlePushNotification"
                  >
                    推送预警
                  </el-button>
                </div>
              </template>
              <div v-if="forecastData" class="forecast-detail">
                <div class="forecast-header">
                  <div class="forecast-grid">{{ forecastData.gridName }}</div>
                  <el-tag :type="getHeatLevelTagType(forecastData.heatLevel)" size="large">
                    {{ forecastData.heatLevelDesc }}
                  </el-tag>
                </div>
                <div class="forecast-score">
                  <div class="score-circle" :class="'score-' + forecastData.heatLevel">
                    <span class="score-value">{{ forecastData.heatScore }}</span>
                    <span class="score-label">预警评分</span>
                  </div>
                  <div class="score-info">
                    <div class="info-item">
                      <span class="info-label">预计事件数</span>
                      <span class="info-value">{{ forecastData.predictedEventCount }} 件</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">天气状况</span>
                      <span class="info-value">{{ forecastData.weatherCondition }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">是否节假日</span>
                      <span class="info-value">{{ forecastData.isHoliday ? '是' : '否' }}</span>
                    </div>
                  </div>
                </div>
                <div class="forecast-suggestion">
                  <div class="suggestion-title">巡查建议</div>
                  <div class="suggestion-content">{{ forecastData.suggestion }}</div>
                </div>
                <div class="forecast-types">
                  <div class="types-title">高发事件类型预测</div>
                  <div ref="forecastTypeChartRef" class="types-chart"></div>
                </div>
              </div>
              <div v-else class="empty-detail">
                <el-empty description="请选择网格查看预测详情" :image-size="80" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, ElEmpty } from 'element-plus'
import {
  getGridHeatForecastByGridId,
  getCalendarHeatmapByMonth,
  pushWarningNotification,
  getHighWarningGrids
} from '@/api/eventHeatWarning'
import { getGridAll } from '@/api/grid'

const selectedGridId = ref(null)
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const gridList = ref([])
const highWarningGrids = ref([])
const calendarData = ref([])
const selectedDateData = ref(null)
const forecastData = ref(null)

const calendarChartRef = ref()
const forecastTypeChartRef = ref()

let calendarChart = null
let forecastTypeChart = null

const currentMonthLabel = computed(() => {
  return `${currentYear.value}年${currentMonth.value}月`
})

function getHeatLevelTagType(level) {
  switch (level) {
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'danger'
    case 4:
      return 'danger'
    default:
      return 'info'
  }
}

async function loadGridList() {
  try {
    const res = await getGridAll()
    gridList.value = res.data || []
  } catch (e) {
    console.error('加载网格列表失败', e)
  }
}

async function loadHighWarningGrids() {
  try {
    const res = await getHighWarningGrids()
    highWarningGrids.value = res.data || []
  } catch (e) {
    console.error('加载高风险预警失败', e)
    highWarningGrids.value = []
  }
}

async function loadCalendarHeatmap() {
  try {
    const res = await getCalendarHeatmapByMonth(
      currentYear.value,
      currentMonth.value,
      selectedGridId.value
    )
    calendarData.value = res.data || []
    nextTick(() => {
      initCalendarChart()
    })
  } catch (e) {
    console.error('加载日历热力图失败', e)
    calendarData.value = []
    nextTick(() => {
      initCalendarChart()
    })
  }
}

async function loadForecastData() {
  if (!selectedGridId.value) {
    forecastData.value = null
    return
  }
  try {
    const res = await getGridHeatForecastByGridId(selectedGridId.value, 24)
    forecastData.value = res.data
    nextTick(() => {
      initForecastTypeChart()
    })
  } catch (e) {
    console.error('加载预测数据失败', e)
    forecastData.value = null
  }
}

function initCalendarChart() {
  if (!calendarChartRef.value) return
  if (calendarChart) calendarChart.dispose()
  calendarChart = echarts.init(calendarChartRef.value)

  const data = calendarData.value.map(item => [item.date, item.heatValue || 0])

  const option = {
    tooltip: {
      formatter: function (params) {
        const date = params.value[0]
        const value = params.value[1]
        const item = calendarData.value.find(d => d.date === date)
        let levelText = ''
        if (value < 30) levelText = '低风险'
        else if (value < 60) levelText = '中风险'
        else if (value < 85) levelText = '高风险'
        else levelText = '极高风险'
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">${date}</div>
            <div>热度值: ${value}</div>
            <div>风险等级: ${levelText}</div>
            <div>事件数量: ${item?.eventCount || 0} 件</div>
          </div>
        `
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    visualMap: {
      show: true,
      min: 0,
      max: 100,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 10,
      inRange: {
        color: ['#e1f3d8', '#67C23A', '#E6A23C', '#F56C6C', '#C0392B']
      },
      text: ['高', '低'],
      textStyle: { color: '#606266' }
    },
    calendar: {
      top: 50,
      left: 50,
      right: 50,
      cellSize: ['auto', 40],
      range: [`${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`],
      itemStyle: {
        borderWidth: 2,
        borderColor: '#fff'
      },
      yearLabel: { show: false },
      monthLabel: { show: false },
      dayLabel: {
        nameMap: 'ZH',
        color: '#606266'
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#f0f2f5',
          width: 1
        }
      }
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        }
      }
    ]
  }

  calendarChart.setOption(option)

  calendarChart.on('click', function (params) {
    const date = params.value[0]
    const item = calendarData.value.find(d => d.date === date)
    if (item) {
      selectedDateData.value = item
    }
  })
}

function initForecastTypeChart() {
  if (!forecastTypeChartRef.value || !forecastData.value) return
  if (forecastTypeChart) forecastTypeChart.dispose()
  forecastTypeChart = echarts.init(forecastTypeChartRef.value)

  const types = forecastData.value.eventTypeForecasts || []
  const topTypes = types.slice(0, 6)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133' },
      formatter: function (params) {
        const data = params[0]
        const typeData = topTypes.find(t => t.eventTypeName === data.name)
        return `
          <div style="padding: 4px;">
            <div style="font-weight: bold;">${data.name}</div>
            <div>概率: ${(typeData?.probability || 0).toFixed(2)}%</div>
            <div>预计数量: ${typeData?.predictedCount || 0} 件</div>
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '10%',
      bottom: '3%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: topTypes.map(t => t.eventTypeName),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        data: topTypes.map(t => t.probability || 0),
        barWidth: '50%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#66b1ff' },
            { offset: 1, color: '#409EFF' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#606266',
          fontSize: 12,
          formatter: '{c}%'
        }
      }
    ]
  }

  forecastTypeChart.setOption(option)
}

function handleGridChange() {
  loadCalendarHeatmap()
  loadForecastData()
}

function handleSelectGrid(gridId) {
  selectedGridId.value = gridId
  loadForecastData()
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  loadCalendarHeatmap()
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  loadCalendarHeatmap()
}

async function handlePushNotification() {
  if (!selectedGridId.value) {
    ElMessage.warning('请先选择网格')
    return
  }
  try {
    await pushWarningNotification(selectedGridId.value)
    ElMessage.success('预警通知推送成功')
  } catch (e) {
    ElMessage.error('预警通知推送失败')
  }
}

function handleResize() {
  calendarChart && calendarChart.resize()
  forecastTypeChart && forecastTypeChart.resize()
}

onMounted(() => {
  nextTick(() => {
    loadGridList()
    loadHighWarningGrids()
    loadCalendarHeatmap()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  calendarChart && calendarChart.dispose()
  forecastTypeChart && forecastTypeChart.dispose()
})
</script>

<style lang="scss" scoped>
.event-heat-warning-page {
  padding: 0;
  background: #f5f7fa;
  min-height: 100%;
  box-sizing: border-box;

  :deep(.el-card) {
    border-radius: 8px;
    border: none;
    margin-bottom: 20px;
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

    .header-actions {
      display: flex;
      align-items: center;
      gap: 8px;

      .month-label {
        font-size: 14px;
        font-weight: 500;
        color: #606266;
        min-width: 100px;
        text-align: center;
      }
    }
  }

  .filter-card {
    .filter-content {
      .filter-item {
        margin-bottom: 20px;

        &:last-child {
          margin-bottom: 0;
        }

        .filter-label {
          display: block;
          color: #606266;
          font-size: 13px;
          font-weight: 500;
          margin-bottom: 8px;
        }
      }
    }

    .level-legend {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .legend-color {
          width: 16px;
          height: 16px;
          border-radius: 3px;

          &.low {
            background: #67C23A;
          }

          &.medium {
            background: #E6A23C;
          }

          &.high {
            background: #F56C6C;
          }

          &.critical {
            background: #C0392B;
          }
        }

        .legend-text {
          font-size: 12px;
          color: #606266;
        }
      }
    }
  }

  .high-warning-card {
    .high-warning-list {
      max-height: 400px;
      overflow-y: auto;

      .warning-item {
        padding: 12px;
        border-radius: 8px;
        margin-bottom: 10px;
        cursor: pointer;
        transition: all 0.3s ease;
        border-left: 4px solid transparent;

        &:hover {
          transform: translateX(4px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        &.level-1 {
          background: #f0f9eb;
          border-left-color: #67C23A;
        }

        &.level-2 {
          background: #fdf6ec;
          border-left-color: #E6A23C;
        }

        &.level-3 {
          background: #fef0f0;
          border-left-color: #F56C6C;
        }

        &.level-4 {
          background: #fde2e2;
          border-left-color: #C0392B;
        }

        .warning-grid-name {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 6px;
        }

        .warning-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 4px;

          .heat-score {
            font-size: 12px;
            color: #909399;
          }
        }

        .warning-predicted {
          font-size: 12px;
          color: #606266;
        }
      }

      .empty-warning {
        padding: 20px 0;
      }
    }
  }

  .calendar-card {
    .calendar-chart {
      height: 320px;
      width: 100%;
    }
  }

  .detail-row {
    .detail-card {
      .date-detail {
        padding: 10px 0;

        .detail-date {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 16px;
          text-align: center;
        }

        .detail-heat,
        .detail-count,
        .detail-grid {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 0;
          border-bottom: 1px solid #f0f2f5;

          &:last-of-type {
            border-bottom: none;
          }

          .heat-label,
          .count-label,
          .grid-label {
            font-size: 13px;
            color: #909399;
          }

          .heat-value {
            font-size: 20px;
            font-weight: 700;

            &.heat-low {
              color: #67C23A;
            }

            &.heat-medium {
              color: #E6A23C;
            }

            &.heat-high {
              color: #F56C6C;
            }

            &.heat-critical {
              color: #C0392B;
            }
          }

          .count-value,
          .grid-value {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }
        }

        .detail-types {
          margin-top: 16px;

          .types-title {
            font-size: 13px;
            font-weight: 500;
            color: #606266;
            margin-bottom: 10px;
          }

          .types-list {
            .type-item {
              display: flex;
              align-items: center;
              padding: 6px 0;
              border-bottom: 1px solid #f0f2f5;

              &:last-child {
                border-bottom: none;
              }

              .type-rank {
                width: 20px;
                height: 20px;
                line-height: 20px;
                text-align: center;
                background: #409EFF;
                color: #fff;
                font-size: 12px;
                border-radius: 4px;
                margin-right: 10px;
                flex-shrink: 0;
              }

              .type-name {
                flex: 1;
                font-size: 13px;
                color: #303133;
              }

              .type-count {
                font-size: 12px;
                color: #909399;
              }
            }
          }
        }
      }

      .forecast-detail {
        .forecast-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
          padding-bottom: 12px;
          border-bottom: 1px solid #f0f2f5;

          .forecast-grid {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
          }
        }

        .forecast-score {
          display: flex;
          align-items: center;
          gap: 20px;
          margin-bottom: 16px;

          .score-circle {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            color: #fff;

            &.score-1 {
              background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
            }

            &.score-2 {
              background: linear-gradient(135deg, #E6A23C 0%, #f0c78a 100%);
            }

            &.score-3 {
              background: linear-gradient(135deg, #F56C6C 0%, #f89898 100%);
            }

            &.score-4 {
              background: linear-gradient(135deg, #C0392B 0%, #e74c3c 100%);
            }

            .score-value {
              font-size: 24px;
              font-weight: 700;
              line-height: 1;
            }

            .score-label {
              font-size: 12px;
              margin-top: 4px;
            }
          }

          .score-info {
            flex: 1;

            .info-item {
              display: flex;
              justify-content: space-between;
              padding: 6px 0;

              .info-label {
                font-size: 13px;
                color: #909399;
              }

              .info-value {
                font-size: 13px;
                font-weight: 500;
                color: #303133;
              }
            }
          }
        }

        .forecast-suggestion {
          background: #ecf5ff;
          border-radius: 8px;
          padding: 12px;
          margin-bottom: 16px;

          .suggestion-title {
            font-size: 13px;
            font-weight: 500;
            color: #409EFF;
            margin-bottom: 6px;
          }

          .suggestion-content {
            font-size: 13px;
            color: #606266;
            line-height: 1.6;
          }
        }

        .forecast-types {
          .types-title {
            font-size: 13px;
            font-weight: 500;
            color: #606266;
            margin-bottom: 10px;
          }

          .types-chart {
            height: 200px;
            width: 100%;
          }
        }
      }

      .empty-detail {
        padding: 40px 0;
      }
    }
  }
}
</style>
