import { defineStore } from 'pinia'
import { ref } from 'vue'
import { initWK, disconnect, getIM, sendPersonalMessage, sendGroupMessage, decodePayload } from '@/utils/wk'
import { login as apiLogin, getUserInfo as apiGetUserInfo } from '@/api/chat'

export interface Contact {
  uid: string
  name: string
  type: 'person' | 'group'
  channelType: 1 | 2
}

export interface Message {
  id: string
  fromUid: string
  channelId: string
  channelType: number
  content: string
  timestamp: number
  isSelf: boolean
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
  const conversations = ref<any[]>([])
  const userInfo = ref<any>(null)

  /** 登录 */
  async function login(account: string, password: string) {
    const res = await apiLogin(account, password)
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

    // 连接悟空IM
    if (data.wsUrl) {
      const im = initWK({
        uid: data.uid,
        token: data.token || '',
        wsUrl: data.wsUrl,
      })
      connected.value = true

      // 监听消息
      im.onMessage((msg: any) => {
        // 解码 base64 payload → { type, content }
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
      im.onConversationUpdate((list: any[]) => {
        conversations.value = list
        updateContactsFromConversations(list)
      })
    }

    return data
  }

  /** 获取用户信息 */
  async function fetchUserInfo() {
    try {
      const res = await apiGetUserInfo()
      if (res.code === 200) {
        userInfo.value = res.data
        uid.value = res.data.uid
        name.value = res.data.nickname || '用户'
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  /** 从会话列表更新联系人 */
  function updateContactsFromConversations(list: any[]) {
    const cs: Contact[] = list.map((c: any) => ({
      uid: c.channelId,
      name: c.channelId,
      type: c.channelType === 2 ? 'group' : 'person',
      channelType: c.channelType as 1 | 2,
    }))
    const existingIds = new Set(contacts.value.map(c => c.uid + c.channelType))
    for (const c of cs) {
      if (!existingIds.has(c.uid + c.channelType)) {
        contacts.value.push(c)
      }
    }
  }

  /** 添加好友为联系人 */
  function addContact(contactUid: string, contactName: string) {
    if (!contacts.value.find(c => c.uid === contactUid && c.channelType === 1)) {
      contacts.value.push({ uid: contactUid, name: contactName, type: 'person', channelType: 1 })
    }
  }

  /** 选择联系人 */
  function selectContact(contact: Contact) {
    activeContact.value = contact
    const im = getIM()
    if (im) {
      im.syncMessage(contact.uid, contact.channelType, 0, 50)
    }
  }

  /** 发送消息 */
  function sendMessage(content: string) {
    if (!activeContact.value || !content.trim()) return
    const im = getIM()
    if (!im) return

    if (activeContact.value.channelType === 2) {
      sendGroupMessage(im, activeContact.value.uid, content)
    } else {
      sendPersonalMessage(im, activeContact.value.uid, content)
    }
  }

  /** 退出登录 */
  function logout() {
    disconnect()
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
    uid, name, token, wsUrl, connected,
    contacts, activeContact, messages, conversations, userInfo,
    login, fetchUserInfo, addContact, selectContact, sendMessage, logout,
  }
})

export default useChatStore