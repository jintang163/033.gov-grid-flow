<template>
  <div class="cross-street-transfer-page">
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon pending">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.pendingApproval || 0 }}</div>
              <div class="stat-label">待我审批</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon transferred">
              <el-icon><Promotion /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.transferred || 0 }}</div>
              <div class="stat-label">待接收</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon processing">
              <el-icon><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.processing || 0 }}</div>
              <div class="stat-label">处理中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon completed">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completed || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="事件标题/编号">
          <el-input v-model="searchForm.keyword" placeholder="请输入搜索关键词" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="流转状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 160px">
            <el-option label="待审批" value="PENDING_APPROVAL" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已转派" value="TRANSFERRED" />
            <el-option label="已接收" value="ACCEPTED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="转派类型">
          <el-select v-model="searchForm.targetType" placeholder="请选择类型" clearable style="width: 140px">
            <el-option label="相邻街道" value="STREET" />
            <el-option label="委办局" value="BUREAU" />
            <el-option label="区级部门" value="COUNTY" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
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

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="eventNo" label="事件编号" width="160" />
        <el-table-column label="事件标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="event-title-cell">
              <span>{{ row.eventTitle }}</span>
              <el-tag v-if="row.urgencyLevel === 'HIGH' || row.urgencyLevel === 'URGENT'" type="danger" size="small" effect="dark">
                {{ getUrgencyLevelLabel(row.urgencyLevel) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDeptName" label="转出机构" width="140" show-overflow-tooltip />
        <el-table-column prop="targetDeptName" label="转入机构" width="140" show-overflow-tooltip />
        <el-table-column prop="targetTypeName" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.targetTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="applicantTime" label="申请时间" width="170" />
        <el-table-column prop="transferReason" label="转派原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button
              v-if="row.canApprove && row.status === 'PENDING_APPROVAL'"
              link
              type="success"
              size="small"
              @click="handleApprove(row)"
            >审批</el-button>
            <el-button
              v-if="row.canReceive && row.status === 'TRANSFERRED'"
              link
              type="warning"
              size="small"
              @click="handleReceive(row)"
            >接收</el-button>
            <el-button
              v-if="row.canProcess && row.status === 'PROCESSING'"
              link
              type="primary"
              size="small"
              @click="handleProcess(row)"
            >处理</el-button>
            <el-button
              v-if="row.canComplete && row.status === 'PROCESSING'"
              link
              type="success"
              size="small"
              @click="handleComplete(row)"
            >办结</el-button>
            <el-button link type="info" size="small" @click="handleViewTrace(row)">追溯</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="detailDialogVisible" title="流转详情" width="900px" destroy-on-close>
      <div v-if="currentTransfer">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="事件编号">{{ currentTransfer.eventNo }}</el-descriptions-item>
          <el-descriptions-item label="事件标题" :span="2">{{ currentTransfer.eventTitle }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">{{ currentTransfer.eventTypeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="getUrgencyLevelTagType(currentTransfer.urgencyLevel)">
              {{ getUrgencyLevelLabel(currentTransfer.urgencyLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="转出机构">{{ currentTransfer.sourceDeptName }}</el-descriptions-item>
          <el-descriptions-item label="转出网格">{{ currentTransfer.sourceGridName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转入机构">{{ currentTransfer.targetDeptName }}</el-descriptions-item>
          <el-descriptions-item label="机构类型">{{ currentTransfer.targetTypeName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusTagType(currentTransfer.status)" size="large">
              {{ getStatusLabel(currentTransfer.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发生地址" :span="2">{{ currentTransfer.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转派原因" :span="2">{{ currentTransfer.transferReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="跨界描述" :span="2">{{ currentTransfer.crossBoundaryDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="影响范围" :span="2">{{ currentTransfer.impactRange || '-' }}</el-descriptions-item>
          <el-descriptions-item label="协作说明" :span="2">{{ currentTransfer.coordinationNote || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentTransfer.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentTransfer.applicantTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.approverName" label="审批人">{{ currentTransfer.approverName }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.approveTime" label="审批时间">{{ currentTransfer.approveTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.receiverName" label="接收人">{{ currentTransfer.receiverName }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.receiveTime" label="接收时间">{{ currentTransfer.receiveTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.handlerName" label="处理人">{{ currentTransfer.handlerName }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.processEndTime" label="办结时间">{{ currentTransfer.processEndTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.processResult" label="处理结果" :span="2">
            <el-alert :title="currentTransfer.processResult" type="success" show-icon :closable="false" />
          </el-descriptions-item>
          <el-descriptions-item v-if="currentTransfer.processDescription" label="处理过程" :span="2">
            {{ currentTransfer.processDescription }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">流转追溯链</el-divider>
        <el-timeline v-if="currentTransfer.traceList && currentTransfer.traceList.length">
          <el-timeline-item
            v-for="(trace, index) in currentTransfer.traceList"
            :key="trace.id || index"
            :timestamp="trace.operateTime"
            placement="top"
            :type="getTimelineType(trace.nodeName)"
          >
            <el-card shadow="never" size="small">
              <div class="trace-header">
                <strong>{{ trace.nodeName }}</strong>
                <span style="color: #909399; font-size: 13px; margin-left: 12px">
                  操作人：{{ trace.operatorName || '-' }}
                </span>
              </div>
              <div style="margin-top: 8px">
                <p v-if="trace.fromDeptName" style="margin: 4px 0; color: #606266">
                  从【{{ trace.fromDeptName }}】
                  <span v-if="trace.toDeptName">转至【{{ trace.toDeptName }}】</span>
                </p>
                <p v-if="trace.comment" style="margin: 4px 0">{{ trace.comment }}</p>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无追溯信息" :image-size="50" />
      </div>
    </el-dialog>

    <el-dialog v-model="approveDialogVisible" title="审批流转申请" width="600px" destroy-on-close>
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="事件">
          <div style="color: #303133">{{ currentTransfer?.eventTitle || '-' }}</div>
        </el-form-item>
        <el-form-item label="转派原因">
          <div style="color: #606266">{{ currentTransfer?.transferReason || '-' }}</div>
        </el-form-item>
        <el-form-item label="转入机构">
          <el-tag type="primary">{{ currentTransfer?.targetDeptName || '-' }}</el-tag>
        </el-form-item>
        <el-form-item label="审批结果" required>
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.approveComment"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="processDialogVisible" title="添加处理记录" width="600px" destroy-on-close>
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="处理记录">
          <el-input
            v-model="processForm.processDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入处理记录内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcess">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialogVisible" title="办结流转" width="600px" destroy-on-close>
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="处理结果" required>
          <el-select v-model="completeForm.processResult" placeholder="请选择处理结果" style="width: 100%">
            <el-option label="已妥善处置" value="已妥善处置" />
            <el-option label="已移交相关部门" value="已移交相关部门" />
            <el-option label="已协调解决" value="已协调解决" />
            <el-option label="需持续跟进" value="需持续跟进" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="completeForm.processDescription"
            type="textarea"
            :rows="4"
            placeholder="请详细说明处理过程和结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitComplete">办结</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="traceDialogVisible" title="流转追溯链" width="700px" destroy-on-close>
      <el-timeline v-if="traceList.length">
        <el-timeline-item
          v-for="(trace, index) in traceList"
          :key="trace.id || index"
          :timestamp="trace.operateTime"
          placement="top"
          :type="getTimelineType(trace.nodeName)"
        >
          <el-card shadow="never" size="small">
            <div class="trace-header">
              <strong>{{ trace.nodeName }}</strong>
              <span style="color: #909399; font-size: 13px; margin-left: 12px">
                操作人：{{ trace.operatorName || '-' }}
              </span>
            </div>
            <div style="margin-top: 8px">
              <p v-if="trace.fromDeptName" style="margin: 4px 0; color: #606266">
                从【{{ trace.fromDeptName }}】
                <span v-if="trace.toDeptName">转至【{{ trace.toDeptName }}】</span>
              </p>
              <p v-if="trace.comment" style="margin: 4px 0">{{ trace.comment }}</p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无追溯信息" />
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Clock,
  Promotion,
  Loading,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import {
  getTransferPage,
  getTransferDetail,
  getTransferTrace,
  approveTransfer,
  receiveTransfer,
  processTransfer,
  completeTransfer,
  getTransferStatistics
} from '@/api/crossStreetTransfer'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const statistics = ref({})

const searchForm = reactive({
  keyword: '',
  status: '',
  targetType: '',
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const processDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const traceDialogVisible = ref(false)

const currentTransfer = ref(null)
const traceList = ref([])

const approveForm = reactive({
  transferId: null,
  approved: true,
  approveComment: ''
})

const processForm = reactive({
  transferId: null,
  processDescription: ''
})

const completeForm = reactive({
  transferId: null,
  processResult: '',
  processDescription: ''
})

function getStatusLabel(status) {
  const map = {
    PENDING_APPROVAL: '待审批',
    APPROVED: '已通过',
    TRANSFERRED: '已转派',
    ACCEPTED: '已接收',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    REJECTED: '已驳回'
  }
  return map[status] || status
}

function getStatusTagType(status) {
  const map = {
    PENDING_APPROVAL: 'warning',
    APPROVED: 'primary',
    TRANSFERRED: 'primary',
    ACCEPTED: 'info',
    PROCESSING: 'info',
    COMPLETED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function getUrgencyLevelLabel(level) {
  const map = {
    LOW: '低',
    MEDIUM: '普通',
    HIGH: '重要',
    URGENT: '紧急'
  }
  return map[level] || '普通'
}

function getUrgencyLevelTagType(level) {
  const map = {
    LOW: 'info',
    MEDIUM: 'success',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[level] || 'info'
}

function getTimelineType(nodeName) {
  if (!nodeName) return 'primary'
  if (nodeName.includes('申请')) return 'warning'
  if (nodeName.includes('审批') || nodeName.includes('通过')) return 'success'
  if (nodeName.includes('驳回')) return 'danger'
  if (nodeName.includes('接收')) return 'warning'
  if (nodeName.includes('处理')) return 'primary'
  if (nodeName.includes('办结')) return 'success'
  return 'primary'
}

async function fetchStatistics() {
  try {
    const res = await getTransferStatistics()
    statistics.value = res.data || {}
  } catch (e) {
    // ignore
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getTransferPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: searchForm.status,
      targetType: searchForm.targetType,
      keyword: searchForm.keyword
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.targetType = ''
  searchForm.dateRange = []
  pagination.pageNum = 1
  fetchList()
}

async function handleViewDetail(row) {
  try {
    const res = await getTransferDetail(row.id)
    currentTransfer.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

function handleApprove(row) {
  currentTransfer.value = row
  approveForm.transferId = row.id
  approveForm.approved = true
  approveForm.approveComment = ''
  approveDialogVisible.value = true
}

async function submitApprove() {
  if (!approveForm.approved && (!approveForm.approveComment || approveForm.approveComment.trim() === '')) {
    ElMessage.warning('驳回应填写驳回理由')
    return
  }

  submitting.value = true
  try {
    await approveTransfer({
      transferId: approveForm.transferId,
      approved: approveForm.approved,
      approveComment: approveForm.approveComment
    })
    ElMessage.success(approveForm.approved ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    fetchList()
    fetchStatistics()
  } catch (e) {
    ElMessage.error(e.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

async function handleReceive(row) {
  try {
    await ElMessageBox.confirm(
      `确定要接收来自【${row.sourceDeptName}】的协作任务【${row.eventTitle}】吗？`,
      '接收流转任务',
      {
        confirmButtonText: '确认接收',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await receiveTransfer(row.id)
    ElMessage.success('接收成功，已开始处理')
    fetchList()
    fetchStatistics()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '接收失败')
    }
  }
}

function handleProcess(row) {
  currentTransfer.value = row
  processForm.transferId = row.id
  processForm.processDescription = ''
  processDialogVisible.value = true
}

async function submitProcess() {
  if (!processForm.processDescription || processForm.processDescription.trim() === '') {
    ElMessage.warning('请输入处理记录')
    return
  }

  submitting.value = true
  try {
    await processTransfer({
      transferId: processForm.transferId,
      action: 'PROCESS',
      processDescription: processForm.processDescription
    })
    ElMessage.success('处理记录已添加')
    processDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleComplete(row) {
  currentTransfer.value = row
  completeForm.transferId = row.id
  completeForm.processResult = ''
  completeForm.processDescription = ''
  completeDialogVisible.value = true
}

async function submitComplete() {
  if (!completeForm.processResult) {
    ElMessage.warning('请选择处理结果')
    return
  }

  submitting.value = true
  try {
    await completeTransfer({
      transferId: completeForm.transferId,
      processResult: completeForm.processResult,
      processDescription: completeForm.processDescription
    })
    ElMessage.success('协作任务已办结')
    completeDialogVisible.value = false
    fetchList()
    fetchStatistics()
  } catch (e) {
    ElMessage.error(e.message || '办结失败')
  } finally {
    submitting.value = false
  }
}

async function handleViewTrace(row) {
  try {
    const res = await getTransferTrace(row.id)
    traceList.value = res.data || []
    traceDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载追溯链失败')
  }
}

onMounted(() => {
  fetchStatistics()
  fetchList()
})
</script>

<style lang="scss" scoped>
.cross-street-transfer-page {
  padding: 16px;

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #fff;

      &.pending {
        background: linear-gradient(135deg, #ff976a 0%, #ed6a0c 100%);
      }

      &.transferred {
        background: linear-gradient(135deg, #409EFF 0%, #1565c0 100%);
      }

      &.processing {
        background: linear-gradient(135deg, #67c23a 0%, #2e7d32 100%);
      }

      &.completed {
        background: linear-gradient(135deg, #a0a4a8 0%, #606266 100%);
      }
    }

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .event-title-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .trace-header {
    display: flex;
    align-items: center;
  }
}
</style>
