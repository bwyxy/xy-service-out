/**
 * 消息类型定义 - 统一基座与子应用的通信协议
 */

/** 消息类型枚举 */
export enum MessageType {
  /** 子应用请求基座路由跳转 */
  NAVIGATE = 'navigate',
  /** 基座推送 WebSocket 报警连接状态 */
  WARNING_STATUS = 'warning-status',
  /** 基座推送 token 更新 */
  TOKEN_UPDATE = 'token-update',
  /** 基座推送用户信息更新 */
  USER_INFO_UPDATE = 'user-info-update',
  /** 子应用请求退出登录 */
  OUT_LOGIN = 'outer-login',
}

/** 基础消息结构 */
export interface BaseMessage {
  type: string
  timestamp?: number
}

/** 路由跳转消息（子应用 → 基座） */
export interface NavigateMessage extends BaseMessage {
  type: MessageType.NAVIGATE
  payload: {
    path: string
    query?: Record<string, string>
  }
}

/** 报警状态消息（基座 → 子应用） */
export interface WarningStatusMessage extends BaseMessage {
  type: MessageType.WARNING_STATUS
  payload: {
    status: string
  }
}

/** Token 更新消息（基座 → 子应用） */
export interface TokenUpdateMessage extends BaseMessage {
  type: MessageType.TOKEN_UPDATE
  payload: {
    token: string
  }
}

/** 用户信息更新消息（基座 → 子应用） */
export interface UserInfoUpdateMessage extends BaseMessage {
  type: MessageType.USER_INFO_UPDATE
  payload: {
    userInfo: Record<string, any>
  }
}

/** 所有已知消息类型的联合类型 */
export type KnownMessage =
  | NavigateMessage
  | WarningStatusMessage
  | TokenUpdateMessage
  | UserInfoUpdateMessage

/** 通用消息（兼容自定义消息类型） */
export type AppMessage = KnownMessage | (BaseMessage & { payload?: any, wsStatus?: number })

/** 消息处理器 */
export type MessageHandler<T extends AppMessage = AppMessage> = (msg: T) => void

/** 取消订阅函数 */
export type Unsubscribe = () => void
