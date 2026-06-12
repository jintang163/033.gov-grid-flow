<template>
  <div class="detail-container">
    <van-nav-bar title="事件详情" left-arrow fixed placeholder @click-left="onBack" />

    <div v-if="loading" class="loading-wrap">
      <van-loading type="spinner" size="32px" color="#1989fa">加载中...</van-loading>
    </div>

    <div v-else-if="detail" class="detail-content">
      <van-card class="header-card">
        <template #title>
          <div class="header-top">
            <span class="event-no">{{ detail.eventNo || detail.id }}</span>
            <van-tag :type="statusTagType" size="medium">{{ statusText }}</van-tag>
          </div>
        </template>
        <template #desc>
          <div class="header-bottom">
            <div class="priority-item">
              <span class="label">优先级：</span>
              <van-tag :type="priorityTagType" round>{{ priorityText }}</van-tag>
            </div>
            <div class="time-item">
              <van-icon name="clock-o" size="14" />
              <span>{{ detail.reportTime || detail.createTime }}</span>
            </div>
          </div>
        </template>
      </van-card>

      <div class="section">
        <div class="section-title">基本信息</div>
        <van-cell-group inset>
          <van-cell title="事件标题" :value="detail.title" class="multi-line-value" />
          <van-cell title="事件类型" :value="detail.eventTypeText || detail.eventType" />
          <van-cell title="事件描述">
            <template #value>
              <div class="description-text">{{ detail.description }}</div>
            </template>
          </van-cell>
          <van-cell title="上报时间" :value="detail.reportTime || detail.createTime" />
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">位置信息</div>
        <van-cell-group inset>
          <van-cell title="详细地址" is-link @click="openMap">
            <template #value>
              <span class="address-text">{{ detail.address }}</span>
            </template>
            <template #right-icon>
              <van-icon name="location-o" size="18" color="#1989fa" />
            </template>
          </van-cell>
          <van-cell v-if="detail.lng && detail.lat" title="经纬度">
            <template #value>
              <span class="coord-text">
                经度 {{ detail.lng }} / 纬度 {{ detail.lat }}
              </span>
            </template>
          </van-cell>
        </van-cell-group>
        <div v-if="detail.address" class="map-preview" @click="openMap">
          <van-icon name="location" size="40" color="#07c160" />
          <span class="map-text">点击查看地图位置</span>
        </div>
      </div>

      <div v-if="imageList.length > 0 || videoList.length > 0 || voiceUrl" class="section">
        <div class="section-title">媒体资料</div>
        <van-cell-group inset>
          <div v-if="imageList.length > 0" class="media-item">
            <div class="media-label">现场照片</div>
            <div class="image-grid">
              <van-image
                v-for="(img, idx) in imageList"
                :key="'img-' + idx"
                :src="img"
                width="100"
                height="100"
                fit="cover"
                radius="6"
                @click="previewImage(idx)"
              />
            </div>
          </div>
          <div v-if="videoList.length > 0" class="media-item">
            <div class="media-label">现场视频</div>
            <div class="video-list">
              <div
                v-for="(video, idx) in videoList"
                :key="'video-' + idx"
                class="video-item"
              >
                <video
                  :src="video"
                  controls
                  class="video-player"
                  playsinline
                  webkit-playsinline
                />
              </div>
            </div>
          </div>
          <div v-if="voiceUrl" class="media-item">
            <div class="media-label">
              <van-icon name="voice" size="14" />
              <span>语音描述</span>
            </div>
            <div class="voice-player-wrap">
              <audio :src="voiceUrl" controls class="voice-player" />
              <div class="voice-hint">语音转写内容供核验参考</div>
            </div>
          </div>
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">上报人信息</div>
        <van-cell-group inset>
          <van-cell title="上报方式">
            <template #value>
              <van-tag v-if="detail.anonymous === 1 || detail.anonymous" type="warning" plain>匿名上报</van-tag>
              <van-tag v-else type="primary" plain>实名上报</van-tag>
            </template>
          </van-cell>
          <van-cell v-if="!(detail.anonymous === 1 || detail.anonymous)" title="姓名" :value="detail.reporterName || detail.reporter || '-' " />
          <van-cell v-if="!(detail.anonymous === 1 || detail.anonymous)" title="联系电话" :value="detail.reporterPhone || detail.phone || '-' " />
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">处理流程</div>
        <van-cell-group inset>
          <div class="timeline-wrap">
            <div
              v-for="(node, index) in processList"
              :key="index"
              class="timeline-node"
              :class="{ 'is-last': index === processList.length - 1 }"
            >
              <div class="timeline-dot" :class="getNodeClass(node)"></div>
              <div v-if="index !== processList.length - 1" class="timeline-line" :class="getNodeClass(node)"></div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="node-name">{{ node.nodeName }}</span>
                  <van-tag v-if="node.status" :type="getNodeTagType(node)" size="small">{{ getNodeStatusText(node) }}</van-tag>
                </div>
                <div v-if="node.handlerName" class="timeline-handler">
                  <van-icon name="user-o" size="12" />
                  <span>{{ node.handlerName }}</span>
                  <span v-if="node.handleTime" class="handle-time">{{ node.handleTime }}</span>
                </div>
                <div v-if="node.comment" class="timeline-comment">
                  <div class="comment-label">处理意见：</div>
                  <div class="comment-text">{{ node.comment }}</div>
                </div>
                <div v-if="node.attachments && node.attachments.length > 0" class="timeline-attachments">
                  <div class="attach-label">附件：</div>
                  <div class="attach-list">
                    <template v-for="(att, attIdx) in parseAttachments(node.attachments)" :key="attIdx">
                      <van-image
                        v-if="isImageFile(att)"
                        :src="att"
                        width="60"
                        height="60"
                        fit="cover"
                        radius="4"
                        @click="previewAttachment(parseAttachments(node.attachments), attIdx)"
                      />
                      <a
                        v-else
                        :href="att"
                        target="_blank"
                        class="attach-link"
                      >
                        <van-icon name="description" size="16" />
                        <span>附件{{ attIdx + 1 }}</span>
                      </a>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </van-cell-group>
      </div>

      <!-- 周边资源调度 -->
      <van-cell-group v-if="detail.lng" inset title="周边资源调度（500米）" style="margin-top:12px">
        <van-cell title="周边摄像头" :label="`${nearbyData.cameraCount}个`">
          <template #icon><van-icon name="eye-o" color="#1989fa" size="20"/></template>
          <template #right-icon>
            <van-tag color="#e6f7ff" text-color="#1989fa" size="medium">查看</van-tag>
          </template>
        </van-cell>
        <div v-if="nearbyData.cameras.length" class="nearby-grid">
          <div v-for="cam in nearbyData.cameras.slice(0,4)" :key="cam.id" class="nearby-item cam-item">
            <div class="item-head">
              <van-icon name="eye-o" color="#1989fa"/>
              <span class="item-name">{{ cam.cameraName }}</span>
            </div>
            <div class="item-meta">
              <span>{{ cam.distance }}米</span>
              <span :class="cam.status===1?'tag-green':'tag-gray'">
                {{ cam.status===1?'在线':'离线' }}
              </span>
            </div>
            <a v-if="cam.hlsUrl" :href="cam.hlsUrl" target="_blank" class="play-link">
              <van-icon name="play-circle-o"/> 查看直播
            </a>
          </div>
        </div>

        <van-cell title="应急物资点" :label="`${nearbyData.emergencyCount}处`">
          <template #icon><van-icon name="fire-o" color="#ee0a24" size="20"/></template>
        </van-cell>
        <div v-if="nearbyData.emergencies.length" class="nearby-grid">
          <div v-for="item in nearbyData.emergencies.slice(0,4)" :key="item.id" class="nearby-item em-item">
            <div class="item-head">
              <van-icon name="fire-o" color="#ee0a24"/>
              <span class="item-name">{{ item.resourceName }}</span>
              <van-tag size="mini" type="warning">x{{ item.quantity }}</van-tag>
            </div>
            <div class="item-meta">
              <span>{{ item.distance }}米</span>
              <span class="tag-type">{{ item.resourceTypeName }}</span>
            </div>
            <div v-if="item.managerPhone" class="call-manager">
              <a :href="`tel:${item.managerPhone}`" @click.stop>
                <van-icon name="phone-o"/> 联系管理员 {{ item.manager }}
              </a>
            </div>
          </div>
        </div>

        <van-cell title="附近网格员" :label="`${nearbyData.memberCount}人在岗`" @click="showCallSheet=true">
          <template #icon><van-icon name="friends-o" color="#07c160" size="20"/></template>
          <template #right-icon>
            <van-button size="small" type="success" round plain @click.stop="showCallSheet=true">
              <van-icon name="phone-o"/> 一键呼叫
            </van-button>
          </template>
        </van-cell>
        <div v-if="nearbyData.members.length" class="nearby-grid">
          <div v-for="mem in nearbyData.members.slice(0,4)" :key="mem.userId" class="nearby-item mem-item">
            <van-avatar :size="48" color="#07c160">{{ mem.userName.charAt(0) }}</van-avatar>
            <div class="mem-info">
              <div class="mem-name">{{ mem.userName }}</div>
              <div class="mem-meta">
                <span>{{ mem.distance }}米</span>
                <span>⚡{{ mem.battery }}%</span>
                <span class="tag-green">在岗</span>
              </div>
            </div>
            <a :href="`tel:${mem.phone}`" class="call-btn" @click.stop>
              <van-icon name="phone-o" size="20"/>
            </a>
          </div>
        </div>
      </van-cell-group>

      <div v-if="showActions" class="action-section">
        <template v-if="canVerify">
          <van-button block round type="success" size="large" @click="handleVerify">
            核查通过
          </van-button>
          <van-button block round plain type="danger" size="large" style="margin-top: 12px" @click="openReturnDialog('verify')">
            不合格退回
          </van-button>
        </template>

        <template v-else-if="canApprove">
          <van-button block round type="success" size="large" @click="handleApprove">
            审核通过
          </van-button>
          <van-button block round plain type="danger" size="large" style="margin-top: 12px" @click="openRejectDialog">
            驳回
          </van-button>
        </template>

        <template v-else-if="canProcess">
          <van-button block round type="success" size="large" @click="handleProcess">
            处置完成
          </van-button>
          <van-button block round plain type="warning" size="large" style="margin-top: 12px" @click="openReturnDialog('process')">
            退回重办
          </van-button>
        </template>

        <template v-else-if="canAssign">
          <van-button block round type="primary" size="large" @click="openAssignPopup">
            分派任务
          </van-button>
        </template>

        <template v-else-if="canEvaluate">
          <van-button block round type="warning" size="large" @click="openEvaluatePopup">
            事件评价
          </van-button>
        </template>
      </div>

      <div class="bottom-placeholder"></div>
    </div>

    <van-popup v-model:show="showAssign" round position="bottom" :style="{ height: '60%' }">
      <div class="popup-header">
        <div class="popup-title">选择处置人员</div>
        <van-icon name="cross" size="22" @click="showAssign = false" />
      </div>
      <div class="popup-content">
        <van-cell-group v-if="memberList.length > 0">
          <van-cell
            v-for="member in memberList"
            :key="member.id"
            :title="member.realName || member.name || member.username"
            :label="member.phone || member.roleText || ''"
            is-link
            @click="confirmAssign(member)"
          >
            <template #icon>
              <van-icon name="user-circle-o" size="32" color="#1989fa" />
            </template>
          </van-cell>
        </van-cell-group>
        <van-empty v-else description="暂无可用人员" />
      </div>
    </van-popup>

    <van-popup v-model:show="showEvaluate" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">事件评价</div>
        <van-icon name="cross" size="22" @click="showEvaluate = false" />
      </div>
      <div class="evaluate-content">
        <van-cell-group inset>
          <van-cell title="处理速度">
            <template #value>
              <van-rate v-model="evaluation.speed" count="5" size="24" />
            </template>
          </van-cell>
          <van-cell title="处理效果">
            <template #value>
              <van-rate v-model="evaluation.effect" count="5" size="24" />
            </template>
          </van-cell>
          <van-field
            v-model="evaluation.comment"
            label="评价内容"
            type="textarea"
            rows="3"
            autosize
            maxlength="200"
            placeholder="请输入评价内容（选填）"
            show-word-limit
          />
        </van-cell-group>
        <div class="evaluate-actions">
          <van-button block round type="warning" size="large" :loading="evaluating" @click="submitEvaluationForm">
            提交评价
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showReturn" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">{{ returnType === 'verify' ? '不合格退回' : '退回重办' }}</div>
        <van-icon name="cross" size="22" @click="showReturn = false" />
      </div>
      <div class="return-content">
        <van-cell-group inset>
          <van-field
            v-model="returnForm.reason"
            label="退回原因"
            type="textarea"
            rows="4"
            autosize
            maxlength="500"
            placeholder="请输入退回原因"
            show-word-limit
            :rules="[{ required: true, message: '请输入退回原因' }]"
          />
        </van-cell-group>
        <div class="return-actions">
          <van-button block round type="danger" size="large" :loading="returnLoading" @click="submitReturn">
            确认退回
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showReject" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">审核驳回</div>
        <van-icon name="cross" size="22" @click="showReject = false" />
      </div>
      <div class="return-content">
        <van-cell-group inset>
          <van-field
            v-model="rejectForm.reason"
            label="驳回原因"
            type="textarea"
            rows="4"
            autosize
            maxlength="500"
            placeholder="请输入驳回原因"
            show-word-limit
            :rules="[{ required: true, message: '请输入驳回原因' }]"
          />
        </van-cell-group>
        <div class="return-actions">
          <van-button block round type="danger" size="large" :loading="rejectLoading" @click="submitReject">
            确认驳回
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 呼叫网格员 ActionSheet -->
    <van-action-sheet v-model:show="showCallSheet" title="选择呼叫的网格员" :actions="nearbyData.members.map(m=>({
      name: `${m.userName}（${m.distance}米）`,
      subname: `${m.phone} · 电量${m.battery}%`,
      callback: () => handleCall(m)
    }))">
    </van-action-sheet>

    <van-image-preview
      v-model:show="previewVisible"
      :images="previewImages"
      :start-position="previewIndex"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showImagePreview } from 'vant'
