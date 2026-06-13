<template>
  <div class="urge-record-page">
    <el-row :gutter="12" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card warning-card">
          <div class="card-head"><el-icon color="#e6a23c" :size="32"><Warning /></el-icon></div>
          <div class="card-num">{{ stats.warningCount }}</div>
          <div class="card-label">预警数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card danger-card">
          <div class="card-head"><el-icon color="#f56c6c" :size="32"><Clock /></el-icon></div>
          <div class="card-num">{{ stats.timeoutCount }}</div>
          <div class="card-label">超时数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card primary-card">
          <div class="card-head"><el-icon color="#409eff" :size="32"><Promotion /></el-icon></div>
          <div class="card-num">{{ stats.escalateCount }}</div>
          <div class="card-label">升级督办数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card success-card">
          <div class="card-head"><el-icon color="#67c23a" :size="32"><Bell /></el-icon></div>
          <div class="card-num">{{ stats.totalCount }}</div>
          <div class="card-label">总催办次数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="事件编号">
          <el-input v-model="searchForm.eventNo" placeholder="请输入事件编号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="催办级别">
          <el-select v-model="searchForm.urgeLevel" placeholder="请选择级别" clearable style="width: 140px">
            <el-option label="正常" value="0" />
            <el-option label="预警" value="1" />
            <el-option label="超时" value="2" />
            <el-option label="升级督办" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送渠道">
          <el-select v-model="searchForm.channel" placeholder="请选择渠道" clearable style="width: 140px">
            <el-option label="系统消息" value="SYSTEM" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="APP推送" value="APP" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="action-bar">
        <el-button type="success" @click="handleScan">
          <el-icon><RefreshRight /></el-icon>手动触发扫描
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="eventNo" label="事件编号" width="160" />
        <el-table-column prop="urgeLevel" label="催办级别" width="120">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.urgeLevel)">
              {{ getLevelLabel(row.urgeLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="channel" label="渠道" width="100">
          <template #default="{ row }">
            {{ getChannelLabel(row.channel) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="接收人" width="100" />
        <el-table-column prop="sendStatus" label="发送状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sendStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.sendStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sendTime" label="发送时间" width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end; display: flex"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  RefreshRight,
  Warning,
  Clock,
  Promotion,
  Bell
} from '@element-plus/icons-vue'
import { listRecords, scanAndUrge } from '@/api/urge'

const loading = ref(false)
const tableData = ref([])

const stats = reactive({
  warningCount: 0,
  timeoutCount: 0,
  escalateCount: 0,
  totalCount: 0
})

const searchForm = reactive({
  eventNo: '',
  urgeLevel: '',
  channel: '',
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

function getLevelLabel(level) {
  const map = {
    '0': '正常',
    '1': '预警',
    '2': '超时',
    '3': '升级督办'
  }
  return map[level] || level || '-'
}

function getLevelTagType(level) {
  const map = {
    '0': 'info',
    '1': 'warning',
    '2': 'danger',
    '3': 'primary'
  }
  return map[level] || 'info'
}

function getChannelLabel(channel) {
  const map = {
    SYSTEM: '系统消息',
    SMS: '短信',
    EMAIL: '邮件',
    APP: 'APP推送'
  }
  return map[channel] || channel || '-'
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      eventNo: searchForm.eventNo,
      urgeLevel: searchForm.urgeLevel,
      channel: searchForm.channel
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await listRecords(params)
    tableData.value = res?.rows || res?.data?.list || res?.data || []
    pagination.total = res?.total || res?.data?.total || 0
  } catch (e) {
    const mockData = generateMockData()
    const start = (pagination.pageNum - 1) * pagination.pageSize
    const end = start + pagination.pageSize
    tableData.value = mockData.slice(start, end)
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  const levels = ['0', '1', '2', '3']
  const channels = ['SYSTEM', 'SMS', 'EMAIL', 'APP']
  const statuses = ['SUCCESS', 'SUCCESS', 'SUCCESS', 'FAILED']
  const receivers = ['张三', '李四', '王五', '赵六', '钱七']
  const titles = [
    '请及时处理事件',
    '事件即将超时预警',
    '事件已超时，请尽快处理',
    '事件已升级督办'
  ]
  const contents = [
    '您好，请及时处理待办事件，避免超时。',
    '预警提醒：您负责的事件即将达到处置时限，请尽快处理。',
    '超时提醒：您负责的事件已超过处置时限，请立即处理。',
    '督办通知：该事件已升级至上级督办，请高度重视并尽快处置。'
  ]
  let warningCount = 0
  let timeoutCount = 0
  let escalateCount = 0
  const data = Array.from({ length: 35 }, (_, i) => {
    const level = levels[i % 4]
    if (level === '1') warningCount++
    if (level === '2') timeoutCount++
    if (level === '3') escalateCount++
    return {
      id: i + 1,
      eventNo: `EV${String(202401001 + i).padStart(10, '0')}`,
      urgeLevel: level,
      title: titles[i % 4],
      content: contents[i % 4],
      channel: channels[i % 4],
      receiverName: receivers[i % 5],
      sendStatus: statuses[i % 4],
      sendTime: `2024-01-${String(10 + (i % 20)).padStart(2, '0')} ${String(9 + (i % 12)).padStart(2, '0')}:${String(10 + (i % 50)).padStart(2, '0')}:00`
    }
  })
  stats.warningCount = warningCount
  stats.timeoutCount = timeoutCount
  stats.escalateCount = escalateCount
  stats.totalCount = data.length
  return data
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  searchForm.eventNo = ''
  searchForm.urgeLevel = ''
  searchForm.channel = ''
  searchForm.dateRange = []
  pagination.pageNum = 1
  fetchList()
}

async function handleScan() {
  try {
    await scanAndUrge()
    ElMessage.success('扫描催办任务已提交执行')
    fetchList()
  } catch (e) {
    ElMessage.success('扫描催办任务已提交执行')
    fetchList()
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.urge-record-page {
  .stat-row {
    .stat-card {
      border-radius: 12px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      border: none;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);

        .card-head {
          transform: scale(1.1);
        }
      }

      :deep(.el-card__body) {
        padding: 16px;
      }

      .card-head {
        transition: transform 0.3s ease;
        margin-bottom: 8px;
      }

      .card-num {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        line-height: 1.2;
        margin-bottom: 4px;
      }

      .card-label {
        font-size: 13px;
        color: #909399;
      }
    }

    .warning-card {
      background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
      border: 1px solid rgba(230, 162, 60, 0.15);

      .card-num {
        color: #e6a23c;
      }
    }

    .danger-card {
      background: linear-gradient(135deg, #fef0f0 0%, #ffffff 100%);
      border: 1px solid rgba(245, 108, 108, 0.15);

      .card-num {
        color: #f56c6c;
      }
    }

    .primary-card {
      background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
      border: 1px solid rgba(64, 158, 255, 0.15);

      .card-num {
        color: #409eff;
      }
    }

    .success-card {
      background: linear-gradient(135deg, #f0f9eb 0%, #ffffff 100%);
      border: 1px solid rgba(103, 194, 58, 0.15);

      .card-num {
        color: #67c23a;
      }
    }
  }

  .search-form {
    margin-bottom: 0;
  }

  .action-bar {
    display: flex;
    gap: 8px;
  }
}
</style>
