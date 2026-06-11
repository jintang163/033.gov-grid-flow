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

export function auditEvent(data) {
  return request({
    url: '/event/audit',
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

export function handleEvent(data) {
  return request({
    url: '/event/handle',
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

export function getEventProcessDiagram(processInstanceId) {
  return request({
    url: `/event/diagram/${processInstanceId}`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getProcessDiagram(eventId) {
  return request({
    url: `/event/process-diagram/${eventId}`,
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

export function getEventHistory(eventId) {
  return request({
    url: `/process/history/${eventId}`,
    method: 'get'
  })
}

export function getProcessDiagram(eventId) {
  return request({
    url: `/process/diagram/${eventId}`,
    method: 'get'
  })
}

export function getEventTypeList() {
  return request({
    url: '/event/type/list',
    method: 'get'
  })
}

export function getMyReport(params) {
  return request({
    url: '/event/my-report',
    method: 'get',
    params
  })
}
