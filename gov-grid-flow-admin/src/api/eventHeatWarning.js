import request from '@/utils/request'

export function getGridHeatForecast(hours = 24) {
  return request({
    url: '/event-heat-warning/forecast',
    method: 'get',
    params: { hours }
  })
}

export function getGridHeatForecastByGridId(gridId, hours = 24) {
  return request({
    url: `/event-heat-warning/forecast/${gridId}`,
    method: 'get',
    params: { hours }
  })
}

export function getCalendarHeatmap(days = 30, gridId = null) {
  return request({
    url: '/event-heat-warning/calendar-heatmap',
    method: 'get',
    params: { days, gridId }
  })
}

export function getCalendarHeatmapByMonth(year, month, gridId = null) {
  return request({
    url: '/event-heat-warning/calendar-heatmap/month',
    method: 'get',
    params: { year, month, gridId }
  })
}

export function pushWarningNotification(gridId) {
  return request({
    url: `/event-heat-warning/push/${gridId}`,
    method: 'post'
  })
}

export function getHighWarningGrids() {
  return request({
    url: '/event-heat-warning/high-warning',
    method: 'get'
  })
}
