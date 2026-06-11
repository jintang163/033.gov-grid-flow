<template>
  <div class="process-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(item, index) in processList"
        :key="index"
        :timestamp="formatTime(item.handleTime || item.createTime)"
        placement="top"
        :type="getTimelineType(item.action)"
        :hollow="index === processList.length - 1"
      >
        <el-card shadow="never" class="timeline-card">
          <div class="timeline-header">
            <span class="node-name">{{ item.nodeName || getActionName(item.action) }}</span>
            <el-tag :type="getActionTagType(item.action)" size="small">
              {{ getActionName(item.action) }}
            </el-tag>
          </div>
          <div class="timeline-content">
            <div class="timeline-info">
              <el-icon><User /></el-icon>
              <span>{{ item.handlerName || '系统' }}</span>
            </div>
            <div v-if="item.comment" class="timeline-comment">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ item.comment }}</span>
            </div>
            <div v-if="item.durationSeconds" class="timeline-duration">
              <el-icon><Timer /></el-icon>
              <span>处理时长：{{ formatDuration(item.durationSeconds) }}</span>
            </div>
            <div v-if="item.attachments" class="timeline-attachments">
              <el-icon><Paperclip /></el-icon>
              <span>附件：{{ item.attachments }}</span>
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!processList || processList.length === 0" description="暂无处理记录" />
  </div>
</template>

<script setup>
import { formatDate } from '@/utils/index'

defineProps({
  processList: {
    type: Array,
    default: () => []
  }
})

function formatTime(time) {
  if (!time) return ''
  return formatDate(time, 'YYYY-MM-DD HH:mm:ss')
}

function getActionName(action) {
  const map = {
    REPORT: '上报',
    AUDIT: '审核',
    ASSIGN: '分派',
    HANDLE: '处置',
    VERIFY: '核查',
    RETURN: '退回',
    COMPLETE: '完成',
    CREATE: '创建'
  }
  return map[action] || action || '处理'
}

function getTimelineType(action) {
  const map = {
    REPORT: 'primary',
    AUDIT: 'warning',
    ASSIGN: 'info',
    HANDLE: 'success',
    VERIFY: 'success',
    RETURN: 'danger',
    COMPLETE: 'success',
    CREATE: 'primary'
  }
  return map[action] || 'primary'
}

function getActionTagType(action) {
  const map = {
    REPORT: '',
    AUDIT: 'warning',
    ASSIGN: 'info',
    HANDLE: 'success',
    VERIFY: 'success',
    RETURN: 'danger',
    COMPLETE: 'success',
    CREATE: ''
  }
  return map[action] || ''
}

function formatDuration(seconds) {
  if (!seconds || seconds < 0) return '0秒'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  return `${hours}时${minutes}分${secs}秒`
}
</script>

<style lang="scss" scoped>
.process-timeline {
  padding: 10px 0;

  .timeline-card {
    border: 1px solid #ebeef5;
    margin-bottom: 8px;

    :deep(.el-card__body) {
      padding: 12px 16px;
    }

    .timeline-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .node-name {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }
    }

    .timeline-content {
      .timeline-info,
      .timeline-comment,
      .timeline-duration,
      .timeline-attachments {
        display: flex;
        align-items: flex-start;
        gap: 6px;
        font-size: 13px;
        color: #606266;
        margin-bottom: 4px;
        line-height: 1.6;

        .el-icon {
          margin-top: 2px;
          color: #909399;
        }
      }

      .timeline-comment {
        color: #303133;
      }
    }
  }
}
</style>
