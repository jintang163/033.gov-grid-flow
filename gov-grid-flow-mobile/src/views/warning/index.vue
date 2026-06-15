<template>
  <div class="warning-page">
    <van-nav-bar fixed placeholder @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
      <template #title>
        预警消息
      </template>
      <template #right>
        <span class="read-all" @click="markAllRead">全部已读</span>
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeTab" sticky offset-top="46px" background="#fff">
      <van-tab title="预警消息" name="warning">
        <div class="tab-content">
          <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
            <van-list
              v-model:loading="loading"
              :finished="finished"
              finished-text="没有更多了"
              @load="onLoad"
            >
              <div v-if="warningList.length > 0">
                <div
                  v-for="item in warningList"
                  :key="item.id"
                  class="warning-item"
                  :class="{ unread: item.isRead === 0 }"
                  @click="goDetail(item)"
                >
                  <div class="item-header">
                    <div class="item-icon" :class="getTypeClass(item.type)">
                      <van-icon :name="getTypeIcon(item.type)" size="20" />
                    </div>
                    <div class="item-info">
                      <div class="item-title">
                        <span v-if="item.isRead === 0" class="unread-dot"></span>
                        {{ item.title }}
                      </div>
                      <div class="item-time">{{ formatTime(item.createdAt) }}</div>
                    </div>
                    <div class="item-tag">
                      <van-tag :type="getTypeTag(item.type)" size="medium" round plain>
                        {{ getTypeText(item.type) }}
                      </van-tag>
                    </div>
                  </div>
                  <div class="item-content">{{ formatContent(item.content) }}</div>
                </div>
              </div>
              <van-empty v-else-if="!loading" description="暂无预警消息" :image-size="80" />
            </van-list>
          </van-pull-refresh>
        </div>
      </van-tab>

      <van-tab title="网格预警" name="grid">
        <div class="tab-content">
          <van-pull-refresh v-model="gridRefreshing" @refresh="onGridRefresh">
            <van-list
              v-model:loading="gridLoading"
              :finished="gridFinished"
              finished-text="没有更多了"
              @load="onGridLoad"
            >
              <div v-if="gridWarningList.length > 0">
                <div
                  v-for="item in gridWarningList"
                  :key="item.gridId"
                  class="grid-warning-item"
                  :class="'level-' + item.heatLevel"
                  @click="goGridDetail(item)"
                >
                  <div class="grid-header">
                    <div class="grid-name">{{ item.gridName }}</div>
                    <van-tag :type="getHeatLevelTagType(item.heatLevel)" size="medium" round>
                      {{ item.heatLevelDesc }}
                    </van-tag>
                  </div>
                  <div class="grid-stats">
                    <div class="stat-item">
                      <div class="stat-value" :style="{ color: getHeatLevelColor(item.heatLevel) }">
                        {{ item.heatScore }}
                      </div>
                      <div class="stat-label">预警评分</div>
                    </div>
                    <div class="stat-divider"></div>
                    <div class="stat-item">
                      <div class="stat-value">{{ item.predictedEventCount }}</div>
                      <div class="stat-label">预计事件数</div>
                    </div>
                    <div class="stat-divider"></div>
                    <div class="stat-item">
                      <div class="stat-value">{{ item.weatherCondition }}</div>
                      <div class="stat-label">天气</div>
                    </div>
                  </div>
                  <div class="grid-suggestion">{{ item.suggestion }}</div>
                  <div class="grid-actions">
                    <van-button size="small" type="primary" plain icon="eye-o" block @click.stop="goGridDetail(item)">
                      查看详情
                    </van-button>
                    <van-button size="small" type="danger" icon="bell-o" block @click.stop="handlePush(item)">
                      推送预警
                    </van-button>
                  </div>
                </div>
              </div>
              <van-empty v-else-if="!gridLoading" description="暂无网格预警" :image-size="80" />
            </van-list>
          </van-pull-refresh>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  getWarningList,
  readWarning,
  getMyGridHighWarning,
  pushGridWarning
} from '@/api/warning'
import { getNotificationList } from '@/api'

const router = useRouter()

const activeTab = ref('warning')
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const warningList = ref([])
const pageNum = ref(1)
const pageSize = ref(20)

const gridRefreshing = ref(false)
const gridLoading = ref(false)
const gridFinished = ref(false)
const gridWarningList = ref([])

const onClickLeft = () => {
  router.back()
}

