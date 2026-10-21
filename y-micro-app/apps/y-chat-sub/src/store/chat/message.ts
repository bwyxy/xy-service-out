/** 消息职责：接收、发送、撤回、历史分页与去重 */
import {
  sendMessage as apiSendMessage,
  getMessageList,
  recallMessage as apiRecallMessage,
} from '@/api/chat.ts'
import { getSystemUserInfo } from '@/api/user.ts'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk.ts'
import { MessageType, isRtcSignalType } from '@/services/im'
import {
  isMentionedMe,
  isSameMessage,
  pickSendReceiptField,
  resolveHasMore,
  toMillis,
} from '@/utils/chat.ts'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im'
import type { Contact, MentionInfo, Message, QuoteInfo, RecallSignalContent } from '@/types/chat.ts'
import type { ChatState } from './context.ts'
import { fetchUserInfo, sortContacts, upsertContactFromMessage } from './conversation.ts'

export interface MessageModule {
  /** 处理 IM 推送的聊天消息 */
  handleIncomingChatMessage(message: WukongIMMessage, payload: WukongIMMessagePayload): Promise<void>
  /** 撤回消息 */
  recallMessage(target: Message): Promise<boolean>
  /** 发送消息（乐观更新） */
  sendMessage(message: {
    type: number
    content: string
    quote?: QuoteInfo
    mentions?: MentionInfo[]
  }): Promise<boolean>
  /** 加载会话最新一页历史消息 */
  loadHistoryMessages(contact: Contact, startSeq?: number, limit?: number): Promise<void>
  /** 上拉加载更早的历史消息，返回本次新增条数 */
  loadMoreHistory(contact: Contact, limit?: number): Promise<number>
  /** 加载指定消息序号及其之后的消息（搜索结果定位） */
  loadMessagesFromSeq(contact: Contact, messageSeq: number, limit?: number): Promise<void>
}

