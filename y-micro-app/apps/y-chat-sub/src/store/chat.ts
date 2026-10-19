import { defineStore, storeToRefs } from 'pinia'
import { ref } from 'vue'
import useUserStore from '@/store/user.ts'
import { getUserInfo, getSystemUserInfo } from '@/api/user.ts'
import { wukongIMService, messageHandlerService, MessageType, isRtcSignalType } from '@/services/im'
import {
  sendMessage as apiSendMessage,
  getMessageList,
  recallMessage as apiRecallMessage,
  setConversationTop as apiSetConversationTop,
} from '@/api/chat.ts'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk'
import { getToken, getCookies } from '@/utils/auth.ts'
import useNotificationStore from './notification'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im'

/** 联系人 */
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

/** @所有人的保留 uid */
export const MENTION_ALL_UID = 'all'

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
interface RecallSignalContent {
  /** 原消息序号 */
  originalMessageSeq?: number
  /** 原消息客户端消息号 */
  originalClientMsgNo?: string
  /** 原消息发送者 */
  fromUid?: string
}

/** 撤回窗口期（毫秒），与后端保持一致 */
export const RECALL_WINDOW_MS = 2 * 60 * 1000

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

/** 生成联系人唯一标识 */
function getContactKey(channelId: string, channelType: number): string {
  return `${channelId}_${channelType}`
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
function isSameMessage(a: Message, b: Message): boolean {
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
function toMillis(timestamp?: number): number {
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
function pickSendReceiptField(response: any, ...keys: string[]): any {
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
function resolveHasMore(data: any, parsedCount: number, limit: number): boolean {
  const more = data?.more

  if (typeof more === 'number') {
    return more === 1
  }

  return parsedCount >= limit
}

const useChatStore = defineStore('chat', () => {
  const uid = ref('')
  const name = ref('')
  const connected = ref(false)
  const contacts = ref<Contact[]>([])
  const activeContact = ref<Contact | null>(null)
  const messages = ref<Message[]>([])
  const conversations = ref<unknown[]>([])
  const userInfo = ref<Record<string, unknown> | null>(null)
  /** 刷新会话 */
  const isRefreshConversations = ref<boolean>(false)
  /** 是否还有更早的历史消息可加载 */
  const hasMoreHistory = ref<boolean>(true)
  /** 是否正在上拉加载历史消息（并发锁） */
  const loadingHistory = ref<boolean>(false)

  const notificationStore = useNotificationStore()

  /**
   *  初始化悟空 IM
   */
  async function initWKIM() {

    const { userInfo } = storeToRefs(useUserStore())

    const userUid = userInfo.value?.uid as string | undefined
    const wssKey = import.meta.env.VITE_WSS_KEY
    const serverUrl = getCookies(wssKey)

    uid.value = userUid || ''

    console.log('[UserStore] 初始化 IM 连接', { wssKey, serverUrl, uid: userUid })

    if (!serverUrl || !uid.value) {
      console.warn('[UserStore] 缺少 IM 连接参数，跳过连接')
      return
    }

    // 连接悟空 IM
    if (serverUrl) {
      wukongIMService.connect({
        serverUrl: serverUrl,
        userInfo: {
          uid: uid.value,
          token: getToken(),
        },
      })
      connected.value = true

      // 初始化消息分类处理器
      messageHandlerService.init()
      notificationStore.registerIMListener()

      // 监听普通聊天消息
      messageHandlerService.onChatMessage(handleIncomingChatMessage)

      // 监听会话更新
      wukongIMService.onConversationUpdate((list: unknown[]) => {

        console.log('会话列表', list)
        conversations.value = list
        updateContactsFromConversations(list)
      })
    }
  }

  /**
   * 处理收到的聊天消息
   */
  async function handleIncomingChatMessage(message: WukongIMMessage, payload: WukongIMMessagePayload): Promise<void> {

    console.log('接收来的消息---->', message)
    // 防御：RTC 实时信令（200-208）不进聊天列表（正常应在 message-handler 已拦截）
    if (isRtcSignalType(payload.type)) {
      return
    }

    // 消息撤回信令：仅把目标消息标记为已撤回，不进入消息列表
    if (payload.type === MessageType.RECALL) {
      applyRecall((payload.content || {}) as RecallSignalContent, message.channelId)
      return
    }

    console.log('聊天消息', message, payload)
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
    // const content = extractMessageContent(payload)
    // const incomingClientMsgNo = message.clientMsgNo
    const incomingMessageId = `${message.messageSeq}_${message.messageId || ''}`

    const msg :any = payload.content
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
    await syncConversationFromMessage(message, payload, isSelf, !isCurrentChannel)
  }

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
    const contact = contacts.value.find((c) => c.uid === target.channelId)

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
   * 查询用户信息
   * */
  async function fetchUserInfo(uid: string) {
    const result = await getUserInfo(Number(uid))

    if (result.code === 200) {
      return result.data
    }
    return {
      nickname: '未知用户',
      avatar: ''
    }
  }

  /**
   * 消息到达时同步会话列表
   *
   * - 若联系人已存在，更新最后一条消息与时间，并增加未读数
   * - 若联系人不存在，新增联系人并异步获取昵称/头像
   * - 按最后消息时间倒序排列，确保最新消息置顶
   */
  async function syncConversationFromMessage(
    message: WukongIMMessage,
    payload: WukongIMMessagePayload,
    isSelf: boolean,
    isCurrentChannel: boolean,
  ): Promise<void> {
    const channelId = message.channelId
    const channelType = message.channelType
    const now = Date.now()

    let contact = contacts.value.find(
      (c) => c.uid === channelId && c.channelType === channelType,
    )

    const content = extractMessageContent(payload)

    // 被 @ 到时在会话列表预览加「[有人@我]」提醒
    const mentionedMe =
      !isSelf &&
      isMentionedMe(payload.mentions as Array<{ uid?: string | number }> | undefined, uid.value)
    const preview = mentionedMe ? `[有人@我] ${content}` : content

    console.log(content)
    if (!contact) {
      contact = {
        uid: channelId,
        name: channelId,
        type: channelType === 2 ? 'group' : 'person',
        channelType: channelType as 1 | 2,
        unreadCount: 0,
        lastMessage: preview,
        lastMessageTime: now,
      }

      // 个人频道尝试获取发送者昵称/头像
      if (channelType === 1) {
        try {
          const user = await fetchUserInfo(message.fromUid)
          contact.name = user.nickname || contact.name
          contact.avatar = user.avatar || contact.avatar
        } catch (error) {
          console.warn('[ChatStore] 获取发送者信息失败:', error)
        }
      }

      contacts.value.push(contact)
    } else {
      contact.lastMessage = preview
      contact.lastMessageTime = now
    }

    // 非自己发送且不在当前会话，才增加未读数
    if (!isSelf && !isCurrentChannel) {
      contact.unreadCount += 1
    }

    // 按置顶优先、最后消息时间倒序排列
    sortContacts()
  }

  /**
   * 设置/取消会话置顶
   *
   * 会话列表来自悟空 IM，置顶状态由后端 t_conversation_setting 持久化。
   */
  async function setContactTop(contact: Contact, top: boolean): Promise<boolean> {
    try {
      const response = await apiSetConversationTop({
        channelId: contact.uid,
        channelType: contact.channelType,
        top: top ? 1 : 0,
      })

      if (response?.code !== 200) {
        return false
      }

      contact.top = top ? 1 : 0
      sortContacts()

      return true
    } catch {
      return false
    }
  }

  /**
   * 联系人列表排序：置顶会话优先，其余按最后消息时间倒序
   */
  function sortContacts(): void {
    contacts.value.sort(
      (a, b) =>
        Number(b.top || 0) - Number(a.top || 0) ||
        (b.lastMessageTime || 0) - (a.lastMessageTime || 0),
    )
  }

  /**
   * 从会话列表更新联系人
   */
  function updateContactsFromConversations(list: unknown[]) {
    const cs: Contact[] = list.map((c: any) => ({
      uid: c.channelId,
      name: c.channelName || c.channelId,
      type: c.channelType === 2 ? 'group' : 'person',
      channelType: c.channelType as 1 | 2,
      unreadCount: c.unreadCount || 0,
      lastMessage: c.lastMessage || '',
      lastMessageTime: c.lastMessageTime,
      avatar: c.avatar,
    }))

    const existingMap = new Map(contacts.value.map((c) => [getContactKey(c.uid, c.channelType), c]))

    for (const c of cs) {
      const key = getContactKey(c.uid, c.channelType)
      const existing = existingMap.get(key)

      if (existing) {
        // 保留本地未读数，后端返回的未读数作为兜底
        existing.unreadCount = existing.unreadCount || c.unreadCount
        existing.lastMessage = c.lastMessage || existing.lastMessage
        existing.lastMessageTime = c.lastMessageTime || existing.lastMessageTime
      } else {
        contacts.value.push(c)
      }
    }
  }

  /**
   * 添加好友为联系人
   */
  function addContact(contactUid: string, contactName: string, avatar?: string) {
    const isExist = contacts.value.find((c) => c.uid === contactUid && c.channelType === 1)
    if (!isExist) {
      contacts.value.push({
        uid: contactUid,
        name: contactName,
        type: 'person',
        channelType: 1,
        unreadCount: 0,
        avatar,
      })
    }
  }

  /**
   * 选择联系人
   */
  async function selectContact(contact: Contact) {
    activeContact.value = contact
    contact.unreadCount = 0
    await loadHistoryMessages(contact, 0, 50)
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
    sortContacts()

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

  /**
   * 清除数据
   */
  function resetChatData() {
    wukongIMService.disconnect()
    connected.value = false
    contacts.value = []
    activeContact.value = null
    messages.value = []
    conversations.value = []
    notificationStore.reset()
    localStorage.removeItem('chat_uid')
    localStorage.removeItem('chat_name')
    localStorage.removeItem('chat_token')
  }

  return {
    name,
    connected,
    contacts,
    activeContact,
    messages,
    conversations,
    userInfo,
    initWKIM,
    addContact,
    selectContact,
    sendMessage,
    recallMessage,
    resetChatData,
    isRefreshConversations,
    hasMoreHistory,
    loadingHistory,
    loadMoreHistory,
    loadMessagesFromSeq,
    setContactTop,
    sortContacts
  }
})

export default useChatStore
