import request from '@/utils/request'

export function getNotificationList(params) {
  return request({
    url: '/notification/list',
    method: 'get',
    params
  })
}

export function getNotificationDetail(id) {
  return request({
    url: `/notification/${id}`,
    method: 'get'
  })
}

export function markAsRead(id) {
  return request({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

export function markAllAsRead() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

export function deleteNotification(id) {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}
