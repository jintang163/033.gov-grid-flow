<template>
  <van-card class="event-card" :class="cardClass" :title="event.title" :desc="event.description" @click="handleClick">
    <template #tag>
      <van-tag :type="statusType" size="medium">{{ statusText }}</van-tag>
      <van-tag v-if="event.warningInfo && event.warningInfo.urgeLevel >= 1" :type="urgeTagType" size="medium" class="urge-tag">
        {{ urgeText }}
      </van-tag>
    </template>
    <template #thumb>
      <van-image
        v-if="firstImage"
        round
        width="88"
        height="88"
        :src="firstImage"
        fit="cover"
      />
      <div v-else class="thumb-placeholder">
        <van-icon name="photo-o" size="32" color="#dcdee0" />
      </div>
    </template>
    <template #tags>
      <van-tag plain type="primary">{{ eventTypeText }}</van-tag>
      <van-tag v-if="event.priority" plain :type="priorityType" style="margin-left: 8px">
        {{ priorityText }}
      </van-tag>
    </template>
    <template #footer>
      <div class="card-footer">
        <span class="footer-info">
          <van-icon name="location-o" size="12" />
          <span class="footer-text">{{ event.address || '暂无位置信息' }}</span>
        </span>
        <span class="footer-info">
          <van-icon name="clock-o" size="12" />
          <span class="footer-text">{{ formatTime(event.createTime) }}</span>
        </span>
        <span v-if="event.warningInfo" class="footer-info urge-footer" :class="urgeFooterClass">
          <van-icon name="warning-o" size="12" />
          <span class="footer-text">{{ urgeFooterText }}</span>
        </span>
      </div>
    </template>
  </van-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  event: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const statusMap = {
  PENDING: { text: '待受理', type: 'warning' },
  APPROVED: { text: '已受理', type: 'primary' },
  DISPATCHED: { text: '已分派', type: 'primary' },
  HANDLED: { text: '已处置', type: 'success' },
  COMPLETED: { text: '已办结', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' }
}

const eventTypeMap = {
  environment: '环境卫生',
  facility: '市政设施',
  security: '治安问题',
  service: '民生服务',
  traffic: '交通出行',
  other: '其他问题'
}

const priorityMap = {
  normal: { text: '一般', type: 'primary' },
  urgent: { text: '紧急', type: 'warning' },
  very_urgent: { text: '特急', type: 'danger' }
}

const statusText = computed(() => {
  return statusMap[props.event.status]?.text || props.event.status || '未知'
})

const statusType = computed(() => {
  return statusMap[props.event.status]?.type || 'default'
})

const eventTypeText = computed(() => {
  return eventTypeMap[props.event.eventType] || props.event.eventType || '未分类'
})

const priorityText = computed(() => {
  return priorityMap[props.event.priority]?.text || '一般'
})

const priorityType = computed(() => {
  return priorityMap[props.event.priority]?.type || 'primary'
})

const firstImage = computed(() => {
  if (!props.event.images) return ''
  if (typeof props.event.images === 'string') {
    const imgs = props.event.images.split(',').filter(Boolean)
    return imgs[0] || ''
  }
  if (Array.isArray(props.event.images)) {
    return props.event.images[0] || ''
  }
  return ''
})

const urgeLevel = computed(() => {
  return props.event.warningInfo?.urgeLevel || 0
})

const cardClass = computed(() => {
  const level = urgeLevel.value
  if (level >= 3) return 'urge-3'
  if (level === 2) return 'urge-2'
  if (level === 1) return 'urge-1'
  return 'urge-0'
})

const urgeText = computed(() => {
  const level = urgeLevel.value
  if (level >= 3) return '督办'
  if (level === 2) return '超时'
  if (level === 1) return '预警'
  return ''
})

const urgeTagType = computed(() => {
  const level = urgeLevel.value
  if (level >= 2) return 'danger'
  if (level === 1) return 'warning'
  return 'default'
})

const urgeFooterText = computed(() => {
  const warningInfo = props.event.warningInfo
  if (!warningInfo) return ''
  const hours = (warningInfo.remainingHours || 0).toFixed(1)
  if (warningInfo.isOverdue) {
    return `已超时 ${hours}小时`
  }
  if (warningInfo.isWarning) {
    return `剩余 ${hours}小时`
  }
  return `剩余 ${hours}小时`
})

const urgeFooterClass = computed(() => {
  const warningInfo = props.event.warningInfo
  if (!warningInfo) return ''
  if (warningInfo.isOverdue) {
    return 'urge-footer-danger'
  }
  if (warningInfo.isWarning) {
    return 'urge-footer-warning'
  }
  return 'urge-footer-normal'
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

const handleClick = () => {
  if (props.event.id) {
    router.push(`/detail/${props.event.id}`)
  }
}
</script>

<style scoped>
.event-card {
  margin-bottom: 12px;
}

.thumb-placeholder {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-footer {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.footer-info {
  display: flex;
  align-items: center;
  color: #969799;
  font-size: 12px;
}

.footer-text {
  margin-left: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.event-card.urge-1 {
  border-left: 3px solid #ff976a;
}

.event-card.urge-2 {
  border-left: 3px solid #ee0a24;
}

.event-card.urge-3 {
  border-left: 3px solid #c30000;
  background-color: #fff7f7;
}

.urge-tag {
  margin-left: 4px;
}

.urge-footer {
  font-weight: 500;
}

.urge-footer-danger {
  color: #ee0a24;
}

.urge-footer-warning {
  color: #ff976a;
}

.urge-footer-normal {
  color: #07c160;
}
</style>
