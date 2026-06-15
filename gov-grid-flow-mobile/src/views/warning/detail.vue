<template>
  <div class="warning-detail-page">
    <van-nav-bar fixed placeholder @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
      <template #title>
        预警详情
      </template>
    </van-nav-bar>

    <div v-if="detail" class="detail-content">
      <div class="detail-header" :class="'level-' + getTypeLevel(detail.type)">
        <div class="header-icon">
          <van-icon :name="getTypeIcon(detail.type)" size="36" />
        </div>
        <div class="header-info">
          <div class="header-title">{{ detail.title }}</div>
          <div class="header-meta">
            <span>{{ formatTime(detail.createdAt) }}</span>
            <van-tag :type="getTypeTag(detail.type)" size="medium" round plain>
              {{ getTypeText(detail.type) }}
            </van-tag>
          </div>
        </div>
      </div>

      <div class="detail-body">
        <div class="section">
          <div class="section-title">预警内容</div>
          <div class="section-content">{{ detail.content }}</div>
        </div>

        <div v-if="gridForecast" class="section forecast-section">
          <div class="section-title">
            <van-icon name="chart-trending-o" size="16" />
            <span>预测数据</span>
          </div>
          <div class="forecast-card">
            <div class="forecast-header">
              <div class="forecast-grid">{{ gridForecast.gridName }}</div>
              <van-tag :type="getHeatLevelTagType(gridForecast.heatLevel)" size="medium" round>
                {{ gridForecast.heatLevelDesc }}
              </van-tag>
            </div>
            <div class="forecast-score">
              <div class="score-ring" :class="'score-' + gridForecast.heatLevel">
                <span class="score-num">{{ gridForecast.heatScore }}</span>
                <span class="score-unit">分</span>
              </div>
              <div class="score-meta">
                <div class="meta-row">
                  <span class="meta-label">预计事件</span>
                  <span class="meta-value">{{ gridForecast.predictedEventCount }} 件</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">天气状况</span>
                  <span class="meta-value">{{ gridForecast.weatherCondition }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">是否节假日</span>
                  <span class="meta-value">{{ gridForecast.isHoliday ? '是' : '否' }}</span>
                </div>
              </div>
            </div>
            <div class="forecast-suggestion">
              <div class="suggestion-header">
                <van-icon name="bulb-o" size="14" />
                <span>巡查建议</span>
              </div>
              <div class="suggestion-text">{{ gridForecast.suggestion }}</div>
            </div>
            <div v-if="gridForecast.eventTypeForecasts" class="forecast-types">
              <div class="types-header">高发事件类型预测</div>
              <div class="type-list">
                <div
                  v-for="(type, index) in gridForecast.eventTypeForecasts.slice(0, 5)"
                  :key="type.eventType"
                  class="type-item"
                >
                  <div class="type-rank">{{ index + 1 }}</div>
                  <div class="type-info">
                    <div class="type-name">{{ type.eventTypeName }}</div>
                    <van-progress
                      :percentage="Math.round(type.probability)"
                      :color="getTypeColor(type.trend)"
                      stroke-width="8"
                      show-pivot
                    />
                  </div>
                  <div class="type-count">
                    <div class="count-num">{{ type.predictedCount || 0 }}</div>
                    <div class="count-unit">预计件</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-footer">
        <van-button
          block
          type="primary"
          round
          size="large"
          @click="handleConfirm"
        >
          我已知晓
        </van-button>
      </div>
    </div>
    <van-empty v-else description="加载中..." :image-size="60" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { readWarning, getGridWarningForecast } from '@/api/warning'
import { getNotificationList } from '@/api'

const router = useRouter()
const route = useRoute()

const detail = ref(null)
const gridForecast = ref(null)

const onClickLeft = () => {
  router.back()
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function getTypeLevel(type) {
  const map = {
    EVENT_WARNING: 3,
    EVENT_URGE: 2,
    SYSTEM: 1,
    TASK: 1
  }
  return map[type] || 1
}

function getTypeIcon(type) {
  const map = {
    EVENT_WARNING: 'warning-o',
    EVENT_URGE: 'clock-o',
    SYSTEM: 'info-o',
    TASK: 'todo-list-o'
  }
  return map[type] || 'bell-o'
}

function getTypeTag(type) {
  const map = {
    EVENT_WARNING: 'danger',
    EVENT_URGE: 'warning',
    SYSTEM: 'primary',
    TASK: 'success'
  }
  return map[type] || 'default'
}

function getTypeText(type) {
  const map = {
    EVENT_WARNING: '事件预警',
    EVENT_URGE: '催办通知',
    SYSTEM: '系统通知',
    TASK: '任务通知'
  }
  return map[type] || '通知'
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

function getTypeColor(trend) {
  if (trend === 'up') return '#ee0a24'
  if (trend === 'down') return '#07c160'
  return '#1989fa'
}

async function loadDetail() {
  try {
    const id = route.query.id
    const res = await getNotificationList({ page: 1, size: 100 })
    const list = res.data?.records || res.data || []
    detail.value = list.find(item => String(item.id) === String(id)) || list[0]

    if (detail.value && detail.value.bizId) {
      try {
        const forecastRes = await getGridWarningForecast(detail.value.bizId)
        gridForecast.value = forecastRes.data
      } catch (e) {
        console.log('加载网格预测失败', e)
      }
    }

    if (detail.value && detail.value.isRead === 0) {
      try {
        await readWarning(id)
      } catch (e) {}
    }
  } catch (e) {
    showToast('加载失败')
  }
}

function handleConfirm() {
  router.back()
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.warning-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 100px;

  .detail-content {
    padding: 12px;
  }

  .detail-header {
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    &.level-1 {
      background: linear-gradient(135deg, #e8f7e8 0%, #fff 100%);
    }

    &.level-2 {
      background: linear-gradient(135deg, #fff4e8 0%, #fff 100%);
    }

    &.level-3 {
      background: linear-gradient(135deg, #ffe8e8 0%, #fff 100%);
    }

    &.level-4 {
      background: linear-gradient(135deg, #ffd0d0 0%, #fff 100%);
    }

    .header-icon {
      width: 64px;
      height: 64px;
      border-radius: 16px;
      background: linear-gradient(135deg, #1989fa 0%, #5d8cff 100%);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .header-info {
      flex: 1;
      min-width: 0;

      .header-title {
        font-size: 17px;
        font-weight: 600;
        color: #323233;
        margin-bottom: 8px;
      }

      .header-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 13px;
        color: #969799;
      }
    }
  }

  .detail-body {
    .section {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

      .section-title {
        font-size: 15px;
        font-weight: 600;
        color: #323233;
        margin-bottom: 12px;
        display: flex;
        align-items: center;
        gap: 6px;

        &::before {
          content: '';
          width: 3px;
          height: 14px;
          background: linear-gradient(180deg, #1989fa 0%, #5d8cff 100%);
          border-radius: 2px;
        }
      }

      .section-content {
        font-size: 14px;
        color: #646566;
        line-height: 1.8;
        white-space: pre-line;
        background: #f7f8fa;
        padding: 12px;
        border-radius: 10px;
      }
    }

    .forecast-section {
      .forecast-card {
        background: #fff;
        border-radius: 12px;
        padding: 16px;
        background: linear-gradient(135deg, #f5faff 0%, #fff 100%);
      }

      .forecast-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .forecast-grid {
          font-size: 16px;
          font-weight: 600;
          color: #323233;
        }
      }

      .forecast-score {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-bottom: 16px;

        .score-ring {
          width: 100px;
          height: 100px;
          border-radius: 50%;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: #fff;
          flex-shrink: 0;

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

          .score-num {
            font-size: 32px;
            font-weight: 700;
            line-height: 1;
          }

          .score-unit {
            font-size: 12px;
            margin-top: 2px;
            opacity: 0.9;
          }
        }

        .score-meta {
          flex: 1;

          .meta-row {
            display: flex;
            justify-content: space-between;
            padding: 7px 0;
            border-bottom: 1px solid #ebedf0;

            &:last-child {
              border-bottom: none;
            }

            .meta-label {
              font-size: 13px;
              color: #969799;
            }

            .meta-value {
              font-size: 13px;
              font-weight: 500;
              color: #323233;
            }
          }
        }
      }

      .forecast-suggestion {
        background: #ecf5ff;
        border-radius: 10px;
        padding: 12px;
        margin-bottom: 16px;

        .suggestion-header {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          font-weight: 500;
          color: #1989fa;
          margin-bottom: 6px;
        }

        .suggestion-text {
          font-size: 13px;
          color: #646566;
          line-height: 1.7;
        }
      }

      .forecast-types {
        .types-header {
          font-size: 13px;
          font-weight: 500;
          color: #606266;
          margin-bottom: 12px;
          padding-bottom: 8px;
          border-bottom: 1px solid #ebedf0;
        }

        .type-list {
          .type-item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 0;

            .type-rank {
              width: 24px;
              height: 24px;
              border-radius: 6px;
              background: #1989fa;
              color: #fff;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 13px;
              font-weight: 600;
              flex-shrink: 0;
            }

            .type-info {
              flex: 1;
              min-width: 0;

              .type-name {
                font-size: 13px;
                color: #323233;
                margin-bottom: 6px;
              }
            }

            .type-count {
              text-align: center;
              flex-shrink: 0;

              .count-num {
                font-size: 18px;
                font-weight: 700;
                color: #323233;
                line-height: 1;
              }

              .count-unit {
                font-size: 10px;
                color: #969799;
                margin-top: 2px;
              }
            }
          }
        }
      }
    }
  }

  .detail-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px 16px;
    background: #fff;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);

    :deep(.van-button) {
      border-radius: 24px;
      font-size: 16px;
      height: 48px;
    }
  }
}
</style>
