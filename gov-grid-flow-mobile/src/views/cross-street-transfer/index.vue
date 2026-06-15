<template>
  <div class="transfer-page">
    <van-nav-bar fixed placeholder title="跨街道协同流转" @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
      <template #right>
        <van-icon name="plus" size="20" @click="goToApply" />
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeTab" sticky offset-top="46" @change="onTabChange">
      <van-tab title="待处理" name="pending" />
      <van-tab title="我发起" name="my" />
      <van-tab title="全部" name="all" />
    </van-tabs>

    <div class="page-content" v-if="activeTab === 'pending'">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="!loading && pendingList.length === 0" class="empty-wrapper">
            <van-empty description="暂无待处理流转" :image-size="60" />
          </div>
          <div
            v-for="item in pendingList"
            :key="item.id"
            class="transfer-card"
            @click="goToDetail(item.id)"
          >
            <div class="card-header">
              <div class="event-title">
                <van-icon name="newspaper-o" size="16" color="#1989fa" />
                <span>{{ item.eventTitle }}</span>
              </div>
              <van-tag :type="getStatusTagType(item.status)" size="medium" round>
                {{ getStatusLabel(item.status) }}
              </van-tag>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="label">事件编号</span>
                <span class="value">{{ item.eventNo || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">转至机构</span>
                <span class="value highlight">{{ item.targetDeptName }}</span>
              </div>
              <div class="info-row">
                <span class="label">申请人</span>
                <span class="value">{{ item.applicantName }}</span>
                <span class="time">{{ formatTime(item.applicantTime) }}</span>
              </div>
              <div class="reason-row">
                <span class="label">转派原因</span>
                <span class="value">{{ item.transferReason }}</span>
              </div>
            </div>

            <div class="card-actions">
              <van-button
                v-if="item.canApprove && item.status === 'PENDING_APPROVAL'"
                type="success"
                size="small"
                round
                block
                @click.stop="handleApprove(item)"
              >
                审批
              </van-button>
              <van-button
                v-if="item.canReceive && item.status === 'TRANSFERRED'"
                type="warning"
                size="small"
                round
                block
                @click.stop="handleReceive(item)"
              >
                接收
              </van-button>
              <van-button
                v-if="item.canProcess && item.status === 'PROCESSING'"
                type="primary"
                size="small"
                round
                block
                @click.stop="handleProcess(item)"
              >
                处理
              </van-button>
              <van-button
                v-if="item.canComplete && item.status === 'PROCESSING'"
                type="success"
                size="small"
                round
                block
                @click.stop="handleComplete(item)"
              >
                办结
              </van-button>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>

    <div class="page-content" v-else>
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="!loading && list.length === 0" class="empty-wrapper">
            <van-empty description="暂无流转记录" :image-size="60" />
          </div>
          <div
            v-for="item in list"
            :key="item.id"
            class="transfer-card"
            @click="goToDetail(item.id)"
          >
            <div class="card-header">
              <div class="event-title">
                <van-icon name="newspaper-o" size="16" color="#1989fa" />
                <span>{{ item.eventTitle }}</span>
              </div>
              <van-tag :type="getStatusTagType(item.status)" size="medium" round>
                {{ getStatusLabel(item.status) }}
              </van-tag>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="label">转至</span>
                <span class="value highlight">{{ item.targetDeptName }}</span>
                <van-tag size="mini" type="primary" plain>
                  {{ item.targetTypeName }}
                </van-tag>
              </div>
              <div class="info-row">
                <span class="label">申请人</span>
                <span class="value">{{ item.applicantName }}</span>
                <span class="time">{{ formatTime(item.applicantTime) }}</span>
              </div>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>

    <van-dialog v-model:show="approveDialogVisible" title="审批流转申请" show-cancel-button>
      <div class="approve-content">
        <div class="approve-item">
          <span class="label">事件</span>
          <span class="value">{{ currentTransfer?.eventTitle }}</span>
        </div>
        <div class="approve-item">
          <span class="label">转至</span>
          <span class="value">{{ currentTransfer?.targetDeptName }}</span>
        </div>
        <div class="approve-item">
          <span class="label">原因</span>
          <span class="value">{{ currentTransfer?.transferReason }}</span>
        </div>
        <van-cell-group inset style="margin-top: 12px">
          <van-field
            v-model="approveForm.approved"
            name="approveResult"
            label="审批结果"
            :rules="[{ required: true, message: '请选择审批结果' }]"
          >
            <template #input>
              <van-radio-group v-model="approveForm.approved" direction="horizontal">
                <van-radio :name="true">通过</van-radio>
                <van-radio :name="false">驳回</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field
            v-model="approveForm.approveComment"
            label="审批意见"
            type="textarea"
            placeholder="请输入审批意见"
            autosize
          />
        </van-cell-group>
      </div>
      <template #confirm>
        <van-button type="primary" :loading="submitting" @click="submitApprove">
          确定
        </van-button>
      </template>
    </van-dialog>

    <van-dialog v-model:show="processDialogVisible" title="添加处理记录" show-cancel-button>
      <div class="process-content">
        <van-field
          v-model="processForm.processDescription"
          type="textarea"
          label="处理记录"
          placeholder="请输入处理记录"
          autosize
          :rows="4"
        />
      </div>
      <template #confirm>
        <van-button type="primary" :loading="submitting" @click="submitProcess">
          添加
        </van-button>
      </template>
    </van-dialog>

    <van-dialog v-model:show="completeDialogVisible" title="办结流转" show-cancel-button>
      <div class="complete-content">
        <van-cell-group inset>
          <van-field
            v-model="completeForm.processResult"
            name="processResult"
            label="处理结果"
            placeholder="请选择处理结果"
            :rules="[{ required: true, message: '请选择处理结果' }]"
          >
            <template #input>
              <van-picker
                :columns="completeResultOptions"
                @confirm="onResultConfirm"
              >
                <template #activator="{ selectedOptions }">
                  <div class="picker-activator">
                    {{ completeForm.processResult || '请选择' }}
                    <van-icon name="arrow" />
                  </div>
                </template>
              </van-picker>
            </template>
          </van-field>
          <van-field
            v-model="completeForm.processDescription"
            type="textarea"
            label="处理说明"
            placeholder="请详细说明处理过程和结果"
            autosize
            :rows="3"
          />
        </van-cell-group>
      </div>
      <template #confirm>
        <van-button type="primary" :loading="submitting" @click="submitComplete">
          办结
        </van-button>
      </template>
    </van-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showSuccessToast } from 'vant'
import {
  getMyInvolvedTransfers,
  getTransferPage,
  approveTransfer,
  receiveTransfer,
  processTransfer,
  completeTransfer
} from '@/api/crossStreetTransfer'

const router = useRouter()

const activeTab = ref('pending')
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const submitting = ref(false)
const list = ref([])
const pendingList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)

