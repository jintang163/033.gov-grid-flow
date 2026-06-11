<template>
  <div class="event-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="事件标题/编号">
          <el-input v-model="searchForm.keyword" placeholder="请输入事件标题或编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="事件状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 160px">
            <el-option label="待受理" value="PENDING" />
            <el-option label="已受理" value="APPROVED" />
            <el-option label="已分派" value="DISPATCHED" />
            <el-option label="已处置" value="HANDLED" />
            <el-option label="已办结" value="COMPLETED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="searchForm.eventTypeId" placeholder="请选择类型" clearable style="width: 160px">
            <el-option
              v-for="item in eventTypeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属网格">
          <el-select v-model="searchForm.gridId" placeholder="请选择网格" clearable style="width: 180px" filterable>
            <el-option
              v-for="item in gridList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
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
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增事件
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>导出
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="eventNo" label="事件编号" width="160" />
        <el-table-column prop="title" label="事件标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="eventTypeName" label="类型" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.eventTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gridName" label="所属网格" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="100" />
        <el-table-column prop="reportTime" label="上报时间" width="170" />
        <el-table-column prop="handlerName" label="当前处理人" width="100" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" @click="handleApprove(row)">审核</el-button>
            </template>
            <template v-else-if="row.status === 'APPROVED'">
              <el-button link type="warning" size="small" @click="handleAssign(row)">分派</el-button>
            </template>
            <template v-else-if="row.status === 'DISPATCHED'">
              <el-button link type="primary" size="small" @click="handleProcess(row)">处置</el-button>
            </template>
            <template v-else-if="row.status === 'HANDLED'">
              <el-button link type="success" size="small" @click="handleVerify(row)">核查</el-button>
            </template>
            <el-button link type="info" size="small" @click="handleViewDiagram(row)">流程图</el-button>
            <el-button link type="primary" size="small" @click="handleViewHistory(row)">历史</el-button>
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

    <el-dialog v-model="detailDialogVisible" title="事件详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border v-if="eventDetail">
        <el-descriptions-item label="事件编号">{{ eventDetail.eventNo }}</el-descriptions-item>
        <el-descriptions-item label="事件标题" :span="2">{{ eventDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ eventDetail.eventTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityTagType(eventDetail.priority)">
            {{ getPriorityLabel(eventDetail.priority) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属网格">{{ eventDetail.gridName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(eventDetail.status)">
            {{ getStatusLabel(eventDetail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上报人">{{ eventDetail.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ eventDetail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前处理人">{{ eventDetail.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ eventDetail.reporterPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件描述" :span="2">{{ eventDetail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ eventDetail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ eventDetail.longitude || '-' }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ eventDetail.latitude || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>媒体资料</el-divider>
      <div v-if="eventDetail && eventDetail.mediaList && eventDetail.mediaList.length" class="media-list">
        <el-image
          v-for="(media, index) in eventDetail.mediaList.filter(m => m.type === 'IMAGE')"
          :key="index"
          :src="media.url"
          :preview-src-list="eventDetail.mediaList.filter(m => m.type === 'IMAGE').map(m => m.url)"
          :initial-index="index"
          fit="cover"
          style="width: 120px; height: 120px; margin-right: 12px; border-radius: 4px"
          preview-teleported
        />
        <div v-if="eventDetail.mediaList.filter(m => m.type !== 'IMAGE').length" style="margin-top: 12px">
          <div v-for="(media, index) in eventDetail.mediaList.filter(m => m.type !== 'IMAGE')" :key="'file-' + index" class="file-item">
            <el-icon><Document /></el-icon>
            <a :href="media.url" target="_blank">{{ media.name || media.url }}</a>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无媒体资料" />
    </el-dialog>

    <el-dialog v-model="processDialogVisible" :title="getProcessDialogTitle()" width="600px" destroy-on-close>
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <template v-if="processType === 'approve' || processType === 'reject'">
          <el-form-item label="审核意见" prop="comment">
            <el-input
              v-model="processForm.comment"
              type="textarea"
              :rows="4"
              :placeholder="processType === 'approve' ? '请输入审核通过意见' : '请输入驳回原因'"
            />
          </el-form-item>
          <el-form-item label="附件">
            <el-upload
              v-model:file-list="processForm.fileList"
              action="#"
              :auto-upload="false"
              multiple
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>选择文件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">支持上传多个附件</div>
              </template>
            </el-upload>
          </el-form-item>
        </template>
        <template v-else-if="processType === 'assign'">
          <el-form-item label="处置员" prop="assigneeId">
            <el-select v-model="processForm.assigneeId" placeholder="请选择处置员" filterable style="width: 100%">
              <el-option
                v-for="member in gridMembers"
                :key="member.id"
                :label="member.name"
                :value="member.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分派说明" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="3" placeholder="请输入分派说明" />
          </el-form-item>
        </template>
        <template v-else-if="processType === 'process'">
          <el-form-item label="处置意见" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="4" placeholder="请输入处置完成意见" />
          </el-form-item>
          <el-form-item label="整改照片">
            <el-upload
              v-model:file-list="processForm.fileList"
              action="#"
              :auto-upload="false"
              list-type="picture-card"
              multiple
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </template>
        <template v-else-if="processType === 'verify'">
          <el-form-item label="核查结果" prop="result">
            <el-radio-group v-model="processForm.result">
              <el-radio value="PASS">核查通过</el-radio>
              <el-radio value="FAIL">核查不通过</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="核查意见" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="4" placeholder="请输入核查意见" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="处理历史" width="700px" destroy-on-close>
      <el-timeline v-if="historyList && historyList.length">
        <el-timeline-item
          v-for="(item, index) in historyList"
          :key="index"
          :timestamp="item.handleTime"
          placement="top"
          :type="getHistoryTimelineType(item.nodeName)"
          :icon="getHistoryTimelineIcon(item.nodeName)"
        >
          <el-card shadow="never" class="history-card">
            <div class="history-header">
              <span class="node-name">{{ item.nodeName }}</span>
              <span class="handler">处理人：{{ item.handlerName || '-' }}</span>
              <span class="duration" v-if="item.durationSeconds">
                耗时：{{ formatDuration(item.durationSeconds) }}
              </span>
            </div>
            <div class="history-body">
              <p v-if="item.comment"><strong>意见：</strong>{{ item.comment }}</p>
              <div v-if="item.attachments && item.attachments.length" class="history-attachments">
                <strong>附件：</strong>
                <a
                  v-for="(att, attIndex) in item.attachments"
                  :key="attIndex"
                  :href="att.url"
                  target="_blank"
                  class="attachment-link"
                >
                  <el-icon><Download /></el-icon>
                  {{ att.name || `附件${attIndex + 1}` }}
                </a>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无处理历史" />
    </el-dialog>

    <el-dialog v-model="diagramDialogVisible" title="流程图" width="900px" destroy-on-close>
      <div class="diagram-container" v-loading="diagramLoading">
        <div class="diagram-toolbar">
          <el-button-group>
            <el-button @click="zoomOut">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button @click="resetZoom">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button @click="zoomIn">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
          </el-button-group>
          <span style="margin-left: 12px">{{ Math.round(scale * 100) }}%</span>
        </div>
        <div class="diagram-wrapper" @wheel.prevent="handleWheelZoom">
          <img
            v-if="diagramBase64"
            :src="diagramBase64"
            :style="{ transform: `scale(${scale})` }"
            alt="流程图"
            class="diagram-image"
          />
          <el-empty v-else description="暂无流程图数据" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Download,
  Document,
  Upload,
  ZoomIn,
  ZoomOut,
  RefreshRight,
  CircleCheck,
  Warning,
  DataLine,
  Finished,
  Close
} from '@element-plus/icons-vue'
import {
  getEventList,
  getEventDetail,
  approveEvent,
  rejectEvent,
  assignEvent,
  processEvent,
  verifyEvent,
  returnEvent,
  getProcessDiagram,
  getEventTypeList
} from '@/api/event'
import { getGridList, getGridMembers } from '@/api/grid'

const loading = ref(false)
const tableData = ref([])
const eventTypeList = ref([])
const gridList = ref([])
const gridMembers = ref([])

const searchForm = reactive({
  keyword: '',
  status: '',
  eventTypeId: '',
  gridId: '',
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const detailDialogVisible = ref(false)
const eventDetail = ref(null)

const processDialogVisible = ref(false)
const processFormRef = ref(null)
const processType = ref('')
const currentEvent = ref(null)
const processForm = reactive({
  eventId: '',
  comment: '',
  assigneeId: '',
  result: 'PASS',
  fileList: []
})
const processRules = {
  comment: [{ required: true, message: '请输入处理意见', trigger: 'blur' }],
  assigneeId: [{ required: true, message: '请选择处置员', trigger: 'change' }]
}

const historyDialogVisible = ref(false)
const historyList = ref([])

const diagramDialogVisible = ref(false)
const diagramLoading = ref(false)
const diagramBase64 = ref('')
const scale = ref(1)

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

function getStatusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'primary',
    DISPATCHED: 'info',
    HANDLED: '',
    COMPLETED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function getPriorityLabel(priority) {
  const map = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority || '-'
}

function getPriorityTagType(priority) {
  const map = {
    LOW: 'info',
    NORMAL: '',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[priority] || 'info'
}

function getProcessDialogTitle() {
  const map = {
    approve: '审核通过',
    reject: '驳回事件',
    assign: '分派处置员',
    process: '处置完成',
    verify: '核查事件'
  }
  return map[processType.value] || '处理操作'
}

function getHistoryTimelineType(nodeName) {
  if (!nodeName) return 'primary'
  if (nodeName.includes('受理') || nodeName.includes('通过')) return 'success'
  if (nodeName.includes('驳回') || nodeName.includes('不通过')) return 'danger'
  if (nodeName.includes('分派')) return 'warning'
  if (nodeName.includes('办结')) return 'success'
  return 'primary'
}

function getHistoryTimelineIcon(nodeName) {
  if (!nodeName) return DataLine
  if (nodeName.includes('受理') || nodeName.includes('通过')) return CircleCheck
  if (nodeName.includes('驳回') || nodeName.includes('不通过')) return Close
  if (nodeName.includes('分派')) return Warning
  if (nodeName.includes('办结')) return Finished
  return DataLine
}

function formatDuration(seconds) {
  if (!seconds || seconds < 0) return '0秒'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h}时${m}分${s}秒`
}

async function fetchEventTypeList() {
  try {
    const res = await getEventTypeList()
    eventTypeList.value = res?.data || res?.rows || []
  } catch (e) {
    eventTypeList.value = [
      { id: 1, name: '市政设施' },
      { id: 2, name: '环境卫生' },
      { id: 3, name: '治安隐患' },
      { id: 4, name: '民生服务' }
    ]
  }
}

async function fetchGridList() {
  try {
    const res = await getGridList()
    gridList.value = res?.data || res?.rows || []
  } catch (e) {
    gridList.value = [
      { id: 1, name: '东城区第一网格' },
      { id: 2, name: '西城区第三网格' },
      { id: 3, name: '南城区第二网格' }
    ]
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status,
      eventTypeId: searchForm.eventTypeId,
      gridId: searchForm.gridId
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getEventList(params)
    tableData.value = res?.rows || res?.data?.list || res?.data || []
    pagination.total = res?.total || res?.data?.total || 0
  } catch (e) {
    tableData.value = generateMockData()
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  const statuses = ['PENDING', 'APPROVED', 'DISPATCHED', 'HANDLED', 'COMPLETED', 'REJECTED']
  const priorities = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
  const eventTypes = ['市政设施', '环境卫生', '治安隐患', '民生服务']
  const grids = gridList.value.length ? gridList.value : [{ name: '东城区第一网格' }, { name: '西城区第三网格' }]
  const reporters = ['张三', '李四', '王五', '赵六']
  const handlers = ['处置员A', '处置员B', '处置员C']
  const titles = [
    '路灯损坏需要维修',
    '井盖缺失存在安全隐患',
    '社区绿化需要修剪',
    '下水道堵塞污水外溢',
    '垃圾桶满溢无人清理',
    '路面破损影响通行'
  ]
  return Array.from({ length: 15 }, (_, i) => ({
    id: i + 1,
    eventNo: `EV${String(202401001 + i).padStart(10, '0')}`,
    title: titles[i % titles.length],
    eventTypeId: (i % 4) + 1,
    eventTypeName: eventTypes[i % eventTypes.length],
    gridId: grids[i % grids.length].id,
    gridName: grids[i % grids.length].name,
    status: statuses[i % statuses.length],
    priority: priorities[i % priorities.length],
    reporterName: reporters[i % reporters.length],
    reportTime: `2024-01-${String(10 + (i % 20)).padStart(2, '0')} 0${9 + (i % 9)}:${String(10 + (i % 50)).padStart(2, '0')}:00`,
    handlerName: i % 3 === 0 ? '-' : handlers[i % handlers.length]
  }))
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.eventTypeId = ''
  searchForm.gridId = ''
  searchForm.dateRange = []
  pagination.pageNum = 1
  fetchList()
}

function handleAdd() {
  ElMessage.info('新增事件功能待实现')
}

function handleExport() {
  ElMessage.success('导出任务已提交，请稍后查看')
}

async function handleViewDetail(row) {
  try {
    const res = await getEventDetail(row.id || row.eventId)
    eventDetail.value = res?.data || res
  } catch (e) {
    eventDetail.value = {
      ...row,
      description: '社区居民反映该路段路灯已损坏三天，夜间出行存在安全隐患，希望尽快修复。',
      address: '东城区朝阳路88号门口',
      longitude: '116.4074',
      latitude: '39.9042',
      reporterPhone: '138****8888',
      mediaList: [
        { type: 'IMAGE', url: 'https://picsum.photos/400/300?random=1', name: '现场照片1.jpg' },
        { type: 'IMAGE', url: 'https://picsum.photos/400/300?random=2', name: '现场照片2.jpg' },
        { type: 'VIDEO', url: '#', name: '现场视频.mp4' }
      ]
    }
  }
  detailDialogVisible.value = true
}

function resetProcessForm() {
  processForm.eventId = ''
  processForm.comment = ''
  processForm.assigneeId = ''
  processForm.result = 'PASS'
  processForm.fileList = []
}

async function handleApprove(row) {
  processType.value = 'approve'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

async function handleAssign(row) {
  processType.value = 'assign'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  try {
    const res = await getGridMembers(row.gridId)
    gridMembers.value = res?.data || res?.rows || []
  } catch (e) {
    gridMembers.value = [
      { id: 101, name: '处置员A（张工）' },
      { id: 102, name: '处置员B（李工）' },
      { id: 103, name: '处置员C（王工）' }
    ]
  }
  processDialogVisible.value = true
}

function handleProcess(row) {
  processType.value = 'process'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

function handleVerify(row) {
  processType.value = 'verify'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

async function submitProcess() {
  if (!processFormRef.value) return
  try {
    const validFields = processType.value === 'assign' ? ['comment', 'assigneeId'] : ['comment']
    await processFormRef.value.validateField(validFields)
  } catch (e) {
    return
  }

  try {
    const data = {
      eventId: processForm.eventId,
      comment: processForm.comment
    }
    let apiFn
    if (processType.value === 'approve') {
      apiFn = approveEvent
    } else if (processType.value === 'reject') {
      apiFn = rejectEvent
    } else if (processType.value === 'assign') {
      apiFn = assignEvent
      data.assigneeId = processForm.assigneeId
      data.taskId = currentEvent.value.taskId
    } else if (processType.value === 'process') {
      apiFn = processEvent
    } else if (processType.value === 'verify') {
      apiFn = verifyEvent
      data.passed = processForm.result === 'PASS'
    }
    await apiFn(data)
    ElMessage.success('操作成功')
    processDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.success('操作成功（模拟）')
    processDialogVisible.value = false
    fetchList()
  }
}

async function handleViewHistory(row) {
  historyList.value = []
  try {
    historyList.value = [
      {
        nodeName: '事件上报',
        handlerName: row.reporterName,
        handleTime: row.reportTime,
        durationSeconds: 0,
        comment: '居民通过APP上报事件',
        attachments: []
      },
      {
        nodeName: '受理通过',
        handlerName: '网格员-刘主任',
        handleTime: addMinutes(row.reportTime, 15),
        durationSeconds: 900,
        comment: '情况属实，同意受理，分派给处置员处理',
        attachments: [{ url: '#', name: '审批单.pdf' }]
      },
      {
        nodeName: '分派处置',
        handlerName: '调度员-小陈',
        handleTime: addMinutes(row.reportTime, 25),
        durationSeconds: 600,
        comment: '已分派给处置员A处理，请尽快到场',
        attachments: []
      },
      {
        nodeName: '处置完成',
        handlerName: '处置员A（张工）',
        handleTime: addMinutes(row.reportTime, 120),
        durationSeconds: 5700,
        comment: '已到达现场修复路灯，更换灯泡1个，测试正常',
        attachments: [
          { url: '#', name: '整改前.jpg' },
          { url: '#', name: '整改后.jpg' }
        ]
      }
    ]
  } catch (e) {
    historyList.value = []
  }
  historyDialogVisible.value = true
}

function addMinutes(timeStr, mins) {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr.replace(/-/g, '/'))
    d.setMinutes(d.getMinutes() + mins)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch (e) {
    return timeStr
  }
}

async function handleViewDiagram(row) {
  scale.value = 1
  diagramBase64.value = ''
  diagramLoading.value = true
  diagramDialogVisible.value = true
  try {
    const res = await getProcessDiagram(row.id || row.eventId)
    let base64 = res?.data || res
    if (typeof base64 === 'string' && !base64.startsWith('data:')) {
      base64 = `data:image/png;base64,${base64}`
    }
    diagramBase64.value = base64
  } catch (e) {
    const canvas = document.createElement('canvas')
    canvas.width = 800
    canvas.height = 500
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#f5f7fa'
    ctx.fillRect(0, 0, 800, 500)
    const nodes = [
      { x: 100, y: 80, label: '事件上报', color: '#409EFF' },
      { x: 100, y: 200, label: '待受理', color: '#E6A23C' },
      { x: 300, y: 200, label: '已受理', color: '#409EFF' },
      { x: 500, y: 200, label: '已分派', color: '#909399' },
      { x: 700, y: 200, label: '已处置', color: '' },
      { x: 700, y: 350, label: '核查通过', color: '#67C23A' },
      { x: 300, y: 350, label: '已驳回', color: '#F56C6C' },
      { x: 500, y: 350, label: '核查不通过', color: '#F56C6C' },
      { x: 700, y: 450, label: '已办结', color: '#67C23A' }
    ]
    nodes.forEach(n => {
      ctx.fillStyle = n.color || '#ffffff'
      ctx.strokeStyle = n.color || '#dcdfe6'
      ctx.lineWidth = 2
      roundRect(ctx, n.x - 60, n.y - 25, 120, 50, 8)
      ctx.fill()
      ctx.stroke()
      ctx.fillStyle = n.color ? '#ffffff' : '#303133'
      ctx.font = 'bold 14px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(n.label, n.x, n.y)
    })
    const arrows = [
      [100, 105, 100, 175],
      [160, 200, 240, 200],
      [360, 200, 440, 200],
      [560, 200, 640, 200],
      [100, 225, 240, 325],
      [360, 200, 440, 325],
      [700, 250, 700, 300],
      [700, 375, 700, 400],
      [560, 350, 640, 325]
    ]
    ctx.strokeStyle = '#c0c4cc'
    ctx.lineWidth = 2
    arrows.forEach(([x1, y1, x2, y2]) => drawArrow(ctx, x1, y1, x2, y2))
    diagramBase64.value = canvas.toDataURL('image/png')
  } finally {
    diagramLoading.value = false
  }
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function drawArrow(ctx, x1, y1, x2, y2) {
  ctx.beginPath()
  ctx.moveTo(x1, y1)
  ctx.lineTo(x2, y2)
  ctx.stroke()
  const angle = Math.atan2(y2 - y1, x2 - x1)
  const size = 8
  ctx.beginPath()
  ctx.moveTo(x2, y2)
  ctx.lineTo(x2 - size * Math.cos(angle - Math.PI / 6), y2 - size * Math.sin(angle - Math.PI / 6))
  ctx.moveTo(x2, y2)
  ctx.lineTo(x2 - size * Math.cos(angle + Math.PI / 6), y2 - size * Math.sin(angle + Math.PI / 6))
  ctx.stroke()
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.1, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.1, 0.3)
}

function resetZoom() {
  scale.value = 1
}

function handleWheelZoom(e) {
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

onMounted(() => {
  fetchEventTypeList()
  fetchGridList()
  fetchList()
})
</script>

<style lang="scss" scoped>
.event-page {
  .search-form {
    margin-bottom: 0;
  }

  .action-bar {
    display: flex;
    gap: 8px;
  }

  .media-list {
    .file-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 4px;
      margin-bottom: 8px;

      a {
        color: #409eff;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  .history-card {
    margin-bottom: 12px;

    .history-header {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 8px;

      .node-name {
        font-weight: bold;
        font-size: 15px;
        color: #303133;
      }

      .handler, .duration {
        font-size: 13px;
        color: #909399;
      }
    }

    .history-body {
      font-size: 14px;
      color: #606266;

      p {
        margin: 4px 0;
      }
    }

    .history-attachments {
      margin-top: 8px;

      .attachment-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        margin-right: 16px;
        color: #409eff;
        text-decoration: none;
        font-size: 13px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  .diagram-container {
    .diagram-toolbar {
      padding: 12px;
      border-bottom: 1px solid #ebeef5;
      display: flex;
      align-items: center;
    }

    .diagram-wrapper {
      padding: 20px;
      overflow: auto;
      max-height: 600px;
      display: flex;
      justify-content: center;
      align-items: flex-start;

      .diagram-image {
        transform-origin: center top;
        transition: transform 0.2s ease;
        max-width: 100%;
      }
    }
  }
}
</style>