function formatTime(time) {
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

function formatContent(content) {
  if (!content) return ''
  if (content.length > 80) {
    return content.substring(0, 80) + '...'
  }
  return content
}

function getTypeClass(type) {
  const map = {
    EVENT_WARNING: 'type-warning',
    EVENT_URGE: 'type-urge',
    SYSTEM: 'type-system',
    TASK: 'type-task'
  }
  return map[type] || 'type-default'
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

function getHeatLevelColor(level) {
  switch (level) {
    case 1: return '#67C23A'
    case 2: return '#E6A23C'
    case 3: return '#F56C6C'
    case 4: return '#C0392B'
    default: return '#909399'
  }
}

function goDetail(item) {
  router.push({
    path: '/warning-detail',
    query: { id: item.id }
  })
}

function goGridDetail(item) {
  router.push({
    path: '/warning-grid',
    query: { gridId: item.gridId }
  })
}

async function handlePush(item) {
  try {
    await showConfirmDialog({
      title: '确认推送',
      message: `确定要向网格【${item.gridName}】全体网格员推送预警通知吗？`
    })
    await pushGridWarning(item.gridId)
    showToast('预警推送成功')
  } catch (e) {
    if (e !== 'cancel') {
      showToast('推送失败')
    }
  }
}

async function markAllRead() {
  showToast('已标记全部已读')
}

const onLoad = async () => {
  try {
    const res = await getWarningList({
      page: pageNum.value,
      size: pageSize.value,
      type: 'EVENT_WARNING'
    })
    const data = res.data?.records || res.data || []
    if (pageNum.value === 1) {
      warningList.value = data
    } else {
      warningList.value = [...warningList.value, ...data]
    }
    if (data.length < pageSize.value) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (e) {
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const onRefresh = async () => {
  finished.value = false
  pageNum.value = 1
  loading.value = false
  onLoad()
  refreshing.value = false
}

const onGridLoad = async () => {
  try {
    const res = await getMyGridHighWarning()
    gridWarningList.value = res.data || []
    gridFinished.value = true
  } catch (e) {
    showToast('加载失败')
  } finally {
    gridLoading.value = false
  }
}

const onGridRefresh = async () => {
  gridFinished.value = false
  gridLoading.value = false
  onGridLoad()
  gridRefreshing.value = false
}

onMounted(() => {})
</script>

<style scoped lang="scss">
.warning-page {
  min-height: 100vh;
  background: #f7f8fa;

  .read-all {
    font-size: 14px;
    color: #1989fa;
  }

  .tab-content {
    padding-top: 4px;
  }

  .warning-item {
    background: #fff;
    margin: 10px 12px;
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    &.unread {
      background: linear-gradient(135deg, #fff8f0 0%, #fff 100%);
      border-left: 3px solid #ee0a24;
    }

    .item-header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      .item-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        margin-right: 10px;
        flex-shrink: 0;

        &.type-warning {
          background: linear-gradient(135deg, #ff6b6b 0%, #ee0a24 100%);
        }

        &.type-urge {
          background: linear-gradient(135deg, #ff976a 0%, #ff6034 100%);
        }

        &.type-system {
          background: linear-gradient(135deg, #5d8cff 0%, #1989fa 100%);
        }

        &.type-task {
          background: linear-gradient(135deg, #72ed7f 0%, #07c160 100%);
        }

        &.type-default {
          background: linear-gradient(135deg, #a8a8a8 0%, #7d7e80 100%);
        }
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-title {
          font-size: 15px;
          font-weight: 600;
          color: #323233;
          display: flex;
          align-items: center;
          gap: 6px;

          .unread-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #ee0a24;
            flex-shrink: 0;
          }
        }

        .item-time {
          font-size: 12px;
          color: #969799;
          margin-top: 4px;
        }
      }

      .item-tag {
        flex-shrink: 0;
      }
    }

    .item-content {
      font-size: 13px;
      color: #646566;
      line-height: 1.6;
      white-space: pre-line;
      background: #f7f8fa;
      padding: 10px;
      border-radius: 8px;
    }
  }

  .grid-warning-item {
    background: #fff;
    margin: 10px 12px;
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    border-left: 4px solid transparent;

    &.level-1 {
      border-left-color: #67C23A;
      background: linear-gradient(135deg, #f0f9eb 0%, #fff 100%);
    }

    &.level-2 {
      border-left-color: #E6A23C;
      background: linear-gradient(135deg, #fdf6ec 0%, #fff 100%);
    }

    &.level-3 {
      border-left-color: #F56C6C;
      background: linear-gradient(135deg, #fef0f0 0%, #fff 100%);
    }

    &.level-4 {
      border-left-color: #C0392B;
      background: linear-gradient(135deg, #fde2e2 0%, #fff 100%);
    }

    .grid-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;

      .grid-name {
        font-size: 16px;
        font-weight: 600;
        color: #323233;
      }
    }

    .grid-stats {
      display: flex;
      align-items: center;
      justify-content: space-around;
      background: #fff;
      border-radius: 10px;
      padding: 14px 10px;
      margin-bottom: 12px;

      .stat-item {
        flex: 1;
        text-align: center;

        .stat-value {
          font-size: 20px;
          font-weight: 700;
          color: #323233;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 12px;
          color: #969799;
        }
      }

      .stat-divider {
        width: 1px;
        height: 36px;
        background: #ebedf0;
      }
    }

    .grid-suggestion {
      font-size: 13px;
      color: #646566;
      line-height: 1.6;
      background: #f7f8fa;
      padding: 10px;
      border-radius: 8px;
      margin-bottom: 12px;
    }

    .grid-actions {
      display: flex;
      gap: 10px;

      :deep(.van-button) {
        border-radius: 8px;
      }
    }
  }
}
</style>