const approveDialogVisible = ref(false)
const processDialogVisible = ref(false)
const completeDialogVisible = ref(false)

const currentTransfer = ref(null)

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

const completeResultOptions = [
  { text: '已妥善处置', value: '已妥善处置' },
  { text: '已移交相关部门', value: '已移交相关部门' },
  { text: '已协调解决', value: '已协调解决' },
  { text: '需持续跟进', value: '需持续跟进' },
  { text: '其他', value: '其他' }
]

const onClickLeft = () => {
  router.back()
}

const goToApply = () => {
  router.push('/transfer-apply')
}

const goToDetail = (id) => {
  router.push(`/transfer-detail?id=${id}`)
}

const getStatusLabel = (status) => {
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

const getStatusTagType = (status) => {
  const map = {
    PENDING_APPROVAL: 'warning',
    APPROVED: 'primary',
    TRANSFERRED: 'primary',
    ACCEPTED: 'info',
    PROCESSING: 'info',
    COMPLETED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'default'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const onTabChange = () => {
  list.value = []
  pendingList.value = []
  pageNum.value = 1
  finished.value = false
  onLoad()
}

const onRefresh = () => {
  list.value = []
  pendingList.value = []
  pageNum.value = 1
  finished.value = false
  onLoad()
  refreshing.value = false
}

const onLoad = async () => {
  try {
    if (activeTab.value === 'pending') {
      const res = await getTransferPage({
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      const data = res.data?.list || []
      const filtered = data.filter(item =>
        item.status === 'PENDING_APPROVAL' ||
        item.status === 'TRANSFERRED' ||
        item.status === 'PROCESSING'
      )
      if (pageNum.value === 1) {
        pendingList.value = filtered
      } else {
        pendingList.value = [...pendingList.value, ...filtered]
      }
    } else if (activeTab.value === 'my') {
      const res = await getMyInvolvedTransfers({ status: '' })
      pendingList.value = res.data || []
      list.value = pendingList.value
    } else {
      const res = await getTransferPage({
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      const data = res.data?.list || []
      if (pageNum.value === 1) {
        list.value = data
      } else {
        list.value = [...list.value, ...data]
      }
    }

    if (activeTab.value === 'pending') {
      if (pendingList.value.length >= (res?.data?.total || 0)) {
        finished.value = true
      }
    } else if (activeTab.value === 'my') {
      finished.value = true
    } else {
      if ((res?.data?.list?.length || 0) < pageSize.value) {
        finished.value = true
      }
    }
    pageNum.value++
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = (row) => {
  currentTransfer.value = row
  approveForm.transferId = row.id
  approveForm.approved = true
  approveForm.approveComment = ''
  approveDialogVisible.value = true
}

const submitApprove = async () => {
  if (!approveForm.approved && !approveForm.approveComment) {
    showToast('请填写驳回理由')
    return
  }
  submitting.value = true
  try {
    await approveTransfer(approveForm)
    showSuccessToast(approveForm.approved ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    onRefresh()
  } catch (e) {
    showToast(e.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

const handleReceive = async (row) => {
  try {
    await showConfirmDialog({
      title: '确认接收',
      message: `确定要接收来自【${row.sourceDeptName}】的协作任务【${row.eventTitle}】吗？`,
      confirmButtonText: '确认接收'
    })
    await receiveTransfer(row.id)
    showSuccessToast('接收成功')
    onRefresh()
  } catch (e) {
    if (e !== 'cancel') {
      showToast(e.message || '接收失败')
    }
  }
}

const handleProcess = (row) => {
  currentTransfer.value = row
  processForm.transferId = row.id
  processForm.processDescription = ''
  processDialogVisible.value = true
}

const submitProcess = async () => {
  if (!processForm.processDescription) {
    showToast('请输入处理记录')
    return
  }
  submitting.value = true
  try {
    await processTransfer(processForm)
    showSuccessToast('添加成功')
    processDialogVisible.value = false
    onRefresh()
  } catch (e) {
    showToast(e.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const handleComplete = (row) => {
  currentTransfer.value = row
  completeForm.transferId = row.id
  completeForm.processResult = ''
  completeForm.processDescription = ''
  completeDialogVisible.value = true
}

const onResultConfirm = ({ selectedOptions }) => {
  completeForm.processResult = selectedOptions[0]?.value || ''
}

const submitComplete = async () => {
  if (!completeForm.processResult) {
    showToast('请选择处理结果')
    return
  }
  submitting.value = true
  try {
    await completeTransfer(completeForm)
    showSuccessToast('办结成功')
    completeDialogVisible.value = false
    onRefresh()
  } catch (e) {
    showToast(e.message || '办结失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  onLoad()
})
</script>

<style scoped lang="scss">
.transfer-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 20px;

  .page-content {
    padding: 12px;
  }

  .empty-wrapper {
    padding: 40px 0;
  }

  .transfer-card {
    background: #fff;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
      padding-bottom: 10px;
      border-bottom: 1px dashed #ebedf0;

      .event-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 15px;
        font-weight: 500;
        color: #323233;
        flex: 1;
        padding-right: 10px;
      }
    }

    .card-body {
      .info-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;
        font-size: 13px;

        .label {
          color: #969799;
          width: 60px;
          flex-shrink: 0;
        }

        .value {
          color: #323233;
          flex: 1;

          &.highlight {
            color: #1989fa;
            font-weight: 500;
          }
        }

        .time {
          color: #c8c9cc;
          font-size: 12px;
        }
      }

      .reason-row {
        display: flex;
        gap: 8px;
        margin-top: 8px;
        font-size: 13px;

        .label {
          color: #969799;
          width: 60px;
          flex-shrink: 0;
        }

        .value {
          color: #646566;
          flex: 1;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }
    }

    .card-actions {
      display: flex;
      gap: 8px;
      margin-top: 12px;
      padding-top: 10px;
      border-top: 1px solid #f2f3f5;

      :deep(.van-button) {
        font-size: 13px;
        padding: 0 12px;
        height: 34px;
      }
    }
  }

  .approve-content,
  .process-content,
  .complete-content {
    padding: 8px 0;

    .approve-item {
      display: flex;
      gap: 12px;
      margin-bottom: 10px;
      font-size: 14px;

      .label {
        color: #969799;
        width: 70px;
        flex-shrink: 0;
      }

      .value {
        color: #323233;
        flex: 1;
      }
    }
  }

  .picker-activator {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 4px;
    color: #323233;
    font-size: 14px;
  }
}
</style>
