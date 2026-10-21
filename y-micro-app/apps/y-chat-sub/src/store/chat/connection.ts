/** IM 连接职责：建立连接、注册消息处理器、断开与数据清理 */
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user.ts'
import { wukongIMService, messageHandlerService } from '@/services/im'
import { getCookies, getToken } from '@/utils/auth.ts'
import useNotificationStore from '@/store/notification.ts'
import type { ChatState } from './context.ts'
import type { ConversationModule } from './conversation.ts'
import type { MessageModule } from './message.ts'

export interface ConnectionModule {
  /** 初始化悟空 IM 连接 */
  initWKIM(): Promise<void>
  /** 断开连接并清空本地数据 */
  resetChatData(): void
}

export function createConnectionModule(
  state: ChatState,
  modules: { conversation: ConversationModule; message: MessageModule },
): ConnectionModule {
  const notificationStore = useNotificationStore()

  /**
   * IM 事件处理器是否已注册
   *
   * 处理器注册在 wukongIMService / messageHandlerService 这两个单例上，
   * 页面生命周期内一直有效，因此只需注册一次；重复注册会让同一条消息
   * 被处理多次（消息重复、未读数翻倍）。
   */
  let imHandlersBound = false

  /**
   *  初始化悟空 IM
   */
  async function initWKIM() {
    const { userInfo } = storeToRefs(useUserStore())

    const userUid = userInfo.value?.uid as string | undefined
    const wssKey = import.meta.env.VITE_WSS_KEY
    const serverUrl = getCookies(wssKey)

    state.uid.value = userUid || ''

    console.log('[UserStore] 初始化 IM 连接')

    if (!serverUrl || !state.uid.value) {
      console.warn('[UserStore] 缺少 IM 连接参数，跳过连接')
      return
    }

    // 连接悟空 IM
    wukongIMService.connect({
      serverUrl,
      userInfo: {
        uid: state.uid.value,
        token: getToken(),
      },
    })
    state.connected.value = true

    // 幂等：消息处理器只注册一次，避免重复调用 initWKIM 时叠加处理器
    if (imHandlersBound) {
      return
    }

    // 初始化消息分类处理器
    messageHandlerService.init()
    notificationStore.registerIMListener()

    // 监听普通聊天消息
    messageHandlerService.onChatMessage(modules.message.handleIncomingChatMessage)

    // 监听会话更新
    wukongIMService.onConversationUpdate((list: unknown[]) => {
      state.conversations.value = list
      modules.conversation.updateContactsFromConversations(list)
    })

    imHandlersBound = true
  }

  /**
   * 清除数据
   */
  function resetChatData() {
    wukongIMService.disconnect()
    state.connected.value = false
    state.contacts.value = []
    state.activeContact.value = null
    state.messages.value = []
    state.conversations.value = []
    notificationStore.reset()
    localStorage.removeItem('chat_uid')
    localStorage.removeItem('chat_name')
    localStorage.removeItem('chat_token')
  }

  return { initWKIM, resetChatData }
}
