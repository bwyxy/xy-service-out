/**
 * 聊天领域纯函数
 *
 * 消息收发、撤回/引用判定、去重与分页判断等与 UI 无关的逻辑集中在此，
 * 供 store 与组件复用。
 */
import { MessageType } from '@/services/im/constants'
import type { Message, QuoteInfo } from '@/types/chat'

/** @所有人的保留 uid */
export const MENTION_ALL_UID = 'all'

/** 撤回窗口期（毫秒），与后端保持一致 */
export const RECALL_WINDOW_MS = 2 * 60 * 1000

/**
 * 成员展示名：群昵称 > 昵称 > 兜底值
 *
 * 兜底值由调用方指定（聊天展示通常兜底 uid，无昵称时也可传空串）。
 */
export function resolveMemberName(
  member?: { groupNickname?: string; nickname?: string; uid?: string | number } | null,
  fallback = '',
): string {
  if (!member) {
    return fallback
  }

  return member.groupNickname || member.nickname || fallback
}

/**
 * 是否 @ 到了当前登录用户
 *
 * 命中本人 uid 或 @所有人 均视为提醒到本人。
 */
export function isMentionedMe(
  mentions: Array<{ uid?: string | number }> | undefined,
  selfUid: string,
): boolean {
  if (!mentions || mentions.length === 0) {
    return false
  }

  return mentions.some(
    (m) => String(m?.uid) === selfUid || String(m?.uid) === MENTION_ALL_UID,
  )
}

/**
 * 是否可撤回：本人发送、未撤回、已发送成功、已拿到消息序号且在窗口期内
 */
export function canRecallMessage(msg: Message): boolean {
  if (!msg.isSelf || msg.revoked || msg.isError) {
    return false
  }
  if (msg.messageSeq == null || !msg.timestamp) {
    return false
  }
  return Date.now() - msg.timestamp <= RECALL_WINDOW_MS
}

/**
 * 是否可重新编辑：本人发送、已撤回的文本消息（保留有原文本内容）
 */
export function canReeditMessage(msg: Message): boolean {
  return msg.isSelf && !!msg.revoked && msg.type === MessageType.TEXT && !!msg.content
}

/**
 * 是否可复制：未撤回的文本消息
 */
export function canCopyMessage(msg: Message): boolean {
  return msg.type === MessageType.TEXT && !msg.revoked && !!msg.content
}

/**
 * 是否可引用：未撤回、发送成功的文本/图片消息
 */
export function canQuoteMessage(msg: Message): boolean {
  if (msg.revoked || msg.isError || !msg.content) {
    return false
  }
  return msg.type === MessageType.TEXT || msg.type === MessageType.IMAGE
}

/**
 * 生成引用快照
 */
export function buildQuoteInfo(msg: Message, selfName: string): QuoteInfo {
  return {
    messageId: msg.messageId,
    messageSeq: msg.messageSeq,
    fromUid: msg.fromUid,
    nickname: msg.isSelf ? selfName : msg.nickname,
    content: msg.type === MessageType.IMAGE ? '[图片]' : msg.content,
  }
}

/**
 * 判断两条消息是否为同一条
 *
 * 同一频道内 messageSeq 唯一，优先用「频道 + 消息序号」判定；
 * 本地乐观消息（尚未拿到序号）用 clientMsgNo 判定。
 *
 * 注意：不能用 id 判定——IM 推送、历史拉取、本地乐观更新三条来源生成的 id 可能不同，
 * 同一消息只带 clientMsgNo 或被补上序号后就会重复入列。
 */
export function isSameMessage(a: Message, b: Message): boolean {
  if (a.clientMsgNo && b.clientMsgNo && a.clientMsgNo === b.clientMsgNo) {
    return true
  }

  if (a.messageSeq != null && b.messageSeq != null) {
    // 序号可能因来源不同被序列化成字符串，这里统一按字符串比较
    return (
      a.channelId === b.channelId &&
      a.channelType === b.channelType &&
      String(a.messageSeq) === String(b.messageSeq)
    )
  }

  return false
}

/**
 * 归一化消息时间戳
 *
 * 悟空 IM 回传的时间戳为秒级，本地乐观更新使用毫秒级，
 * 统一转为毫秒，避免排序与时间窗口判断出错。
 */
export function toMillis(timestamp?: number): number {
  if (!timestamp) {
    return Date.now()
  }

  return timestamp < 1e12 ? timestamp * 1000 : timestamp
}

/**
 * 从发送回执中按多种结构/命名提取字段
 *
 * 悟空 IM 的回执可能被包在 R.data（ResultVO）里，data 也可能被序列化成 JSON 字符串，
 * 字段既有下划线（message_seq）也有驼峰（messageSeq）的可能，
 * 这里做一层兼容，避免因结构差异拿不到消息序号（撤回/引用都依赖它）。
 */
export function pickSendReceiptField(response: any, ...keys: string[]): any {
  const candidates = [response?.data?.data, response?.data, response]

  for (const candidate of candidates) {
    if (!candidate) {
      continue
    }

    let source = candidate
    if (typeof source === 'string') {
      try {
        source = JSON.parse(source)
      } catch {
        continue
      }
    }

    for (const key of keys) {
      if (source?.[key] != null) {
        return source[key]
      }
    }
  }

  return undefined
}

/**
 * 判断是否还有更早的历史消息
 *
 * 优先使用悟空 messageSync 返回的 more 字段（1=还有更早消息，0=没有），
 * 缺失时退化为按「本页条数是否拉满」判断。
 */
export function resolveHasMore(data: any, parsedCount: number, limit: number): boolean {
  const more = data?.more

  if (typeof more === 'number') {
    return more === 1
  }

  return parsedCount >= limit
}
