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
