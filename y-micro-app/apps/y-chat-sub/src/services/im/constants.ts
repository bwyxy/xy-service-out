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

  /**
   * 群禁言通知（后端向群频道发送的系统消息，无发送者）
   *
   * 值小于 100，走聊天消息分支进入会话列表，渲染为灰色居中提示。
   */
  GROUP_MUTE = 11,

  /**
   * 群成员变更通知（入群 / 被移出群聊）
   *
   * 值小于 100，走聊天消息分支进入会话列表，渲染为灰色居中提示。
   */
  GROUP_MEMBER_CHANGE = 12,

  /**
   * 群成员被移出群聊通知（后端推送给被移出的用户本人）
   *
   * 值 >= 100，走系统通知分支，弹出系统通知并交给聊天模块处理。
   */
  GROUP_MEMBER_REMOVED = 121,

  /**
   * 消息撤回信令（后端广播）
   *
   * 值 >= 100，若不特判会被 isSystemMessageType 当作系统通知弹出，
   * 因此在 message-handler 中前置拦截，交由聊天模块消费：
   * 不进消息列表，仅把目标消息标记为「已撤回」。
   *
   * 注意：取值必须与后端 MessageType 对齐（100=rtcInvite、101=readReceipt、
   * 102=recall、103=rtcAccept），前端不得再拿这些值定义好友通知等类型。
   */
  RECALL = 102,

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

  /** 登录会话管控：同类型设备登录导致当前设备被强制下线 */
  FORCE_LOGOUT = 220,
}

/**
 * 系统/业务通知类型的起始值：>= 100 视为系统通知
 *
 * 该阈值用于 isSystemMessageType 分流，与具体业务类型解耦，
 * 避免依赖某个具体枚举成员（如历史上被误用的 SYSTEM = 100）取值。
 */
export const SYSTEM_MESSAGE_TYPE_MIN = 100

/**
 * 通知业务类型
 *
 * 与后端 NotificationType 一一对应，随系统通知负载的 notificationType 字段下发。
 * 注意：它描述的是「通知业务语义」，与承载它的 IM 消息类型（MessageType）无关。
 */
export enum NotificationType {
  /** 好友请求 */
  FRIEND_REQUEST = 1,
  /** 好友请求已同意 */
  FRIEND_ACCEPTED = 2,
  /** 系统通知 */
  SYSTEM = 3,
}

/**
 * 判断是否为 RTC 实时信令类型（200-208）。
 * 该类消息由通话模块拦截处理，不弹通知、不进入消息列表。
 */
export function isRtcSignalType(type: number): boolean {
  return type >= MessageType.RTC_INVITE && type <= MessageType.RTC_CANDIDATE
}

/**
 * 判断是否为 RTC 呼叫控制信令（200-205）。
 * 媒体面（206-208）已迁移至 LiveKit，前端仅消费呼叫控制。
 */
export function isCallControlType(type: number): boolean {
  return type >= MessageType.RTC_INVITE && type <= MessageType.RTC_BUSY
}

/**
 * 判断是否为系统/业务通知类型
 */
export function isSystemMessageType(type: number): boolean {
  return type >= SYSTEM_MESSAGE_TYPE_MIN
}
