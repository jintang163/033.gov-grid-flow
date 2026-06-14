<template>
  <div class="blockchain-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="存证编号">
          <el-input v-model="searchForm.evidenceNo" placeholder="请输入存证编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="事件编号/标题">
          <el-input v-model="searchForm.keyword" placeholder="请输入事件编号或标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="存证状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
            <el-option label="上链成功" value="SUCCESS" />
            <el-option label="处理中" value="PENDING" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="核验状态">
          <el-select v-model="searchForm.verified" placeholder="请选择" clearable style="width: 140px">
            <el-option label="已核验" :value="1" />
            <el-option label="待核验" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="存证时间">
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
      <div class="table-header">
        <div class="header-title">区块链存证列表</div>
        <div class="header-actions">
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="evidenceNo" label="存证编号" width="200" align="center">
          <template #default="{ row }">
            <span class="evidence-no">{{ row.evidenceNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eventId" label="事件ID" width="100" align="center" />
        <el-table-column prop="chainType" label="存证链" width="120" align="center" />
        <el-table-column prop="txHash" label="交易哈希" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="hash-text">{{ row.txHash }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="blockHeight" label="区块高度" width="120" align="center" />
        <el-table-column prop="imageCount" label="图片数" width="80" align="center" />
        <el-table-column prop="videoCount" label="视频数" width="80" align="center" />
        <el-table-column prop="status" label="存证状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" size="small">
              {{ row.status === 'SUCCESS' ? '上链成功' : row.status === 'PENDING' ? '处理中' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verified" label="核验状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.verified === 1 ? 'success' : 'info'" size="small">
              {{ row.verified === 1 ? '已核验' : '待核验' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="存证时间" width="170" align="center" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button type="success" link size="small" @click="handleVerify(row)" :disabled="row.status !== 'SUCCESS'">
              核验
            </el-button>
            <el-button type="warning" link size="small" @click="handleViewCertificate(row)">
              证书
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      title="存证详情"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-if="currentEvidence" class="evidence-detail">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="存证编号">{{ currentEvidence.evidenceNo }}</el-descriptions-item>
          <el-descriptions-item label="事件ID">{{ currentEvidence.eventId }}</el-descriptions-item>
          <el-descriptions-item label="存证链">{{ currentEvidence.chainType }}</el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ currentEvidence.blockHeight }}</el-descriptions-item>
          <el-descriptions-item label="交易哈希" :span="2">
            <span class="hash-text">{{ currentEvidence.txHash }}</span>
            <el-button link type="primary" size="small" @click="copyText(currentEvidence.txHash)">
              复制
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="区块时间">{{ currentEvidence.blockTime }}</el-descriptions-item>
          <el-descriptions-item label="存证时间">{{ currentEvidence.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="存证状态">
            <el-tag :type="currentEvidence.status === 'SUCCESS' ? 'success' : 'warning'" size="small">
              {{ currentEvidence.status === 'SUCCESS' ? '上链成功' : '处理中' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="核验状态">
            <el-tag :type="currentEvidence.verified === 1 ? 'success' : 'info'" size="small">
              {{ currentEvidence.verified === 1 ? '已核验' : '待核验' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="证据哈希" :column="1" border style="margin-top: 16px">
          <el-descriptions-item label="证据总哈希">
            <span class="hash-text">{{ currentEvidence.evidenceHash }}</span>
            <el-button link type="primary" size="small" @click="copyText(currentEvidence.evidenceHash)">
              复制
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="图片数量">{{ currentEvidence.imageCount }} 张</el-descriptions-item>
          <el-descriptions-item label="视频数量">{{ currentEvidence.videoCount }} 个</el-descriptions-item>
          <el-descriptions-item v-if="currentEvidence.voiceHash" label="语音哈希">
            <span class="hash-text">{{ currentEvidence.voiceHash }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="GPS哈希">
            <span class="hash-text">{{ currentEvidence.gpsHash }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="标题哈希">
            <span class="hash-text">{{ currentEvidence.titleHash }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述哈希">
            <span class="hash-text">{{ currentEvidence.descHash }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentEvidence.remark" class="remark-section">
          <div class="remark-label">备注</div>
          <div class="remark-content">{{ currentEvidence.remark }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleVerify(currentEvidence)" :disabled="currentEvidence?.status !== 'SUCCESS'">
          立即核验
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { getEvidenceList, verifyEvidence, getEvidenceDetail } from '@/api/blockchain'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  evidenceNo: '',
  keyword: '',
  status: '',
  verified: null,
  dateRange: []
})

const detailDialogVisible = ref(false)
const currentEvidence = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getEvidenceList(params)
    tableData.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchList()
}

const handleReset = () => {
  searchForm.evidenceNo = ''
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.verified = null
  searchForm.dateRange = []
  pageNum.value = 1
  fetchList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchList()
}

const handleView = async (row) => {
  try {
    const res = await getEvidenceDetail(row.id)
    currentEvidence.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败')
  }
}

const handleVerify = async (row) => {
  try {
    await ElMessageBox.confirm('确认对该存证进行链上核验吗？', '核验确认', {
      confirmButtonText: '确认核验',
      cancelButtonText: '取消',
      type: 'info'
    })
    const res = await verifyEvidence(row.id)
    if (res.data?.valid) {
      ElMessage.success('核验通过，数据真实有效')
      if (currentEvidence.value && currentEvidence.value.id === row.id) {
        currentEvidence.value.verified = 1
      }
      fetchList()
    } else {
      ElMessage.warning('核验未通过')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '核验失败')
    }
  }
}

const handleViewCertificate = (row) => {
  ElMessage.info('证书功能开发中，可在详情页查看完整存证信息')
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

const copyText = (text) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } else {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.blockchain-page {
  padding: 16px;

  .search-form {
    .el-form-item {
      margin-bottom: 0;
    }
  }

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .evidence-no {
    font-family: monospace;
    font-size: 12px;
    color: #409eff;
  }

  .hash-text {
    font-family: monospace;
    font-size: 12px;
    color: #909399;
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    padding-top: 16px;
  }

  .evidence-detail {
    .hash-text {
      word-break: break-all;
    }

    .remark-section {
      margin-top: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;

      .remark-label {
        font-size: 13px;
        color: #909399;
        margin-bottom: 8px;
      }

      .remark-content {
        font-size: 14px;
        color: #303133;
        line-height: 1.6;
      }
    }
  }
}
</style>
