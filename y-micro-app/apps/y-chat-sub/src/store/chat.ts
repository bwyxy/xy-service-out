import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wukongIMService } from '@/services/im'
import { decodePayload } from '@/utils/wk'
import { login as apiLogin, getUserInfo as apiGetUserInfo } from '@/api/auth'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im'

/** 联系人 */
export interface Contact {
  uid: string
  name: string
  type: 'person' | 'group'
  channelType: 1 | 2
}

/** 聊天消息 */
export interface Message {
  id: string
  fromUid: string
  channelId: string
  channelType: number
  content: string
  timestamp: number
  isSelf: boolean
}

/** 默认文本消息负载 */
function buildTextPayload(content: string): WukongIMMessagePayload {
  return { type: 1, content }
}

const useChatStore = defineStore('chat', () => {
  const uid = ref('')
  const name = ref('')
  const token = ref('')
  const wsUrl = ref('')
  const connected = ref(false)
  const contacts = ref<Contact[]>([])
  const activeContact = ref<Contact | null>(null)
  const messages = ref<Message[]>([])
  const conversations = ref<unknown[]>([])
  const userInfo = ref<Record<string, unknown> | null>(null)

  /**
   * 登录并连接悟空 IM
   * @param account 账号
   * @param password 密码
   */
  async function login(account: string, password: string) {
    const res = await apiLogin({ account, password })
    const data = res.data

    uid.value = data.uid
    name.value = data.nickname || '用户'
    wsUrl.value = data.wsUrl

    // 保存登录信息
    localStorage.setItem('chat_uid', data.uid)
    localStorage.setItem('chat_name', data.nickname || '用户')
    if (data.token) {
      localStorage.setItem('chat_token', data.token)
      token.value = data.token
    }

    // 连接悟空 IM
    if (data.wsUrl) {
      wukongIMService.connect({
        serverUrl: data.wsUrl,
        userInfo: {
          uid: String(data.uid),
          token: data.token || '',
        },
      })
      connected.value = true

      // 监听消息
      wukongIMService.onMessage((msg: WukongIMMessage) => {
        const msgData = decodePayload(msg.payload || '')
        messages.value.push({
          id: `${msg.messageSeq}_${Date.now()}`,
          fromUid: msg.fromUid,
          channelId: msg.channelId,
          channelType: msg.channelType,
          content: msgData.content || '',
          timestamp: msg.timestamp || Date.now(),
          isSelf: msg.fromUid === uid.value,
        })
      })

      // 监听会话更新
      wukongIMService.onConversationUpdate((list: unknown[]) => {
        conversations.value = list
        updateContactsFromConversations(list)
      })
    }

    return data
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo() {
    try {
      const res = await apiGetUserInfo()
      if (res.code === 200) {
        userInfo.value = res.data
        uid.value = res.data.uid
        name.value = res.data.nickname || '用户'
      }
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  /**
   * 从会话列表更新联系人
   */
  function updateContactsFromConversations(list: unknown[]) {
    const cs: Contact[] = list.map((c: any) => ({
      uid: c.channelId,
      name: c.channelId,
      type: c.channelType === 2 ? 'group' : 'person',
      channelType: c.channelType as 1 | 2,
    }))

    const existingIds = new Set(contacts.value.map((c) => c.uid + c.channelType))
    for (const c of cs) {
      if (!existingIds.has(c.uid + c.channelType)) {
        contacts.value.push(c)
      }
    }
  }

  /**
   * 添加好友为联系人
   */
  function addContact(contactUid: string, contactName: string) {
    const isExist = contacts.value.find((c) => c.uid === contactUid && c.channelType === 1)
    if (!isExist) {
      contacts.value.push({ uid: contactUid, name: contactName, type: 'person', channelType: 1 })
    }
  }

  /**
   * 选择联系人
   */
  function selectContact(contact: Contact) {
    activeContact.value = contact
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
   * 退出登录
   */
  function logout() {
    wukongIMService.disconnect()
    connected.value = false
    uid.value = ''
    name.value = ''
    token.value = ''
    contacts.value = []
    activeContact.value = null
    messages.value = []
    conversations.value = []
    userInfo.value = null
    localStorage.removeItem('chat_uid')
    localStorage.removeItem('chat_name')
    localStorage.removeItem('chat_token')
  }

  return {
    uid,
    name,
    token,
    wsUrl,
    connected,
    contacts,
    activeContact,
    messages,
    conversations,
    userInfo,
    login,
    fetchUserInfo,
    addContact,
    selectContact,
    sendMessage,
    logout,
  }
})

export default useChatStore
