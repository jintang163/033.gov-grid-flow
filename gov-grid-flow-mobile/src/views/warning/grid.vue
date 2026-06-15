<template>
  <div class="grid-warning-page">
    <van-nav-bar fixed placeholder @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
      <template #title>
        {{ forecast?.gridName || '网格预警' }}
      </template>
    </van-nav-bar>

    <div v-if="forecast" class="page-content">
      <div class="header-card" :class="'level-' + forecast.heatLevel">
        <div class="header-top">
          <div class="score-area">
            <div class="score-ring">
              <span class="score-num">{{ forecast.heatScore }}</span>
              <span class="score-label">预警评分</span>
            </div>
          </div>
          <div class="level-area">
            <van-tag :type="getHeatLevelTagType(forecast.heatLevel)" size="large" round>
              {{ forecast.heatLevelDesc }}
            </van-tag>
            <div class="level-tip">{{ getLevelTip(forecast.heatLevel) }}</div>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <van-icon name="todo-list-o" size="18" color="#1989fa" />
            <span class="info-label">预计事件</span>
            <span class="info-value">{{ forecast.predictedEventCount }} 件</span>
          </div>
          <div class="info-item">
            <van-icon name="sun-o" size="18" color="#ff976a" />
            <span class="info-label">天气</span>
            <span class="info-value">{{ forecast.weatherCondition }}</span>
          </div>
          <div class="info-item">
            <van-icon name="calendar-o" size="18" color="#07c160" />
            <span class="info-label">节假日</span>
            <span class="info-value">{{ forecast.isHoliday ? '是' : '否' }}</span>
          </div>
          <div class="info-item">
            <van-icon name="clock-o" size="18" color="#7232dd" />
            <span class="info-label">预测时段</span>
            <span class="info-value">未来24h</span>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-header">
          <van-icon name="bulb-o" size="16" color="#1989fa" />
          <span class="section-title">巡查建议</span>
        </div>
        <div class="suggestion-text">{{ forecast.suggestion }}</div>
      </div>

      <div class="section-card">
        <div class="section-header">
          <van-icon name="chart-trending-o" size="16" color="#ee0a24" />
          <span class="section-title">高发事件类型预测</span>
        </div>
        <div class="type-ranking">
          <div
            v-for="(type, index) in topTypes"
            :key="type.eventType"
            class="type-item"
          >
            <div class="type-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <div class="type-info">
              <div class="type-header">
                <span class="type-name">{{ type.eventTypeName }}</span>
                <span class="type-prob">{{ type.probability.toFixed(1) }}%</span>
              </div>
              <van-progress
                :percentage="Math.min(Math.round(type.probability), 100)"
                :stroke-width="10"
                :color="getTrendColor(type.trend)"
                show-pivot
              />
              <div class="type-meta">
                <span class="type-count">预计 {{ type.predictedCount || 0 }} 件</span>
                <span class="type-trend" :class="type.trend">
                  {{ getTrendText(type.trend) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card calendar-section">
        <div class="section-header">
          <van-icon name="calendar" size="16" color="#1989fa" />
          <span class="section-title">近期预警日历</span>
        </div>
        <div class="mini-calendar">
          <div class="calendar-header">
            <span class="weekday" v-for="w in weekDays" :key="w">{{ w }}</span>
          </div>
          <div class="calendar-body">
            <div
              v-for="(day, index) in calendarDays"
              :key="index"
              class="calendar-day"
              :class="{
                today: day.isToday,
                future: day.isFuture,
                weekend: day.isWeekend
              }"
              @click="selectDay(day)"
            >
              <span class="day-num">{{ day.day }}</span>
              <div
                class="day-heat"
                v-if="day.heatValue != null"
                :style="{ background: getHeatColor(day.heatValue) }"
              ></div>
            </div>
          </div>
          <div class="calendar-legend">
            <div class="legend-item">
              <span class="legend-dot" style="background: #67C23A"></span>
              <span>低风险</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot" style="background: #E6A23C"></span>
              <span>中风险</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot" style="background: #F56C6C"></span>
              <span>高风险</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot" style="background: #C0392B"></span>
              <span>极高</span>
            </div>
          </div>
        </div>
      </div>

      <div class="action-bar">
        <van-button block type="primary" size="large" round @click="handleShare">
          <template #icon>
            <van-icon name="share-o" />
          </template>
          分享预警
        </van-button>
        <van-button block type="danger" size="large" round @click="handlePushAll">
          <template #icon>
            <van-icon name="bell-o" />
          </template>
          推送全体网格员
        </van-button>
      </div>
    </div>

    <van-empty v-else description="加载中..." :image-size="60" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { getGridWarningForecast, getWarningCalendar, pushGridWarning } from '@/api/warning'

const router = useRouter()
const route = useRoute()

const forecast = ref(null)
const calendarData = ref([])
const currentMonth = ref(new Date().getMonth() + 1)
const currentYear = ref(new Date().getFullYear())

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const topTypes = computed(() => {
  if (!forecast.value?.eventTypeForecasts) return []
  return forecast.value.eventTypeForecasts.slice(0, 6)
})

const calendarDays = computed(() => {
  const firstDay = new Date(currentYear.value, currentMonth.value - 1, 1)
  const lastDay = new Date(currentYear.value, currentMonth.value, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()
  const today = new Date()

  const days = []

  for (let i = 0; i < startWeekday; i++) {
    days.push({ day: '', heatValue: null })
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    const calendarItem = calendarData.value.find(d => d.date === dateStr)
    const date = new Date(currentYear.value, currentMonth.value - 1, i)
    const isWeekend = date.getDay() === 0 || date.getDay() === 6
    const isToday = date.toDateString() === today.toDateString()
    const isFuture = date > today

    days.push({
      day: i,
      date: dateStr,
      heatValue: calendarItem?.heatValue,
      isWeekend,
      isToday,
      isFuture
    })
  }

  return days
})

const onClickLeft = () => {
  router.back()
}

function getHeatLevelTagType(level) {
  switch (level) {
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'danger'
    case 4: return 'danger'
    default: return 'default'
  }
}

function getLevelTip(level) {
  switch (level) {
    case 1: return '正常巡查即可'
    case 2: return '建议适当增巡'
    case 3: return '请加强巡查力度'
    case 4: return '紧急！请立即防控'
    default: return ''
  }
}

function getTrendColor(trend) {
  if (trend === 'up') return '#ee0a24'
  if (trend === 'down') return '#07c160'
  return '#1989fa'
}

function getTrendText(trend) {
  if (trend === 'up') return '↑ 上升'
  if (trend === 'down') return '↓ 下降'
  return '→ 稳定'
}

function getHeatColor(value) {
  if (value == null) return 'transparent'
  if (value < 30) return '#e1f3d8'
  if (value < 60) return '#faecd8'
  if (value < 85) return '#fde2e2'
  return '#f56c6c'
}

async function loadForecast() {
  try {
    const gridId = route.query.gridId
    const res = await getGridWarningForecast(gridId)
    forecast.value = res.data

    const today = new Date()
    const calRes = await getWarningCalendar(
      currentYear.value,
      currentMonth.value,
      gridId
    )
    calendarData.value = calRes.data || []
  } catch (e) {
    showToast('加载失败')
  }
}

function selectDay(day) {
  if (day.date) {
    showToast(`${day.date}: 热度 ${day.heatValue ?? '无数据'}`)
  }
}

function handleShare() {
  showToast('分享功能')
}

async function handlePushAll() {
  try {
    const gridId = route.query.gridId
    await pushGridWarning(gridId)
    showSuccessToast('已推送至全体网格员')
  } catch (e) {
    showToast('推送失败')
  }
}

onMounted(() => {
  loadForecast()
})
</script>

<style scoped lang="scss">
.grid-warning-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 150px;

  .page-content {
    padding: 12px;
  }

  .header-card {
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 12px;
    border-left: 4px solid transparent;

    &.level-1 {
      background: linear-gradient(135deg, #f0f9eb 0%, #fff 100%);
      border-left-color: #67C23A;
    }

    &.level-2 {
      background: linear-gradient(135deg, #fdf6ec 0%, #fff 100%);
      border-left-color: #E6A23C;
    }

    &.level-3 {
      background: linear-gradient(135deg, #fef0f0 0%, #fff 100%);
      border-left-color: #F56C6C;
    }

    &.level-4 {
      background: linear-gradient(135deg, #fde2e2 0%, #fff 100%);
      border-left-color: #C0392B;
    }

    .header-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;
      padding-bottom: 20px;
      border-bottom: 1px dashed #ebedf0;
    }

    .score-area {
      .score-ring {
        width: 110px;
        height: 110px;
        border-radius: 50%;
        background: linear-gradient(135deg, #1989fa 0%, #5d8cff 100%);
        color: #fff;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .score-num {
          font-size: 36px;
          font-weight: 700;
          line-height: 1;
        }

        .score-label {
          font-size: 12px;
          margin-top: 4px;
          opacity: 0.9;
        }
      }
    }

    .level-area {
      text-align: right;
      flex: 1;
      padding-left: 16px;

      .level-tip {
        font-size: 13px;
        color: #969799;
        margin-top: 10px;
      }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;

      .info-item {
        display: flex;
        align-items: center;
        gap: 8px;
        background: #f7f8fa;
        border-radius: 10px;
        padding: 10px 12px;

        .info-label {
          font-size: 13px;
          color: #969799;
        }

        .info-value {
          font-size: 13px;
          font-weight: 500;
          color: #323233;
          margin-left: auto;
        }
      }
    }
  }

  .section-card {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .section-header {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 14px;

      .section-title {
        font-size: 15px;
        font-weight: 600;
        color: #323233;
      }
    }

    .suggestion-text {
      font-size: 14px;
      color: #646566;
      line-height: 1.8;
      background: linear-gradient(135deg, #ecf5ff 0%, #f5faff 100%);
      padding: 14px;
      border-radius: 10px;
    }
  }

  .type-ranking {
    .type-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 0;
      border-bottom: 1px solid #f2f3f5;

      &:last-child {
        border-bottom: none;
      }

      .type-rank {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        background: #ebedf0;
        color: #969799;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        font-weight: 600;
        flex-shrink: 0;

        &.rank-1 {
          background: linear-gradient(135deg, #ff6b6b 0%, #ee0a24 100%);
          color: #fff;
        }

        &.rank-2 {
          background: linear-gradient(135deg, #ff976a 0%, #ff6034 100%);
          color: #fff;
        }

        &.rank-3 {
          background: linear-gradient(135deg, #ffd01e 0%, #ff9500 100%);
          color: #fff;
        }
      }

      .type-info {
        flex: 1;
        min-width: 0;

        .type-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .type-name {
            font-size: 14px;
            font-weight: 500;
            color: #323233;
          }

          .type-prob {
            font-size: 14px;
            font-weight: 700;
            color: #1989fa;
          }
        }

        .type-meta {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 8px;

          .type-count {
            font-size: 12px;
            color: #969799;
          }

          .type-trend {
            font-size: 12px;
            font-weight: 500;

            &.up {
              color: #ee0a24;
            }

            &.down {
              color: #07c160;
            }

            &.stable {
              color: #1989fa;
            }
          }
        }
      }
    }
  }

  .calendar-section {
    .mini-calendar {
      .calendar-header {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        text-align: center;
        margin-bottom: 8px;

        .weekday {
          font-size: 12px;
          color: #969799;
          padding: 6px 0;
        }
      }

      .calendar-body {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 4px;
        margin-bottom: 14px;

        .calendar-day {
          aspect-ratio: 1;
          border-radius: 6px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          position: relative;
          cursor: pointer;
          background: #f7f8fa;
          transition: all 0.2s;

          &:active {
            transform: scale(0.95);
          }

          &.today {
            background: #1989fa;

            .day-num {
              color: #fff;
              font-weight: 600;
            }
          }

          &.future {
            background: #fff9e6;

            &.today {
              background: #1989fa;
            }
          }

          &.weekend {
            .day-num {
              color: #ff6034;
            }

            &.today {
              .day-num {
                color: #fff;
              }
            }
          }

          .day-num {
            font-size: 13px;
            color: #323233;
          }

          .day-heat {
            position: absolute;
            bottom: 4px;
            width: 18px;
            height: 4px;
            border-radius: 2px;
          }
        }
      }

      .calendar-legend {
        display: flex;
        justify-content: center;
        gap: 14px;
        padding-top: 12px;
        border-top: 1px solid #f2f3f5;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: #969799;

          .legend-dot {
            width: 12px;
            height: 12px;
            border-radius: 3px;
          }
        }
      }
    }
  }

  .action-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 12px 16px;
    display: flex;
    gap: 12px;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);

    :deep(.van-button) {
      border-radius: 24px;
      font-size: 15px;
      height: 46px;
    }
  }
}
</style>
