import request from '@/utils/request'

export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get'
  })
}

export function getEventMarkers() {
  return request({
    url: '/dashboard/event-markers',
    method: 'get'
  })
}

export function getEventHeatmap() {
  return request({
    url: '/dashboard/heatmap',
    method: 'get'
  })
}

export function getCommunityRank() {
  return request({
    url: '/dashboard/community-rank',
    method: 'get'
  })
}

export function getMemberStatus() {
  return request({
    url: '/dashboard/member-status',
    method: 'get'
  })
}

export function getEventDetail(eventId) {
  return request({
    url: `/dashboard/event/${eventId}`,
    method: 'get'
  })
}

export function dispatchEvent(data) {
  return request({
    url: '/dashboard/dispatch',
    method: 'post',
    data
  })
}

export function getDashboardAll() {
  return request({
    url: '/dashboard/all',
    method: 'get'
  })
}
