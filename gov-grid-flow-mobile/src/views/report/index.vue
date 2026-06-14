<template>
  <div class="report-container">
    <van-nav-bar title="事件上报" left-arrow fixed placeholder @click-left="onBack">
      <template #right>
        <div class="nav-right">
          <span
            v-if="offlineStore.isOnline"
            class="status-dot online"
          ></span>
          <span v-else class="status-dot offline"></span>
          <span v-if="offlineStore.pendingCount > 0" class="pending-badge" @click="onManualSync">
            {{ offlineStore.pendingCount }}待同步
          </span>
        </div>
      </template>
    </van-nav-bar>

    <div class="report-content">
      <van-form ref="formRef" @submit="onSubmit">
        <van-cell-group inset title="基本信息">
          <van-field
            v-model="form.title"
            label="事件标题"
            placeholder="请输入事件标题"
            :rules="[{ required: true, message: '请输入事件标题' }]"
          />
          <van-field
            v-model="form.eventTypeText"
            is-link
            readonly
            label="事件类型"
            placeholder="请选择事件类型"
            :rules="[{ required: true, message: '请选择事件类型' }]"
            @click="showTypePicker = true"
          />
          <van-field
            v-model="form.priorityText"
            is-link
            readonly
            label="事件等级"
            placeholder="请选择事件等级"
            :rules="[{ required: true, message: '请选择事件等级' }]"
            @click="showPriorityPicker = true"
          />
          <van-field
            v-if="gridList.length > 1"
            v-model="form.gridName"
            is-link
            readonly
            label="所属网格"
            placeholder="请选择网格"
            :rules="[{ required: true, message: '请选择所属网格' }]"
            @click="showGridPicker = true"
          />
        </van-cell-group>

        <van-cell-group v-if="nlpRecommendation || nlpClassifying" inset title="AI智能推荐处置部门" style="margin-top: 12px">
          <div v-if="nlpClassifying" class="nlp-loading">
            <van-loading size="16px" color="#1989fa" />
            <span>AI正在分析事件内容...</span>
          </div>
          <div v-else-if="nlpRecommendation" class="nlp-recommend-card">
            <div class="nlp-recommend-header">
              <van-icon name="guide-o" size="18" color="#1989fa" />
              <span class="nlp-recommend-label">推荐处置部门</span>
              <van-tag :type="nlpRecommendation.autoDispatch ? 'success' : 'warning'" size="small" round>
                {{ nlpRecommendation.autoDispatch ? '高置信度' : '待确认' }}
              </van-tag>
            </div>
            <div class="nlp-recommend-body">
              <div class="nlp-dept-name">{{ nlpRecommendation.departmentName }}</div>
              <div class="nlp-confidence">
                置信度：<span :class="nlpRecommendation.confidence >= 0.8 ? 'high' : 'low'">
                  {{ (nlpRecommendation.confidence * 100).toFixed(1) }}%
                </span>
                <van-tag size="mini" type="primary" plain>{{ nlpRecommendation.method === 'rule' ? '规则匹配' : nlpRecommendation.method === 'model' ? '模型推理' : '混合' }}</van-tag>
              </div>
            </div>
            <div class="nlp-recommend-actions">
              <van-button size="small" type="primary" round @click="adoptNlpRecommendation">
                一键采纳
              </van-button>
            </div>
          </div>
        </van-cell-group>

        <van-cell-group inset title="位置信息" style="margin-top: 12px">
          <van-field
            v-model="form.address"
            is-link
            readonly
            label="详细地址"
            placeholder="点击获取当前位置"
            :rules="[{ required: true, message: '请获取位置信息' }]"
            @click="getLocation"
          >
            <template #right-icon>
              <van-icon name="location-o" size="18" color="#1989fa" />
            </template>
          </van-field>
          <div v-if="form.address && showMap" class="map-preview">
            <div class="map-coords">
              <span>经度: {{ form.lng }}</span>
              <span>纬度: {{ form.lat }}</span>
            </div>
            <div class="map-hint">位置已标注，可在地图上调整（高德地图SDK）</div>
          </div>
        </van-cell-group>

        <van-cell-group v-if="form.lng" inset title="周边资源（500米内）" style="margin-top: 12px">
          <van-cell title="摄像头" :value="`${nearbyResources.cameraCount}个`" is-link>
            <template #icon><van-icon name="eye-o" color="#1989fa" /></template>
          </van-cell>
          <div v-if="nearbyResources.cameraCount>0" class="resource-list">
            <div v-for="cam in nearbyResources.cameras.slice(0,3)" :key="cam.id" class="resource-item">
              <span class="res-name">{{ cam.cameraName }}</span>
              <span class="res-distance">{{ cam.distance }}米</span>
              <span :class="['res-status', cam.status===1?'online':'offline']">
                {{ cam.status===1?'在线':'离线' }}
              </span>
            </div>
          </div>

          <van-cell title="应急物资" :value="`${nearbyResources.emergencyCount}处`" is-link>
            <template #icon><van-icon name="fire-o" color="#ee0a24" /></template>
          </van-cell>
          <div v-if="nearbyResources.emergencyCount>0" class="resource-list">
            <div v-for="em in nearbyResources.emergencies.slice(0,3)" :key="em.id" class="resource-item">
              <span class="res-name">{{ em.resourceName }}({{ em.quantity }})</span>
              <span class="res-distance">{{ em.distance }}米</span>
              <span class="res-type">{{ em.resourceTypeName }}</span>
            </div>
          </div>

          <van-cell title="附近网格员" :value="`${nearbyResources.memberCount}人在岗`" is-link>
            <template #icon><van-icon name="friends-o" color="#07c160" /></template>
          </van-cell>
          <div v-if="nearbyResources.memberCount>0" class="resource-list">
            <div v-for="m in nearbyResources.members.slice(0,3)" :key="m.userId" class="resource-item">
              <span class="res-name">{{ m.userName }}</span>
              <span class="res-distance">{{ m.distance }}米</span>
              <a :href="`tel:${m.phone}`" class="call-link" @click.stop>
                <van-icon name="phone-o" />呼叫
              </a>
            </div>
          </div>

          <div class="map-preview-panel">
            <div class="map-legend">
              <span><i class="dot camera"></i>摄像头</span>
              <span><i class="dot emergency"></i>应急物资</span>
              <span><i class="dot member"></i>网格员</span>
              <span><i class="dot event"></i>事件点</span>
            </div>
            <div class="map-canvas-hint">
              接入高德地图SDK后可展示图标分布（高德 JS API 2.0 AMap.Marker）
            </div>
          </div>
        </van-cell-group>

        <van-cell-group inset title="事件描述" style="margin-top: 12px">
          <van-field
            v-model="form.description"
            rows="4"
            autosize
            label="描述"
            type="textarea"
            maxlength="500"
            placeholder="请详细描述事件情况（最多500字），也可点击右侧语音按钮录入"
            show-word-limit
            :rules="[{ required: true, message: '请输入事件描述' }]"
          />
          <div class="voice-input-wrap">
            <div class="voice-input-hint">
              <van-icon name="phone-o" size="14" />
              <span>支持语音输入，说完自动转文字</span>
            </div>
            <div class="voice-record-area">
              <div v-if="isRecording" class="recording-status">
                <div class="recording-pulse"></div>
                <span class="recording-time">{{ formatTime(recordingTime) }}</span>
              </div>
              <div v-else-if="voiceUrl" class="voice-preview">
                <audio :src="voiceUrl" controls class="voice-player" />
                <van-button size="small" type="danger" plain @click="clearVoice">
                  <van-icon name="delete-o" />删除
                </van-button>
              </div>
              <div v-else class="record-btn-wrap">
                <van-button
                  type="primary"
                  round
                  size="normal"
                  icon="microphone"
                  @touchstart.prevent="startRecording"
                  @touchend.prevent="stopRecording"
                  @mousedown.prevent="startRecording"
                  @mouseup.prevent="stopRecording"
                  @mouseleave.prevent="stopRecording"
                >
                  按住说话
                </van-button>
                <div class="record-tip">最长支持60秒语音</div>
              </div>
            </div>
            <div v-if="transcribing" class="transcribing-hint">
              <van-loading size="16px" color="#1989fa" />
              <span>语音转文字中...</span>
            </div>
          </div>
        </van-cell-group>

        <van-cell-group inset title="附件上传" style="margin-top: 12px">
          <div class="uploader-wrapper">
            <van-field label="现场照片">
              <template #input>
                <div class="uploader-container">
                  <van-uploader
                    v-model="imageList"
                    multiple
                    :max-count="9"
                    :before-read="beforeImageRead"
                    :after-read="afterImageRead"
                    @delete="onImageDelete"
                    accept="image/*"
                  />
                </div>
              </template>
            </van-field>
            <van-field label="现场视频">
              <template #input>
                <div class="uploader-container">
                  <van-uploader
                    v-model="videoList"
                    multiple
                    :max-count="3"
                    :before-read="beforeVideoRead"
                    :after-read="afterVideoRead"
                    @delete="onVideoDelete"
                    accept="video/*"
                  />
                </div>
              </template>
            </van-field>
          </div>
          <div class="upload-hint">
            * 图片自动添加水印（上报时间、网格员姓名、事件编号），MD5存证防篡改
            <br />* 视频暂不支持可视化水印叠加，将进行MD5存证并支持加密存储
          </div>
        </van-cell-group>

        <van-cell-group inset title="安全设置" style="margin-top: 12px">
          <van-field name="sensitive">
            <template #input>
              <div class="anonymous-wrap">
                <span class="anonymous-label">敏感证据加密</span>
                <van-switch v-model="watermarkInfo.sensitive" :active-value="true" :inactive-value="false" />
              </div>
            </template>
          </van-field>
          <van-field
            v-if="watermarkInfo.sensitive"
            v-model="watermarkInfo.targetDeptName"
            is-link
            readonly
            label="目标处置部门"
            placeholder="请选择可解密查看的处置部门"
            :rules="watermarkInfo.sensitive ? [{ required: true, message: '请选择目标处置部门' }] : []"
            @click="showDeptPicker = true"
          />
          <div class="sensitive-hint" v-if="watermarkInfo.sensitive">
            <van-icon name="info-o" size="12" color="#1989fa" />
            <span>启用后，附件将使用数字信封加密，仅您选择的「{{ watermarkInfo.targetDeptName || '处置部门' }}」可解密查看</span>
          </div>

          <van-field name="blockchain">
            <template #input>
              <div class="anonymous-wrap">
                <div class="blockchain-label-wrap">
                  <span class="anonymous-label">区块链存证</span>
                  <van-tag v-if="isHighRiskEvent" size="mini" type="danger" round>建议开启</van-tag>
                </div>
                <van-switch v-model="form.blockchainEnabled" :active-value="1" :inactive-value="0" />
              </div>
            </template>
          </van-field>
          <div class="blockchain-hint">
            <van-icon name="info-o" size="12" color="#07c160" />
            <span>开启后，图片、视频、GPS等证据哈希将同步至司法联盟链，生成不可篡改的存证证书</span>
          </div>
        </van-cell-group>

        <van-cell-group inset title="上报人信息" style="margin-top: 12px">
          <van-field name="anonymous">
            <template #input>
              <div class="anonymous-wrap">
                <span class="anonymous-label">匿名上报</span>
                <van-switch v-model="form.anonymous" :active-value="1" :inactive-value="0" />
              </div>
            </template>
          </van-field>
          <van-field
            v-if="form.anonymous === 0"
            v-model="form.reporterName"
            label="姓名"
            placeholder="请输入姓名"
            :rules="form.anonymous === 0 ? [{ required: true, message: '请输入姓名' }] : []"
          />
          <van-field
            v-if="form.anonymous === 0"
            v-model="form.reporterPhone"
            label="联系电话"
            placeholder="请输入联系电话"
            type="tel"
            :rules="form.anonymous === 0 ? [
              { required: true, message: '请输入联系电话' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ] : []"
          />
        </van-cell-group>

        <div class="submit-btn-wrap">
          <van-button round block type="primary" size="large" native-type="submit" :loading="submitting">
            {{ offlineStore.isOnline ? '提交上报' : '暂存并上报（离线）' }}
          </van-button>
          <van-button
            round
            block
            plain
            type="warning"
            size="large"
            style="margin-top: 12px"
            @click="onSaveOffline"
            :disabled="submitting"
          >
            <van-icon name="down" />保存到本地（离线）
          </van-button>
          <van-button
            v-if="offlineStore.pendingCount > 0"
            round
            block
            plain
            type="success"
            size="large"
            style="margin-top: 12px"
            @click="onManualSync"
            :loading="offlineStore.syncing"
          >
            <van-icon name="exchange" />立即同步（{{ offlineStore.pendingCount }}条待同步）
          </van-button>
          <van-button
            round
            block
            plain
            type="default"
            size="large"
            style="margin-top: 12px"
            @click="onReset"
          >
            重置
          </van-button>
        </div>
      </van-form>
    </div>

    <van-popup v-model:show="showTypePicker" round position="bottom">
      <van-picker
        :columns="typeColumns"
        title="选择事件类型"
        @confirm="onTypeConfirm"
        @cancel="showTypePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showPriorityPicker" round position="bottom">
      <van-picker
        :columns="priorityColumns"
        title="选择事件等级"
        @confirm="onPriorityConfirm"
        @cancel="showPriorityPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showGridPicker" round position="bottom">
      <van-picker
        :columns="gridColumns"
        title="选择所属网格"
        @confirm="onGridConfirm"
        @cancel="showGridPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showDeptPicker" round position="bottom">
      <van-picker
        :columns="deptColumns"
        title="选择目标处置部门"
        @confirm="onDeptConfirm"
        @cancel="showDeptPicker = false"
      />
    </van-popup>

    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/report" icon="add-o">上报</van-tabbar-item>
      <van-tabbar-item to="/todo" icon="todo-list-o">待办</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { reportEvent, reportEventAnonymous, uploadFile, getEventTypeList, getGridList, getNearbyResources, transcribeVoice, getDeptList, nlpClassify, isHighRiskEventType } from '@/api'
