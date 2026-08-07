import request from '@/utils/request.ts'

/**
 * 通知列表
 * */
export const getNotificationList = (params?: { type?: number; isRead?: number; pageNum?: number; pageSize?: number }) => {
    return request({
        url: '/notification/list',
        method: 'GET',
        params
    })
}

/**
 * 未读通知列表
 * */
export const getUnreadNotifications = () => {
    return request({
        url: '/notification/unread',
        method: 'GET'
    })
}

/**
 * 未读通知数量
 * */
export const getUnreadCount = () => {
    return request({
        url: '/notification/unread/count',
        method: 'GET'
    })
}

/**
 * 标记单条已读
 * */
export const markNotificationRead = (notificationId: string | number) => {
    return request({
        url: `/notification/read/${notificationId}`,
        method: 'POST'
    })
}

/**
 * 标记全部已读
 * */
export const markAllNotificationsRead = () => {
    return request({
        url: '/notification/read/all',
        method: 'POST'
    })
}
