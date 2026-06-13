import request from '@/utils/request'

export function analyzeEvent(eventId) {
  return request({
    url: `/event/analysis/analyze/${eventId}`,
    method: 'post'
  })
}

export function batchScanHighRecurrence() {
  return request({
    url: '/event/analysis/scan',
    method: 'post'
  })
}

export function listHighRecurrenceGroups(days) {
  return request({
    url: '/event/analysis/recurrence/list',
    method: 'get',
    params: { days }
  })
}

export function getRecurrenceGroup(groupKey) {
  return request({
    url: `/event/analysis/recurrence/${encodeURIComponent(groupKey)}`,
    method: 'get'
  })
}

export function getEventRelationGraph(eventId, depth) {
  return request({
    url: `/event/analysis/graph/${eventId}`,
    method: 'get',
    params: { depth }
  })
}

export function generateAnalysisReport(days) {
  return request({
    url: '/event/analysis/report/generate',
    method: 'post',
    params: { days }
  })
}

export function pushReportToStreetOffice(days) {
  return request({
    url: '/event/analysis/report/push',
    method: 'post',
    params: { days }
  })
}