import { uploadWithWatermark, linkEventToWatermark } from '@/api/watermark'
import { addImageWatermark, addVideoWatermark, calculateMD5 } from '@/utils/watermark'
import { getCurrentLocation, getAddressByLngLat } from '@/utils/amap'
import { useOfflineStore, useUserStore } from '@/store'
import { generateClientId } from '@/utils/offlineDB'
import dayjs from 'dayjs'

const router = useRouter()
const offlineStore = useOfflineStore()
const userStore = useUserStore()
const active = ref(1)
const formRef = ref(null)
const submitting = ref(false)

let deviceId = localStorage.getItem('device_id')
if (!deviceId) {
  deviceId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  localStorage.setItem('device_id', deviceId)
}

onMounted(() => {
  offlineStore.refresh()
  fetchEventTypeList()
  fetchGridList()
  fetchDeptList()
})
const showTypePicker = ref(false)
const showPriorityPicker = ref(false)
const showGridPicker = ref(false)
const showDeptPicker = ref(false)
const showMap = ref(false)
const imageList = ref([])
const videoList = ref([])
const uploadedImages = ref([])
const uploadedVideos = ref([])
const gridList = ref([])
const gridColumns = ref([])
const deptList = ref([])
const deptColumns = ref([])

const nearbyResources = reactive({
  cameras: [],
  emergencies: [],
  members: [],
  cameraCount: 0,
  emergencyCount: 0,
  memberCount: 0,
  loading: false
})

