import { defineStore, storeToRefs } from 'pinia'
import { ref } from 'vue'
import useUserStore from '@/store/user.ts'
import { getUserInfo } from '@/api/user.ts'
import { wukongIMService, messageHandlerService, MessageType } from '@/services/im'
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
}

/** 默认文本消息负载 */
function buildTextPayload(content: string): WukongIMMessagePayload {
  return { type: MessageType.TEXT, content }
}

/** 生成联系人唯一标识 */
function getContactKey(channelId: string, channelType: number): string {
  return `${channelId}_${channelType}`
}

const useChatStore = defineStore('chat', () => {
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

    const { uid } = userInfo.value
    const wssKey = import.meta.env.VITE_WSS_KEY
    const serverUrl = getCookies(wssKey)

    console.log('[UserStore] 初始化 IM 连接', { wssKey, serverUrl, uid })

    if (!serverUrl || !uid) {
      console.warn('[UserStore] 缺少 IM 连接参数，跳过连接')
      return
    }

    // 连接悟空 IM
    if (serverUrl) {
      wukongIMService.connect({
        serverUrl: serverUrl,
        userInfo: {
          uid: String(uid),
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
    let name = ''
    let head = ''
    if (message.channelType === 1) {
      const { nickname, avatar } = await fetchUserInfo(message.fromUid)
      name = nickname
      head = avatar || ''
    }
    const { userInfo } = storeToRefs(useUserStore())

    const { uid } = userInfo.value

    const isSelf = message.fromUid === String(uid)
    const isCurrentChannel =
      activeContact.value &&
      activeContact.value.uid === message.channelId &&
      activeContact.value.channelType === message.channelType

    // 追加到消息列表
    messages.value.push({
      id: `${message.messageSeq}_${Date.now()}`,
      fromUid: message.fromUid,
      nickname: name,
      avatar: head,
      channelId: message.channelId,
      channelType: message.channelType,
      type: payload.type,
      content: payload.content || '',
      timestamp: message.timestamp || Date.now(),
      isSelf,
    })

    // 同步会话列表：更新/新增联系人、未读数、排序
    syncConversationFromMessage(message, payload, isSelf, !!isCurrentChannel)
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

    if (!contact) {
      contact = {
        uid: channelId,
        name: channelId,
        type: channelType === 2 ? 'group' : 'person',
        channelType: channelType as 1 | 2,
        unreadCount: 0,
        lastMessage: payload.content || '',
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
      contact.lastMessage = payload.content || ''
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
  function selectContact(contact: Contact) {
    activeContact.value = contact
    contact.unreadCount = 0
    wukongIMService.syncMessage(contact.uid, contact.channelType, 0, 50)
  }

  /**
   * 发送消息
   */
  async function sendMessage(content: string) {
    if (!activeContact.value || !content.trim()) {
      return
    }

    const payload = buildTextPayload(content.trim())

    if (activeContact.value.channelType === 2) {
      await wukongIMService.sendGroupMessage(activeContact.value.uid, payload)
    } else {
      await wukongIMService.sendPersonalMessage(activeContact.value.uid, payload)
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
