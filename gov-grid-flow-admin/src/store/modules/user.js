import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login, logout, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    username: '',
    realName: '',
    avatar: '',
    phone: '',
    email: '',
    role: '',
    deptId: null,
    gridId: null,
    userId: null
  }),
  getters: {
    token: state => state.token,
    username: state => state.username,
    realName: state => state.realName,
    avatar: state => state.avatar,
    role: state => state.role,
    userId: state => state.userId
  },
  actions: {
    async login(userInfo) {
      const { username, password, code } = userInfo
      try {
        const res = await login({ username: username.trim(), password, code })
        this.token = res.data.token
        setToken(res.data.token)
        return res
      } catch (error) {
        return Promise.reject(error)
      }
    },
    async getUserInfoAction() {
      try {
        const res = await getUserInfo()
        const user = res.data
        this.username = user.username
        this.realName = user.realName
        this.avatar = user.avatar
        this.phone = user.phone
        this.email = user.email
        this.role = user.role
        this.deptId = user.deptId
        this.gridId = user.gridId
        this.userId = user.id
        return res
      } catch (error) {
        return Promise.reject(error)
      }
    },
    async logout() {
      try {
        await logout()
      } catch (error) {
        console.error('Logout API error:', error)
      } finally {
        this.resetState()
        removeToken()
        return Promise.resolve()
      }
    },
    resetState() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.avatar = ''
      this.phone = ''
      this.email = ''
      this.role = ''
      this.deptId = null
      this.gridId = null
      this.userId = null
    }
  }
})