import {
  getEventDetail,
  approveEvent,
  verifyEvent,
  processEvent,
  returnEvent,
  rejectEvent,
  assignEvent,
  submitEvaluation as apiSubmitEvaluation,
  getMemberList,
  getNearbyResources,
  callMember
} from '@/api'
import { useUserStore } from '@/store'
import { getFullFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const detail = ref(null)

const showAssign = ref(false)
const showEvaluate = ref(false)
const showReturn = ref(false)
const showReject = ref(false)
const returnType = ref('verify')

const evaluating = ref(false)
const returnLoading = ref(false)
const rejectLoading = ref(false)

const memberList = ref([])
const previewVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

const evaluation = ref({
  speed: 5,
  effect: 5,
  comment: ''
})

const returnForm = ref({
  reason: ''
})

const rejectForm = ref({
  reason: ''
})

const nearbyLoading = ref(false)
const nearbyData = reactive({
  radius: 500,
  cameraCount: 0,
  emergencyCount: 0,
  memberCount: 0,
  cameras: [],
  emergencies: [],
  members: []
})
const showCallSheet = ref(false)
const showResourceMap = ref(false)

const statusMap = {
  PENDING: { text: '待受理', tag: 'warning' },
  APPROVED: { text: '已受理', tag: 'primary' },
  DISPATCHED: { text: '已分派', tag: 'primary' },
  HANDLED: { text: '已处置', tag: 'success' },
  COMPLETED: { text: '已办结', tag: 'success' },
  REJECTED: { text: '已驳回', tag: 'danger' }
}

const priorityMap = {
  LOW: { text: '低', tag: 'default' },
  NORMAL: { text: '一般', tag: 'primary' },
  HIGH: { text: '紧急', tag: 'warning' },
  URGENT: { text: '特急', tag: 'danger' }
}

const statusText = computed(() => {
  const s = detail.value?.status || ''
  if (statusMap[s]) return statusMap[s].text
  return s || '未知'
})

const statusTagType = computed(() => {
  const s = detail.value?.status || ''
  if (statusMap[s]) return statusMap[s].tag
  return 'default'
})

const priorityText = computed(() => {
  const p = detail.value?.priority || ''
  if (priorityMap[p]) return priorityMap[p].text
  return p || '一般'
})

const priorityTagType = computed(() => {
  const p = detail.value?.priority || ''
  if (priorityMap[p]) return priorityMap[p].tag
  return 'primary'
})

const imageList = computed(() => {
  if (!detail.value?.images) return []
  let list = []
  if (Array.isArray(detail.value.images)) {
    list = detail.value.images
  } else {
    list = String(detail.value.images).split(',').filter(Boolean)
  }
  return list.map(url => getFullFileUrl(url))
})

const videoList = computed(() => {
  if (!detail.value?.videos) return []
  let list = []
  if (Array.isArray(detail.value.videos)) {
    list = detail.value.videos
  } else {
    list = String(detail.value.videos).split(',').filter(Boolean)
  }
  return list.map(url => getFullFileUrl(url))
})

const voiceUrl = computed(() => {
  const url = detail.value?.voiceUrl || ''
  return getFullFileUrl(url)
})

const processList = computed(() => {
  if (detail.value?.processList && Array.isArray(detail.value.processList) && detail.value.processList.length > 0) {
    return detail.value.processList
  }
  return buildDefaultProcessList()
})

const currentUserRole = computed(() => userStore.userRole || '')
const currentUserId = computed(() => userStore.userInfo?.id || '')

const canVerify = computed(() => {
  return currentUserRole.value === 'worker' && detail.value?.status === 'HANDLED'
})

const canApprove = computed(() => {
  return currentUserRole.value === 'grid_leader' && detail.value?.status === 'PENDING'
})

const canProcess = computed(() => {
  return currentUserRole.value === 'handler' && detail.value?.status === 'DISPATCHED'
})

const canAssign = computed(() => {
  const isAdmin = currentUserRole.value === 'admin' || currentUserRole.value === 'grid_leader'
  return isAdmin && detail.value?.status === 'APPROVED'
})

const canEvaluate = computed(() => {
  const isReporter = detail.value?.reporterId === currentUserId.value ||
    detail.value?.reporterPhone === userStore.userPhone
  return detail.value?.status === 'COMPLETED' && isReporter
})

const showActions = computed(() => {
  return canVerify.value || canApprove.value || canProcess.value || canAssign.value || canEvaluate.value
})

const buildDefaultProcessList = () => {
  const d = detail.value
  const list = []
  const statusOrder = ['PENDING', 'APPROVED', 'DISPATCHED', 'HANDLED', 'COMPLETED']
  const currentIdx = statusOrder.indexOf(d?.status)
  list.push({
    nodeName: '事件上报',
    handlerName: d?.reporterName || d?.reporter || '匿名用户',
    handleTime: d?.reportTime || d?.createTime,
    status: 'COMPLETED',
    comment: d?.description ? '事件已提交' : '',
    attachments: null
  })
  if (d?.approveTime || currentIdx >= 1) {
    list.push({
      nodeName: '网格长受理',
      handlerName: d?.approverName || '-',
      handleTime: d?.approveTime || '待受理',
      status: d?.approveTime ? 'COMPLETED' : 'PENDING',
      comment: d?.approveComment || '',
      attachments: null
    })
  }
  if (d?.assignTime || currentIdx >= 2) {
    list.push({
      nodeName: '任务分派',
      handlerName: d?.assignerName || '-',
      handleTime: d?.assignTime || '待分派',
      status: d?.assignTime ? 'COMPLETED' : 'PENDING',
      comment: d?.assignComment || (d?.handlerName ? `分派给：${d.handlerName}` : ''),
      attachments: null
    })
  }
  if (d?.processTime || currentIdx >= 3) {
    list.push({
      nodeName: '事件处置',
      handlerName: d?.handlerName || '-',
      handleTime: d?.processTime || '待处置',
      status: d?.processTime ? 'COMPLETED' : 'PENDING',
      comment: d?.processComment || '',
      attachments: d?.processAttachments || null
    })
  }
  if (d?.status === 'COMPLETED') {
    list.push({
      nodeName: '事件办结',
      handlerName: '系统',
      handleTime: d?.finishTime || d?.completeTime,
      status: 'COMPLETED',
      comment: d?.evaluationScore ? `已评价：${d.evaluationScore}分` : '事件已完成',
      attachments: null
    })
  }
  if (d?.status === 'REJECTED') {
    list.push({
      nodeName: '已驳回',
      handlerName: d?.approverName || '-',
      handleTime: d?.rejectTime || d?.approveTime,
      status: 'REJECTED',
      comment: d?.rejectComment || d?.approveComment || '',
      attachments: null
    })
  }
  return list
}

const onBack = () => router.back()

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getEventDetail(route.params.id)
    detail.value = res.data
    if (detail.value.lng && detail.value.lat) {
      await fetchNearbyResources(detail.value.lng, detail.value.lat)
    }
  } catch (e) {
    console.warn('Load event detail failed, using mock data', e)
    detail.value = getMockDetail()
    if (detail.value.lng && detail.value.lat) {
      await fetchNearbyResources(detail.value.lng, detail.value.lat)
    }
  } finally {
    loading.value = false
  }
}

