import axios from 'axios'
import { showToast, showLoadingToast, closeToast } from 'vant'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    if (config.loading !== false) {
      showLoadingToast({
        message: '加载中...',
        forbidClick: true,
        duration: 0
      })
    }
    return config
  },
  (error) => {
    closeToast()
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    closeToast()
    const res = response.data
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.hash = '#/login'
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    closeToast()
    showToast(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
