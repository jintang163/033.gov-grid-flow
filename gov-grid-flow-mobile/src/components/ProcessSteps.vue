<template>
  <div class="process-steps">
    <van-steps direction="vertical" :active="activeStep" active-color="#1989fa">
      <van-step v-for="(step, index) in stepList" :key="index">
        <div class="step-content">
          <div class="step-header">
            <span class="step-title">{{ step.title }}</span>
            <span v-if="step.time" class="step-time">{{ formatTime(step.time) }}</span>
          </div>
          <div v-if="step.handler" class="step-handler">
            <van-icon name="user-o" size="12" />
            <span>{{ step.handler }}</span>
          </div>
          <div v-if="step.comment" class="step-comment">{{ step.comment }}</div>
          <div v-if="step.attachments && step.attachments.length" class="step-attachments">
            <van-image
              v-for="(img, idx) in step.attachments"
              :key="idx"
              width="60"
              height="60"
              :src="img"
              fit="cover"
              round
              @click="previewImage(idx, step.attachments)"
            />
          </div>
        </div>
      </van-step>
    </van-steps>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { showImagePreview } from 'vant'

const props = defineProps({
  processList: {
    type: Array,
    default: () => []
  },
  status: {
    type: String,
    default: ''
  }
})

const actionMap = {
  REPORT: '上报事件',
  APPROVE: '审核通过',
  REJECT: '审核不通过',
  ASSIGN: '分派任务',
  DISPOSE: '开始处置',
  PROCESS: '处置完成',
  VERIFY: '核查通过',
  RETURN: '退回处置',
  COMPLETE: '事件完成',
  EVALUATE: '提交评价'
}

const defaultSteps = [
  { key: 'REPORT', title: '事件上报' },
  { key: 'APPROVE', title: '审核受理' },
  { key: 'ASSIGN', title: '任务分派' },
  { key: 'PROCESS', title: '部门处置' },
  { key: 'VERIFY', title: '结果核查' },
  { key: 'COMPLETE', title: '事件完成' }
]

const stepList = computed(() => {
  if (props.processList && props.processList.length > 0) {
    return props.processList.map((item) => ({
      title: actionMap[item.action] || item.nodeName || '处理节点',
      time: item.handleTime || item.createTime,
      handler: item.handlerName,
      comment: item.comment,
      attachments: parseAttachments(item.attachments)
    }))
  }
  return defaultSteps.map((item) => ({
    title: item.title,
    time: '',
    handler: '',
    comment: '',
    attachments: []
  }))
})

const activeStep = computed(() => {
  if (props.processList && props.processList.length > 0) {
    return props.processList.length - 1
  }
  const statusStepMap = {
    PENDING: 0,
    APPROVED: 1,
    PROCESSING: 2,
    DISPOSING: 3,
    VERIFYING: 4,
    COMPLETED: 5
  }
  return statusStepMap[props.status] ?? 0
})

const parseAttachments = (attachments) => {
  if (!attachments) return []
  if (typeof attachments === 'string') {
    return attachments.split(',').filter(Boolean)
  }
  if (Array.isArray(attachments)) {
    return attachments
  }
  return []
}

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

const previewImage = (index, images) => {
  showImagePreview({
    images,
    startPosition: index
  })
}
</script>

<style scoped>
.process-steps {
  padding: 12px 16px;
  background: #fff;
}

.step-content {
  padding-bottom: 16px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.step-title {
  font-size: 14px;
  font-weight: 500;
  color: #323233;
}

.step-time {
  font-size: 12px;
  color: #969799;
}

.step-handler {
  font-size: 12px;
  color: #646566;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.step-comment {
  font-size: 13px;
  color: #646566;
  line-height: 1.5;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 4px;
  margin-top: 6px;
}

.step-attachments {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
</style>