const watermarkInfo = reactive({
  eventNo: '',
  enabled: true,
  sensitive: false,
  targetDeptId: null,
  targetDeptName: ''
})

const isRecording = ref(false)
const recordingTime = ref(0)
const transcribing = ref(false)
const voiceUrl = ref('')
const voiceFile = ref(null)
let mediaRecorder = null
let audioChunks = []
let recordingTimer = null
const MAX_RECORDING_TIME = 60

const nlpRecommendation = ref(null)
const nlpClassifying = ref(false)
let nlpDebounceTimer = null

const form = reactive({
  title: '',
  eventType: '',
  eventTypeText: '',
  priority: 'NORMAL',
  priorityText: '一般',
  description: '',
  lng: null,
  lat: null,
  address: '',
  anonymous: 0,
  reporterName: '',
  reporterPhone: '',
  gridId: null,
  gridName: '',
  blockchainEnabled: 0
})

const isHighRiskEvent = ref(false)

const typeColumns = ref([
  { text: '环境卫生', value: 'environment' },
  { text: '市政设施', value: 'public_facility' },
  { text: '矛盾纠纷', value: 'dispute' },
  { text: '安全隐患', value: 'safety_hazard' },
  { text: '治安问题', value: 'security' },
  { text: '民生服务', value: 'service' },
  { text: '交通出行', value: 'traffic' },
  { text: '其他问题', value: 'other' }
])

