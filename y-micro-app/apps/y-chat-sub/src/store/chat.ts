import { defineStore, storeToRefs } from 'pinia'
import { ref } from 'vue'
import useUserStore from '@/store/user.ts'
import { getUserInfo } from '@/api/user.ts'
import { wukongIMService, messageHandlerService, MessageType } from '@/services/im'
import { sendMessage as apiSendMessage, getMessageList } from '@/api/chat.ts'
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
}

/** 生成联系人唯一标识 */
function getContactKey(channelId: string, channelType: number): string {
  return `${channelId}_${channelType}`
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

    console.log('聊天消息', message, payload)
    let senderName = ''
    if (message.channelType === 1) {
      if (!isNaN(Number(message.fromUid))) {
        const { nickname } = await fetchUserInfo(message.fromUid)
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

    if (!messages.value.find(m => String(m.id) === String(message.clientMsgNo))) {
      messages.value.push({
        id: incomingMessageId,
        fromUid: String(message.fromUid),
        nickname: isSelf ? name.value : senderName || message.fromUid,
        channelId: message.channelId,
        channelType: message.channelType,
        type: payload.type,
        content: msg.content || '',
        timestamp: message.timestamp || Date.now(),
        isSelf,
      })
    }

    // 同步会话列表：更新/新增联系人、未读数、排序
    await syncConversationFromMessage(message, msg, isSelf, !isCurrentChannel)
  }

  /**
   * 查询用户信息
   * */
  async function fetchUserInfo(uid: string) {
    const result = await getUserInfo(uid)

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

    console.log(content)
    if (!contact) {
      contact = {
        uid: channelId,
        name: channelId,
        type: channelType === 2 ? 'group' : 'person',
        channelType: channelType as 1 | 2,
        unreadCount: 0,
        lastMessage: content,
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
      contact.lastMessage = content
      contact.lastMessageTime = now
    }

    // 非自己发送且不在当前会话，才增加未读数
    if (!isSelf && !isCurrentChannel) {
      contact.unreadCount += 1
    }

    // 按最后消息时间倒序排列
    sortContactsByLastMessage()
  }

  /**
   * 按最后消息时间倒序排序联系人列表
   */
  function sortContactsByLastMessage(): void {
    contacts.value.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
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

      const list = Array.isArray(res.data) ? res.data : res.data.messages || []
      const historyMessages: Message[] = list.map((item: any) => {
        const msg = decodeBase64FromJava(item.payload || '')
        const payload = JSON.parse(msg)
        const isSelf = String(item.fromUid) === uid.value
        const content = extractMessageContent(payload)

        return {
          id: `${item.messageSeq}_${item.messageId || Date.now()}`,
          fromUid: String(item.fromUid || contact.uid),
          nickname: isSelf ? name.value : item.fromUid,
          channelId: contact.uid,
          channelType: contact.channelType,
          type: payload.type,
          content,
          timestamp: item.timestamp || Date.now(),
          isSelf,
        }
      })

      // 去重：相同 messageSeq 只保留一条
      const existingIds = new Set(messages.value.map((m) => m.id))
      const newMessages = historyMessages.filter((m) => !existingIds.has(m.id))

      messages.value.push(...newMessages)

      // 按时间正序排列，便于展示
      messages.value.sort((a, b) => a.timestamp - b.timestamp)
    } catch (error) {
      console.error('[ChatStore] 加载历史消息失败:', error)
    }
  }

  /**
   * 发送消息
   *
   * 采用乐观更新策略：先写入本地消息列表并置顶会话，再调用后端 HTTP 接口转发至悟空 IM。
   * 统一走服务端便于后续做内容安全校验、敏感词过滤、图片鉴黄等业务扩展。
   */
  async function sendMessage(message: { type: number; content: string }): Promise<void> {
    if (!activeContact.value || !message.content) {
      return
    }

    const { type, content } = message
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
    })

    // 2. 更新当前会话最后一条消息并置顶
    contact.lastMessage = isImage ? '[图片]' : content
    contact.lastMessageTime = now
    sortContactsByLastMessage()

    // 3. 调用后端 HTTP 接口发送
    try {
      const response = await apiSendMessage({
        channelId: contact.uid,
        channelType: contact.channelType,
        type,
        content,
        clientMsgNo,
      })

      if (response.code === 200) {
        const msg = response.data.data

        const index = messages.value.findIndex((m) => m.id === msg.client_msg_no)

        if (index !== -1) {
          messages.value[index].messageId = msg.message_id
        }
      } else {
        const index = messages.value.findIndex((m) => m.id === clientMsgNo)

        if (index !== -1) {
          messages.value[index].isError = true
        }
      }
    } catch (error) {
      console.error('[ChatStore] 发送消息失败:', error)
      throw error
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
    resetChatData
  }
})

export default useChatStore