async function fetchNearbyResources(lng, lat) {
  nearbyLoading.value = true
  try {
    const res = await getNearbyResources({ lng, lat, radius: 500 })
    if (res.data) {
      Object.assign(nearbyData, res.data)
    }
  } catch (e) {
    console.warn('周边资源查询失败', e)
  } finally {
    nearbyLoading.value = false
  }
}

async function handleCall(member) {
  try {
    await callMember(member.userId)
  } catch(e) {}
  if (member.phone) {
    window.location.href = `tel:${member.phone}`
  } else {
    showToast('该网格员暂无手机号')
  }
}

const getMockDetail = () => ({
  id: route.params.id || 'GD20240115001',
  eventNo: 'GD20240115001',
  title: '小区门口垃圾堆积未清理',
  eventType: 'environment',
  eventTypeText: '环境卫生',
  priority: 'HIGH',
  status: 'HANDLED',
  description: 'XX小区东门门口垃圾堆积未及时清理，已有3天时间，天气炎热产生异味，严重影响居民出行和生活环境，请相关部门尽快处理。',
  voiceUrl: '',
  createTime: '2024-01-15 10:30:25',
  reportTime: '2024-01-15 10:30:25',
  address: '浙江省杭州市西湖区XX街道XX小区东门',
  lng: '120.123456',
  lat: '30.256789',
  images: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg,https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
  videos: '',
  anonymous: 0,
  reporterName: '张三',
  reporterPhone: '138****8888',
  reporterId: userStore.userInfo?.id,
  processList: [
    {
      nodeName: '事件上报',
      handlerName: '张三',
      handleTime: '2024-01-15 10:30:25',
      status: 'COMPLETED',
      comment: '小区门口垃圾堆积，已提交事件上报。',
      attachments: null
    },
    {
      nodeName: '网格员核查',
      handlerName: '李网格员',
      handleTime: '2024-01-15 10:45:12',
      status: 'COMPLETED',
      comment: '现场核查情况属实，垃圾数量约3袋，建议环卫部门尽快处理。',
      attachments: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
    },
    {
      nodeName: '网格长审核',
      handlerName: '-',
      handleTime: '',
      status: 'PENDING',
      comment: '',
      attachments: null
    }
  ]
})

