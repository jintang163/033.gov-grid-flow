import request from '@/utils/request'

export function getOverview() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

export function getOverviewStats() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

export function getEventTrend(params) {
  return request({
    url: '/statistics/eventTrend',
    method: 'get',
    params: typeof params === 'number' ? { days: params } : params
  })
}

export function getEventTypeDistribution() {
  return request({
    url: '/statistics/eventTypeDistribution',
    method: 'get'
  })
}

export function getEventTypeStats() {
  return request({
    url: '/statistics/eventTypeDistribution',
    method: 'get'
  })
}

export function getStatusDistribution() {
  return request({
    url: '/statistics/statusDistribution',
    method: 'get'
  })
}

export function getDeptStatistics() {
  return request({
    url: '/statistics/deptStatistics',
    method: 'get'
  })
}

export function getDeptStats() {
  return request({
    url: '/statistics/deptStatistics',
    method: 'get'
  })
}

export function getGridStatistics() {
  return request({
    url: '/statistics/gridStatistics',
    method: 'get'
  })
}

export function getGridStats() {
  return request({
    url: '/statistics/gridStatistics',
    method: 'get'
  })
}

export function getPriorityDistribution() {
  return request({
    url: '/statistics/priorityDistribution',
    method: 'get'
  })
}

export function getHandlerRanking(params) {
  return request({
    url: '/statistics/handlerRanking',
    method: 'get',
    params
  })
}

export function getSatisfactionStats() {
  return request({
    url: '/statistics/satisfaction',
    method: 'get'
  })
}
