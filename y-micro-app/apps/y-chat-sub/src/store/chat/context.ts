import { ref, type Ref } from 'vue'
import type { Contact, Message } from '@/types/chat.ts'

/**
 * 聊天 store 的共享状态
 *
 * 各职责模块（连接、会话、消息）通过它读写同一份数据，
 * 保证状态只有一处来源，跨模块调用不会出现数据副本。
 */
export interface ChatState {
  /** 当前登录用户 uid */
  uid: Ref<string>
  /** 当前登录用户昵称 */
  name: Ref<string>
  /** IM 是否已连接 */
  connected: Ref<boolean>
  /** 会话列表（含未读数、置顶、最后消息） */
  contacts: Ref<Contact[]>
  /** 当前打开的会话 */
  activeContact: Ref<Contact | null>
  /** 全部已加载消息（按当前会话过滤后展示） */
  messages: Ref<Message[]>
  /** 悟空 IM 推送的会话列表原始数据 */
  conversations: Ref<unknown[]>
  /** 触发会话列表重新拉取 */
  isRefreshConversations: Ref<boolean>
  /** 是否还有更早的历史消息可加载 */
  hasMoreHistory: Ref<boolean>
  /** 是否正在上拉加载历史消息（并发锁） */
  loadingHistory: Ref<boolean>
}

export function createChatState(): ChatState {
  return {
    uid: ref(''),
    name: ref(''),
    connected: ref(false),
    contacts: ref<Contact[]>([]),
    activeContact: ref<Contact | null>(null),
    messages: ref<Message[]>([]),
    conversations: ref<unknown[]>([]),
    isRefreshConversations: ref(false),
    hasMoreHistory: ref(true),
    loadingHistory: ref(false),
  }
}
