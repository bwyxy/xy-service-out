/**
 * 悟空 IM 消息类型常量
 *
 * 与后端消息负载中的 type 字段保持一致。
 * 1-99 为普通聊天消息，100+ 为系统/业务通知消息。
 */
export enum MessageType {
  /** 文本消息 */
  TEXT = 1,
  /** 图片消息 */
  IMAGE = 2,
  /** 文件消息 */
  FILE = 3,

  /** 系统通知 */
  SYSTEM = 100,
  /** 好友请求通知 */
  FRIEND_REQUEST = 101,
  /** 好友请求被同意通知 */
  FRIEND_ACCEPTED = 102,
  /** 好友请求被拒绝通知 */
  FRIEND_REJECTED = 103,
}

/**
 * 判断是否为系统/业务通知类型
 */
export function isSystemMessageType(type: number): boolean {
  return type >= MessageType.SYSTEM
}