const priorityColumns = [
  { text: '一般', value: 'NORMAL' },
  { text: '紧急', value: 'HIGH' },
  { text: '特急', value: 'URGENT' },
  { text: '低', value: 'LOW' }
]

onUnmounted(() => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  if (nlpDebounceTimer) {
    clearTimeout(nlpDebounceTimer)
  }
})

watch([() => form.title, () => form.description, () => form.eventType], () => {
  if (nlpDebounceTimer) clearTimeout(nlpDebounceTimer)
  nlpDebounceTimer = setTimeout(() => {
    triggerNlpClassify()
  }, 800)
})

const triggerNlpClassify = async () => {
  if (!form.title || form.title.trim().length < 2) {
    nlpRecommendation.value = null
    return
  }
  nlpClassifying.value = true
  try {
    const res = await nlpClassify({
      title: form.title,
      description: form.description || '',
      eventType: form.eventType || ''
    })
    if (res.data) {
      nlpRecommendation.value = res.data
    }
  } catch (e) {
    console.warn('NLP分类失败', e)
    nlpRecommendation.value = null
  } finally {
    nlpClassifying.value = false
  }
}

const adoptNlpRecommendation = () => {
  if (!nlpRecommendation.value) return
  watermarkInfo.targetDeptName = nlpRecommendation.value.departmentName
  showToast({ type: 'success', message: `已采纳推荐：${nlpRecommendation.value.departmentName}` })
}

watch([() => form.lng, () => form.lat], ([newLng, newLat]) => {
  if (newLng && newLat) {
    fetchNearbyResources(newLng, newLat)
  }
})

const fetchEventTypeList = async () => {
  try {
    const res = await getEventTypeList()
    if (res.data && res.data.length) {
      typeColumns.value = res.data.map((item) => ({
        text: item.name,
        value: item.code || item.value
      }))
    }
  } catch (e) {
    console.warn('Load event types failed, use defaults')
  }
}

const fetchGridList = async () => {
  try {
    const res = await getGridList()
    if (res.data && res.data.length) {
      gridList.value = res.data
      gridColumns.value = res.data.map((item) => ({
        text: item.gridName,
        value: item.id
      }))
      if (res.data.length === 1) {
        form.gridId = res.data[0].id
        form.gridName = res.data[0].gridName
      }
    }
  } catch (e) {
    console.warn('Load grid list failed', e)
  }
}

