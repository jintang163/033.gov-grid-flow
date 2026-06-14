<template>
  <div class="voice-settings">
    <van-nav-bar title="语音播报设置" fixed placeholder left-arrow @click-left="onBack" />

    <div class="settings-content">
      <van-cell-group inset>
        <van-cell title="语音播报总开关">
          <template #right-icon>
            <van-switch
              v-model="voiceStore.enabled"
              size="24"
              @change="onVoiceEnabledChange"
            />
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell
          title="待办事件播报"
          :disabled="!voiceStore.enabled"
        >
          <template #right-icon>
            <van-switch
              v-model="voiceStore.todoEnabled"
              size="24"
              :disabled="!voiceStore.enabled"
              @change="onTodoEnabledChange"
            />
          </template>
        </van-cell>
        <van-cell
          title="催办信息播报"
          :disabled="!voiceStore.enabled"
        >
          <template #right-icon>
            <van-switch
              v-model="voiceStore.reminderEnabled"
              size="24"
              :disabled="!voiceStore.enabled"
              @change="onReminderEnabledChange"
            />
          </template>
        </van-cell>
        <van-cell
          title="详情页自动朗读"
          :disabled="!voiceStore.enabled"
        >
          <template #right-icon>
            <van-switch
              v-model="voiceStore.autoPlayOnDetail"
              size="24"
              :disabled="!voiceStore.enabled"
              @change="onAutoDetailChange"
            />
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell title="语速调节">
          <template #right-icon>
            <span class="rate-label">{{ voiceStore.rateText }}</span>
          </template>
        </van-cell>
        <div class="slider-cell">
          <span class="slider-label">慢</span>
          <van-slider
            v-model="rateValue"
            :disabled="!voiceStore.enabled"
            step="0.1"
            min="0.5"
            max="2"
            :bar-height="4"
            active-color="#1989fa"
            @change="onRateChange"
          />
          <span class="slider-label">快</span>
        </div>

        <van-cell title="音量调节" style="margin-top: 8px">
        </van-cell>
        <div class="slider-cell">
          <span class="slider-label">低</span>
          <van-slider
            v-model="volumeValue"
            :disabled="!voiceStore.enabled"
            step="0.1"
            min="0"
            max="1"
            :bar-height="4"
            active-color="#1989fa"
            @change="onVolumeChange"
          />
          <span class="slider-label">高</span>
        </div>

        <van-cell title="音调调节" style="margin-top: 8px">
        </van-cell>
        <div class="slider-cell">
          <span class="slider-label">低</span>
          <van-slider
            v-model="pitchValue"
            :disabled="!voiceStore.enabled"
            step="0.1"
            min="0.5"
            max="2"
            :bar-height="4"
            active-color="#1989fa"
            @change="onPitchChange"
          />
          <span class="slider-label">高</span>
        </div>
      </van-cell-group>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell
          title="试听效果"
          is-link
          :disabled="!voiceStore.enabled || voiceStore.isBroadcasting"
          @click="onTestVoice"
        >
          <template #right-icon>
            <van-loading
              v-if="voiceStore.isBroadcasting"
              size="16px"
              color="#1989fa"
            />
            <span v-else class="cell-arrow">试听</span>
          </template>
        </van-cell>
        <van-cell
          title="停止播报"
          is-link
          :disabled="!voiceStore.isBroadcasting"
          @click="onStopVoice"
        >
          <template #right-icon>
            <span class="cell-arrow">停止</span>
          </template>
        </van-cell>
      </van-cell-group>

      <div v-if="voiceStore.isBroadcasting" class="broadcast-indicator">
        <div class="indicator-wave">
          <span></span><span></span><span></span><span></span>
        </div>
        <span class="indicator-text">正在语音播报...</span>
      </div>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell title="播报环境" center>
          <template #right-icon>
            <span :class="ttsModeClass">{{ ttsModeText }}</span>
          </template>
        </van-cell>
        <van-cell title="待播报队列" center>
          <template #right-icon>
            <span>{{ voiceStore.broadcastQueue.length }} 条</span>
          </template>
        </van-cell>
      </van-cell-group>

      <div class="tip-card">
        <van-icon name="info-o" size="14" color="#1989fa" />
        <span class="tip-text">
          开启语音播报后，新的待办事件和催办信息将自动朗读，适合户外作业。语速可根据个人习惯调节。
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useVoiceStore } from '@/store'
import { speak, stop, isAvailable, getMode } from '@/utils/tts'

