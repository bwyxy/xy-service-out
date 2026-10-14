/**
 * 悟空 IM 相关类型定义
 */

/** IM 用户信息 */
export interface WukongIMUserInfo {
  /** 用户唯一标识 */
  uid: string
  /** 连接令牌 */
  token: string
}

/** IM 连接配置 */
export interface WukongIMConfig {
  /** 悟空 IM 服务器 WebSocket 地址 */
  serverUrl: string
  /** 当前登录用户信息 */
  userInfo: WukongIMUserInfo
}

/** 消息负载结构（与后端 Base64 编码的 JSON 对应） */
export interface WukongIMMessagePayload {
  /** 消息类型 */
  type: number
  /** 消息内容（部分旧数据或第三方客户端可能使用 message 字段） */
  content?: string
  /** 兼容字段：某些场景下 payload 使用 message 表示内容 */
  message?: string
}

/** 系统通知消息负载 */
export interface WukongIMSystemPayload extends WukongIMMessagePayload {
  /** 通知标题 */
  title?: string
  /** 通知业务类型（与后端 NotificationType 对应） */
  notificationType?: number
  /** 关联业务 ID，如好友请求 ID */
  relatedId?: string | number
  /** 扩展字段 */
  extra?: Record<string, unknown>
}

/** 接收到的消息结构 */
export interface WukongIMMessage {
  /** 消息序列号 */
  messageSeq: number
  /** 消息唯一 ID */
  messageId?: string | number
  /** 客户端消息编号，用于去重 */
  clientMsgNo?: string
  /** 发送者 ID */
  fromUid: string
  /** 频道 ID */
  channelId: string
  /** 频道类型 */
  channelType: number
  /** 消息负载：后端为 Base64 字符串，easyjssdk 派发时已解码为对象 */
  payload: any
  /** 消息时间戳 */
  timestamp: number
}

/** 会话/频道信息 */
export interface WukongIMConversation {
  /** 频道 ID */
  channelId: string
  /** 频道类型 */
  channelType: number
  /** 其他扩展字段 */
  [key: string]: unknown
}

/** IM 事件类型 */
export type WukongIMEventType = 'connect' | 'disconnect' | 'message' | 'error'

/** IM 事件回调 */
export type WukongIMEventHandler<T = unknown> = (data: T) => void

/** 消息发送通道类型 */
export type WukongIMChannelType = 'person' | 'group'