const fetchDeptList = async () => {
  try {
    const res = await getDeptList()
    const list = res?.data || res?.rows || []
    if (list && list.length) {
      const processDepts = list.filter(d => d.deptType === 'PROCESS' || d.type === '处置' || (d.name && d.name.includes('处置')))
      const finalList = processDepts.length > 0 ? processDepts : list
      deptList.value = finalList
      deptColumns.value = finalList.map((item) => ({
        text: item.deptName || item.name || item.dept_name,
        value: item.id || item.deptId || item.dept_id
      }))
      if (finalList.length === 1) {
        watermarkInfo.targetDeptId = finalList[0].id || finalList[0].deptId
        watermarkInfo.targetDeptName = finalList[0].deptName || finalList[0].name
      }
    } else {
      deptColumns.value = [
        { text: '城东处置中心', value: 3 },
        { text: '城西处置中心', value: 4 },
        { text: '综合执法大队', value: 5 }
      ]
    }
  } catch (e) {
    console.warn('Load dept list failed, use defaults', e)
    deptColumns.value = [
      { text: '城东处置中心', value: 3 },
      { text: '城西处置中心', value: 4 },
      { text: '综合执法大队', value: 5 }
    ]
  }
}

const onBack = async () => {
  const hasContent = form.title || form.description || imageList.value.length > 0 || videoList.value.length > 0
  if (hasContent) {
    try {
      await showConfirmDialog({
        title: '提示',
        message: '内容尚未提交，确定要离开吗？'
      })
      router.back()
    } catch {}
  } else {
    router.back()
  }
}

const onTypeConfirm = ({ selectedOptions }) => {
  form.eventType = selectedOptions[0].value
  form.eventTypeText = selectedOptions[0].text
  showTypePicker.value = false
  checkHighRiskType(selectedOptions[0].value)
  triggerNlpClassify()
}

const checkHighRiskType = async (eventType) => {
  if (!eventType) {
    isHighRiskEvent.value = false
    return
  }
  try {
    const res = await isHighRiskEventType(eventType)
    const highRisk = res?.data === true
    isHighRiskEvent.value = highRisk
    if (highRisk && form.blockchainEnabled === 0) {
      form.blockchainEnabled = 1
    }
  } catch (e) {
    const highRiskTypes = ['security', 'dispute', 'safety_hazard', 'public_security']
    isHighRiskEvent.value = highRiskTypes.includes(eventType)
    if (isHighRiskEvent.value && form.blockchainEnabled === 0) {
      form.blockchainEnabled = 1
    }
  }
}

const onPriorityConfirm = ({ selectedOptions }) => {
  form.priority = selectedOptions[0].value
  form.priorityText = selectedOptions[0].text
  showPriorityPicker.value = false
}

const onGridConfirm = ({ selectedOptions }) => {
  form.gridId = selectedOptions[0].value
  form.gridName = selectedOptions[0].text
  showGridPicker.value = false
}

const onDeptConfirm = ({ selectedOptions }) => {
  watermarkInfo.targetDeptId = selectedOptions[0].value
  watermarkInfo.targetDeptName = selectedOptions[0].text
  showDeptPicker.value = false
}

const getLocation = async () => {
  try {
    showToast({ message: '正在获取位置...', type: 'loading', duration: 0 })
    const location = await getCurrentLocation()
    const { lng, lat } = location.position
    form.lng = Number(lng.toFixed(6))
    form.lat = Number(lat.toFixed(6))
    const address = await getAddressByLngLat(lng, lat)
    form.address = address.formattedAddress || `${lng}, ${lat}`
    showMap.value = true
    fetchNearbyResources(form.lng, form.lat)
    showToast({ type: 'success', message: '位置获取成功' })
  } catch (e) {
    console.error(e)
    showToast('获取位置失败，请手动输入或检查定位权限')
  }
}

const fetchNearbyResources = async (lng, lat) => {
  try {
    nearbyResources.loading = true
    const res = await getNearbyResources({ lng, lat, radius: 500 })
    const data = res.data || {}
    nearbyResources.cameras = data.cameras || []
    nearbyResources.emergencies = data.emergencies || []
    nearbyResources.members = data.members || []
    nearbyResources.cameraCount = data.cameraCount || 0
    nearbyResources.emergencyCount = data.emergencyCount || 0
    nearbyResources.memberCount = data.memberCount || 0
  } catch (e) {
    console.error('Fetch nearby resources failed', e)
    nearbyResources.cameras = []
    nearbyResources.emergencies = []
    nearbyResources.members = []
    nearbyResources.cameraCount = 0
    nearbyResources.emergencyCount = 0
    nearbyResources.memberCount = 0
  } finally {
    nearbyResources.loading = false
  }
}

const compressImage = (file) => {
  return new Promise((resolve) => {
    if (file.size < 2 * 1024 * 1024) {
      resolve(file)
      return
    }
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target.result
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let { width, height } = img
        const maxSize = 1280
        if (width > height && width > maxSize) {
          height = (height * maxSize) / width
          width = maxSize
        } else if (height > maxSize) {
          width = (width * maxSize) / height
          height = maxSize
        }
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob(
          (blob) => {
            resolve(new File([blob], file.name, { type: 'image/jpeg' }))
          },
          'image/jpeg',
          0.8
        )
      }
    }
  })
}

