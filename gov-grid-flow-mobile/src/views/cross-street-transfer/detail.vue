<template>
  <div class="transfer-detail-page">
    <van-nav-bar fixed placeholder title="流转详情" @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
    </van-nav-bar>

    <div v-if="transfer" class="page-content">
      <div class="status-banner" :class="'status-' + transfer.status">
        <div class="status-tag">
          <van-tag :type="getStatusTagType(transfer.status)" size="large" round plain>
            {{ getStatusLabel(transfer.status) }}
          </van-tag>
        </div>
        <div class="status-tip">
          {{ getStatusTip(transfer.status) }}
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">
          <van-icon name="newspaper-o" size="16" color="#1989fa" />
          <span>事件信息</span>
        </div>
        <van-cell-group inset border>
          <van-cell title="事件标题" :value="transfer.eventTitle" />
          <van-cell title="事件编号" :value="transfer.eventNo || '-'" />
          <van-cell title="事件类型" :value="transfer.eventTypeName || '-'" />
          <van-cell title="发生地址" :value="transfer.address || '-'" />
          <van-cell title="紧急程度">
            <template #right-icon>
              <van-tag :type="getUrgencyTagType(transfer.urgencyLevel)" size="small">
                {{ getUrgencyLabel(transfer.urgencyLevel) }}
              </van-tag>
            </template>
          </van-cell>
        </van-cell-group>
      </div>

      <div class="section-card">
        <div class="section-title">
          <van-icon name="arrow-up" size="16" color="#07c160" />
          <span>流转信息</span>
        </div>
        <van-cell-group inset border>
          <van-cell title="转出机构" :value="transfer.sourceDeptName" />
          <van-cell title="转出网格" :value="transfer.sourceGridName || '-'" />
          <van-cell title="转入机构" :value="transfer.targetDeptName">
            <template #right-icon>
              <van-tag type="primary" size="small">
                {{ transfer.targetTypeName }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="转派原因" :value="transfer.transferReason || '-'">
            <template #value>
              <div class="multi-line-value">{{ transfer.transferReason || '-' }}</div>
            </template>
          </van-cell>
          <van-cell title="跨界描述" :value="transfer.crossBoundaryDescription || '-'">
            <template #value>
              <div class="multi-line-value">{{ transfer.crossBoundaryDescription || '-' }}</div>
            </template>
          </van-cell>
          <van-cell title="影响范围" :value="transfer.impactRange || '-'">
            <template #value>
              <div class="multi-line-value">{{ transfer.impactRange || '-' }}</div>
            </template>
          </van-cell>
          <van-cell title="协作说明" :value="transfer.coordinationNote || '-'">
            <template #value>
              <div class="multi-line-value">{{ transfer.coordinationNote || '-' }}</div>
            </template>
          </van-cell>
        </van-cell-group>
      </div>

      <div class="section-card">
        <div class="section-title">
          <van-icon name="friends-o" size="16" color="#ff976a" />
          <span>人员信息</span>
        </div>
        <van-cell-group inset border>
          <van-cell title="申请人">
            <template #right-icon>
              <div class="user-info">
                <span>{{ transfer.applicantName || '-' }}</span>
                <span class="time">{{ formatTime(transfer.applicantTime) }}</span>
              </div>
            </template>
          </van-cell>
          <van-cell v-if="transfer.approverName" title="审批人">
            <template #right-icon>
              <div class="user-info">
                <span>{{ transfer.approverName || '-' }}</span>
                <span class="time">{{ formatTime(transfer.approveTime) }}</span>
              </div>
            </template>
          </van-cell>
          <van-cell v-if="transfer.receiverName" title="接收人">
            <template #right-icon>
              <div class="user-info">
                <span>{{ transfer.receiverName || '-' }}</span>
                <span class="time">{{ formatTime(transfer.receiveTime) }}</span>
              </div>
            </template>
          </van-cell>
          <van-cell v-if="transfer.handlerName" title="处理人">
            <template #right-icon>
              <div class="user-info">
                <span>{{ transfer.handlerName || '-' }}</span>
              </div>
            </template>
          </van-cell>
        </van-cell-group>
      </div>

      <div v-if="transfer.processResult || transfer.processDescription" class="section-card">
        <div class="section-title">
          <van-icon name="checked" size="16" color="#67c23a" />
          <span>处理结果</span>
        </div>
        <van-cell-group inset border>
          <van-cell v-if="transfer.processResult" title="处理结果" :value="transfer.processResult" />
          <van-cell v-if="transfer.processDescription" title="处理说明">
            <template #value>
              <div class="multi-line-value">{{ transfer.processDescription }}</div>
            </template>
          </van-cell>
          <van-cell v-if="transfer.processEndTime" title="办结时间" :value="formatTime(transfer.processEndTime)" />
        </van-cell-group>
      </div>

      <div class="section-card">
        <div class="section-title">
          <van-icon name="cluster-o" size="16" color="#7232dd" />
          <span>流转追溯链</span>
        </div>
        <div v-if="transfer.traceList && transfer.traceList.length" class="timeline-wrapper">
          <van-steps :active="transfer.traceList.length" direction="vertical" active-color="#1989fa">
            <van-step
              v-for="(trace, index) in transfer.traceList"
              :key="trace.id || index"
              :active="index + 1"
            >
              <div class="step-content">
                <div class="step-header">
                  <span class="step-title">{{ trace.nodeName }}</span>
                  <span class="step-time">{{ formatTime(trace.operateTime) }}</span>
                </div>
                <div class="step-body">
                  <span v-if="trace.operatorName" class="step-operator">
                    <van-icon name="user-o" size="12" />
                    {{ trace.operatorName }}
                    <span v-if="trace.operatorDeptName">({{ trace.operatorDeptName }})</span>
                  </span>
                  <p v-if="trace.fromDeptName" class="step-transfer">
                    从【{{ trace.fromDeptName }}】
                    <span v-if="trace.toDeptName">转至【{{ trace.toDeptName }}】</span>
                  </p>
                  <p v-if="trace.comment" class="step-comment">{{ trace.comment }}</p>
                </div>
              </div>
            </van-step>
          </van-steps>
        </div>
        <van-empty v-else description="暂无追溯信息" :image-size="50" />
      </div>
    </div>

    <div v-else class="loading-wrapper">
      <van-loading type="spinner" size="32px" />
      <span style="margin-top: 12px; color: #969799">加载中...</span>
    </div>

    <div v-if="transfer" class="action-bar">
      <van-button
        v-if="transfer.canApprove && transfer.status === 'PENDING_APPROVAL'"
        type="success"
        block
        @click="handleApprove"
      >
        审批
      </van-button>
      <van-button
        v-if="transfer.canReceive && transfer.status === 'TRANSFERRED'"
        type="warning"
        block
        @click="handleReceive"
      >
        接收并开始处理
      </van-button>
      <van-button
        v-if="transfer.canProcess && transfer.status === 'PROCESSING'"
        type="primary"
        block
        @click="handleProcess"
      >
        添加处理记录
      </van-button>
      <van-button
        v-if="transfer.canComplete && transfer.status === 'PROCESSING'"
        type="success"
        block
        @click="handleComplete"
      >
        办结流转
      </van-button>
    </div>

    <van-dialog v-model:show="approveDialogVisible" title="审批流转申请" show-cancel-button>
      <div class="approve-content">
        <van-cell-group inset>
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
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import {
  getTransferDetail,
  approveTransfer,
  receiveTransfer,
  processTransfer,
  completeTransfer
} from '@/api/crossStreetTransfer'

const router = useRouter()
const route = useRoute()

const transfer = ref(null)
const loading = ref(false)
const submitting = ref(false)

const approveDialogVisible = ref(false)
const processDialogVisible = ref(false)
const completeDialogVisible = ref(false)

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

const getStatusTip = (status) => {
  const map = {
    PENDING_APPROVAL: '请等待上级部门审批',
    APPROVED: '审批已通过，等待接收',
    TRANSFERRED: '已转至协作机构，等待接收',
    ACCEPTED: '协作机构已接收',
    PROCESSING: '协作机构正在处理中',
    COMPLETED: '协作任务已完成',
    REJECTED: '流转申请已被驳回'
  }
  return map[status] || ''
}

const getUrgencyLabel = (level) => {
  const map = {
    LOW: '低',
    MEDIUM: '普通',
    HIGH: '重要',
    URGENT: '紧急'
  }
  return map[level] || '普通'
}

const getUrgencyTagType = (level) => {
  const map = {
    LOW: 'info',
    MEDIUM: 'success',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[level] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const loadDetail = async () => {
  loading.value = true
  try {
    const transferId = route.query.id
    if (!transferId) {
      showToast('参数错误')
      return
    }
    const res = await getTransferDetail(transferId)
    transfer.value = res.data
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = () => {
  approveForm.transferId = transfer.value.id
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
    loadDetail()
  } catch (e) {
    showToast(e.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

const handleReceive = async () => {
  try {
    await showConfirmDialog({
      title: '确认接收',
      message: `确定要接收来自【${transfer.value.sourceDeptName}】的协作任务吗？`,
      confirmButtonText: '确认接收'
    })
    await receiveTransfer(transfer.value.id)
    showSuccessToast('接收成功')
    loadDetail()
  } catch (e) {
    if (e !== 'cancel') {
      showToast(e.message || '接收失败')
    }
  }
}

const handleProcess = () => {
  processForm.transferId = transfer.value.id
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
    loadDetail()
  } catch (e) {
    showToast(e.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const handleComplete = () => {
  completeForm.transferId = transfer.value.id
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
    loadDetail()
  } catch (e) {
    showToast(e.message || '办结失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.transfer-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 100px;

  .page-content {
    padding: 12px;
  }

  .loading-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
  }

  .status-banner {
    background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 12px;
    text-align: center;

    &.status-PENDING_APPROVAL {
      background: linear-gradient(135deg, #fffbe6 0%, #fff3d0 100%);
    }

    &.status-TRANSFERRED {
      background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
    }

    &.status-PROCESSING {
      background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
    }

    &.status-COMPLETED {
      background: linear-gradient(135deg, #f0f9eb 0%, #d1edc4 100%);
    }

    &.status-REJECTED {
      background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
    }

    .status-tag {
      margin-bottom: 8px;
    }

    .status-tip {
      font-size: 13px;
      color: #606266;
    }
  }

  .section-card {
    background: #fff;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .section-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 15px;
      font-weight: 600;
      color: #323233;
      margin-bottom: 12px;
    }
  }

  .multi-line-value {
    color: #323233;
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    text-align: right;
  }

  .user-info {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;

    .time {
      font-size: 11px;
      color: #c8c9cc;
    }
  }

  .timeline-wrapper {
    padding: 8px 0;

    .step-content {
      padding-bottom: 16px;

      .step-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        .step-title {
          font-size: 14px;
          font-weight: 500;
          color: #323233;
        }

        .step-time {
          font-size: 12px;
          color: #969799;
        }
      }

      .step-body {
        .step-operator {
          font-size: 12px;
          color: #646566;
          display: block;
          margin-bottom: 4px;
        }

        .step-transfer {
          font-size: 13px;
          color: #1989fa;
          margin: 4px 0;
        }

        .step-comment {
          font-size: 13px;
          color: #646566;
          margin: 4px 0;
          line-height: 1.6;
        }
      }
    }
  }

  .action-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 12px 16px;
    display: flex;
    gap: 12px;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);

    :deep(.van-button) {
      border-radius: 24px;
      font-size: 15px;
      height: 46px;
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
