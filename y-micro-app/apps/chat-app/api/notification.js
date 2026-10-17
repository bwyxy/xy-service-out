import request from '@/utils/request'

/**
 * 通知列表
 * */
export function getNotificationList(params) {
  return request.get('/notification/list', params)
}

/**
 * 未读通知列表
 * */
export function getUnreadNotifications() {
  return request({
    url: '/notification/unread',
    method: 'GET',
  })
}

/**
 * 未读通知数量
 * */
export function getUnreadCount() {
  return request({
    url: '/notification/unread/count',
    method: 'GET',
  })
}

/**
 * 标记单条已读
 * */
export function markNotificationRead(notificationId) {
  return request({
    url: `/notification/read/${notificationId}`,
    method: 'POST',
  })
}

/**
 * 标记全部已读
 * */
export function markAllNotificationsRead() {
  return request({
    url: '/notification/read/all',
    method: 'POST',
  })
}
