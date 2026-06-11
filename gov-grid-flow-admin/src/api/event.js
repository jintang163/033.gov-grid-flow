import request from '@/utils/request'

export function getEventList(params) {
  return request({
    url: '/event/list',
    method: 'get',
    params
  })
}

export function getEventDetail(id) {
  return request({
    url: `/event/${id}`,
    method: 'get'
  })
}

export function reportEvent(data) {
  return request({
    url: '/event/report',
    method: 'post',
    data
  })
}

export function reportEventAnonymous(data) {
  return request({
    url: '/event/report/anonymous',
    method: 'post',
    data
  })
}

export function processEvent(data) {
  return request({
    url: '/event/process',
    method: 'post',
    data
  })
}

export function assignEvent(data) {
  return request({
    url: '/event/assign',
    method: 'post',
    data
  })
}

export function verifyEvent(data) {
  return request({
    url: '/event/verify',
    method: 'post',
    data
  })
}

export function returnEvent(data) {
  return request({
    url: '/event/return',
    method: 'post',
    data
  })
}

export function approveEvent(data) {
  return request({
    url: '/event/approve',
    method: 'post',
    data
  })
}

export function rejectEvent(data) {
  return request({
    url: '/event/reject',
    method: 'post',
    data
  })
}

export function getProcessDiagram(eventId) {
  return request({
    url: `/process/diagram/${eventId}`,
    method: 'get'
  })
}

export function getEventTypeList(params) {
  return request({
    url: '/event/type/list',
    method: 'get',
    params
  })
}

export function getMyTodoList(params) {
  return request({
    url: '/event/my-todo',
    method: 'get',
    params
  })
}

export function getMyDoneList(params) {
  return request({
    url: '/event/my-done',
    method: 'get',
    params
  })
}

export function getMyReport(params) {
  return request({
    url: '/event/my-report',
    method: 'get',
    params
  })
}

export function getEventHistory(eventId) {
  return request({
    url: `/process/history/${eventId}`,
    method: 'get'
  })
}

// ===== 周边资源 =====
// 获取500米内周边资源（摄像头/应急物资/网格员）
export const getNearbyResources = (params) => {
  return request({
    url: '/nearby/resources',
    method: 'get',
    params
  })
}
// 一键呼叫网格员
export const callMember = (userId) => {
  return request({
    url: `/notification/call/${userId}`,
    method: 'post'
  })
}
// 网格员位置上报
export const reportMemberLocation = (data) => {
  return request({
    url: '/nearby/member/location/report',
    method: 'post',
    data
  })
}
