import request from '@/utils/request'

export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export const logout = () => {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export const getUserInfo = () => {
  return request({
    url: '/auth/userinfo',
    method: 'get'
  })
}

export const sendCode = (phone) => {
  return request({
    url: '/auth/send-code',
    method: 'post',
    params: { phone }
  })
}

export const reportEvent = (data) => {
  return request({
    url: '/event/report',
    method: 'post',
    data
  })
}

export const reportEventAnonymous = (data) => {
  return request({
    url: '/event/report/anonymous',
    method: 'post',
    data
  })
}

export const getEventList = (params) => {
  return request({
    url: '/event/list',
    method: 'get',
    params
  })
}

export const getEventDetail = (id) => {
  return request({
    url: `/event/${id}`,
    method: 'get'
  })
}

export const getMyTodo = (params) => {
  return request({
    url: '/event/my-todo',
    method: 'get',
    params
  })
}

export const getMyDone = (params) => {
  return request({
    url: '/event/my-done',
    method: 'get',
    params
  })
}

export const getMyReport = (params) => {
  return request({
    url: '/event/my-report',
    method: 'get',
    params
  })
}

export const approveEvent = (data) => {
  return request({
    url: '/event/approve',
    method: 'post',
    data
  })
}

export const rejectEvent = (data) => {
  return request({
    url: '/event/reject',
    method: 'post',
    data
  })
}

export const processEvent = (data) => {
  return request({
    url: '/event/process',
    method: 'post',
    data
  })
}

export const verifyEvent = (data) => {
  return request({
    url: '/event/verify',
    method: 'post',
    data
  })
}

export const returnEvent = (data) => {
  return request({
    url: '/event/return',
    method: 'post',
    data
  })
}

export const assignEvent = (data) => {
  return request({
    url: '/event/assign',
    method: 'post',
    data
  })
}

export const uploadFile = (files) => {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)
  })
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getNotificationList = (params) => {
  return request({
    url: '/notification/list',
    method: 'get',
    params
  })
}

export const readNotification = (id) => {
  return request({
    url: `/notification/read/${id}`,
    method: 'post'
  })
}

export const submitEvaluation = (data) => {
  return request({
    url: '/evaluation/submit',
    method: 'post',
    data
  })
}

export const getEvaluation = (eventId) => {
  return request({
    url: `/evaluation/${eventId}`,
    method: 'get'
  })
}

export const changePassword = (data) => {
  return request({
    url: '/user/change-password',
    method: 'post',
    data
  })
}

export const getEventTypeList = () => {
  return request({
    url: '/event/type/list',
    method: 'get'
  })
}

export const getGridList = () => {
  return request({
    url: '/grid/list',
    method: 'get'
  })
}

export const getMemberList = (gridId) => {
  return request({
    url: '/grid/members',
    method: 'get',
    params: { gridId }
  })
}
