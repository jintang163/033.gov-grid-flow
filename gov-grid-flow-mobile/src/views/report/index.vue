<template>
  <div class="report-container">
    <van-nav-bar title="事件上报" left-arrow fixed placeholder @click-left="onBack" />

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

        <van-cell-group inset title="事件描述" style="margin-top: 12px">
          <van-field
            v-model="form.description"
            rows="4"
            autosize
            label="描述"
            type="textarea"
            maxlength="500"
            placeholder="请详细描述事件情况（最多500字）"
            show-word-limit
            :rules="[{ required: true, message: '请输入事件描述' }]"
          />
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
          <div class="upload-hint">* 图片自动压缩上传，建议单张不超过2MB</div>
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
            提交上报
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

    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/report" icon="add-o">上报</van-tabbar-item>
      <van-tabbar-item to="/todo" icon="todo-list-o">待办</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { reportEvent, reportEventAnonymous, uploadFile, getEventTypeList, getGridList } from '@/api'
import { getCurrentLocation, getAddressByLngLat } from '@/utils/amap'

const router = useRouter()
const active = ref(1)
const formRef = ref(null)
const submitting = ref(false)
const showTypePicker = ref(false)
const showPriorityPicker = ref(false)
const showGridPicker = ref(false)
const showMap = ref(false)
const imageList = ref([])
const videoList = ref([])
const uploadedImages = ref([])
const uploadedVideos = ref([])
const gridList = ref([])
const gridColumns = ref([])

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
  gridName: ''
})

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

onMounted(() => {
  fetchEventTypeList()
  fetchGridList()
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
    showToast({ type: 'success', message: '位置获取成功' })
  } catch (e) {
    console.error(e)
    showToast('获取位置失败，请手动输入或检查定位权限')
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

const afterImageRead = async (file) => {
  const files = Array.isArray(file) ? file : [file]
  for (const f of files) {
    try {
      f.status = 'uploading'
      f.message = '上传中...'
      const compressedFile = await compressImage(f.file)
      const res = await uploadFile([compressedFile])
      const urls = res.data || []
      if (urls.length > 0) {
        uploadedImages.value.push(urls[0])
        f.status = 'done'
        f.message = ''
      } else {
        f.status = 'failed'
        f.message = '上传失败'
      }
    } catch (e) {
      f.status = 'failed'
      f.message = '上传失败'
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
  for (const f of files) {
    try {
      f.status = 'uploading'
      f.message = '上传中...'
      const res = await uploadFile([f.file])
      const urls = res.data || []
      if (urls.length > 0) {
        uploadedVideos.value.push(urls[0])
        f.status = 'done'
        f.message = ''
      } else {
        f.status = 'failed'
        f.message = '上传失败'
      }
    } catch (e) {
      f.status = 'failed'
      f.message = '上传失败'
    }
  }
}

const onVideoDelete = (file, detail) => {
  uploadedVideos.value.splice(detail.index, 1)
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
  showMap.value = false
  formRef.value && formRef.value.resetValidation()
}

const onSubmit = async () => {
  try {
    submitting.value = true
    const submitData = {
      title: form.title,
      eventType: form.eventType,
      description: form.description,
      lng: form.lng,
      lat: form.lat,
      address: form.address,
      images: uploadedImages.value,
      videos: uploadedVideos.value,
      anonymous: form.anonymous,
      priority: form.priority,
      gridId: form.gridId
    }

    if (form.anonymous === 0) {
      submitData.reporterName = form.reporterName
      submitData.reporterPhone = form.reporterPhone
    }

    let res
    if (form.anonymous === 1) {
      res = await reportEventAnonymous(submitData)
    } else {
      res = await reportEvent(submitData)
    }

    showToast({ type: 'success', message: '上报成功' })
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (e) {
    console.error(e)
    showToast(e.message || '上报失败，请重试')
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
</style>