const beforeImageRead = async (file) => {
  if (file.type && !file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过10MB')
    return false
  }
  return true
}

const getReporterInfo = () => {
  const reporterName = form.anonymous === 0 ? form.reporterName : '匿名用户'
  const reporterId = form.anonymous === 0 ? userStore.userId : null
  return { reporterName, reporterId }
}

const afterImageRead = async (file) => {
  const files = Array.isArray(file) ? file : [file]
  const { reporterName, reporterId } = getReporterInfo()
  const reportTime = dayjs().format('YYYY-MM-DD HH:mm:ss')

  for (const f of files) {
    try {
      f.status = 'uploading'
      f.message = '添加水印中...'

      const watermarkResult = await addImageWatermark(f.file, {
        reportTime,
        reporterName,
        eventNo: watermarkInfo.eventNo
      })

      f.message = '上传中...'
      const res = await uploadWithWatermark(
        watermarkResult.file,
        reportTime,
        reporterName,
        watermarkInfo.eventNo,
        null,
        reporterId,
        watermarkInfo.sensitive,
        watermarkInfo.sensitive ? watermarkInfo.targetDeptId : null
      )

      const data = res.data || {}
      if (data.fileUrl) {
        uploadedImages.value.push(data.fileUrl)
        f.status = 'done'
        f.message = watermarkInfo.sensitive ? '已加水印+加密' : '已加水印'
      } else {
        f.status = 'failed'
        f.message = '上传失败'
      }
    } catch (e) {
      console.error('图片水印上传失败:', e)
      f.status = 'failed'
      f.message = e.message || '上传失败'
    }
  }
}

const onImageDelete = (file, detail) => {
  uploadedImages.value.splice(detail.index, 1)
}

const beforeVideoRead = (file) => {
  if (file.type && !file.type.startsWith('video/')) {
    showToast('请选择视频文件')
    return false
  }
  if (file.size > 100 * 1024 * 1024) {
    showToast('视频大小不能超过100MB')
    return false
  }
  return true
}

const afterVideoRead = async (file) => {
  const files = Array.isArray(file) ? file : [file]
  const { reporterName, reporterId } = getReporterInfo()
  const reportTime = dayjs().format('YYYY-MM-DD HH:mm:ss')

  for (const f of files) {
    try {
      f.status = 'uploading'
      f.message = 'MD5存证中...'

      const watermarkResult = await addVideoWatermark(f.file, {
        reportTime,
        reporterName,
        eventNo: watermarkInfo.eventNo
      })

      f.message = '上传中...'
      const res = await uploadWithWatermark(
        watermarkResult.file,
        reportTime,
        reporterName,
        watermarkInfo.eventNo,
        null,
        reporterId,
        watermarkInfo.sensitive,
        watermarkInfo.sensitive ? watermarkInfo.targetDeptId : null
      )

      const data = res.data || {}
      if (data.fileUrl) {
        uploadedVideos.value.push(data.fileUrl)
        f.status = 'done'
        f.message = watermarkInfo.sensitive ? '已存证+加密' : '已存证'
      } else {
        f.status = 'failed'
        f.message = '上传失败'
      }
    } catch (e) {
      console.error('视频上传失败:', e)
      f.status = 'failed'
      f.message = e.message || '上传失败'
    }
  }
}

const onVideoDelete = (file, detail) => {
  uploadedVideos.value.splice(detail.index, 1)
}

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const startRecording = async () => {
  if (isRecording.value) return
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4'
    mediaRecorder = new MediaRecorder(stream, { mimeType })
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunks, { type: mimeType })
      const ext = mimeType === 'audio/webm' ? 'webm' : 'm4a'
      const file = new File([audioBlob], `voice_${Date.now()}.${ext}`, { type: mimeType })
      voiceFile.value = file
      await transcribeAndFill(file)
      stream.getTracks().forEach(track => track.stop())
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingTime.value = 0

    recordingTimer = setInterval(() => {
      recordingTime.value++
      if (recordingTime.value >= MAX_RECORDING_TIME) {
        stopRecording()
      }
    }, 1000)

  } catch (e) {
    console.error('录音失败', e)
    showToast('无法访问麦克风，请检查权限设置')
  }
}

const stopRecording = () => {
  if (!isRecording.value || !mediaRecorder) return

  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }

  if (recordingTime.value < 1) {
    showToast('录音时间太短')
    mediaRecorder.stop()
    isRecording.value = false
    audioChunks = []
    return
  }

  mediaRecorder.stop()
  isRecording.value = false
}

const transcribeAndFill = async (file) => {
  transcribing.value = true
  try {
    const res = await transcribeVoice(file)
    if (res.data) {
      const transcribedText = res.data
      if (transcribedText && transcribedText.length > 0) {
        if (form.description && form.description.length > 0) {
          form.description = form.description + ' ' + transcribedText
        } else {
          form.description = transcribedText
        }
        const objectUrl = URL.createObjectURL(file)
        voiceUrl.value = objectUrl
        showToast('语音转文字成功')
      }
    } else {
      showToast('语音转文字失败')
    }
  } catch (e) {
    console.error('语音转写失败', e)
    showToast('语音转文字失败，请手动输入')
  } finally {
    transcribing.value = false
  }
}

