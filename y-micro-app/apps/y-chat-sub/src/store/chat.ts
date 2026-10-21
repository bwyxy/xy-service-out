import { defineStore } from 'pinia'
import { createChatState } from './chat/context.ts'
import { createConversationModule } from './chat/conversation.ts'
import { createMessageModule } from './chat/message.ts'
import { createConnectionModule } from './chat/connection.ts'

/**
 * 聊天 store
 *
 * 状态集中在这里创建，具体职责拆到 store/chat 下：
 * - connection：IM 连接与处理器注册
 * - conversation：会话列表（未读/置顶/排序/删除/切换）
 * - message：消息接收、发送、撤回与历史分页
 *
 * 对外 API 与拆分前保持一致，调用方无需感知内部结构。
 */
const useChatStore = defineStore('chat', () => {
  const state = createChatState()
  const message = createMessageModule(state)
  const conversation = createConversationModule(state, {
    loadHistoryMessages: message.loadHistoryMessages,
  })
  const connection = createConnectionModule(state, { conversation, message })

  return {
    ...state,
    ...connection,
    ...conversation,
    ...message,
  }
})

export default useChatStore
