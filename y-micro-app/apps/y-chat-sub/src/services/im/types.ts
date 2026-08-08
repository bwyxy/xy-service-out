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
  /** 消息内容 */
  content: string
}

/** 接收到的消息结构 */
export interface WukongIMMessage {
  /** 消息序列号 */
  messageSeq: number
  /** 发送者 ID */
  fromUid: string
  /** 频道 ID */
  channelId: string
  /** 频道类型 */
  channelType: number
  /** Base64 编码的消息负载 */
  payload: string
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
