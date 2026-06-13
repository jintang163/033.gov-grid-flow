<template>
  <div class="analysis-page">
    <el-card shadow="never">
      <div class="report-header">
        <div>
          <el-radio-group v-model="reportDays" size="default">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
            <el-radio-button :value="90">近90天</el-radio-button>
          </el-radio-group>
          <el-button type="primary" style="margin-left: 12px" :loading="loading" @click="handleGenerate">
            <el-icon><Refresh /></el-icon>生成报告
          </el-button>
          <el-button type="success" :loading="pushing" @click="handlePush">
            <el-icon><Promotion /></el-icon>推送到街道办
          </el-button>
          <el-button type="warning" @click="handleBatchScan">
            <el-icon><WarningFilled /></el-icon>批量扫描高复发
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px" v-if="report">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">事件总数</div>
          <div class="stat-value">{{ report.totalEvents || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card danger">
          <div class="stat-label">高复发事件</div>
          <div class="stat-value">{{ report.highRecurrenceCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card warning">
          <div class="stat-label">高复发地点数</div>
          <div class="stat-value">{{ report.totalRecurrenceGroups || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">报告编号</div>
          <div class="stat-value small">{{ report.reportNo || '-' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px" v-if="report">
      <template #header>
        <div class="card-header">
          <span>分析概要</span>
          <span class="card-header-time">
            生成时间：{{ report.generatedAt }}
          </span>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="统计周期">{{ report.period }}</el-descriptions-item>
        <el-descriptions-item label="核心总结">{{ report.summary }}</el-descriptions-item>
        <el-descriptions-item label="处置建议">{{ report.suggestions }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>高复发事件分组 TOP {{ recurrenceGroups.length }}</span>
        </div>
      </template>
      <el-table
        :data="recurrenceGroups"
        border
        stripe
        v-loading="groupLoading"
        @row-click="handleRowClick"
        style="cursor: pointer"
      >
        <el-table-column type="index" label="排名" width="70" />
        <el-table-column prop="eventType" label="事件类型" width="140" />
        <el-table-column prop="address" label="事发地点" min-width="220" show-overflow-tooltip />
        <el-table-column prop="totalCount" label="累计次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="dark">{{ row.totalCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态统计" width="260">
          <template #default="{ row }">
            <div class="status-badges">
              <el-tag size="small" type="warning">待处理 {{ row.pendingCount || 0 }}</el-tag>
              <el-tag size="small" type="success">已办结 {{ row.completedCount || 0 }}</el-tag>
              <el-tag size="small" type="danger">超时 {{ row.overdueCount || 0 }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="firstOccurAt" label="首次发生" width="170" />
        <el-table-column prop="lastOccurAt" label="最近发生" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="handleViewGroup(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="groupDialogVisible" title="高复发分组详情" width="900px" destroy-on-close>
      <div v-if="currentGroup">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="事件类型">{{ currentGroup.eventType }}</el-descriptions-item>
          <el-descriptions-item label="发生地点">{{ currentGroup.address }}</el-descriptions-item>
          <el-descriptions-item label="累计次数">{{ currentGroup.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="最近发生">{{ currentGroup.lastOccurAt }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">同组事件时间线</el-divider>
        <el-timeline v-if="currentGroup.events?.length">
          <el-timeline-item
            v-for="ev in currentGroup.events"
            :key="ev.id"
            :timestamp="ev.createdAt"
            :type="getStatusTimelineType(ev.status)"
            placement="top"
          >
            <div class="timeline-item">
              <div class="timeline-title">
                <b>{{ ev.eventNo }}</b> {{ ev.title }}
              </div>
              <div class="timeline-meta">
                <el-tag size="small">{{ getStatusLabel(ev.status) }}</el-tag>
                <span style="margin-left: 8px; color: #909399">优先级：{{ ev.priority }}</span>
                <span v-if="ev.urgeLevel" style="margin-left: 8px">
                  催办：{{ getUrgeLevelLabel(ev.urgeLevel) }}
                </span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无事件" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Promotion, WarningFilled } from '@element-plus/icons-vue'
import {
  listHighRecurrenceGroups,
  getRecurrenceGroup,
  generateAnalysisReport,
  pushReportToStreetOffice,
  batchScanHighRecurrence
} from '@/api/analysis'

const loading = ref(false)
const pushing = ref(false)
const groupLoading = ref(false)
const reportDays = ref(7)
const report = ref(null)
const recurrenceGroups = ref([])

const groupDialogVisible = ref(false)
const currentGroup = ref(null)

async function handleGenerate() {
  loading.value = true
  try {
    const res = await generateAnalysisReport(reportDays.value)
    report.value = res?.data || res
    ElMessage.success('报告生成成功')
  } catch (e) {
    ElMessage.error(e.message || '报告生成失败')
  } finally {
    loading.value = false
  }
}

async function handlePush() {
  try {
    await ElMessageBox.confirm(
      `确认将近${reportDays.value}天的事件关联分析报告推送给街道办管理员吗？`,
      '推送确认',
      { type: 'warning' }
    )
    pushing.value = true
    const res = await pushReportToStreetOffice(reportDays.value)
    const ok = res?.data ?? res
    ElMessage.success(ok ? '推送成功' : '推送失败')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '推送失败')
    }
  } finally {
    pushing.value = false
  }
}

async function handleBatchScan() {
  try {
    await ElMessageBox.confirm(
      '将重新扫描全量近90天事件并标记高复发事件，可能需要一些时间，是否继续？',
      '批量扫描确认',
      { type: 'warning' }
    )
    loading.value = true
    const res = await batchScanHighRecurrence()
    const count = res?.data ?? res
    ElMessage.success(`扫描完成，共标记${count}组高复发事件`)
    await fetchGroups()
    await handleGenerate()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '扫描失败')
    }
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  groupLoading.value = true
  try {
    const res = await listHighRecurrenceGroups(reportDays.value)
    recurrenceGroups.value = res?.data || res || []
  } catch (e) {
    recurrenceGroups.value = []
  } finally {
    groupLoading.value = false
  }
}

async function handleViewGroup(row) {
  try {
    const res = await getRecurrenceGroup(row.groupKey)
    currentGroup.value = res?.data || res || row
    groupDialogVisible.value = true
  } catch (e) {
    currentGroup.value = row
    groupDialogVisible.value = true
  }
}

function handleRowClick(row) {
  handleViewGroup(row)
}

function getStatusTimelineType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'primary',
    DISPATCHED: 'primary',
    HANDLED: 'warning',
    COMPLETED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'primary'
}

function getStatusLabel(status) {
  const map = {
    PENDING: '待受理',
    APPROVED: '已受理',
    DISPATCHED: '已分派',
    HANDLED: '已处置',
    COMPLETED: '已办结',
    REJECTED: '已驳回'
  }
  return map[status] || status || '-'
}

function getUrgeLevelLabel(level) {
  const map = { 0: '正常', 1: '预警', 2: '超时', 3: '升级督办' }
  return map[level] || '正常'
}

onMounted(async () => {
  await handleGenerate()
  await fetchGroups()
})
</script>

<style lang="scss" scoped>
.analysis-page {
  padding: 16px;

  .report-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stat-card {
    text-align: center;

    &.danger :deep(.stat-value) { color: #f56c6c; }
    &.warning :deep(.stat-value) { color: #e6a23c; }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 8px;
    }
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: #303133;

      &.small {
        font-size: 16px;
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    &-time {
      font-size: 12px;
      color: #909399;
    }
  }

  .status-badges {
    display: flex;
    gap: 6px;
  }

  .timeline-item {
    .timeline-title {
      font-size: 14px;
      margin-bottom: 4px;
      color: #303133;
    }
    .timeline-meta {
      font-size: 12px;
    }
  }
}
</style>
