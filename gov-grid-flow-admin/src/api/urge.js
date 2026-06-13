import request from '@/utils/request'

export function listRules() {
  return request({
    url: '/event/urge/rule/list',
    method: 'get'
  })
}

export function getRule(id) {
  return request({
    url: `/event/urge/rule/${id}`,
    method: 'get'
  })
}

export function saveRule(data) {
  return request({
    url: '/event/urge/rule',
    method: 'post',
    data
  })
}

export function updateRule(data) {
  return request({
    url: '/event/urge/rule',
    method: 'put',
    data
  })
}

export function deleteRule(id) {
  return request({
    url: `/event/urge/rule/${id}`,
    method: 'delete'
  })
}

export function listTemplates() {
  return request({
    url: '/event/urge/template/list',
    method: 'get'
  })
}

export function getTemplate(id) {
  return request({
    url: `/event/urge/template/${id}`,
    method: 'get'
  })
}

export function saveTemplate(data) {
  return request({
    url: '/event/urge/template',
    method: 'post',
    data
  })
}

export function updateTemplate(data) {
  return request({
    url: '/event/urge/template',
    method: 'put',
    data
  })
}

export function deleteTemplate(id) {
  return request({
    url: `/event/urge/template/${id}`,
    method: 'delete'
  })
}

export function listRecords(params) {
  return request({
    url: '/event/urge/record/list',
    method: 'get',
    params
  })
}

export function getWarningInfo(eventId) {
  return request({
    url: `/event/urge/warning/${eventId}`,
    method: 'get'
  })
}

export function scanAndUrge() {
  return request({
    url: '/event/urge/scan',
    method: 'post'
  })
}

export function escalateEvent(eventId) {
  return request({
    url: `/event/urge/escalate/${eventId}`,
    method: 'post'
  })
}
