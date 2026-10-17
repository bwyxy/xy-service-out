import { defineStore, storeToRefs } from 'pinia'
import { ref } from 'vue'
import useUserStore from './user'
import { getUserInfo, getSystemUserInfo } from '@/api/user'
import { wukongIMService, messageHandlerService, MessageType, isRtcSignalType } from '@/services/im'
import { sendMessage as apiSendMessage, getMessageList } from '@/api/chat'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk'
import { getToken, getCookies } from '@/utils/auth'
import { WSS_KEY } from '@/utils/config'
import useNotificationStore from './notification'

/**
 * 聊天 Store
 *
 * 迁移自网页端 store/chat.ts，替换点：
 * - import.meta.env.VITE_WSS_KEY → utils/config 的 WSS_KEY 常量
 * - localStorage → uni.removeStorageSync
 * - 新增强制下线（220）信令注册，转由 App.vue 统一弹窗并清登录态
 */

/** 生成联系人唯一标识 */
function getContactKey(channelId, channelType) {
  return `${channelId}_${channelType}`
}

const useChatStore = defineStore('chat', () => {
  const uid = ref('')
  const name = ref('')
  const connected = ref(false)
  const contacts = ref([])
  const activeContact = ref(null)
  const messages = ref([])
  const conversations = ref([])
  const userInfo = ref(null)
  /** 刷新会话 */
  const isRefreshConversations = ref(false)

  const notificationStore = useNotificationStore()

  /** IM 监听是否已注册（避免重复绑定导致消息重复入列表） */
  let listenersBound = false

  /**
   * 初始化悟空 IM
   */
  async function initWKIM() {
    if (listenersBound) {
      return
    }

    const { userInfo: loginUserInfo } = storeToRefs(useUserStore())

    const userUid = loginUserInfo.value?.uid
    const serverUrl = getCookies(WSS_KEY)

    uid.value = userUid || ''

    console.log('[ChatStore] 初始化 IM 连接', { serverUrl, uid: userUid })

    if (!serverUrl || !uid.value) {
      console.warn('[ChatStore] 缺少 IM 连接参数，跳过连接')
      return
    }

    // 连接悟空 IM
    wukongIMService.connect({
      serverUrl,
      userInfo: {
        uid: uid.value,
        token: getToken(),
      },
    })
    connected.value = true
    listenersBound = true

    // 初始化消息分类处理器
    messageHandlerService.init()
    notificationStore.registerIMListener()

    // 监听普通聊天消息
    messageHandlerService.onChatMessage(handleIncomingChatMessage)

    // 监听强制下线通知（220）：交给 App.vue 做弹窗 + 清理登录态
    messageHandlerService.onForceLogout((message, payload) => {
      uni.$emit('force-logout', { content: payload.content })
    })

    // 监听会话更新
    wukongIMService.onConversationUpdate((list) => {
      conversations.value = list
      updateContactsFromConversations(list)
    })
  }

  /**
   * 处理收到的聊天消息
   */
  async function handleIncomingChatMessage(message, payload) {
    // 防御：RTC 实时信令（200-208）不进聊天列表（正常应在 message-handler 已拦截）
    if (isRtcSignalType(payload.type)) {
      return
    }

    let senderName = ''
    if (message.channelType === 1) {
      if (!isNaN(Number(message.fromUid))) {
        const user = await fetchUserInfo(message.fromUid)
        senderName = user.nickname
      } else {
        const systemUser = await getSystemUserInfo(message.fromUid)

        senderName = systemUser.data?.nickname
      }
    }

    const isSelf = message.fromUid === uid.value
    const isCurrentChannel =
      activeContact.value &&
      activeContact.value.uid === message.channelId &&
      activeContact.value.channelType === message.channelType

    const incomingMessageId = `${message.messageSeq}_${message.messageId || ''}`

    const rawContent = payload.content
    // content 兼容：普通消息 content 为字符串直接使用；对象取其 content 字段
    const content =
      typeof rawContent === 'object' && rawContent !== null
        ? (rawContent.content ?? '')
        : (rawContent ?? '')

    if (!messages.value.find((m) => String(m.id) === String(message.clientMsgNo))) {
      messages.value.push({
        id: incomingMessageId,
        fromUid: String(message.fromUid),
        nickname: isSelf ? name.value : senderName || message.fromUid,
        channelId: message.channelId,
        channelType: message.channelType,
        type: payload.type,
        content,
        timestamp: message.timestamp || Date.now(),
        isSelf,
      })
    }

    // 同步会话列表：更新/新增联系人、未读数、排序
    await syncConversationFromMessage(message, payload, isSelf, !isCurrentChannel)
  }

  /**
   * 查询用户信息
   */
  async function fetchUserInfo(userUid) {
    const result = await getUserInfo(Number(userUid))

    if (result.code === 200) {
      return result.data
    }
    return {
      nickname: '未知用户',
      avatar: '',
    }
  }

  /**
   * 消息到达时同步会话列表
   *
   * - 若联系人已存在，更新最后一条消息与时间，并增加未读数
   * - 若联系人不存在，新增联系人并异步获取昵称/头像
   * - 按最后消息时间倒序排列，确保最新消息置顶
   */
  async function syncConversationFromMessage(message, payload, isSelf, isCurrentChannel) {
    const channelId = message.channelId
    const channelType = message.channelType
    const now = Date.now()

    let contact = contacts.value.find((c) => c.uid === channelId && c.channelType === channelType)

    const content = extractMessageContent(payload)

    if (!contact) {
      contact = {
        uid: channelId,
        name: channelId,
        type: channelType === 2 ? 'group' : 'person',
        channelType,
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
  function sortContactsByLastMessage() {
    contacts.value.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
  }

  /**
   * 从会话列表更新联系人
   */
  function updateContactsFromConversations(list) {
    const cs = list.map((c) => ({
      uid: c.channelId,
      name: c.channelName || c.channelId,
      type: c.channelType === 2 ? 'group' : 'person',
      channelType: c.channelType,
      unreadCount: c.unreadCount || 0,
      lastMessage: c.lastMessage || '',
      lastMessageTime: c.lastMessageTime,
      avatar: c.avatar,
    }))

    const existingMap = new Map(
      contacts.value.map((c) => [getContactKey(c.uid, c.channelType), c]),
    )

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
  function addContact(contactUid, contactName, avatar) {
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
  async function selectContact(contact) {
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
  async function loadHistoryMessages(contact, startSeq = 0, limit = 50) {
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
      const historyMessages = list.map((item) => {
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

      // 过滤 RTC 实时信令残留（历史数据防御，no_persist 正常时不会出现）
      const rtcFiltered = historyMessages.filter((m) => !isRtcSignalType(m.type))

      // 去重：相同 messageSeq 只保留一条
      const existingIds = new Set(messages.value.map((m) => m.id))
      const newMessages = rtcFiltered.filter((m) => !existingIds.has(m.id))

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
  async function sendMessage(message) {
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
    listenersBound = false
    contacts.value = []
    activeContact.value = null
    messages.value = []
    conversations.value = []
    notificationStore.reset()
    uni.removeStorageSync('chat_uid')
    uni.removeStorageSync('chat_name')
    uni.removeStorageSync('chat_token')
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
    loadHistoryMessages,
    resetChatData,
    isRefreshConversations,
  }
})

export default useChatStore