const clearVoice = () => {
  voiceUrl.value = ''
  voiceFile.value = null
}

const uploadVoiceFile = async () => {
  if (!voiceFile.value) return ''
  try {
    const res = await uploadFile([voiceFile.value])
    const urls = res.data || []
    return urls.length > 0 ? urls[0] : ''
  } catch (e) {
    console.error('语音文件上传失败', e)
    return ''
  }
}

const onReset = () => {
  form.title = ''
  form.eventType = ''
  form.eventTypeText = ''
  form.priority = 'NORMAL'
  form.priorityText = '一般'
  form.description = ''
  form.lng = null
  form.lat = null
  form.address = ''
  form.anonymous = 0
  form.reporterName = ''
  form.reporterPhone = ''
  imageList.value = []
  videoList.value = []
  uploadedImages.value = []
  uploadedVideos.value = []
  voiceUrl.value = ''
  voiceFile.value = ''
  showMap.value = false
  watermarkInfo.eventNo = ''
  watermarkInfo.sensitive = false
  watermarkInfo.targetDeptId = null
  watermarkInfo.targetDeptName = ''
  formRef.value && formRef.value.resetValidation()
}

const buildSubmitData = (withClientId = true) => {
  const submitData = {
    title: form.title,
    eventType: form.eventType,
    type: form.eventType,
    description: form.description,
    lng: form.lng,
    lat: form.lat,
    longitude: form.lng,
    latitude: form.lat,
    address: form.address,
    images: uploadedImages.value,
    imageUrls: uploadedImages.value,
    videos: uploadedVideos.value,
    voiceUrl: voiceUrl.value,
    anonymous: form.anonymous,
    priority: form.priority,
    gridId: form.gridId,
    blockchainEnabled: form.blockchainEnabled,
    eventTimestamp: Date.now()
  }

  if (withClientId) {
    submitData.clientId = generateClientId()
  }

  if (form.anonymous === 0) {
    submitData.reporterName = form.reporterName
    submitData.reporterPhone = form.reporterPhone
    submitData.reporterId = userStore.userId || ''
  }

  return submitData
}

const onSaveOffline = async () => {
  try {
    if (!form.title || !form.eventType || !form.description) {
      showToast('请填写完整的事件信息再暂存')
      return
    }
    const eventData = buildSubmitData(true)
    offlineStore.saveEvent(eventData)
    showToast({ type: 'success', message: '已暂存到本地，联网后自动同步' })
    setTimeout(() => {
      router.back()
    }, 1200)
  } catch (e) {
    console.error(e)
    showToast('暂存失败：' + (e.message || '未知错误'))
  }
}

const onManualSync = async () => {
  try {
    if (!offlineStore.isOnline) {
      showToast('当前网络不可用，无法同步')
      return
    }
    const result = await offlineStore.processQueue(userStore.userId, deviceId)
    if (result) {
      showToast(
        `同步完成：成功${result.successCount}条，失败${result.failedCount}条`
      )
    }
  } catch (e) {
    console.error(e)
    showToast('同步失败：' + (e.message || '未知错误'))
  }
}