const openMap = () => {
  if (!detail.value?.address && !detail.value?.lng) {
    showToast('位置信息不完整')
    return
  }
  const { lng, lat, address } = detail.value
  if (lng && lat) {
    window.open(`https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(address || '事件位置')}`, '_blank')
  } else {
    window.open(`https://uri.amap.com/search?keyword=${encodeURIComponent(address)}`, '_blank')
  }
}

const previewImage = (index) => {
  previewImages.value = imageList.value
  previewIndex.value = index
  previewVisible.value = true
}

const previewAttachment = (attachments, index) => {
  const images = attachments.filter(a => isImageFile(a))
  if (images.length > 0) {
    showImagePreview({
      images,
      startPosition: images.indexOf(attachments[index]) >= 0 ? images.indexOf(attachments[index]) : 0
    })
  }
}

const isImageFile = (url) => {
  if (!url) return false
  return /\.(jpg|jpeg|png|gif|bmp|webp|svg)(\?.*)?$/i.test(url)
}

const parseAttachments = (attachments) => {
  if (!attachments) return []
  if (Array.isArray(attachments)) return attachments
  return String(attachments).split(',').filter(Boolean)
}

const getNodeClass = (node) => {
  const s = node?.status || ''
  if (s === 'COMPLETED') return 'node-completed'
  if (s === 'PENDING') return 'node-processing'
  if (s === 'REJECTED') return 'node-rejected'
  return 'node-pending'
}

