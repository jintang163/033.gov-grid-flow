export const getWarningList = (params) => {
  return request({
    url: '/notification/list',
    method: 'get',
    params
  })
}

export const readWarning = (id) => {
  return request({
    url: `/notification/read/${id}`,
    method: 'post'
  })
}

export const getGridWarningForecast = (gridId, hours = 24) => {
  return request({
    url: `/event-heat-warning/forecast/${gridId}`,
    method: 'get',
    params: { hours }
  })
}

export const getWarningCalendar = (year, month, gridId) => {
  return request({
    url: '/event-heat-warning/calendar-heatmap/month',
    method: 'get',
    params: { year, month, gridId }
  })
}

export const getMyGridHighWarning = () => {
  return request({
    url: '/event-heat-warning/high-warning',
    method: 'get'
  })
}

export const pushGridWarning = (gridId) => {
  return request({
    url: `/event-heat-warning/push/${gridId}`,
    method: 'post'
  })
}
