<template>
  <div class="transfer-apply-page">
    <van-nav-bar fixed placeholder title="申请跨街道流转" @click-left="onClickLeft">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
    </van-nav-bar>

    <div class="page-content">
      <van-cell-group inset title="事件信息">
        <van-cell title="事件" :value="currentEvent?.title || '请选择事件'" is-link @click="selectEvent" />
        <van-cell title="事件编号" :value="currentEvent?.eventNo || '-'" />
        <van-cell title="事件类型" :value="currentEvent?.eventTypeName || '-'" />
        <van-cell title="所属网格" :value="currentEvent?.gridName || '-'" />
        <van-cell title="当前状态">
          <template #right-icon>
            <van-tag v-if="currentEvent" :type="getStatusTagType(currentEvent.status)">
              {{ getStatusLabel(currentEvent.status) }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset title="流转信息" style="margin-top: 16px">
        <van-field
          v-model="form.targetType"
          name="targetType"
          label="转派类型"
          placeholder="请选择"
          is-link
          readonly
          @click="showTargetTypePicker = true"
        >
          <template #right-icon>
            <van-tag v-if="form.targetType" size="small">
              {{ getTargetTypeLabel(form.targetType) }}
            </van-tag>
          </template>
        </van-field>

        <van-field
          v-model="form.targetDeptName"
          name="targetDept"
          label="协作机构"
          placeholder="请选择"
          is-link
          readonly
          @click="showDeptPicker = true"
        >
          <template #right-icon>
            <van-tag v-if="form.targetDeptId" type="primary" size="small">
              {{ form.targetDeptName }}
            </van-tag>
          </template>
        </van-field>

        <van-field
          v-model="form.urgencyLevel"
          name="urgencyLevel"
          label="紧急程度"
          placeholder="请选择"
          is-link
          readonly
          @click="showUrgencyPicker = true"
        >
          <template #right-icon>
            <van-tag v-if="form.urgencyLevel" :type="getUrgencyTagType(form.urgencyLevel)" size="small">
              {{ getUrgencyLabel(form.urgencyLevel) }}
            </van-tag>
          </template>
        </van-field>

        <van-field
          v-model="form.transferReason"
          type="textarea"
          label="转派原因"
          placeholder="请详细说明需要跨街道处理的原因"
          autosize
          :rows="4"
          maxlength="500"
          show-word-limit
        />

        <van-field
          v-model="form.crossBoundaryDescription"
          type="textarea"
          label="跨界描述"
          placeholder="描述事件跨界的具体情况"
          autosize
          :rows="2"
          maxlength="300"
          show-word-limit
        />

        <van-field
          v-model="form.impactRange"
          type="textarea"
          label="影响范围"
          placeholder="描述事件影响的区域范围"
          autosize
          :rows="2"
          maxlength="300"
          show-word-limit
        />

        <van-field
          v-model="form.coordinationNote"
          type="textarea"
          label="协作说明"
          placeholder="需要协作方特别注意的事项"
          autosize
          :rows="2"
          maxlength="300"
          show-word-limit
        />
      </van-cell-group>

      <van-cell-group inset title="推荐协作机构" style="margin-top: 16px">
        <div v-loading="recommendedLoading" class="recommend-wrapper">
          <div v-if="!currentEvent" class="tip-text">
            请先选择事件，系统将自动推荐匹配的协作机构
          </div>
          <div v-else-if="recommendedList.length === 0" class="tip-text">
            暂无推荐机构
          </div>
          <div v-else>
            <div
              v-for="item in recommendedList.slice(0, 5)"
              :key="item.id"
              class="recommend-item"
              :class="{ active: form.targetDeptId === item.id }"
              @click="selectRecommendedDept(item)"
            >
              <div class="recommend-header">
                <span class="dept-name">{{ item.name }}</span>
                <van-tag size="mini" :type="item.matchScore >= 80 ? 'success' : item.matchScore >= 60 ? 'warning' : 'primary'">
                  {{ item.matchScore }}% 匹配
                </van-tag>
              </div>
              <div class="recommend-body">
                <span class="reason">{{ item.matchReason }}</span>
                <span v-if="item.phone" class="phone">
                  <van-icon name="phone-o" size="12" /> {{ item.phone }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </van-cell-group>
    </div>

    <div class="footer-bar">
      <van-button type="primary" block size="large" :loading="submitting" @click="submit">
        提交流转申请
      </van-button>
    </div>

    <van-popup v-model:show="showTargetTypePicker" position="bottom" :style="{ height: '30%' }" round>
      <van-picker
        :columns="targetTypeOptions"
        @confirm="onTargetTypeConfirm"
        @cancel="showTargetTypePicker = false"
        :title="'选择转派类型'"
      />
    </van-popup>

    <van-popup v-model:show="showUrgencyPicker" position="bottom" :style="{ height: '30%' }" round>
      <van-picker
        :columns="urgencyOptions"
        @confirm="onUrgencyConfirm"
        @cancel="showUrgencyPicker = false"
        :title="'选择紧急程度'"
      />
    </van-popup>

    <van-popup v-model:show="showDeptPicker" position="bottom" :style="{ height: '60%' }" round>
      <div class="dept-picker-header">
        <div class="picker-tabs">
          <div
            v-for="type in targetTypeOptions"
            :key="type.value"
            class="picker-tab"
            :class="{ active: deptPickerTab === type.value }"
            @click="switchDeptPickerTab(type.value)"
          >
            {{ type.text }}
          </div>
        </div>
      </div>
      <div v-loading="deptTreeLoading" class="dept-picker-body">
        <van-tree-select
          :items="deptTreeItems"
          :main-active-index="deptMainActiveIndex"
          :active-id="form.targetDeptId"
          @click-nav="onDeptNavClick"
          @click-item="onDeptItemClick"
          height="80%"
        />
      </div>
      <div class="dept-picker-footer">
        <van-button type="primary" block @click="confirmDeptSelection">
          确定选择
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showEventPicker" position="bottom" :style="{ height: '70%' }" round>
      <div class="event-picker-header">
        <van-search
          v-model="eventSearchKeyword"
          placeholder="搜索事件标题"
          shape="round"
          @search="searchEvents"
        />
      </div>
      <div v-loading="eventPickerLoading" class="event-picker-body">
        <van-list
          v-model:loading="eventListLoading"
          :finished="eventListFinished"
          finished-text="没有更多了"
          @load="loadEventList"
        >
          <div
            v-for="event in eventList"
            :key="event.id"
            class="event-picker-item"
            :class="{ active: form.eventId === event.id }"
            @click="selectPickerEvent(event)"
          >
            <div class="event-title">{{ event.title }}</div>
            <div class="event-meta">
              <span>{{ event.eventNo }}</span>
              <van-tag size="mini" :type="getStatusTagType(event.status)">
                {{ getStatusLabel(event.status) }}
              </van-tag>
            </div>
          </div>
        </van-list>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { reactive, ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import {
  getEventList
} from '@/api/event'
import {
  applyTransfer,
  getRecommendedTargets,
  getCooperationDeptTree
} from '@/api/crossStreetTransfer'

const router = useRouter()
const route = useRoute()

const submitting = ref(false)
const recommendedLoading = ref(false)
const deptTreeLoading = ref(false)

const form = reactive({
  eventId: null,
  targetType: 'STREET',
  targetDeptId: null,
  targetDeptName: '',
  urgencyLevel: 'MEDIUM',
  transferReason: '',
  crossBoundaryDescription: '',
  impactRange: '',
  coordinationNote: '',
  attachments: []
})

const currentEvent = ref(null)
const recommendedList = ref([])

const showTargetTypePicker = ref(false)
const showUrgencyPicker = ref(false)
const showDeptPicker = ref(false)
const showEventPicker = ref(false)

const deptPickerTab = ref('STREET')
const deptTreeItems = ref([])
const deptMainActiveIndex = ref(0)

const eventSearchKeyword = ref('')
const eventPickerLoading = ref(false)
const eventListLoading = ref(false)
const eventListFinished = ref(false)
const eventList = ref([])
const eventPageNum = ref(1)
const eventPageSize = ref(20)

const targetTypeOptions = [
  { text: '相邻街道', value: 'STREET' },
  { text: '委办局', value: 'BUREAU' },
  { text: '区级部门', value: 'COUNTY' }
]

const urgencyOptions = [
  { text: '低', value: 'LOW' },
  { text: '普通', value: 'MEDIUM' },
  { text: '重要', value: 'HIGH' },
  { text: '紧急', value: 'URGENT' }
]

const onClickLeft = () => {
  router.back()
}

const getStatusLabel = (status) => {
  const map = {
    PENDING: '待受理',
    APPROVED: '已受理',
    DISPATCHED: '已分派',
    HANDLED: '已处置',
    COMPLETED: '已办结',
    REJECTED: '已驳回',
    TRANSFERRING: '流转审批中',
    TRANSFERRED: '已跨街道转派'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'primary',
    DISPATCHED: 'info',
    HANDLED: '',
    COMPLETED: 'success',
    REJECTED: 'danger',
    TRANSFERRING: 'warning',
    TRANSFERRED: 'danger'
  }
  return map[status] || 'info'
}

const getTargetTypeLabel = (type) => {
  const map = {
    STREET: '相邻街道',
    BUREAU: '委办局',
    COUNTY: '区级部门'
  }
  return map[type] || type
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

const selectEvent = () => {
  showEventPicker.value = true
  eventPageNum.value = 1
  eventList.value = []
  eventListFinished.value = false
  loadEventList()
}

const searchEvents = () => {
  eventPageNum.value = 1
  eventList.value = []
  eventListFinished.value = false
  loadEventList()
}

const loadEventList = async () => {
  eventListLoading.value = true
  try {
    const res = await getEventList({
      pageNum: eventPageNum.value,
      pageSize: eventPageSize.value,
      keyword: eventSearchKeyword.value,
      status: 'APPROVED'
    })
    const data = res.data?.list || []
    eventList.value = [...eventList.value, ...data]
    if (data.length < eventPageSize.value) {
      eventListFinished.value = true
    }
    eventPageNum.value++
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    eventListLoading.value = false
    eventPickerLoading.value = false
  }
}

const selectPickerEvent = (event) => {
  form.eventId = event.id
  currentEvent.value = event
  showEventPicker.value = false
  loadRecommendedTargets()
}

const selectRecommendedDept = (dept) => {
  form.targetDeptId = dept.id
  form.targetDeptName = dept.name
}

const onTargetTypeConfirm = ({ selectedOptions }) => {
  form.targetType = selectedOptions[0]?.value || 'STREET'
  showTargetTypePicker.value = false
  loadRecommendedTargets()
  loadDeptTree()
}

const onUrgencyConfirm = ({ selectedOptions }) => {
  form.urgencyLevel = selectedOptions[0]?.value || 'MEDIUM'
  showUrgencyPicker.value = false
}

const switchDeptPickerTab = (type) => {
  deptPickerTab.value = type
  form.targetType = type
  loadDeptTree()
}

const loadRecommendedTargets = async () => {
  if (!form.eventId) return
  recommendedLoading.value = true
  try {
    const res = await getRecommendedTargets(form.eventId, form.targetType)
    recommendedList.value = res.data || []
  } catch (e) {
    // ignore
  } finally {
    recommendedLoading.value = false
  }
}

const loadDeptTree = async () => {
  deptTreeLoading.value = true
  try {
    const res = await getCooperationDeptTree({ targetType: deptPickerTab.value })
    deptTreeItems.value = transformToTreeSelectItems(res.data || [])
  } catch (e) {
    // ignore
  } finally {
    deptTreeLoading.value = false
  }
}

const transformToTreeSelectItems = (tree) => {
  return tree.map(item => ({
    text: item.name,
    value: item.id,
    children: item.children?.map(child => ({
      text: child.name,
      value: child.id
    })) || []
  }))
}

const onDeptNavClick = (index) => {
  deptMainActiveIndex.value = index
}

const onDeptItemClick = (item) => {
  form.targetDeptId = item.value
  form.targetDeptName = item.text
}

const confirmDeptSelection = () => {
  if (!form.targetDeptId) {
    showToast('请选择协作机构')
    return
  }
  showDeptPicker.value = false
}

const submit = async () => {
  if (!form.eventId) {
    showToast('请选择事件')
    return
  }
  if (!form.targetDeptId) {
    showToast('请选择协作机构')
    return
  }
  if (!form.transferReason || form.transferReason.trim() === '') {
    showToast('请填写转派原因')
    return
  }

  submitting.value = true
  try {
    await applyTransfer({
      eventId: form.eventId,
      targetDeptId: form.targetDeptId,
      targetType: form.targetType,
      transferReason: form.transferReason,
      crossBoundaryDescription: form.crossBoundaryDescription,
      impactRange: form.impactRange,
      urgencyLevel: form.urgencyLevel,
      coordinationNote: form.coordinationNote,
      attachments: form.attachments
    })
    showSuccessToast('申请提交成功')
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (e) {
    showToast(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const eventId = route.query.eventId
  if (eventId) {
    form.eventId = Number(eventId)
    loadEventDetail(eventId)
    loadRecommendedTargets()
  }
  loadDeptTree()
})

async function loadEventDetail(eventId) {
  try {
    const res = await getEventList({ pageNum: 1, pageSize: 1 })
    const list = res.data?.list || []
    const event = list.find(e => e.id === Number(eventId))
    if (event) {
      currentEvent.value = event
    }
  } catch (e) {
    // ignore
  }
}
</script>

<style scoped lang="scss">
.transfer-apply-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 100px;

  .page-content {
    padding: 12px 0 20px;
  }

  .tip-text {
    text-align: center;
    padding: 30px 20px;
    color: #969799;
    font-size: 14px;
  }

  .recommend-wrapper {
    min-height: 80px;
    padding: 8px 0;

    .recommend-item {
      padding: 12px 16px;
      margin: 8px 16px;
      background: #f7f8fa;
      border: 2px solid transparent;
      border-radius: 10px;

      &.active {
        border-color: #1989fa;
        background: #ecf5ff;
      }

      .recommend-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        .dept-name {
          font-size: 15px;
          font-weight: 500;
          color: #323233;
        }
      }

      .recommend-body {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12px;

        .reason {
          color: #969799;
        }

        .phone {
          color: #646566;
        }
      }
    }
  }

  .footer-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 12px 16px;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  }

  .dept-picker-header {
    padding: 12px 16px 0;
    border-bottom: 1px solid #ebedf0;

    .picker-tabs {
      display: flex;
      gap: 8px;

      .picker-tab {
        flex: 1;
        text-align: center;
        padding: 10px 0;
        font-size: 14px;
        color: #646566;
        border-radius: 8px;
        transition: all 0.2s;

        &.active {
          background: #1989fa;
          color: #fff;
          font-weight: 500;
        }
      }
    }
  }

  .dept-picker-body {
    min-height: 300px;
  }

  .dept-picker-footer {
    padding: 12px 16px;
    border-top: 1px solid #ebedf0;
  }

  .event-picker-header {
    padding: 12px;
    border-bottom: 1px solid #ebedf0;
  }

  .event-picker-body {
    min-height: 300px;
    padding: 8px 0;

    .event-picker-item {
      padding: 14px 16px;
      border-bottom: 1px solid #f2f3f5;

      &.active {
        background: #ecf5ff;
      }

      .event-title {
        font-size: 14px;
        color: #323233;
        margin-bottom: 6px;
      }

      .event-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12px;
        color: #969799;
      }
    }
  }
}
</style>
