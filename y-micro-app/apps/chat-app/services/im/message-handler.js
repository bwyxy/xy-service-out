import { wukongIMService } from './wukong-im'
import { decodePayload, extractMessageContent } from '@/utils/wk'
import { isRtcSignalType, isSystemMessageType, MessageType } from './constants'

/**
 * 消息分类处理服务
 *
 * 负责将 IM 推送的原始消息按类型分发：
 * - 普通聊天消息（TEXT/IMAGE/FILE）交给聊天模块处理
 * - 系统/业务通知（SYSTEM/FRIEND_REQUEST/...）toast 提示并交给通知模块处理
 * - RTC 实时信令（200-208）本期无人消费，直接丢弃
 * - 强制下线（220）交给登录态管控处理
 */
export class MessageHandlerService {
  constructor() {
    this.chatHandlers = []
    this.systemHandlers = []
    this.rtcHandlers = []
    this.forceLogoutHandlers = []
    this.initialized = false
  }

  /** 获取单例实例 */
  static getInstance() {
    if (!MessageHandlerService.instance) {
      MessageHandlerService.instance = new MessageHandlerService()
    }
    return MessageHandlerService.instance
  }

  /**
   * 初始化消息监听
   * @remarks 需在 IM 连接成功后调用，重复调用会自动忽略
   */
  init() {
    if (this.initialized) {
      return
    }

    wukongIMService.onMessage(this.handleIncomingMessage)
    this.initialized = true
  }

  /** 注册聊天消息处理器 */
  onChatMessage(handler) {
    this.chatHandlers.push(handler)
  }

  /** 注册系统消息处理器 */
  onSystemMessage(handler) {
    this.systemHandlers.push(handler)
  }

  /** 移除聊天消息处理器 */
  offChatMessage(handler) {
    const index = this.chatHandlers.indexOf(handler)
    if (index > -1) {
      this.chatHandlers.splice(index, 1)
    }
  }

  /** 移除系统消息处理器 */
  offSystemMessage(handler) {
    const index = this.systemHandlers.indexOf(handler)
    if (index > -1) {
      this.systemHandlers.splice(index, 1)
    }
  }

  /** 注册 RTC 信令处理器 */
  onRtcSignal(handler) {
    this.rtcHandlers.push(handler)
  }

  /** 注册强制下线处理器 */
  onForceLogout(handler) {
    this.forceLogoutHandlers.push(handler)
  }

  /** 移除强制下线处理器 */
  offForceLogout(handler) {
    const index = this.forceLogoutHandlers.indexOf(handler)
    if (index > -1) {
      this.forceLogoutHandlers.splice(index, 1)
    }
  }

  handleIncomingMessage = (message) => {
    const payload = decodePayload(message.payload || '')

    // RTC 实时信令（200-208）：移动端本期无通话模块，直接丢弃
    if (isRtcSignalType(payload.type)) {
      return
    }

    // 通话记录（210）：作为聊天消息进入会话展示
    if (payload.type === MessageType.RTC_RECORD) {
      this.chatHandlers.forEach((handler) => handler(message, payload))
      return
    }

    // 强制下线通知（220）：必须早于 isSystemMessageType(>=100) 分流
    if (payload.type === MessageType.FORCE_LOGOUT) {
      this.forceLogoutHandlers.forEach((handler) => handler(message, payload))
      return
    }

    if (isSystemMessageType(payload.type)) {
      this.handleSystemMessage(message, payload)
    } else {
      this.chatHandlers.forEach((handler) => handler(message, payload))
    }
  }

  handleSystemMessage(message, payload) {
    const content = extractMessageContent(payload) || this.resolveSystemTitle(payload)

    uni.showToast({
      title: content,
      icon: 'none',
      duration: 4500,
    })

    this.systemHandlers.forEach((handler) => handler(message, payload))
  }

  resolveSystemTitle(payload) {
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
      default:
        return '系统通知'
    }
  }
}

/** 全局唯一的消息分类处理服务实例 */
export const messageHandlerService = MessageHandlerService.getInstance()
