import { ElNotification } from 'element-plus'
import { wukongIMService } from './wukong-im'
import { decodePayload, extractMessageContent } from '@/utils/wk'
import { isSystemMessageType, MessageType } from './constants'
import type { WukongIMMessage, WukongIMMessagePayload, WukongIMSystemPayload } from './types'

/** 聊天消息处理器 */
export type ChatMessageHandler = (
  message: WukongIMMessage,
  payload: WukongIMMessagePayload,
) => void

/** 系统消息处理器 */
export type SystemMessageHandler = (
  message: WukongIMMessage,
  payload: WukongIMSystemPayload,
) => void

/**
 * 消息分类处理服务
 *
 * 负责将 IM 推送的原始消息按类型分发：
 * - 普通聊天消息（TEXT/IMAGE/FILE）交给聊天模块处理
 * - 系统/业务通知（SYSTEM/FRIEND_REQUEST/...）触发通知提醒并交给通知模块处理
 */
export class MessageHandlerService {
  private static instance: MessageHandlerService | null = null

  private readonly chatHandlers: ChatMessageHandler[] = []
  private readonly systemHandlers: SystemMessageHandler[] = []
  private initialized = false

  /**
   * 获取单例实例
   */
  static getInstance(): MessageHandlerService {
    if (!MessageHandlerService.instance) {
      MessageHandlerService.instance = new MessageHandlerService()
    }
    return MessageHandlerService.instance
  }

  /**
   * 初始化消息监听
   * @remarks 需在 IM 连接成功后调用，重复调用会自动忽略
   */
  init(): void {
    if (this.initialized) {
      return
    }

    wukongIMService.onMessage(this.handleIncomingMessage)
    this.initialized = true
  }

  /**
   * 注册聊天消息处理器
   */
  onChatMessage(handler: ChatMessageHandler): void {
    this.chatHandlers.push(handler)
  }

  /**
   * 注册系统消息处理器
   */
  onSystemMessage(handler: SystemMessageHandler): void {
    this.systemHandlers.push(handler)
  }

  /**
   * 移除聊天消息处理器
   */
  offChatMessage(handler: ChatMessageHandler): void {
    const index = this.chatHandlers.indexOf(handler)
    if (index > -1) {
      this.chatHandlers.splice(index, 1)
    }
  }

  /**
   * 移除系统消息处理器
   */
  offSystemMessage(handler: SystemMessageHandler): void {
    const index = this.systemHandlers.indexOf(handler)
    if (index > -1) {
      this.systemHandlers.splice(index, 1)
    }
  }

  private handleIncomingMessage = (message: WukongIMMessage): void => {
    const payload = decodePayload(message.payload || '') as WukongIMSystemPayload

    const content = extractMessageContent(payload)
    console.log('[MessageHandler] 收到消息, type=', payload.type, 'content=', content)

    if (isSystemMessageType(payload.type)) {
      this.handleSystemMessage(message, payload)
    } else {
      this.handleChatMessage(message, payload)
    }
  }

  private handleChatMessage(message: WukongIMMessage, payload: WukongIMMessagePayload): void {
    this.chatHandlers.forEach((handler) => handler(message, payload))
  }

  private handleSystemMessage(message: WukongIMMessage, payload: WukongIMSystemPayload): void {
    // 默认展示右上角系统通知
    const title = this.resolveSystemTitle(payload)
    ElNotification({
      title,
      message: extractMessageContent(payload),
      type: this.resolveNotifyType(payload.type),
      duration: 4500,
    })

    this.systemHandlers.forEach((handler) => handler(message, payload))
  }

  private resolveSystemTitle(payload: WukongIMSystemPayload): string {
    if (payload.title) {
      return payload.title
    }

    switch (payload.type) {
      case MessageType.FRIEND_REQUEST:
        return '好友请求'
      case MessageType.FRIEND_ACCEPTED:
        return '好友请求已同意'
      case MessageType.FRIEND_REJECTED:
        return '好友请求被拒绝'
      case MessageType.SYSTEM:
      default:
        return '系统通知'
    }
  }

  private resolveNotifyType(type: number): 'success' | 'warning' | 'info' | 'error' {
    switch (type) {
      case MessageType.FRIEND_ACCEPTED:
        return 'success'
      case MessageType.FRIEND_REJECTED:
        return 'warning'
      case MessageType.FRIEND_REQUEST:
        return 'info'
      case MessageType.SYSTEM:
      default:
        return 'info'
    }
  }
}

/**
 * 全局唯一的消息分类处理服务实例
 */
export const messageHandlerService = MessageHandlerService.getInstance()