const onSubmit = async () => {
  try {
    submitting.value = true

    let uploadedVoiceUrl = ''
    if (voiceFile.value) {
      uploadedVoiceUrl = await uploadVoiceFile()
    }

    const submitData = buildSubmitData(true)
    submitData.voiceUrl = uploadedVoiceUrl

    if (!offlineStore.isOnline) {
      offlineStore.saveEvent(submitData)
      showToast({ type: 'success', message: '网络不可用，已暂存到本地' })
      setTimeout(() => {
        router.back()
      }, 1500)
      return
    }

    let res
    if (form.anonymous === 1) {
      res = await reportEventAnonymous(submitData)
    } else {
      res = await reportEvent(submitData)
    }

    const eventId = res?.data?.id
    const eventNo = res?.data?.eventNo || res?.data?.id
    offlineStore.saveEvent({ ...submitData, status: 'synced', serverId: eventId, eventNo, syncedAt: Date.now() })

    if (eventId && (uploadedImages.value.length > 0 || uploadedVideos.value.length > 0)) {
      try {
        const allUrls = [...uploadedImages.value, ...uploadedVideos.value]
        await linkEventToWatermark(eventId, eventNo, allUrls)
        console.log('[Watermark] 水印存证回写成功: eventId=', eventId, ', eventNo=', eventNo, ', urls=', allUrls.length)
      } catch (linkErr) {
        console.error('[Watermark] 水印存证回写失败:', linkErr)
      }
    }

    showToast({ type: 'success', message: '上报成功' })
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (e) {
    console.error(e)
    try {
      const fallbackData = buildSubmitData(true)
      offlineStore.saveEvent({ ...fallbackData, status: 'failed', lastError: e.message || '上报失败' })
      showToast(e.message || '上报失败，已暂存本地稍后重试')
    } catch (saveErr) {
      showToast(e.message || '上报失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.report-container {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 40px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 8px;

  .status-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.online {
      background-color: #07c160;
      box-shadow: 0 0 4px #07c160;
    }

    &.offline {
      background-color: #969799;
    }
  }

  .pending-badge {
    font-size: 12px;
    color: #ee0a24;
    background-color: #ffeae8;
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.report-content {
  padding: 12px 0;
}

.map-preview {
  padding: 12px 16px;
  background-color: #f0f9ff;
  border-radius: 4px;
  margin: 0 16px 12px;

  .map-coords {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: #646566;
    margin-bottom: 6px;
  }

  .map-hint {
    font-size: 12px;
    color: #969799;
  }
}

.uploader-wrapper {
  padding: 8px 0;

  .uploader-container {
    padding-right: 16px;
  }
}

.upload-hint {
  padding: 4px 16px 12px;
  font-size: 12px;
  color: #969799;
}

.sensitive-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px 12px;
  font-size: 12px;
  color: #1989fa;
  background: #f0f9ff;
  border-radius: 4px;
  margin: 0 16px 12px;
}

.blockchain-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px 12px;
  font-size: 12px;
  color: #07c160;
  background: #f0fff4;
  border-radius: 4px;
  margin: 0 16px 12px;
}

.blockchain-label-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.voice-input-wrap {
  padding: 8px 16px 16px;
  background: #fafbfc;
  border-top: 1px solid #f0f0f0;

  .voice-input-hint {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #969799;
    margin-bottom: 12px;
  }

  .voice-record-area {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 12px 0;
  }

  .record-btn-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    .record-tip {
      font-size: 12px;
      color: #c8c9cc;
    }
  }

  .recording-status {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .recording-pulse {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: #ee0a24;
      position: relative;
      animation: record-pulse 1s infinite;
    }

    .recording-time {
      font-size: 20px;
      font-weight: bold;
      color: #323233;
      font-family: monospace;
    }
  }

  .voice-preview {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;

    .voice-player {
      flex: 1;
      height: 40px;
    }
  }

  .transcribing-hint {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 8px;
    font-size: 12px;
    color: #1989fa;
  }
}

@keyframes record-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(238, 10, 36, 0.4);
  }
  70% {
    box-shadow: 0 0 0 15px rgba(238, 10, 36, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(238, 10, 36, 0);
  }
}

.anonymous-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 0;

  .anonymous-label {
    font-size: 14px;
    color: #323233;
  }
}

.submit-btn-wrap {
  padding: 24px 16px 16px;
}

.resource-list {
  padding: 8px 16px 12px;
  background-color: #fafafa;

  .resource-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    background-color: #fff;
    border-radius: 6px;
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .res-name {
    flex: 1;
    font-size: 14px;
    color: #323233;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 12px;
  }

  .res-distance {
    font-size: 13px;
    color: #646566;
    margin-right: 12px;
  }

  .res-status {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 10px;

    &.online {
      color: #07c160;
      background-color: #e8f7ee;
    }

    &.offline {
      color: #969799;
      background-color: #f2f3f5;
    }
  }

  .res-type {
    font-size: 12px;
    color: #ff976a;
    background-color: #fff4ed;
    padding: 2px 8px;
    border-radius: 10px;
  }

  .call-link {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #1989fa;
    text-decoration: none;

    .van-icon {
      font-size: 14px;
    }
  }
}

.map-preview-panel {
  margin: 12px 16px 16px;
  padding: 12px;
  background-color: #f7f8fa;
  border-radius: 8px;
  border: 1px dashed #dcdee0;

  .map-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 10px;

    span {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #646566;

      .dot {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 4px;

        &.camera {
          background-color: #1989fa;
        }

        &.emergency {
          background-color: #ee0a24;
        }

        &.member {
          background-color: #07c160;
        }

        &.event {
          background-color: #ff976a;
        }
      }
    }
  }

  .map-canvas-hint {
    font-size: 12px;
    color: #969799;
    text-align: center;
    padding: 8px 0;
  }
}

.nlp-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  font-size: 13px;
  color: #1989fa;
}

.nlp-recommend-card {
  padding: 12px 16px;

  .nlp-recommend-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 10px;

    .nlp-recommend-label {
      font-size: 14px;
      font-weight: 600;
      color: #323233;
      flex: 1;
    }
  }

  .nlp-recommend-body {
    background: #f0f9ff;
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 10px;

    .nlp-dept-name {
      font-size: 16px;
      font-weight: bold;
      color: #1989fa;
      margin-bottom: 6px;
    }

    .nlp-confidence {
      font-size: 12px;
      color: #646566;
      display: flex;
      align-items: center;
      gap: 8px;

      .high {
        color: #07c160;
        font-weight: 600;
      }

      .low {
        color: #ff976a;
        font-weight: 600;
      }
    }
  }

  .nlp-recommend-actions {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
