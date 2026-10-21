/** 聊天领域共享类型 */

/** 联系人（单聊/群聊统一抽象，渠道号即对方 uid 或群 id） */
export interface Contact {
  uid: string
  name: string
  type: 'person' | 'group'
  channelType: 1 | 2
  unreadCount: number
  lastMessage?: string
  lastMessageTime?: number
  avatar?: string
  /** 是否置顶：1=置顶 0/undefined=未置顶 */
  top?: number
}

/** 引用消息快照（存快照而非引用原消息，避免原消息被撤回/未加载时无法展示） */
export interface QuoteInfo {
  /** 原消息 id */
  messageId?: number | string
  /** 原消息序号 */
  messageSeq?: number
  /** 原消息发送者 uid */
  fromUid: string
  /** 原消息发送者昵称 */
  nickname?: string
  /** 原消息内容 */
  content: string
}

/** @提醒的成员 */
export interface MentionInfo {
  uid: string
  name: string
}

/** 聊天消息 */
export interface Message {
  id: string
  fromUid: string
  nickname?: string
  avatar?: string
  channelId: string
  channelType: number
  type: number
  content: string
  timestamp: number
  isSelf: boolean
  messageId?: number | string
  isError?: boolean
  /** 客户端消息编号，用于与 IM 回执去重 */
  clientMsgNo?: string
  /** 消息序号（悟空 IM 分配），撤回/引用按此定位原消息 */
  messageSeq?: number
  /** 是否已撤回 */
  revoked?: boolean
  /** 引用的消息（快照） */
  quote?: QuoteInfo
  /** @提醒的成员列表 */
  mentions?: MentionInfo[]
  /** 是否 @ 到了当前登录用户（用于「有人@我」提醒） */
  mentionMe?: boolean
  /** 是否已点击「有人@我」气泡查看过该提醒 */
  mentionAck?: boolean
}

/** 消息撤回信令的 content 结构 */
export interface RecallSignalContent {
  /** 原消息序号 */
  originalMessageSeq?: number
  /** 原消息客户端消息号 */
  originalClientMsgNo?: string
  /** 原消息发送者 */
  fromUid?: string
}
