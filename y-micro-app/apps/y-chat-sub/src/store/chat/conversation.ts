/** 会话（联系人）列表职责：未读、置顶、排序、删除、会话切换 */
import {
  setConversationTop as apiSetConversationTop,
  clearConversationMessages as apiClearConversationMessages,
  deleteConversation as apiDeleteConversation,
} from '@/api/chat.ts'
import { getUserInfo } from '@/api/user.ts'
import { findGroupInfo } from '@/api/group.ts'
import { extractMessageContent } from '@/utils/wk.ts'
import { isMentionedMe } from '@/utils/chat.ts'
import type { WukongIMMessage, WukongIMMessagePayload } from '@/services/im'
import type { Contact } from '@/types/chat.ts'
import type { ChatState } from './context.ts'

/** 生成联系人唯一标识 */
function getContactKey(channelId: string, channelType: number): string {
  return `${channelId}_${channelType}`
}

/**
 * 联系人列表排序：置顶会话优先，其余按最后消息时间倒序
 */
export function sortContacts(contacts: Contact[]): void {
  contacts.sort(
    (a, b) =>
      Number(b.top || 0) - Number(a.top || 0) ||
      (b.lastMessageTime || 0) - (a.lastMessageTime || 0),
  )
}

/**
 * 查询用户信息
 */
export async function fetchUserInfo(uid: string) {
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
export async function upsertContactFromMessage(
  contacts: Contact[],
  message: WukongIMMessage,
  payload: WukongIMMessagePayload,
  selfUid: string,
  isSelf: boolean,
  isCurrentChannel: boolean,
): Promise<void> {
  const channelId = message.channelId
  const channelType = message.channelType
  const now = Date.now()

  let contact = contacts.find(
    (c) => c.uid === channelId && c.channelType === channelType,
  )

  const content = extractMessageContent(payload)

  // 被 @ 到时在会话列表预览加「[有人@我]」提醒
  const mentionedMe =
    !isSelf &&
    isMentionedMe(payload.mentions as Array<{ uid?: string | number }> | undefined, selfUid)
  const preview = mentionedMe ? `[有人@我] ${content}` : content

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
    } else if (channelType === 2) {
      // 群频道尝试获取群名称/头像，避免新会话直接展示频道 ID
      try {
        const result = await findGroupInfo(channelId)

        if (result?.code === 200 && result.data) {
          contact.name = result.data.name || contact.name
          contact.avatar = result.data.avatar || contact.avatar
        }
      } catch (error) {
        console.warn('[ChatStore] 获取群信息失败:', error)
      }
    }

    contacts.push(contact)
  } else {
    contact.lastMessage = preview
    contact.lastMessageTime = now
  }

  // 非自己发送且不在当前会话，才增加未读数
  if (!isSelf && !isCurrentChannel) {
    contact.unreadCount += 1
  }

  // 按置顶优先、最后消息时间倒序排列
  sortContacts(contacts)
}

export interface ConversationModule {
  /** 从 IM 推送的会话列表更新联系人 */
  updateContactsFromConversations(list: unknown[]): void
  /** 添加好友/群为联系人 */
  addContact(contactUid: string, contactName: string, avatar?: string, channelType?: 1 | 2): void
  /** 选择联系人（打开会话并加载历史消息） */
  selectContact(contact: Contact): Promise<void>
  /** 设置/取消会话置顶 */
  setContactTop(contact: Contact, top: boolean): Promise<boolean>
  /** 删除聊天记录（会话保留） */
  clearMessages(contact: Contact): Promise<boolean>
  /** 删除会话（仅当前用户不可见） */
  deleteContact(contact: Contact): Promise<boolean>
  /** 联系人排序 */
  sortContacts(): void
}

export function createConversationModule(
  state: ChatState,
  deps: {
    /** 打开会话时加载历史消息（由消息模块提供） */
    loadHistoryMessages: (contact: Contact, startSeq?: number, limit?: number) => Promise<void>
  },
): ConversationModule {
  const { contacts, activeContact, messages } = state

  /** 是否为当前正在展示的会话 */
  function isActiveContact(contact: Contact): boolean {
    if (!activeContact.value) {
      return false
    }

    return (
      getContactKey(activeContact.value.uid, activeContact.value.channelType) ===
      getContactKey(contact.uid, contact.channelType)
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
  function addContact(
    contactUid: string,
    contactName: string,
    avatar?: string,
    channelType: 1 | 2 = 1,
  ) {
    const isExist = contacts.value.find(
      (c) => c.uid === contactUid && c.channelType === channelType,
    )
    if (!isExist) {
      contacts.value.push({
        uid: contactUid,
        name: contactName,
        type: channelType === 2 ? 'group' : 'person',
        channelType,
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
    await deps.loadHistoryMessages(contact, 0, 50)
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
      sortContacts(contacts.value)

      return true
    } catch {
      return false
    }
  }

  /**
   * 删除聊天记录
   *
   * 仅清空当前用户的聊天记录，会话保留，对方仍可正常查看。
   */
  async function clearMessages(contact: Contact): Promise<boolean> {
    try {
      const response = await apiClearConversationMessages({
        channelId: contact.uid,
        channelType: contact.channelType,
      })

      if (response?.code !== 200) {
        return false
      }
    } catch {
      return false
    }

    // 仅在当前会话正在展示时清空消息列表
    if (isActiveContact(contact)) {
      messages.value = []
    }

    contact.lastMessage = ''

    return true
  }

  /**
   * 删除会话
   *
   * 仅当前用户不可见（聊天记录一并隐藏），收到新消息后会重新出现，对方不受影响。
   */
  async function deleteContact(contact: Contact): Promise<boolean> {
    try {
      const response = await apiDeleteConversation({
        channelId: contact.uid,
        channelType: contact.channelType,
      })

      if (response?.code !== 200) {
        return false
      }
    } catch {
      return false
    }

    const key = getContactKey(contact.uid, contact.channelType)
    const index = contacts.value.findIndex((c) => getContactKey(c.uid, c.channelType) === key)

    if (index > -1) {
      contacts.value.splice(index, 1)
    }

    if (isActiveContact(contact)) {
      activeContact.value = null
      messages.value = []
    }

    return true
  }

  return {
    updateContactsFromConversations,
    addContact,
    selectContact,
    setContactTop,
    clearMessages,
    deleteContact,
    sortContacts: () => sortContacts(contacts.value),
  }
}