const router = useRouter()
const voiceStore = useVoiceStore()

const rateValue = ref(voiceStore.rate)
const pitchValue = ref(voiceStore.pitch)
const volumeValue = ref(voiceStore.volume)

const ttsModeText = computed(() => {
  const mode = getMode()
  if (!isAvailable()) return '不可用'
  if (mode === 'web') return '浏览器 TTS'
  if (mode === 'miniapp') return '微信小程序'
  return '自动识别'
})

const ttsModeClass = computed(() => ({
  'mode-available': isAvailable(),
  'mode-unavailable': !isAvailable()
}))

const onBack = () => {
  router.back()
}

const onVoiceEnabledChange = (val) => {
  voiceStore.setEnabled(val)
  if (!val) {
    voiceStore.clearQueue()
  }
  showToast(val ? '语音播报已开启' : '语音播报已关闭')
}

const onTodoEnabledChange = (val) => {
  voiceStore.setTodoEnabled(val)
  showToast(val ? '待办播报已开启' : '待办播报已关闭')
}

const onReminderEnabledChange = (val) => {
  voiceStore.setReminderEnabled(val)
  showToast(val ? '催办播报已开启' : '催办播报已关闭')
}

const onAutoDetailChange = (val) => {
  voiceStore.setAutoPlayOnDetail(val)
  showToast(val ? '自动朗读已开启' : '自动朗读已关闭')
}

const onRateChange = (val) => {
  voiceStore.setRate(val)
}

const onPitchChange = (val) => {
  voiceStore.setPitch(val)
}

const onVolumeChange = (val) => {
  voiceStore.setVolume(val)
}

const onTestVoice = async () => {
  if (!isAvailable()) {
    showToast('当前环境不支持语音合成')
    return
  }
  try {
    const testText = '您好，网格员张三，您有一条新的待办事件：市政设施破损，请及时前往处置。'
    await speak(testText, voiceStore.getVoiceOptions())
    showToast('试听完成')
  } catch (e) {
    console.error(e)
    showToast('语音播放失败')
  }
}

const onStopVoice = () => {
  stop()
  voiceStore.clearQueue()
  showToast('已停止播报')
}

onUnmounted(() => {
  stop()
})
</script>

<style scoped>
.voice-settings {
  min-height: 100vh;
  background: #f7f8fa;
}

.settings-content {
  padding: 12px 0 40px;
}

.slider-cell {
  display: flex;
  align-items: center;
  padding: 8px 16px 16px;
  background: #fff;
}

.slider-label {
  width: 24px;
  font-size: 12px;
  color: #969799;
  text-align: center;
}

.slider-cell :deep(.van-slider) {
  flex: 1;
  margin: 0 12px;
}

.rate-label {
  color: #1989fa;
  font-size: 13px;
  margin-right: 4px;
}

.cell-arrow {
  color: #1989fa;
  font-size: 14px;
  margin-right: 4px;
}

.mode-available {
  color: #07c160;
}

.mode-unavailable {
  color: #ee0a24;
}

.broadcast-indicator {
  position: fixed;
  top: 46px;
  left: 0;
  right: 0;
  background: rgba(25, 137, 250, 0.95);
  color: #fff;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 100;
}

.indicator-wave {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 16px;
}

.indicator-wave span {
  width: 3px;
  background: #fff;
  animation: wave 1s ease-in-out infinite;
}

.indicator-wave span:nth-child(1) {
  height: 40%;
  animation-delay: 0s;
}

.indicator-wave span:nth-child(2) {
  height: 100%;
  animation-delay: 0.1s;
}

.indicator-wave span:nth-child(3) {
  height: 60%;
  animation-delay: 0.2s;
}

.indicator-wave span:nth-child(4) {
  height: 80%;
  animation-delay: 0.3s;
}

@keyframes wave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.indicator-text {
  font-size: 13px;
}

.tip-card {
  margin: 16px 16px 0;
  padding: 12px;
  background: #e5f3ff;
  border-radius: 8px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.tip-text {
  font-size: 12px;
  color: #1989fa;
  line-height: 1.6;
  flex: 1;
}
</style>
