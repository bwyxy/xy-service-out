import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getNotificationList,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/api/notification.ts'
import { messageHandlerService } from '@/services/im'
import type { WukongIMMessage, WukongIMSystemPayload } from '@/services/im'

/** 通知项 */
export interface NotificationItem {
  id: string | number
  title: string
  content: string
  type: number
  isRead: 0 | 1
  relatedId?: string | number
  createTime: string
  extra?: Record<string, unknown>
}

/** 通知 Store */
const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  /**
   * 从 IM 系统消息中实时增加未读计数
   */
  function incrementFromIM(_message: WukongIMMessage, payload: WukongIMSystemPayload): void {
    unreadCount.value += 1

    // 同时追加一条本地通知记录，避免用户打开通知中心前列表为空
    notifications.value.unshift({
      id: `${Date.now()}_${Math.random().toString(36).slice(2)}`,
      title: payload.title || '系统通知',
      content: payload.content || '',
      type: payload.notificationType || payload.type,
      isRead: 0,
      relatedId: payload.relatedId,
      createTime: new Date().toISOString(),
      extra: payload.extra,
    })
  }

  /**
   * 从后端拉取未读通知数量
   */
  async function fetchUnreadCount(): Promise<void> {
    try {
      const res = await getUnreadCount()
      if (res.code === 200) {
        unreadCount.value = Number(res.data || 0)
      }
    } catch {
      // 静默失败，避免影响主流程
    }
  }

  /**
   * 从后端拉取通知列表
   * @param params 查询参数
   */
  async function fetchNotifications(params: Record<string, unknown> = {}): Promise<void> {
    loading.value = true
    try {
      const res = await getNotificationList({ pageSize: 50, ...params })
      if (res.code === 200) {
        notifications.value = res.data || []
      }
    } catch {
      notifications.value = []
      ElMessage.error('获取通知列表失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 标记单条通知为已读
   */
  async function markRead(notificationId: string | number): Promise<boolean> {
    try {
      const res = await markNotificationRead(notificationId)
      if (res.code === 200) {
        const item = notifications.value.find((n) => n.id === notificationId)
        if (item && item.isRead === 0) {
          item.isRead = 1
          unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
        return true
      }
    } catch {
      ElMessage.error('标记已读失败')
    }
    return false
  }

  /**
   * 标记所有通知为已读
   */
  async function markAllRead(): Promise<boolean> {
    try {
      const res = await markAllNotificationsRead()
      if (res.code === 200) {
        notifications.value.forEach((item) => {
          item.isRead = 1
        })
        unreadCount.value = 0
        return true
      }
    } catch {
      ElMessage.error('标记全部已读失败')
    }
    return false
  }

  /**
   * 注册 IM 系统消息监听
   * @remarks 在 IM 连接成功后调用一次即可
   */
  function registerIMListener(): void {
    messageHandlerService.onSystemMessage(incrementFromIM)
  }

  /**
   * 重置通知状态（退出登录时调用）
   */
  function reset(): void {
    notifications.value = []
    unreadCount.value = 0
    loading.value = false
  }

  return {
    notifications,
    unreadCount,
    loading,
    fetchUnreadCount,
    fetchNotifications,
    markRead,
    markAllRead,
    registerIMListener,
    reset,
  }
})

export default useNotificationStore
