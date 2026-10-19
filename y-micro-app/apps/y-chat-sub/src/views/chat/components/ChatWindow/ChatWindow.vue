<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, nextTick, computed } from 'vue'
import { storeToRefs } from 'pinia'
import useChatStore, { canRecallMessage, canReeditMessage, canCopyMessage, canQuoteMessage, buildQuoteInfo } from '@/store/chat.ts'
import type { Message, MentionInfo, QuoteInfo } from '@/store/chat.ts'
import useCallStore from '@/store/call.ts'
import useUserStore from '@/store/user.ts'
import { MessageType } from '@/services/im/constants'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { getUserInfo, getSystemUserInfo } from '@/api/user.ts'
import { findGroupInfo, findGroupMembers } from '@/api/group.ts'
import RichInput from './RichInput.vue'
import ChatInfo from "@/views/chat/components/ChatWindow/ChatInfo.vue";
import MessageBox from "@/components/MessageBox/MessageBox.vue";
import GroupInfo from "@/views/chat/components/ChatWindow/GroupInfo.vue";

const chatStore = useChatStore()
const callStore = useCallStore()
const { activeContact, messages } = storeToRefs(chatStore)
const { userInfo } = storeToRefs(useUserStore())

const sending = ref(false)
const isShowChatInfo = ref<boolean>(false)
const messageListRef = ref<HTMLDivElement | null>(null)
/** 消息内容容器，监听其高度变化以在贴底时保持滚动到底部 */
const messageContentRef = ref<HTMLDivElement | null>(null)
let contentObserver: ResizeObserver | null = null
/** 是否处于贴底状态：用户上滑查看历史后置为 false，滚回底部自动恢复 */
const stickToBottom = ref(true)
/** 列表顶部哨兵，用于 IntersectionObserver 兜底触发上拉加载 */
const historySentinelRef = ref<HTMLDivElement | null>(null)
let historyObserver: IntersectionObserver | null = null
/** 是否正在上拉加载更早的消息 */
const loadingMore = ref(false)
const channelInfo = ref<{ nickname?: string; avatar?: string, id?: string, isBan?: number }>({})
const richInputRef = ref<InstanceType<typeof RichInput> | null>(null)
// 群成员
const groupMembers = ref<GroupMember[]>([])
/** 当前待引用的消息 */
const quoteTarget = ref<Message | null>(null)

/** 本人昵称（用于生成引用快照的发送者名） */
const selfName = computed(() => String(userInfo.value?.nickname || '我'))

/** 输入框上方引用条的展示内容 */
const quotePreview = computed<QuoteInfo | null>(() => {
  if (!quoteTarget.value) return null
  return buildQuoteInfo(quoteTarget.value, selfName.value)
})

/**
 * 引用条展示的发送者名称
 *
 * 快照里的 nickname 可能是 uid（历史消息只带 uid），优先用群成员昵称覆盖。
 */
const quotePreviewName = computed(() => {
  const quote = quotePreview.value
  if (!quote) return ''

  const member = groupMembers.value.find((m) => String(m.uid) === String(quote.fromUid))
  if (member) {
    return member.groupNickname || member.nickname || quote.fromUid
  }

  // 单聊中对方即引用消息的发送者；引用自己的消息时快照已写入本人昵称
  if (activeContact.value?.channelType === 1 && String(quote.fromUid) !== String(userInfo.value?.uid)) {
    return activeContact.value?.name || quote.nickname || quote.fromUid
  }

  return quote.nickname || quote.fromUid
})

/** 当前会话的消息列表 */
const currentMessages = computed(() => {
  if (!activeContact.value) return []

  return messages.value.filter(
    (m) =>
      m.channelId === activeContact.value!.uid &&
      m.channelType === activeContact.value!.channelType,
  )
})

/** 相邻消息时间间隔超过该值时插入时间分隔（与微信一致，5 分钟） */
const TIME_DIVIDER_GAP = 5 * 60 * 1000

