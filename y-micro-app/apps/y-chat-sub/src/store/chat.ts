import { defineStore } from 'pinia'
import { ref } from 'vue'
import { initWK, disconnect, getIM, sendPersonalMessage, sendGroupMessage } from '@/utils/wk'
import { chatLogin } from '@/api/chat'

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

  /** 登录聊天 */
  async function login(userId: string, userName: string) {
    const res = await chatLogin(userId, userName)
    const data = res.data
    uid.value = data.uid
    name.value = userName
    token.value = data.token
    wsUrl.value = data.wsUrl

    // 连接悟空IM
    const im = initWK({ uid: data.uid, token: data.token, wsUrl: data.wsUrl })
    connected.value = true

    // 监听消息
    im.onMessage((msg: any) => {
      const msgData = typeof msg.content === 'string' ? JSON.parse(msg.content) : msg.content
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

    return data
  }

  /** 从会话列表更新联系人 */
  function updateContactsFromConversations(list: any[]) {
    const cs: Contact[] = list.map((c: any) => ({
      uid: c.channelId,
      name: c.channelId,
      type: c.channelType === 2 ? 'group' : 'person',
      channelType: c.channelType as 1 | 2,
    }))
    // 合并去重
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
    // 加载该联系人的消息
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
  }

  return {
    uid, name, token, wsUrl, connected,
    contacts, activeContact, messages, conversations,
    login, addContact, selectContact, sendMessage, logout,
  }
})

export default useChatStore