<template>
  <div class="todo-container">
    <van-nav-bar title="待办中心" fixed placeholder />

    <van-tabs v-model:active="activeTab" sticky offset-top="46px" color="#1989fa" line-width="24px">
      <van-tab title="待办事项" name="todo" />
      <van-tab title="已办事项" name="done" />
      <van-tab title="我上报的" name="report" />
    </van-tabs>

    <div class="todo-content">
      <van-pull-refresh
        v-model="pullState.refreshing"
        @refresh="onRefresh"
        success-text="刷新成功"
      >
        <van-list
          v-model:loading="pullState.loading"
          :finished="pullState.finished"
          finished-text="没有更多了"
          loading-text="加载中..."
          @load="onLoad"
        >
          <template v-if="currentList.length > 0">
            <EventCard
              v-for="item in currentList"
              :key="item.id"
              :event="item"
              @click="goDetail(item.id)"
            />
          </template>
          <van-empty
            v-else-if="!pullState.loading && !pullState.refreshing"
            description="暂无数据"
            image="default"
          />
        </van-list>
      </van-pull-refresh>
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
import { ref, reactive, computed, watch, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import { Tag, Icon, Cell, showToast } from 'vant'
import { getMyTodo, getMyDone, getMyReport } from '@/api'

const router = useRouter()
const active = ref(1)
const activeTab = ref('todo')

const todoList = ref([])
const doneList = ref([])
const reportList = ref([])

const pullState = reactive({
  loading: false,
  refreshing: false,
  finished: false,
  pageNum: 1,
  pageSize: 10
})

const currentList = computed(() => {
  switch (activeTab.value) {
    case 'todo':
      return todoList.value
    case 'done':
      return doneList.value
    case 'report':
      return reportList.value
    default:
      return []
  }
})

const statusColorMap = {
  PENDING: '#ff976a',
  APPROVED: '#1989fa',
  DISPATCHED: '#7232dd',
  HANDLED: '#00c4b6',
  COMPLETED: '#07c160',
  REJECTED: '#ee0a24',
  PROCESSING: '#1989fa'
}

const statusTextMap = {
  PENDING: '待受理',
  APPROVED: '已受理',
  DISPATCHED: '已分派',
  HANDLED: '已处置',
  COMPLETED: '已办结',
  REJECTED: '已驳回',
  PROCESSING: '处理中'
}

const eventTypeMap = {
  environment: '环境卫生',
  public_facility: '市政设施',
  facility: '市政设施',
  dispute: '矛盾纠纷',
  safety_hazard: '安全隐患',
  security: '治安问题',
  service: '民生服务',
  traffic: '交通出行',
  other: '其他问题'
}

const priorityColorMap = {
  URGENT: '#ee0a24',
  HIGH: '#ff976a',
  NORMAL: '#1989fa',
  LOW: '#969799',
  very_urgent: '#ee0a24',
  urgent: '#ff976a',
  normal: '#1989fa',
  low: '#969799'
}

const priorityTextMap = {
  URGENT: '特急',
  HIGH: '紧急',
  NORMAL: '一般',
  LOW: '低',
  very_urgent: '特急',
  urgent: '紧急',
  normal: '一般',
  low: '低'
}

const statusTagStyle = (status) => ({
  background: statusColorMap[status] + '15',
  color: statusColorMap[status] || '#969799',
  border: `1px solid ${statusColorMap[status] || '#dcdee0'}`,
  padding: '2px 8px',
  borderRadius: '10px',
  fontSize: '12px',
  lineHeight: '1.4'
})

const priorityTagStyle = (priority) => ({
  background: priorityColorMap[priority] + '15',
  color: priorityColorMap[priority] || '#1989fa',
  border: `1px solid ${priorityColorMap[priority] || '#1989fa'}`,
  padding: '2px 8px',
  borderRadius: '10px',
  fontSize: '12px',
  lineHeight: '1.4',
  marginLeft: '8px'
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

const EventCard = defineComponent({
  name: 'EventCard',
  props: {
    event: {
      type: Object,
      required: true
    }
  },
  setup(props, { emit }) {
    const eventTypeText = computed(() => {
      return eventTypeMap[props.event.eventType] || props.event.eventTypeText || props.event.eventType || '未分类'
    })

    const priorityText = computed(() => {
      return priorityTextMap[props.event.priority] || props.event.priorityText || '一般'
    })

    const statusText = computed(() => {
      return statusTextMap[props.event.status] || props.event.status || '未知'
    })

    const eventNo = computed(() => {
      return props.event.eventNo || props.event.code || props.event.id
    })

    return () => {
      const event = props.event
      return h(
        'div',
        {
          class: 'event-card-local',
          onClick: () => emit('click')
        },
        [
          h(
            'div',
            { class: 'card-header' },
            [
              h(
                'div',
                { class: 'card-title' },
                event.title || '无标题'
              ),
              h(
                'div',
                {
                  class: 'card-no',
                  title: String(eventNo.value)
                },
                '#' + String(eventNo.value).slice(-6)
              )
            ]
          ),
          h(
            'div',
            { class: 'card-tags' },
            [
              h(
                'span',
                {
                  class: 'tag type-tag',
                  style: statusTagStyle(event.status)
                },
                statusText.value
              ),
              h(
                'span',
                {
                  class: 'tag type-tag',
                  style: {
                    background: '#e5f3ff',
                    color: '#1989fa',
                    border: '1px solid #1989fa',
                    padding: '2px 8px',
                    borderRadius: '10px',
                    fontSize: '12px',
                    lineHeight: '1.4',
                    marginLeft: '8px'
                  }
                },
                eventTypeText.value
              ),
              h(
                'span',
                {
                  class: 'tag priority-tag',
                  style: priorityTagStyle(event.priority)
                },
                priorityText.value
              )
            ]
          ),
          h(
            'div',
            { class: 'card-location' },
            [
              h(Icon, { name: 'location-o', size: '14', color: '#969799' }),
              h(
                'span',
                { class: 'location-text' },
                event.address || '暂无位置信息'
              )
            ]
          ),
          h(
            'div',
            { class: 'card-desc' },
            event.description || '暂无描述'
          ),
          h(
            'div',
            { class: 'card-footer' },
            [
              h(
                'div',
                { class: 'footer-left' },
                [
                  h(
                    'span',
                    { class: 'footer-reporter' },
                    event.reporterName || event.createBy || '匿名用户'
                  ),
                  h(
                    'span',
                    { class: 'footer-divider' },
                    '·'
                  ),
                  h(
                    'span',
                    { class: 'footer-time' },
                    formatTime(event.createTime || event.reportTime || event.time)
                  )
                ]
              ),
              h(
                'div',
                { class: 'footer-right' },
                [h(Icon, { name: 'arrow', size: '14', color: '#c8c9cc' })]
              )
            ]
          )
        ]
      )
    }
  }
})

const fetchApi = async () => {
  const params = {
    pageNum: pullState.pageNum,
    pageSize: pullState.pageSize
  }

  switch (activeTab.value) {
    case 'todo':
      return await getMyTodo(params)
    case 'done':
      return await getMyDone(params)
    case 'report':
      return await getMyReport(params)
    default:
      return { data: { records: [], total: 0 } }
  }
}

const getTargetList = () => {
  switch (activeTab.value) {
    case 'todo':
      return todoList
    case 'done':
      return doneList
    case 'report':
      return reportList
    default:
      return ref([])
  }
}

const onLoad = async () => {
  try {
    const res = await fetchApi()
    const records = res.data?.records || res.data?.list || res.data || []
    const targetList = getTargetList()

    if (pullState.refreshing) {
      targetList.value = records
    } else {
      targetList.value = [...targetList.value, ...records]
    }

    pullState.loading = false
    pullState.refreshing = false

    const total = res.data?.total || 0
    if (total > 0) {
      if (targetList.value.length >= total) {
        pullState.finished = true
      } else {
        pullState.pageNum++
      }
    } else {
      if (records.length < pullState.pageSize) {
        pullState.finished = true
      } else {
        pullState.pageNum++
      }
    }
  } catch (e) {
    console.error(e)
    pullState.loading = false
    pullState.refreshing = false
    pullState.finished = true
    showToast('加载失败，请稍后重试')
  }
}

const onRefresh = () => {
  pullState.finished = false
  pullState.pageNum = 1
  onLoad()
}

watch(activeTab, () => {
  const targetList = getTargetList()
  if (targetList.value.length === 0) {
    pullState.refreshing = true
    onRefresh()
  }
})

const goDetail = (id) => {
  if (id) {
    router.push(`/detail/${id}`)
  }
}
</script>

<style lang="scss" scoped>
.todo-container {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}

.todo-content {
  padding: 12px;
}

.event-card-local {
  background: #ffffff;
  border-radius: 12px;
  padding: 14px 14px 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;

  &:active {
    transform: scale(0.99);
    background: #f7f8fa;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  gap: 10px;
}

.card-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #323233;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.card-no {
  flex-shrink: 0;
  font-size: 12px;
  color: #969799;
  font-family: 'Courier New', monospace;
  background: #f2f3f5;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 10px;

  .tag {
    display: inline-block;
  }
}

.type-tag {
  flex-shrink: 0;
}

.priority-tag {
  flex-shrink: 0;
}

.card-location {
  display: flex;
  align-items: center;
  margin-bottom: 8px;

  .location-text {
    margin-left: 4px;
    font-size: 13px;
    color: #646566;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }
}

.card-desc {
  font-size: 13px;
  color: #646566;
  line-height: 1.5;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
}

.footer-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;

  .footer-reporter {
    font-size: 12px;
    color: #646566;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .footer-divider {
    margin: 0 8px;
    color: #dcdee0;
    font-size: 12px;
  }

  .footer-time {
    font-size: 12px;
    color: #969799;
  }
}

.footer-right {
  flex-shrink: 0;
  margin-left: 8px;
}
</style>