const getNodeTagType = (node) => {
  const s = node?.status || ''
  if (s === 'COMPLETED') return 'success'
  if (s === 'PENDING') return 'primary'
  if (s === 'REJECTED') return 'danger'
  return 'default'
}

const getNodeStatusText = (node) => {
  const map = {
    COMPLETED: '已完成',
    PENDING: '进行中',
    REJECTED: '已驳回'
  }
  return map[node?.status] || '待处理'
}

const handleVerify = async () => {
  try {
    await showConfirmDialog({
      title: '确认核查通过',
      message: '确认该事件核查无误，提交审核？'
    })
    const res = await verifyEvent({
      eventId: detail.value.id,
      passed: true
    })
    showToast({ type: 'success', message: '核查通过' })
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleApprove = async () => {
  try {
    await showConfirmDialog({
      title: '确认审核通过',
      message: '确认该事件审核通过，进入分派流程？'
    })
    const res = await approveEvent({
      eventId: detail.value.id,
      passed: true
    })
    showToast({ type: 'success', message: '审核通过' })
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleProcess = async () => {
  try {
    await showConfirmDialog({
      title: '确认处置完成',
      message: '确认该事件已处置完成？'
    })
    const res = await processEvent({
      eventId: detail.value.id,
      completed: true
    })
    showToast({ type: 'success', message: '处置完成' })
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const openReturnDialog = (type) => {
  returnType.value = type
  returnForm.value.reason = ''
  showReturn.value = true
}

const submitReturn = async () => {
  if (!returnForm.value.reason.trim()) {
    showToast('请输入退回原因')
    return
  }
  returnLoading.value = true
  try {
    const api = returnType.value === 'verify' ? verifyEvent : processEvent
    const res = await api({
      eventId: detail.value.id,
      passed: false,
      completed: false,
      reason: returnForm.value.reason,
      comment: returnForm.value.reason
    })
    showToast({ type: 'success', message: '已退回' })
    showReturn.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '操作失败，请重试')
  } finally {
    returnLoading.value = false
  }
}

const openRejectDialog = () => {
  rejectForm.value.reason = ''
  showReject.value = true
}

const submitReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    showToast('请输入驳回原因')
    return
  }
  rejectLoading.value = true
  try {
    const res = await rejectEvent({
      eventId: detail.value.id,
      reason: rejectForm.value.reason,
      comment: rejectForm.value.reason
    })
    showToast({ type: 'success', message: '已驳回' })
    showReject.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '操作失败，请重试')
  } finally {
    rejectLoading.value = false
  }
}

const openAssignPopup = async () => {
  showAssign.value = true
  memberList.value = []
  try {
    const gridId = detail.value?.gridId
    const res = await getMemberList(gridId)
    if (res.data && Array.isArray(res.data)) {
      memberList.value = res.data
    } else {
      memberList.value = [
        { id: 1, realName: '王处置', phone: '139****1111', roleText: '处置员' },
        { id: 2, realName: '赵工', phone: '139****2222', roleText: '处置员' }
      ]
    }
  } catch (e) {
    console.warn('Load member list failed, using mock', e)
    memberList.value = [
      { id: 1, realName: '王处置', phone: '139****1111', roleText: '处置员' },
      { id: 2, realName: '赵工', phone: '139****2222', roleText: '处置员' }
    ]
  }
}

const confirmAssign = async (member) => {
  try {
    await showConfirmDialog({
      title: '确认分派',
      message: `确定将该事件分派给「${member.realName || member.name || member.username}」？`
    })
    const res = await assignEvent({
      eventId: detail.value.id,
      handlerId: member.id,
      handlerName: member.realName || member.name || member.username
    })
    showToast({ type: 'success', message: '分派成功' })
    showAssign.value = false
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const openEvaluatePopup = () => {
  evaluation.value = {
    speed: 5,
    effect: 5,
    comment: ''
  }
  showEvaluate.value = true
}

const submitEvaluationForm = async () => {
  evaluating.value = true
  try {
    const res = await apiSubmitEvaluation({
      eventId: detail.value.id,
      speedScore: evaluation.value.speed,
      effectScore: evaluation.value.effect,
      comment: evaluation.value.comment
    })
    showToast({ type: 'success', message: '评价提交成功' })
    showEvaluate.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '评价提交失败，请重试')
  } finally {
    evaluating.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.detail-container {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.loading-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.detail-content {
  padding: 12px 0;
}

.header-card {
  margin: 0 16px 12px;
  border-radius: 12px;

  :deep(.van-card__header) {
    padding: 20px 16px;
  }

  :deep(.van-card__footer) {
    display: none;
  }
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.event-no {
  font-size: 15px;
  font-weight: 600;
}

.header-bottom {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.priority-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #646566;

  .label {
    margin-right: 4px;
  }
}

.time-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #969799;
  gap: 4px;
}

.section {
  margin-top: 12px;
}

.section-title {
  padding: 12px 16px;
  font-size: 14px;
  color: #646566;
  font-weight: 600;
}

.multi-line-value {
  :deep(.van-cell__value) {
    max-width: 60%;
    word-break: break-all;
    line-height: 1.5;
  }
}

.description-text {
  font-size: 14px;
  color: #323233;
  line-height: 1.6;
  padding: 4px 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.address-text {
  color: #323233;
}

.coord-text {
  font-size: 13px;
  color: #969799;
}

.map-preview {
  margin: 12px 16px;
  height: 140px;
  background: linear-gradient(135deg, #e8f7ef 0%, #d4f0e3 100%);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;

  .map-text {
    font-size: 14px;
    color: #07c160;
    font-weight: 500;
  }
}

.media-item {
  padding: 12px 16px;

  & + .media-item {
    border-top: 1px solid #f2f3f5;
  }
}

.media-label {
  font-size: 13px;
  color: #646566;
  margin-bottom: 10px;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.video-item {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.video-player {
  width: 100%;
  max-height: 220px;
  display: block;
}

.voice-player-wrap {
  margin-top: 8px;

  .voice-player {
    width: 100%;
    height: 40px;
    margin-bottom: 6px;
  }

  .voice-hint {
    font-size: 12px;
    color: #969799;
    text-align: center;
  }
}

.timeline-wrap {
  padding: 8px 0;
}

.timeline-node {
  position: relative;
  display: flex;
  padding: 12px 16px 12px 36px;

  &.is-last {
    .timeline-line {
      display: none;
    }
  }
}

.timeline-dot {
  position: absolute;
  left: 16px;
  top: 16px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  z-index: 2;

  &.node-completed {
    background-color: #07c160;
    box-shadow: 0 0 0 2px #07c160;
  }

  &.node-processing {
    background-color: #1989fa;
    box-shadow: 0 0 0 2px #1989fa;
    animation: pulse 2s infinite;
  }

  &.node-pending {
    background-color: #fff;
    box-shadow: 0 0 0 2px #dcdee0;
  }

  &.node-rejected {
    background-color: #ee0a24;
    box-shadow: 0 0 0 2px #ee0a24;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.timeline-line {
  position: absolute;
  left: 21px;
  top: 28px;
  bottom: 0;
  width: 2px;

  &.node-completed {
    background: #07c160;
  }

  &.node-processing {
    background: linear-gradient(to bottom, #1989fa 0%, #dcdee0 100%);
  }

  &.node-pending,
  &.node-rejected {
    background: #dcdee0;
  }
}

.timeline-content {
  flex: 1;
  padding-bottom: 8px;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
}

.timeline-handler {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #646566;
  margin-bottom: 8px;
  flex-wrap: wrap;

  .handle-time {
    color: #969799;
  }
}

.timeline-comment {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;

  .comment-label {
    font-size: 12px;
    color: #969799;
    margin-bottom: 4px;
  }

  .comment-text {
    font-size: 13px;
    color: #323233;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.timeline-attachments {
  .attach-label {
    font-size: 12px;
    color: #969799;
    margin-bottom: 6px;
  }

  .attach-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .attach-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 10px;
    background: #f0f9ff;
    border-radius: 4px;
    font-size: 12px;
    color: #1989fa;
    text-decoration: none;
  }
}

.action-section {
  padding: 24px 16px 8px;
  position: sticky;
  bottom: 0;
  background: rgba(247, 248, 250, 0.95);
  backdrop-filter: blur(8px);
}

.bottom-placeholder {
  height: env(safe-area-inset-bottom);
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f2f3f5;

  .popup-title {
    font-size: 16px;
    font-weight: 600;
    color: #323233;
  }
}

.popup-content {
  max-height: calc(60vh - 60px);
  overflow-y: auto;
}

.evaluate-content {
  padding-bottom: 20px;
}

.evaluate-actions {
  padding: 20px 16px 0;
}

.return-content {
  padding-bottom: 20px;
}

.return-actions {
  padding: 20px 16px 0;
}

.nearby-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 10px 16px;
  background: #fafbfc;
}
.nearby-item {
  background: #fff;
  border-radius: 8px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  .item-head {
    display: flex; align-items: center; gap: 6px;
    margin-bottom: 6px; font-weight: 500;
    .item-name { font-size: 13px; color: #333; }
  }
  .item-meta {
    display: flex; gap: 8px; font-size: 12px; color: #969799;
    margin-bottom: 4px;
  }
}
.tag-green { color:#07c160; background:#eefbf3; padding:1px 6px; border-radius:4px; font-size:11px; }
.tag-gray { color:#969799; background:#f2f3f5; padding:1px 6px; border-radius:4px; font-size:11px; }
.tag-type { color:#7232dd; background:#f5efff; padding:1px 6px; border-radius:4px; font-size:11px; }
.play-link { font-size: 12px; color: #1989fa; text-decoration: none; }
.call-manager a { font-size: 12px; color: #1989fa; text-decoration: none; }
.mem-item {
  display: flex; align-items: center; gap: 10px;
  .mem-info { flex:1; .mem-name {font-weight:500;font-size:13px;} .mem-meta{font-size:11px;color:#969799;display:flex;gap:6px;margin-top:2px;}}
  .call-btn { width:36px; height:36px; border-radius:50%; background:#07c160; display:flex; align-items:center; justify-content:center; color:#fff; }
}
</style>
