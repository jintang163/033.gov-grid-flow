import request from '@/utils/request'

export function applyTransfer(data) {
  return request({
    url: '/cross-street-transfer/apply',
    method: 'post',
    data
  })
}

export function approveTransfer(data) {
  return request({
    url: '/cross-street-transfer/approve',
    method: 'post',
    data
  })
}

export function receiveTransfer(transferId) {
  return request({
    url: `/cross-street-transfer/receive/${transferId}`,
    method: 'post'
  })
}

export function processTransfer(data) {
  return request({
    url: '/cross-street-transfer/process',
    method: 'post',
    data
  })
}

export function completeTransfer(data) {
  return request({
    url: '/cross-street-transfer/complete',
    method: 'post',
    data
  })
}

export function getTransferDetail(transferId) {
  return request({
    url: `/cross-street-transfer/${transferId}`,
    method: 'get'
  })
}

export function getTransferPage(params) {
  return request({
    url: '/cross-street-transfer/page',
    method: 'get',
    params
  })
}

export function getMyInvolvedTransfers(params) {
  return request({
    url: '/cross-street-transfer/my-involved',
    method: 'get',
    params
  })
}

export function getEventTransferHistory(eventId) {
  return request({
    url: `/cross-street-transfer/event/${eventId}`,
    method: 'get'
  })
}

export function getTransferTrace(transferId) {
  return request({
    url: `/cross-street-transfer/trace/${transferId}`,
    method: 'get'
  })
}

export function getCooperationDeptTree(params) {
  return request({
    url: '/cross-street-transfer/cooperation-dept-tree',
    method: 'get',
    params
  })
}

export function getTransferStatistics() {
  return request({
    url: '/cross-street-transfer/statistics',
    method: 'get'
  })
}

export function getRecommendedTargets(eventId, targetType) {
  return request({
    url: `/cross-street-transfer/recommend-targets/${eventId}`,
    method: 'get',
    params: { targetType }
  })
}
