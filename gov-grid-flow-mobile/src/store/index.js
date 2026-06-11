import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getUserInfo as apiGetUserInfo } from '@/api'

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
    userPhone: (state) => state.userInfo.phone || ''
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
