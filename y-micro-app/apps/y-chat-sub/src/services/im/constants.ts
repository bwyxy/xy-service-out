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

  /** RTC：呼叫邀请（后端推送） */
  RTC_INVITE = 200,
  /** RTC：主叫取消 */
  RTC_CANCEL = 201,
  /** RTC：被叫接听 */
  RTC_ACCEPT = 202,
  /** RTC：被叫拒绝 */
  RTC_REJECT = 203,
  /** RTC：挂断 */
  RTC_HANGUP = 204,
  /** RTC：忙线 */
  RTC_BUSY = 205,
  /** RTC：WebRTC Offer（前端经 /message/send） */
  RTC_OFFER = 206,
  /** RTC：WebRTC Answer */
  RTC_ANSWER = 207,
  /** RTC：ICE Candidate */
  RTC_CANDIDATE = 208,
  /** RTC：通话记录（后端持久化推送，聊天窗口灰色居中提示） */
  RTC_RECORD = 210,
}

/**
 * 判断是否为 RTC 实时信令类型（200-208）。
 * 该类消息由通话模块拦截处理，不弹通知、不进入消息列表。
 */
export function isRtcSignalType(type: number): boolean {
  return type >= MessageType.RTC_INVITE && type <= MessageType.RTC_CANDIDATE
}

/**
 * 判断是否为系统/业务通知类型
 */
export function isSystemMessageType(type: number): boolean {
  return type >= MessageType.SYSTEM
}
