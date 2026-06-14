import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getUserInfo as apiGetUserInfo } from '@/api'
import * as offlineDB from '@/utils/offlineDB'
import * as syncQueue from '@/utils/syncQueue'
import * as network from '@/utils/network'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    userName: (state) => state.userInfo.realName || state.userInfo.username || '',
    userAvatar: (state) => state.userInfo.avatar || '',
    userRole: (state) => state.userInfo.role || '',
    userPhone: (state) => state.userInfo.phone || '',
    userId: (state) => state.userInfo.id || state.userInfo.userId || ''
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    async login(loginData) {
      const res = await apiLogin(loginData)
      const { token, user } = res.data
      this.setToken(token)
      this.setUserInfo(user)
      return res.data
    },
    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        console.warn('Logout api failed, clearing local data anyway', e)
      }
      this.token = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    async fetchUserInfo() {
      const res = await apiGetUserInfo()
      this.setUserInfo(res.data)
      return res.data
    }
  }
})

export const useAppStore = defineStore('app', {
  state: () => ({
    loading: false
  }),
  actions: {
    setLoading(loading) {
      this.loading = loading
    }
  }
})

export const useVoiceStore = defineStore('voice', {
  state: () => ({
    enabled: localStorage.getItem('voice_enabled') === '1',
    todoEnabled: localStorage.getItem('voice_todo_enabled') !== '0',
    reminderEnabled: localStorage.getItem('voice_reminder_enabled') !== '0',
    rate: parseFloat(localStorage.getItem('voice_rate')) || 1.0,
    pitch: parseFloat(localStorage.getItem('voice_pitch')) || 1.0,
    volume: parseFloat(localStorage.getItem('voice_volume')) || 1.0,
    autoPlayOnDetail: localStorage.getItem('voice_auto_detail') === '1',
    broadcastQueue: [],
    isBroadcasting: false,
    lastBroadcastTime: null,
    broadcastHistory: []
  }),
  getters: {
    isEnabled: (state) => state.enabled,
    canBroadcastTodo: (state) => state.enabled && state.todoEnabled,
    canBroadcastReminder: (state) => state.enabled && state.reminderEnabled,
    rateText: (state) => {
      if (state.rate <= 0.6) return '慢速'
      if (state.rate <= 0.9) return '较慢'
      if (state.rate <= 1.1) return '正常'
      if (state.rate <= 1.4) return '较快'
      return '快速'
    }
  },
  actions: {
    setEnabled(enabled) {
      this.enabled = enabled
      localStorage.setItem('voice_enabled', enabled ? '1' : '0')
    },
    setTodoEnabled(enabled) {
      this.todoEnabled = enabled
      localStorage.setItem('voice_todo_enabled', enabled ? '1' : '0')
    },
    setReminderEnabled(enabled) {
      this.reminderEnabled = enabled
      localStorage.setItem('voice_reminder_enabled', enabled ? '1' : '0')
    },
    setRate(rate) {
      const clamped = Math.max(0.5, Math.min(2, rate))
      this.rate = clamped
      localStorage.setItem('voice_rate', String(clamped))
    },
    setPitch(pitch) {
      const clamped = Math.max(0.5, Math.min(2, pitch))
      this.pitch = clamped
      localStorage.setItem('voice_pitch', String(clamped))
    },
    setVolume(volume) {
      const clamped = Math.max(0, Math.min(1, volume))
      this.volume = clamped
      localStorage.setItem('voice_volume', String(clamped))
    },
    setAutoPlayOnDetail(enabled) {
      this.autoPlayOnDetail = enabled
      localStorage.setItem('voice_auto_detail', enabled ? '1' : '0')
    },
    getVoiceOptions() {
      return {
        rate: this.rate,
        pitch: this.pitch,
        volume: this.volume,
        lang: 'zh-CN'
      }
    },
    addToBroadcastQueue(item) {
      this.broadcastQueue.push({
        ...item,
        id: Date.now() + '_' + Math.random().toString(36).slice(2, 8),
        addedAt: Date.now()
      })
      this._processQueue()
    },
    async _processQueue() {
      if (this.isBroadcasting || this.broadcastQueue.length === 0 || !this.enabled) {
        return
      }
      this.isBroadcasting = true
      const tts = await import('@/utils/tts')
      while (this.broadcastQueue.length > 0 && this.enabled) {
        const item = this.broadcastQueue.shift()
        try {
          await tts.speak(item.text, this.getVoiceOptions())
          this.broadcastHistory.unshift(item)
          if (this.broadcastHistory.length > 20) {
            this.broadcastHistory.pop()
          }
          this.lastBroadcastTime = Date.now()
          if (this.broadcastQueue.length > 0) {
            await new Promise(r => setTimeout(r, 300))
          }
        } catch (e) {
          console.warn('[Voice] 播报失败:', e, item)
        }
      }
      this.isBroadcasting = false
    },
    async clearQueue() {
      this.broadcastQueue = []
      const tts = await import('@/utils/tts')
      tts.stop()
      this.isBroadcasting = false
    },
    clearHistory() {
      this.broadcastHistory = []
    }
  }
})

export const useOfflineStore = defineStore('offline', {
  state: () => ({
    events: [],
    stats: { total: 0, pending: 0, syncing: 0, failed: 0, synced: 0 },
    syncing: false,
    lastSyncTime: null,
    lastSyncResult: null,
    isOnline: network.isOnline()
  }),
  getters: {
    pendingCount: (state) => state.stats.pending + state.stats.failed,
    hasPending: (state) => (state.stats.pending + state.stats.failed) > 0,
    failedEvents: (state) => state.events.filter((e) => e.status === 'failed'),
    pendingEvents: (state) =>
      state.events.filter(
        (e) => e.status === 'pending' || e.status === 'syncing' || e.status === 'failed'
      )
  },
  actions: {
    refresh() {
      this.events = offlineDB.getEventList()
      this.stats = offlineDB.getStats()
      this.isOnline = network.isOnline()
    },
    saveEvent(event) {
      const saved = offlineDB.saveEvent(event)
      this.refresh()
      return saved
    },
    deleteEvent(clientId) {
      offlineDB.deleteEvent(clientId)
      this.refresh()
    },
    clearSynced() {
      offlineDB.clearSynced()
      this.refresh()
    },
    async processQueue(userId, deviceId) {
      if (this.syncing) return
      this.syncing = true
      this.refresh()
      try {
        const result = await syncQueue.processQueue(userId, deviceId)
        this.lastSyncResult = result
        this.lastSyncTime = Date.now()
        return result
      } finally {
        this.syncing = false
        this.refresh()
      }
    },
    startAutoSync(userId, deviceId, interval = 30000) {
      syncQueue.startAutoSync(interval, userId, deviceId)
    },
    stopAutoSync() {
      syncQueue.stopAutoSync()
    },
    setOnline(online) {
      this.isOnline = online
    },
    initNetworkListener() {
      this.isOnline = network.isOnline()
      network.onOnline(() => {
        this.isOnline = true
        this.refresh()
      })
      network.onOffline(() => {
        this.isOnline = false
        this.refresh()
      })
    }
  }
})