export function createMessageModule(state: ChatState): MessageModule {
  const { uid, name, messages, activeContact, hasMoreHistory, loadingHistory } = state

  /**
   * 应用撤回信令：按消息序号（或客户端消息号）把目标消息标记为已撤回
   *
   * @param content 信令内容
   * @param channelId 信令所属频道
   */
  function applyRecall(content: RecallSignalContent, channelId: string): void {
    const targetSeq = content.originalMessageSeq
    const targetClientMsgNo = content.originalClientMsgNo

    const target = messages.value.find(
      (m) =>
        m.channelId === channelId &&
        !m.revoked &&
        ((targetSeq != null && m.messageSeq === targetSeq) ||
          (!!targetClientMsgNo && m.clientMsgNo === targetClientMsgNo)),
    )

    if (!target) {
      return
    }

    target.revoked = true
    syncRecalledPreview(target)
  }

  /**
   * 撤回后同步会话列表预览：被撤回的若是该频道最新消息，预览改为「消息已撤回」
   */
  function syncRecalledPreview(target: Message): void {
    const contact = state.contacts.value.find((c) => c.uid === target.channelId)

    if (!contact) {
      return
    }

    const isLatest = messages.value
      .filter((m) => m.channelId === target.channelId)
      .every((m) => m.id === target.id || m.timestamp <= target.timestamp)

    if (isLatest) {
      contact.lastMessage = '消息已撤回'
    }
  }

  /**
   * 处理收到的聊天消息
   */
  async function handleIncomingChatMessage(message: WukongIMMessage, payload: WukongIMMessagePayload): Promise<void> {
    // 防御：RTC 实时信令（200-208）不进聊天列表（正常应在 message-handler 已拦截）
    if (isRtcSignalType(payload.type)) {
      return
    }

    // 消息撤回信令：仅把目标消息标记为已撤回，不进入消息列表
    if (payload.type === MessageType.RECALL) {
      applyRecall((payload.content || {}) as RecallSignalContent, message.channelId)
      return
    }

    let senderName = ''
    if (message.channelType === 1) {
      if (!isNaN(Number(message.fromUid))) {
        const { nickname } = await fetchUserInfo(message.fromUid)
        senderName = nickname
      } else {
        const { nickname } = await getSystemUserInfo(message.fromUid)

        senderName = nickname
      }
    }
    const isSelf = message.fromUid === uid.value
    const isCurrentChannel =
      activeContact.value &&
      activeContact.value.uid === message.channelId &&
      activeContact.value.channelType === message.channelType

    // 追加到消息列表
    const incomingMessageId = `${message.messageSeq}_${message.messageId || ''}`

    const msg: any = payload.content
    // content 兼容：普通消息 content 为字符串直接使用；对象取其 content 字段
    const content = typeof msg === 'object' && msg !== null ? (msg.content ?? '') : (msg ?? '')

    const mentions = (payload.mentions as MentionInfo[] | undefined) || undefined
    // 是否 @ 到了当前登录用户（含 @所有人；自己发的不提醒自己）
    const mentionMe = !isSelf && isMentionedMe(mentions, uid.value)

    // 按 clientMsgNo（本地乐观消息）或「频道 + 消息序号」识别同一条消息，
    // 避免 IM 推送与历史拉取/乐观更新重叠时重复入列
    const existing = messages.value.find(
      (m) =>
        (!!message.clientMsgNo && m.clientMsgNo === message.clientMsgNo) ||
        (message.messageSeq != null &&
          m.messageSeq != null &&
          String(m.messageSeq) === String(message.messageSeq) &&
          m.channelId === message.channelId &&
          m.channelType === message.channelType),
    )

    if (existing) {
      // 消息已存在（IM 回显或重复推送）：仅补齐悟空分配的消息序号与消息 id
      if (message.messageSeq != null && existing.messageSeq == null) {
        existing.messageSeq = message.messageSeq
      }
      if (message.messageId != null && existing.messageId == null) {
        existing.messageId = message.messageId
      }
    } else {
      messages.value.push({
        id: incomingMessageId,
        fromUid: String(message.fromUid),
        nickname: isSelf ? name.value : senderName || message.fromUid,
        channelId: message.channelId,
        channelType: message.channelType,
        type: payload.type,
        content,
        timestamp: toMillis(message.timestamp),
        isSelf,
        clientMsgNo: message.clientMsgNo,
        messageSeq: message.messageSeq,
        quote: payload.quote as QuoteInfo | undefined,
        mentions,
        mentionMe,
      })
    }

    // 同步会话列表：更新/新增联系人、未读数、排序
    await upsertContactFromMessage(
      state.contacts.value,
      message,
      payload,
      uid.value,
      isSelf,
      !isCurrentChannel,
    )
  }

  /**
   * 撤回消息
   *
   * 服务端校验后广播信令；本地同时立即标记撤回态（原消息内容保留，供「重新编辑」使用）。
   *
   * @returns 是否撤回成功
   */
  async function recallMessage(target: Message): Promise<boolean> {
    if (target.messageSeq == null) {
      return false
    }

    try {
      const response = await apiRecallMessage({
        channelId: target.channelId,
        channelType: target.channelType,
        messageSeq: target.messageSeq,
        clientMsgNo: target.clientMsgNo,
      })

      if (response.code !== 200) {
        return false
      }

      target.revoked = true
      syncRecalledPreview(target)

      return true
    } catch (error) {
      console.error('[ChatStore] 撤回消息失败:', error)
      return false
    }
  }

  /**
   * 加载历史消息
   *
   * @param contact 联系人
   * @param startSeq 起始消息序号（0 表示从最新开始）
   * @param limit 拉取条数
   */
  async function loadHistoryMessages(
    contact: Contact,
    startSeq = 0,
    limit = 50,
  ): Promise<void> {
    // 切换会话时重置「是否还有更早消息」，供上拉加载判断
    hasMoreHistory.value = true

    try {
      const res = await getMessageList({
        channelId: contact.uid,
        channelType: contact.channelType,
        startMessageSeq: startSeq,
        endMessageSeq: 0,
        limit,
        pullMode: 0,
      })

      if (res.code !== 200 || !res.data) {
        return
      }

      const { messages: parsed, recallSignals } = parseHistoryResponse(res, contact)
      mergeHistoryMessages(parsed, recallSignals, contact)

      // 以接口返回判断是否还有更早的消息（more 优先，其次按本页是否拉满）
      hasMoreHistory.value = resolveHasMore(res.data, parsed.length, limit)
    } catch (error) {
      console.error('[ChatStore] 加载历史消息失败:', error)
    }
  }

  /**
   * 上拉加载更早的历史消息
   *
   * 悟空 IM 语义：以 startMessageSeq 为起点，pullMode=0 向序号更小（更早）的方向拉取，
   * endMessageSeq=0 表示不设下界、以 limit 为准。
   * 故以本地最旧的消息序号为起点向上翻页（返回结果包含 startMessageSeq，重复的会被去重）。
   *
   * @param contact 联系人
   * @param limit 单次拉取条数
   * @returns 本次新增的消息条数
   */
  async function loadMoreHistory(contact: Contact, limit = 30): Promise<number> {
    if (loadingHistory.value || !hasMoreHistory.value) {
      return 0
    }

    const channelSeqs = messages.value
      .filter((m) => m.channelId === contact.uid && m.channelType === contact.channelType)
      .map((m) => m.messageSeq)
      .filter((seq): seq is number => seq != null)

    if (channelSeqs.length === 0) {
      hasMoreHistory.value = false
      return 0
    }

    const oldestSeq = Math.min(...channelSeqs)

    if (oldestSeq <= 1) {
      hasMoreHistory.value = false
      return 0
    }

    loadingHistory.value = true

    try {
      const res = await getMessageList({
        channelId: contact.uid,
        channelType: contact.channelType,
        startMessageSeq: oldestSeq,
        endMessageSeq: 0,
        limit,
        pullMode: 0,
      })

      if (res.code !== 200 || !res.data) {
        return 0
      }

      const { messages: parsed, recallSignals } = parseHistoryResponse(res, contact)
      const added = mergeHistoryMessages(parsed, recallSignals, contact)

      hasMoreHistory.value = resolveHasMore(res.data, parsed.length, limit)

      // 本页没有任何新消息（全是被去重的数据），说明再往上翻也是重复，停止加载
      if (added === 0) {
        hasMoreHistory.value = false
      }

      return added
    } catch (error) {
      console.error('[ChatStore] 上拉加载历史消息失败:', error)
      return 0
    } finally {
      loadingHistory.value = false
    }
  }

  /**
   * 加载指定消息序号及其之后的消息
   *
   * <p>用于「查找聊天内容」结果跳转：目标消息可能不在已加载的历史窗口内，
   * 需以该序号为起点向更新方向拉取一段，保证目标消息进入列表。</p>
   *
   * @param contact 联系人
   * @param messageSeq 目标消息序号
   * @param limit 拉取条数
   */
  async function loadMessagesFromSeq(
    contact: Contact,
    messageSeq: number,
    limit = 30,
  ): Promise<void> {
    try {
      const res = await getMessageList({
        channelId: contact.uid,
        channelType: contact.channelType,
        startMessageSeq: messageSeq,
        endMessageSeq: 0,
        limit,
        pullMode: 1,
      })

      if (res.code !== 200 || !res.data) {
        return
      }

      const { messages: parsed, recallSignals } = parseHistoryResponse(res, contact)
      mergeHistoryMessages(parsed, recallSignals, contact)
    } catch (error) {
      console.error('[ChatStore] 定位消息加载失败:', error)
    }
  }

  /**
   * 解析历史消息接口返回，转换为本地消息结构，并分离出撤回信令
   */
  function parseHistoryResponse(
    res: any,
    contact: Contact,
  ): { messages: Message[]; recallSignals: RecallSignalContent[] } {
    const list = Array.isArray(res.data) ? res.data : res.data.messages || []
    const historyMessages: Message[] = []
    // 撤回信令先收集，等历史消息入列表后统一回放（保证换设备/重进会话后撤回态一致）
    const recallSignals: RecallSignalContent[] = []

    list.forEach((item: any) => {
      const msg = decodeBase64FromJava(item.payload || '')
      const payload = JSON.parse(msg)

      if (payload.type === MessageType.RECALL) {
        recallSignals.push((payload.content || {}) as RecallSignalContent)
        return
      }

      const isSelf = String(item.fromUid) === uid.value
      const mentions = (payload.mentions as MentionInfo[] | undefined) || undefined

      historyMessages.push({
        id: `${item.messageSeq}_${item.messageId || Date.now()}`,
        fromUid: String(item.fromUid || contact.uid),
        nickname: isSelf ? name.value : item.fromUid,
        channelId: contact.uid,
        channelType: contact.channelType,
        type: payload.type,
        content: extractMessageContent(payload),
        timestamp: toMillis(item.timestamp),
        isSelf,
        clientMsgNo: item.clientMsgNo,
        messageSeq: item.messageSeq,
        quote: (payload.quote as QuoteInfo | undefined) || undefined,
        mentions,
        mentionMe: !isSelf && isMentionedMe(mentions, uid.value),
      })
    })

    // 过滤 RTC 实时信令残留（历史数据防御，no_persist 正常时不会出现）
    const rtcFiltered = historyMessages.filter((m) => !isRtcSignalType(m.type))

    return { messages: rtcFiltered, recallSignals }
  }

  /**
   * 合并历史消息到消息列表：按 id 去重、按时间正序排列，并回放撤回信令
   *
   * @returns 本次新增的消息条数
   */
  function mergeHistoryMessages(
    newMessages: Message[],
    recallSignals: RecallSignalContent[],
    contact: Contact,
  ): number {
    // 以「频道 + 消息序号 / clientMsgNo」判定重复：
    // 历史拉取与 IM 推送生成的 id 可能不一致，仅按 id 去重会漏判导致重复消息
    const unique: Message[] = []

    for (const candidate of newMessages) {
      if (messages.value.some((m) => isSameMessage(m, candidate))) {
        continue
      }
      if (unique.some((m) => isSameMessage(m, candidate))) {
        continue
      }
      unique.push(candidate)
    }

    if (unique.length > 0) {
      messages.value.push(...unique)
      // 按时间正序排列；悟空时间戳为秒级，同秒内再按消息序号排序，避免上拉合并后乱序
      messages.value.sort(
        (a, b) => a.timestamp - b.timestamp || (a.messageSeq ?? 0) - (b.messageSeq ?? 0),
      )
    }

    // 回放撤回信令，标记已撤回的消息
    recallSignals.forEach((signal) => applyRecall(signal, contact.uid))

    return unique.length
  }

  /**
   * 发送消息
   *
   * 采用乐观更新策略：先写入本地消息列表并置顶会话，再调用后端 HTTP 接口转发至悟空 IM。
   * 统一走服务端便于后续做内容安全校验、敏感词过滤、图片鉴黄等业务扩展。
   *
   * @returns 是否发送成功（失败可能因群禁言等业务限制，调用方可据此刷新禁言状态）
   */
  async function sendMessage(message: {
    type: number
    content: string
    /** 引用的消息快照 */
    quote?: QuoteInfo
    /** @提醒的成员 */
    mentions?: MentionInfo[]
  }): Promise<boolean> {
    if (!activeContact.value || !message.content) {
      return false
    }

    const { type, content, quote, mentions } = message
    const contact = activeContact.value
    const now = Date.now()
    const clientMsgNo = `self_${now}_${Math.random().toString(36).slice(2)}`
    const isImage = type === MessageType.IMAGE

    // 1. 先追加到本地消息列表（乐观更新）
    messages.value.push({
      id: clientMsgNo,
      fromUid: uid.value,
      nickname: name.value,
      channelId: contact.uid,
      channelType: contact.channelType,
      type,
      content,
      timestamp: now,
      isSelf: true,
      clientMsgNo,
      quote,
      mentions,
    })

    // 2. 更新当前会话最后一条消息并刷新排序（置顶会话始终在最前）
    contact.lastMessage = isImage ? '[图片]' : content
    contact.lastMessageTime = now
    sortContacts(state.contacts.value)

    // 标记本地消息发送失败
    const markError = () => {
      const index = messages.value.findIndex((m) => m.id === clientMsgNo)
      if (index !== -1) {
        messages.value[index].isError = true
      }
    }

    // 3. 调用后端 HTTP 接口发送
    try {
      const response = await apiSendMessage({
        channelId: contact.uid,
        channelType: contact.channelType,
        type,
        content,
        clientMsgNo,
        quote,
        mentions,
      })

      if (response.code === 200) {
        // 用本地生成的消息号定位（不依赖回执里的 client_msg_no）
        const index = messages.value.findIndex((m) => m.id === clientMsgNo)

        if (index !== -1) {
          const messageSeq = pickSendReceiptField(response, 'message_seq', 'messageSeq')
          const messageId = pickSendReceiptField(response, 'message_id', 'messageId')

          if (messageSeq != null) {
            messages.value[index].messageSeq = messageSeq
          } else {
            console.warn('[ChatStore] 发送回执未取到消息序号，撤回/引用将不可用：', response)
          }

          if (messageId != null) {
            messages.value[index].messageId = messageId
          }
        }
        return true
      }

      markError()
      return false
    } catch (error) {
      console.error('[ChatStore] 发送消息失败:', error)
      markError()
      return false
    }
  }

  return {
    handleIncomingChatMessage,
    recallMessage,
    sendMessage,
    loadHistoryMessages,
    loadMoreHistory,
    loadMessagesFromSeq,
  }
}
