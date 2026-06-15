<template>
  <div class="audit-log-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="事件ID">
          <el-input v-model="searchForm.eventId" placeholder="请输入事件ID" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="searchForm.username" placeholder="请输入操作人" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="searchForm.module" placeholder="请选择模块" clearable style="width: 140px">
            <el-option label="事件管理" value="event" />
            <el-option label="用户管理" value="user" />
            <el-option label="网格管理" value="grid" />
            <el-option label="流程管理" value="process" />
            <el-option label="催办管理" value="urge" />
            <el-option label="区块链存证" value="blockchain" />
            <el-option label="统计分析" value="statistics" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="searchForm.operation" placeholder="请选择操作" clearable style="width: 140px">
            <el-option label="新增" value="create" />
            <el-option label="修改" value="update" />
            <el-option label="删除" value="delete" />
            <el-option label="查询" value="query" />
            <el-option label="审核" value="audit" />
            <el-option label="分派" value="dispatch" />
            <el-option label="处置" value="handle" />
            <el-option label="登录" value="login" />
            <el-option label="登出" value="logout" />
            <el-option label="导出" value="export" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="描述/参数/结果" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="操作时间">
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
        <el-button type="primary" @click="handleExportPdf">
          <el-icon><Download /></el-icon>导出审计报告(PDF)
        </el-button>
        <div class="stats-info">
          <span>共 {{ total }} 条记录</span>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ getModuleLabel(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="80">
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operation)" size="small">{{ getOperationLabel(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="eventId" label="事件ID" width="130" show-overflow-tooltip />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="120" />
        <el-table-column prop="method" label="请求方法" width="80">
          <template #default="{ row }">
            <el-tag :type="getMethodTagType(row.method)" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90">
          <template #default="{ row }">
            {{ row.costTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="完整性" width="80" fixed="right">
          <template #default="{ row }">
            <el-tag
              :type="row.tamperChecked ? 'success' : (row.tamperChecked === false ? 'danger' : 'info')"
              size="small"
              @click="handleVerify(row)"
              style="cursor: pointer"
            >
              <template v-if="row.tamperChecked === true">
                <el-icon><CircleCheck /></el-icon>正常
              </template>
              <template v-else-if="row.tamperChecked === false">
                <el-icon><CircleClose /></el-icon>篡改
              </template>
              <template v-else>
                <el-icon><QuestionFilled /></el-icon>校验
              </template>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="审计日志详情" width="800px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="操作时间">{{ currentLog.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ getModuleLabel(currentLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ getOperationLabel(currentLog.operation) }}</el-descriptions-item>
        <el-descriptions-item label="事件ID">{{ currentLog.eventId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ currentLog.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="code-block">{{ currentLog.requestParams || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <pre class="code-block">{{ currentLog.responseResult || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMsg">
          <span style="color: #f56c6c">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="哈希值" :span="2">
          <div class="hash-info">
            <div><strong>当前哈希：</strong>{{ currentLog.currentHash }}</div>
            <div style="margin-top: 8px"><strong>前一哈希：</strong>{{ currentLog.previousHash || '0' }}</div>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  queryAuditLogs,
  getAuditLogDetail,
  exportAuditPdf,
  verifyLogIntegrity
} from '@/api/auditLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const detailVisible = ref(false)
const currentLog = ref(null)

const searchForm = reactive({
  eventId: '',
  username: '',
  module: '',
  operation: '',
  status: null,
  keyword: '',
  dateRange: []
})

const moduleMap = {
  event: '事件管理',
  user: '用户管理',
  grid: '网格管理',
  process: '流程管理',
  urge: '催办管理',
  blockchain: '区块链存证',
  statistics: '统计分析',
  auth: '认证管理',
  file: '文件管理',
  audit: '审计管理'
}

const operationMap = {
  create: '新增',
  update: '修改',
  delete: '删除',
  query: '查询',
  audit: '审核',
  dispatch: '分派',
  handle: '处置',
  login: '登录',
  logout: '登出',
  export: '导出',
  import: '导入',
  approve: '审批',
  reject: '驳回',
  verify: '核验'
}

function getModuleLabel(module) {
  return moduleMap[module] || module || '-'
}

function getOperationLabel(operation) {
  return operationMap[operation] || operation || '-'
}

function getOperationTagType(operation) {
  const typeMap = {
    create: 'success',
    update: 'warning',
    delete: 'danger',
    login: 'primary',
    logout: 'info',
    export: 'success'
  }
  return typeMap[operation] || 'info'
}

function getMethodTagType(method) {
  const typeMap = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return typeMap[method] || 'info'
}

function buildQueryParams() {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }
  if (searchForm.eventId) params.eventId = searchForm.eventId
  if (searchForm.username) params.username = searchForm.username
  if (searchForm.module) params.module = searchForm.module
  if (searchForm.operation) params.operation = searchForm.operation
  if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startTime = searchForm.dateRange[0]
    params.endTime = searchForm.dateRange[1]
  }
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await queryAuditLogs(params)
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchData()
}

function handleReset() {
  searchForm.eventId = ''
  searchForm.username = ''
  searchForm.module = ''
  searchForm.operation = ''
  searchForm.status = null
  searchForm.keyword = ''
  searchForm.dateRange = []
  pageNum.value = 1
  fetchData()
}

function handleSizeChange(val) {
  pageSize.value = val
  fetchData()
}

function handleCurrentChange(val) {
  pageNum.value = val
  fetchData()
}

async function handleView(row) {
  try {
    const res = await getAuditLogDetail(row.id)
    if (res.code === 200) {
      currentLog.value = res.data
      detailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

async function handleVerify(row) {
  try {
    const res = await verifyLogIntegrity(row.id)
    if (res.code === 200) {
      row.tamperChecked = res.data
      if (res.data) {
        ElMessage.success('日志完整性校验通过')
      } else {
        ElMessage.error('日志可能已被篡改！')
      }
    }
  } catch (e) {
    ElMessage.error('校验失败')
  }
}

async function handleExportPdf() {
  try {
    await ElMessageBox.confirm('确定要导出当前查询条件的审计报告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const params = buildQueryParams()
    const res = await exportAuditPdf(params)

    const blob = new Blob([res], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `审计报告_${new Date().getTime()}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('导出失败')
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.audit-log-page {
  .search-form {
    .el-form-item {
      margin-bottom: 0;
    }
  }

  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .stats-info {
      color: #909399;
      font-size: 14px;
    }
  }

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .code-block {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    max-height: 200px;
    overflow-y: auto;
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .hash-info {
    font-family: 'Courier New', monospace;
    font-size: 12px;
    color: #606266;
    word-break: break-all;
  }
}
</style>