/**
 * 是否需要在该条消息前显示时间
 *
 * 首条必显示；其余仅当与上一条间隔超过阈值时才显示，
 * 这样同一时段连续发送的几条消息只会显示最早的那一个时间。
 */
const shouldShowTimeDivider = (index: number): boolean => {
  if (index === 0) return true

  const current = currentMessages.value[index]
  const previous = currentMessages.value[index - 1]

  if (!current || !previous) return true

  return current.timestamp - previous.timestamp > TIME_DIVIDER_GAP
}

/** 消息时间展示：今天显示 HH:mm，昨天带「昨天」，更早显示具体日期 */
const formatMessageTime = (timestamp: number): string => {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startOfThatDay = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const DAY = 24 * 60 * 60 * 1000

  if (startOfThatDay === startOfToday) {
    return time
  }
  if (startOfToday - startOfThatDay === DAY) {
    return `昨天 ${time}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
  }

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}
/** 是否系统用户 */
const isSystemUser = computed(() => {
  console.log('是否是系统用户', activeContact.value)
  if (!activeContact.value || activeContact.value.channelType !== 1) return false

  return activeContact.value.channelType === 1 && isNaN(Number(activeContact.value.uid));

})

/** 当前登录用户在群内的成员信息 */
const currentGroupMember = computed(() => {
  if (!activeContact.value || activeContact.value.channelType !== 2) return undefined

  return groupMembers.value.find((m) => String(m.uid) === String(userInfo.value.uid))
})

/** 当前登录用户在群内是否为群主/管理员 */
const isGroupAdmin = computed(() => (currentGroupMember.value?.role ?? 0) > 0)

/** 禁言提示文案，为空表示可正常发言 */
const muteTip = computed(() => {
  if (!activeContact.value || activeContact.value.channelType !== 2) return ''

  const muteEndTime = currentGroupMember.value?.muteEndTime

  if (muteEndTime && new Date(muteEndTime.replace(/-/g, '/')).getTime() > Date.now()) {
    return '您已被禁言，暂时无法发送消息'
  }

  if (channelInfo.value.isBan === 1 && !isGroupAdmin.value) {
    return '群主已开启全员禁言，仅群主/管理员可发言'
  }

  return ''
})

/** 获取频道信息 */
const loadChannelInfo = async () => {
  if (!activeContact.value) {
    channelInfo.value = {}
    return
  }

  if (activeContact.value.channelType === 1) {
    let response;
    if (!isNaN(Number(activeContact.value.uid))) {
      response = await getUserInfo(Number(activeContact.value.uid))
    } else {
      response = await getSystemUserInfo(activeContact.value.uid)
    }
    if (response && response.code === 200) {
      channelInfo.value = response.data || {}
      // 同步昵称/头像到联系人
      activeContact.value.name = response.data?.nickname || activeContact.value.name
      activeContact.value.avatar = response.data?.avatar || activeContact.value.avatar
    } else {
      channelInfo.value = { nickname: activeContact.value.name }
    }
  } else if (activeContact.value.channelType === 2) {
    console.log('群聊ID', activeContact.value.uid)
    await getGroupInfo(activeContact.value.uid)
    await getGroupMembers(activeContact.value.uid)
  } else {
    channelInfo.value = { nickname: activeContact.value.name }
  }
}

/** 查询群聊信息 */
const getGroupInfo = async (channelId: string) => {
  const result = await findGroupInfo(channelId)

  if (result.code === 200) {
    channelInfo.value = result.data || {}
  }
}

/** 查询群成员 */
const getGroupMembers = async (channelId: string) => {
  const result = await findGroupMembers(channelId)

  if (result.code === 200) {
    groupMembers.value = result.data
  }
}

/** 群资料变更后刷新群信息与成员列表 */
const refreshGroup = async () => {
  if (!activeContact.value || activeContact.value.channelType !== 2) return

  await getGroupInfo(activeContact.value.uid)
  await getGroupMembers(activeContact.value.uid)
}

/** 发送文本消息 */
const handleSendText = async (text: string, mentions?: MentionInfo[]) => {
  if (sending.value || !activeContact.value) return

  sending.value = true
  try {
    const quote = quoteTarget.value ? buildQuoteInfo(quoteTarget.value, selfName.value) : undefined
    const success = await chatStore.sendMessage({ type: MessageType.TEXT, content: text, quote, mentions })

    if (success) {
      quoteTarget.value = null
      scrollToBottom()
    } else {
      // 发送失败可能因被禁言/全员禁言，重新同步群状态以隐藏输入框
      await refreshGroup()
    }
  } finally {
    sending.value = false
  }
}

/** 发送图片消息 */
const handleSendImage = async (url: string) => {
  if (sending.value || !activeContact.value) return

  sending.value = true
  try {
    const quote = quoteTarget.value ? buildQuoteInfo(quoteTarget.value, selfName.value) : undefined
    const success = await chatStore.sendMessage({ type: MessageType.IMAGE, content: url, quote })

    if (success) {
      quoteTarget.value = null
      scrollToBottom()
    } else {
      // 发送失败可能因被禁言/全员禁言，重新同步群状态以隐藏输入框
      await refreshGroup()
    }
  } finally {
    sending.value = false
  }
}

/** 撤回消息 */
const handleRecall = async (msg: Message) => {
  const success = await chatStore.recallMessage(msg)

  if (success) {
    ElMessage.success('已撤回')
  } else {
    ElMessage.warning('撤回失败，可能已超过 2 分钟')
  }
}

/** 撤回后重新编辑：回填原文本内容到输入框 */
const handleReedit = (msg: Message) => {
  if (msg.type !== MessageType.TEXT) return
  richInputRef.value?.setContent(msg.content)
}

/** 右键菜单状态 */
const contextMenu = ref<{
  visible: boolean
  msg: Message | null
  canCopy: boolean
  canQuote: boolean
  canRecall: boolean
  canReedit: boolean
}>({ visible: false, msg: null, canCopy: false, canQuote: false, canRecall: false, canReedit: false })
const contextMenuRef = ref<HTMLDivElement | null>(null)
const contextMenuStyle = ref<{ left: string; top: string }>({ left: '0px', top: '0px' })

/** 打开消息右键菜单（仅当存在可用操作时） */
const openContextMenu = async (e: MouseEvent, msg: Message) => {
  const canCopy = canCopyMessage(msg)
  const canQuote = canQuoteMessage(msg)
  const canRecall = canRecallMessage(msg)
  const canReedit = canReeditMessage(msg)

  if (!canCopy && !canQuote && !canRecall && !canReedit) {
    closeContextMenu()
    return
  }

  contextMenu.value = { visible: true, msg, canCopy, canQuote, canRecall, canReedit }
  await nextTick()

  // 视口边缘收敛，避免菜单溢出屏幕
  const menuW = contextMenuRef.value?.offsetWidth ?? 96
  const menuH = contextMenuRef.value?.offsetHeight ?? 40
  const maxX = window.innerWidth - menuW - 8
  const maxY = window.innerHeight - menuH - 8

  contextMenuStyle.value = {
    left: `${Math.min(e.clientX, maxX)}px`,
    top: `${Math.min(e.clientY, maxY)}px`,
  }
}

/** 关闭消息右键菜单 */
const closeContextMenu = () => {
  contextMenu.value.visible = false
  contextMenu.value.msg = null
}

/** 右键菜单：复制 */
const onMenuCopy = async () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (!msg) return

  try {
    await navigator.clipboard.writeText(msg.content)
    ElMessage.success('已复制')
  } catch (error) {
    console.error('[ChatWindow] 复制失败:', error)
    ElMessage.error('复制失败')
  }
}

/** 右键菜单：引用 */
const onMenuQuote = () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (!msg) return

  quoteTarget.value = msg
  richInputRef.value?.focus()
}

/** 右键菜单：撤回 */
const onMenuRecall = async () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (msg) await handleRecall(msg)
}

/** 右键菜单：重新编辑 */
const onMenuReedit = () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (msg) handleReedit(msg)
}

/** 让目标消息整片区域闪烁几下（仿微信 @/引用 定位效果） */
const flashMessageEl = (el: HTMLElement) => {
  // 先移除再强制重排，保证连续点击时动画能重新播放
  el.classList.remove('msg-flash')
  void el.offsetWidth
  el.classList.add('msg-flash')
  window.setTimeout(() => el.classList.remove('msg-flash'), 1800)
}

/** 滚动到目标消息并闪烁 */
const scrollToMessageEl = (el: HTMLElement) => {
  el.scrollIntoView({ block: 'center', behavior: 'smooth' })
  flashMessageEl(el)
}

/** 按本地消息 id 找到消息对应的 DOM */
const findMessageEl = (msg: Message): HTMLElement | null => {
  const list = messageListRef.value
  if (!list) return null
  return list.querySelector<HTMLElement>(`[data-msg-key="${CSS.escape(msg.id)}"]`)
}

/** 定位到被引用的原消息并闪烁 */
const handleQuoteClick = (quote: QuoteInfo) => {
  const list = messageListRef.value
  if (!list) return

  let target: HTMLElement | null = null

  if (quote.messageSeq != null) {
    target = list.querySelector<HTMLElement>(`[data-message-seq="${quote.messageSeq}"]`)
  }
  if (!target && quote.messageId != null) {
    target = list.querySelector<HTMLElement>(`[data-message-id="${CSS.escape(String(quote.messageId))}"]`)
  }

  if (!target) {
    ElMessage.info('原消息不在当前会话中')
    return
  }

  scrollToMessageEl(target)
}

/** 当前会话中 @我 且尚未查看过的消息 */
const unackedMentions = computed(() =>
  currentMessages.value.filter((m) => m.mentionMe && !m.mentionAck),
)

/** 是否展示「有人@我」气泡 */
const mentionBubbleVisible = computed(() => unackedMentions.value.length > 0)

/** 点击「有人@我」气泡：定位到下一条未读 @消息 并闪烁 */
const jumpToMention = () => {
  const target = unackedMentions.value[0]
  if (!target) return

  // 标记已查看，避免重复提醒
  target.mentionAck = true

  const el = findMessageEl(target)
  if (!el) return

  scrollToMessageEl(el)
}

/**
 * 把已出现在可视区域内的 @我 消息标记为已查看
 *
 * 用户滚动/停留在消息所在位置后即视为已看到，不再提醒。
 */
const markVisibleMentionsAcked = () => {
  const listEl = messageListRef.value
  if (!listEl || unackedMentions.value.length === 0) return

  const listRect = listEl.getBoundingClientRect()

  unackedMentions.value.forEach((msg) => {
    const msgEl = findMessageEl(msg)
    if (!msgEl) return

    const rect = msgEl.getBoundingClientRect()
    const visible = rect.bottom > listRect.top && rect.top < listRect.bottom

    if (visible) {
      msg.mentionAck = true
    }
  })
}

/** Esc 关闭右键菜单 */
const onContextMenuKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') closeContextMenu()
}

/** 滚动到底部 */
const scrollToBottom = () => {
  stickToBottom.value = true
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
    // 滚到底后最新的消息已可见，同步清理对应提醒
    markVisibleMentionsAcked()
  })
}

/** 消息列表滚动到顶部附近时上拉加载更早的历史消息 */
const handleMessageScroll = async () => {
  const el = messageListRef.value
  const contact = activeContact.value

  // 滚动过程中随时检查 @我 消息是否已被看到
  markVisibleMentionsAcked()

  if (!el || !contact) {
    return
  }

  // 记录贴底状态：用户上滑查看历史后不再自动贴底，滚回底部自动恢复
  stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight <= 40

  // 距顶部超过阈值不处理，避免每次滚动都判断
  if (el.scrollTop > 40) {
    return
  }

  if (loadingMore.value || !chatStore.hasMoreHistory) {
    return
  }

  loadingMore.value = true

  // 记录加载前的滚动高度与位置，加载后据此把视口固定在原位置
  const prevScrollHeight = el.scrollHeight
  const prevScrollTop = el.scrollTop

  try {
    await chatStore.loadMoreHistory(contact)
  } finally {
    await nextTick()
    const elAfter = messageListRef.value
    if (elAfter) {
      elAfter.scrollTop = elAfter.scrollHeight - prevScrollHeight + prevScrollTop
    }
    loadingMore.value = false
  }
}

/** 发起语音通话 */
const handleAudioCall = () => {
  if (!activeContact.value) return
  callStore.startCall({
    uid: activeContact.value.uid,
    nickname: channelInfo.value.nickname || activeContact.value.name || activeContact.value.uid,
    avatar: channelInfo.value.avatar || activeContact.value.avatar || '',
  }, 1)
}

/** 发起视频通话 */
const handleVideoCall = () => {
  if (!activeContact.value) return
  callStore.startCall({
    uid: activeContact.value.uid,
    nickname: channelInfo.value.nickname || activeContact.value.name || activeContact.value.uid,
    avatar: channelInfo.value.avatar || activeContact.value.avatar || '',
  }, 2)
}

/** 顶部哨兵进入视口即触发加载，兼容滚动发生在祖先容器的场景 */
const setupHistoryObserver = () => {
  if (!historySentinelRef.value || typeof IntersectionObserver === 'undefined') {
    return
  }

  historyObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        handleMessageScroll()
      }
    })
  })

  historyObserver.observe(historySentinelRef.value)
}

/**
 * 监听消息内容高度变化
 *
 * 切换会话后历史消息是异步合并进来的、消息内的图片也是异步加载的，
 * 这些都会在滚动到底部之后继续撑高内容。处于贴底状态时重新滚到底，
 * 避免滚动条停在中间（不触底）的位置。
 */
const setupContentObserver = () => {
  if (!messageContentRef.value || typeof ResizeObserver === 'undefined') {
    return
  }

  contentObserver = new ResizeObserver(() => {
    if (!stickToBottom.value || loadingMore.value) {
      return
    }
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })

  contentObserver.observe(messageContentRef.value)
}

watch(
  () => activeContact.value?.uid,
  () => {
    // 切换会话时清空待引用消息，避免引用到其他会话的消息
    quoteTarget.value = null
    loadChannelInfo()
    scrollToBottom()
  },
  { immediate: true },
)

// 仅当「最新一条消息」变化时滚到底部：
// 上拉加载更早消息时最新一条不变，不会把视口弹到底部
watch(
  () => currentMessages.value[currentMessages.value.length - 1]?.id,
  () => {
    if (loadingMore.value) return
    scrollToBottom()
  },
)

/** 收到当前群的禁言通知时刷新群状态，实时同步输入框可用性 */
watch(
  () => messages.value[messages.value.length - 1],
  (last) => {
    if (!last || last.type !== MessageType.GROUP_MUTE) return
    if (activeContact.value?.channelType !== 2) return
    if (last.channelId !== activeContact.value.uid) return

    refreshGroup()
  },
)

onMounted(() => {
  // loadChannelInfo 已由 activeContact 的 immediate watcher 触发，此处不再重复调用
  scrollToBottom()
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu, true)
  window.addEventListener('resize', closeContextMenu)
  window.addEventListener('keydown', onContextMenuKeydown)
  // 兜底：哨兵元素进入视口即触发上拉加载，兼容「滚动发生在祖先容器」的场景
  setupHistoryObserver()
  // 内容高度变化时保持贴底
  setupContentObserver()
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('scroll', closeContextMenu, true)
  window.removeEventListener('resize', closeContextMenu)
  window.removeEventListener('keydown', onContextMenuKeydown)
  historyObserver?.disconnect()
  historyObserver = null
  contentObserver?.disconnect()
  contentObserver = null
})
</script>

<template>
  <div class="chat-window">
    <div class="chat-window-header">
      <img
          v-if="activeContact?.avatar"
          class="header-avatar"
          :src="activeContact.avatar"
          alt="avatar"
      />
      <span class="channel-name">{{ channelInfo.nickname || activeContact?.name || '未知' }}</span>
      <div class="chat-window-header-rt">
        <el-dropdown v-if="!isSystemUser && (activeContact && activeContact.channelType === 1)">
          <div class="chat-rt-icon" title="视频/语音">
            <img src="@/assets/images/video_audio_icon.png" alt="icon" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleAudioCall">语音通话</el-dropdown-item>
              <el-dropdown-item @click="handleVideoCall">视频通话</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="chat-rt-icon" title="更多操作" @click="isShowChatInfo = !isShowChatInfo">
          <img src="@/assets/images/more_icon.png" alt="icon" />
        </div>
      </div>
    </div>

    <div class="chat-window-content">
      <div class="chat-window-lt">
        <div ref="messageListRef" class="chat-window-main" @scroll.passive="handleMessageScroll">
          <!-- 内容容器：监听高度变化，处于贴底状态时保持滚动到底部 -->
          <div ref="messageContentRef" class="message-content-wrap">
            <div ref="historySentinelRef" class="history-sentinel"></div>
            <div v-if="currentMessages.length > 0" class="history-tip">
              <span v-if="loadingMore">正在加载更早的消息...</span>
              <span v-else-if="chatStore.hasMoreHistory">上拉加载更早的消息</span>
              <span v-else>没有更多消息了</span>
            </div>
            <template v-for="(msg, index) in currentMessages" :key="msg.id">
              <div v-if="shouldShowTimeDivider(index)" class="message-time">
                {{ formatMessageTime(msg.timestamp) }}
              </div>
              <div
                  class="message-item"
                  :class="{ self: msg.isSelf }"
                  :data-msg-key="msg.id"
                  :data-message-seq="msg.messageSeq"
                  :data-message-id="msg.messageId"
                  @contextmenu.prevent="openContextMenu($event, msg)"
              >
                <message-box
                    :item="msg"
                    :channelId="activeContact ? activeContact.uid : ''"
                    :avatar="activeContact ? activeContact.avatar : ''"
                    :contact-name="activeContact ? activeContact.name : ''"
                    :members="groupMembers"
                    @quote-click="handleQuoteClick"
                />
              </div>
            </template>

            <div v-if="currentMessages.length === 0" class="empty-hint">
              暂无消息，发送一条消息开始聊天吧
            </div>
          </div>
        </div>

        <div class="chat-window-footer">
          <!-- 「有人@我」气泡：定位在输入框上方，点击跳转到未读 @消息 -->
          <div
              v-if="mentionBubbleVisible"
              class="mention-bubble"
              title="点击定位到 @我 的消息"
              @click="jumpToMention"
          >
            <span class="mention-bubble__text">有人@我</span>
            <span v-if="unackedMentions.length > 1" class="mention-bubble__count">
              {{ unackedMentions.length }}
            </span>
          </div>

          <!-- 群禁言时隐藏输入框，展示提示 -->
          <div v-if="muteTip" class="mute-tip">{{ muteTip }}</div>
          <template v-else>
            <!-- 引用条：展示待引用的消息，可关闭 -->
            <div v-if="quotePreview" class="quote-bar">
              <div class="quote-bar__body">
                <span class="quote-bar__name">{{ quotePreviewName }}</span>
                <span class="quote-bar__text">{{ quotePreview.content }}</span>
              </div>
              <el-icon class="quote-bar__close" title="取消引用" @click="quoteTarget = null">
                <Close />
              </el-icon>
            </div>
            <RichInput
                ref="richInputRef"
                :isSystemUser="isSystemUser"
                :disabled="!activeContact"
                :members="groupMembers"
                :enable-mention="activeContact?.channelType === 2"
                @send-text="handleSendText"
                @send-image="handleSendImage"
            />
          </template>
        </div>
      </div>
      <!-- 聊天信息 -->
      <chat-info
          v-model="isShowChatInfo"
          :info="channelInfo"
          v-if="activeContact && activeContact.channelType === 1"
      />

      <group-info
          v-model="isShowChatInfo"
          v-if="activeContact && activeContact.channelType === 2"
          :members="groupMembers"
          :info="channelInfo"
          @refresh="refreshGroup"
      />
    </div>

    <!-- 消息右键菜单（仿微信）：复制 / 撤回 / 重新编辑 -->
    <teleport to="body">
      <div
          v-if="contextMenu.visible"
          ref="contextMenuRef"
          class="msg-context-menu"
          :style="contextMenuStyle"
          @click.stop
      >
        <div
            v-if="contextMenu.canCopy"
            class="msg-context-menu__item"
            @click="onMenuCopy"
        >复制</div>
        <div
            v-if="contextMenu.canQuote"
            class="msg-context-menu__item"
            @click="onMenuQuote"
        >引用</div>
        <div
            v-if="contextMenu.canRecall"
            class="msg-context-menu__item"
            @click="onMenuRecall"
        >撤回</div>
        <div
            v-if="contextMenu.canReedit"
            class="msg-context-menu__item"
            @click="onMenuReedit"
        >重新编辑</div>
      </div>
    </teleport>
  </div>
</template>

<style scoped lang="scss">
.chat-window {
  width: 100%;
  height: 100%;
  background: #fafafa;
  transition: 1s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  .chat-window-content {
    flex: 1;
    transition: 1s;
    display: flex;
    overflow: hidden;
  }
  &-lt {
    flex: 1;
    height: 100%;
    transition: 1s;
    display: flex;
    flex-direction: column;
    position: relative;
  }

  &-header {
    height: size(70);
    background: #fff;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    box-sizing: border-box;
    padding: 0 size(32);
    display: flex;
    align-items: center;

    .header-avatar {
      width: size(36);
      height: size(36);
      border-radius: 50%;
      margin-right: size(8);
    }

    .channel-name {
      font-size: size(18);
      color: #1f1f1f;
    }

    &-rt {
      flex: 1;
      display: flex;
      justify-content: flex-end;
      .chat-rt-icon {
        width: size(42);
        height: size(42);
        border-radius: 4px;
        cursor: pointer;
        @include flexCenter;
        img {
          width: size(24);
          height: size(24);
        }
        &:hover {
          background: rgba(0, 0, 0, .06);
        }
      }
    }
  }

  &-main {
    flex: 1;
    overflow-y: auto;
    padding: size(24);
    box-sizing: border-box;

    &::-webkit-scrollbar {
      appearance: none;
      width: 0;
    }

    // 时间分隔：同一时段的消息只在最早一条前显示
    .message-time {
      margin: 0 auto size(12);
      font-size: size(12);
      color: #b2b2b2;
      text-align: center;
      user-select: none;
    }

    // 顶部哨兵：无高度，仅用于 IntersectionObserver 触发上拉加载
    .history-sentinel {
      height: 1px;
    }

    // 上拉加载更早消息时的提示
    .history-tip {
      margin-bottom: size(12);
      font-size: size(12);
      color: #b2b2b2;
      text-align: center;
      user-select: none;
    }

    .message-item {
      display: flex;
      margin-bottom: size(16);

      .msg-image-box {
        .message-image {
          max-width: size(240);
          max-height: size(240);
          border-radius: size(8);
          cursor: pointer;
          object-fit: cover;
        }
      }

      .message-bubble {
        max-width: size(480);
        padding: size(12) size(16);
        background: #fff;
        border-radius: size(8);
        box-shadow: 0 1px 2px rgba(0, 0, 0, .06);

        .message-meta {
          display: flex;
          align-items: center;
          gap: size(12);
          margin-bottom: size(6);

          .sender-name {
            font-size: size(14);
            color: #409eff;
            font-weight: bold;
          }

          .message-time {
            font-size: size(12);
            color: #999;
          }
        }

        .message-content {
          font-size: size(15);
          color: #333;
          line-height: 1.5;
          white-space: pre-wrap;
          word-break: break-word;
        }
      }

      &.self {
        flex-direction: row-reverse;
        //justify-content: flex-;

        .user-head {
          margin-left: size(13);
          margin-right: 0 !important;
        }

        .error-msg {
          margin-left: 0 !important;
          margin-right: size(12);
        }

        .message-bubble {
          background: $theme-color;

          .message-content {
            color: #fff;
          }

          .message-meta {
            justify-content: flex-end;

            .sender-name {
              color: #67c23a;
            }
          }
        }
      }
    }

    .empty-hint {
      text-align: center;
      color: #999;
      padding-top: size(120);
    }
  }

  &-footer {
    box-sizing: border-box;
    padding: size(8);
    position: relative;
    //background: #fff;
    //border-top: 1px solid rgba(0, 0, 0, .06);
  }
}

.user-head {
  width: size(40);
  height: size(40);
  border-radius: 50px;
  margin-right: size(13);
  margin-top: size(4);
}

.mute-tip {
  height: size(48);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: size(8);
  color: #8c8c8c;
  font-size: size(14);
  letter-spacing: 1px;
}

// 引用条：待引用的消息预览
.quote-bar {
  display: flex;
  align-items: center;
  gap: size(8);
  margin-bottom: size(8);
  padding: size(8) size(12);
  background: #f5f5f5;
  border-left: 3px solid #d9d9d9;
  border-radius: size(6);

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: size(12);
    color: #999;
  }

  &__text {
    font-size: size(13);
    color: #666;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__close {
    cursor: pointer;
    color: #999;
    flex-shrink: 0;

    &:hover {
      color: #666;
    }
  }
}

// 「有人@我」气泡：悬浮在输入框上方
.mention-bubble {
  position: absolute;
  right: size(16);
  bottom: calc(100% + 8px);
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: size(4);
  padding: size(5) size(12);
  background: #fff;
  border: 1px solid rgba(64, 158, 255, .5);
  border-radius: size(14);
  box-shadow: 0 2px 10px rgba(0, 0, 0, .12);
  cursor: pointer;
  user-select: none;
  transition: background .2s;

  &__text {
    font-size: size(13);
    color: #409eff;
  }

  &__count {
    font-size: size(11);
    color: #fff;
    background: #f56c6c;
    border-radius: size(8);
    padding: 0 size(5);
    line-height: 1.5;
  }

  &:hover {
    background: #ecf5ff;
  }
}

// 点击引用块/「有人@我」跳转到原消息时，整片区域闪烁几下
.message-item.msg-flash {
  border-radius: size(6);
  animation: msgFlash .6s ease 3;
}

@keyframes msgFlash {
  0%,
  100% {
    background: transparent;
  }
  50% {
    background: rgba(64, 158, 255, .22);
  }
}
</style>

<!-- 消息右键菜单挂载到 body，需使用非 scoped 样式 -->
<style lang="scss">
.msg-context-menu {
  position: fixed;
  z-index: 999999;
  min-width: size(96);
  padding: size(4) 0;
  background: #fff;
  border-radius: size(6);
  box-shadow: 0 4px 16px rgba(0, 0, 0, .16);
  font-size: size(14);
  color: #1f1f1f;

  &__item {
    padding: size(8) size(16);
    line-height: 1.4;
    white-space: nowrap;
    cursor: pointer;
    user-select: none;

    &:hover {
      background: #f2f2f2;
    }
  }
}
</style>
