<script setup lang="ts">
import {onMounted, onUnmounted, ref, watch} from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/user.ts'
import useChatStore from '@/store/chat.ts'
import { getConversation } from '@/api/chat.ts'
import { decodeBase64FromJava, extractMessageContent } from '@/utils/wk.ts'
import { MessageType } from '@/services/im'
import type { Contact } from '@/store/chat.ts'
import {isImageUrl} from "@/utils/utils.ts";

const userStore = useUserStore()
const chatStore = useChatStore()
const { contacts, isRefreshConversations } = storeToRefs(chatStore)

/** 每页会话数量 */
const PAGE_SIZE = 20
/** 距底部多少像素触发加载下一页 */
const LOAD_MORE_THRESHOLD = 60

const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(0)
const listRef = ref<HTMLDivElement | null>(null)

interface ApiConversationItem {
  avatar?: string
  name: string
  recents?: { payload?: string }[]
  channel_id: string
  channel_type: number
  unread?: number
  timestamp?: number
  /** 是否置顶 1=置顶 0=未置顶 */
  top?: number
}

/** 会话右键菜单 */
const contactMenu = ref<{
  visible: boolean
  x: number
  y: number
  contact: Contact | null
}>({ visible: false, x: 0, y: 0, contact: null })

/** 打开会话右键菜单 */
const openContactMenu = (event: MouseEvent, contact: Contact) => {
  contactMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    contact,
  }
}

/** 关闭会话右键菜单 */
const closeContactMenu = () => {
  contactMenu.value.visible = false
  contactMenu.value.contact = null
}

/** 置顶/取消置顶会话 */
const handleToggleTop = async () => {
  const contact = contactMenu.value.contact
  closeContactMenu()

  if (!contact) {
    return
  }

  const success = await chatStore.setContactTop(contact, !contact.top)

  if (!success) {
    ElMessage.error('操作失败，请重试')
  }
}

/** Esc 关闭右键菜单 */
const onMenuKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    closeContactMenu()
  }
}

watch(() => isRefreshConversations.value, () => {
  if (isRefreshConversations.value) {
    fetchConversations(true)
    isRefreshConversations.value = false
  }
}, {deep: true})

/**
 * 从后端分页获取会话列表并同步到 chat store
 *
 * @param reset 是否重置分页（刷新场景从第一页重新拉取）
 */
const fetchConversations = async (reset = false) => {
  if (reset) {
    page.value = 0
    hasMore.value = true
  }

  if (!hasMore.value || loadingMore.value || loading.value) {
    return
  }

  const nextPage = page.value + 1
  const isFirstPage = nextPage === 1

  if (isFirstPage) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const response = await getConversation({ msgCount: 1, page: nextPage, pageSize: PAGE_SIZE })
    if (response.code === 200) {
      const list = response.data || []
      page.value = nextPage
      if (list.length < PAGE_SIZE) {
        hasMore.value = false
      }
      syncToChatStore(list)
    }
  } catch {
    ElMessage.error('获取会话列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/** 滚动到底部附近时加载下一页 */
const handleListScroll = () => {
  const el = listRef.value

  if (!el || loading.value || loadingMore.value || !hasMore.value) {
    return
  }

  if (el.scrollHeight - el.scrollTop - el.clientHeight <= LOAD_MORE_THRESHOLD) {
    fetchConversations()
  }
}

/**
 * 将后端会话列表数据合并到 chat store
 */
const syncToChatStore = (list: ApiConversationItem[]) => {
  for (const item of list) {
    const exists = contacts.value.find(
      (c) => c.uid === item.channel_id && c.channelType === item.channel_type,
    )

    const lastMessage = resolveLatestMessageContent(item.recents)

    if (exists) {
      exists.name = item.name || exists.name
      exists.avatar = item.avatar || exists.avatar
      exists.lastMessage = lastMessage || exists.lastMessage
      if (item.timestamp) {
        exists.lastMessageTime = item.timestamp * 1000
      }
      if (item.top !== undefined) {
        exists.top = item.top
      }
    } else {
      chatStore.contacts.push({
        uid: item.channel_id,
        name: item.name,
        type: item.channel_type === 2 ? 'group' : 'person',
        channelType: item.channel_type as 1 | 2,
        unreadCount: item.unread || 0,
        lastMessage,
        lastMessageTime: item.timestamp ? item.timestamp * 1000 : 0,
        avatar: item.avatar,
        top: item.top,
      })
    }
  }

  chatStore.sortContacts()
}

/**
 * 解析最新消息内容
 */
const resolveLatestMessageContent = (recents?: { payload?: string }[]): string => {
  if (!recents || recents.length === 0) {
    return ''
  }

  try {
    const decoded = decodeBase64FromJava(recents[0].payload || '')
    const payload = JSON.parse(decoded)

    // 撤回信令的 content 是对象结构，不能直接当消息内容展示
    if (payload.type === MessageType.RECALL) {
      return '消息已撤回'
    }

    return extractMessageContent(payload)
  } catch {
    return ''
  }
}

/**
 * 选择频道
 */
const handleSelectChannel = (row: Contact) => {
  console.log('selected channel', row)
  userStore.currentChannel = {
    channelId: row.uid,
    type: row.channelType,
  }
  chatStore.selectContact(row)
}

/**
 * 显示消息信息
 * */
const showMessage = (message: string) => {
  if (isImageUrl(message)) {
    return '[图片消息]'
  } else {
    return message
  }
}

onMounted(() => {
  fetchConversations(true)
  // 点击空白处 / 滚动 / Esc 关闭右键菜单
  document.addEventListener('click', closeContactMenu)
  document.addEventListener('scroll', closeContactMenu, true)
  window.addEventListener('keydown', onMenuKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContactMenu)
  document.removeEventListener('scroll', closeContactMenu, true)
  window.removeEventListener('keydown', onMenuKeydown)
})
</script>

<template>
  <div
    ref="listRef"
    v-loading="loading"
    class="contact-list"
    @scroll.passive="handleListScroll"
  >
    <div
      v-for="(item, index) in contacts"
      :key="index"
      class="msg-item"
      :class="{ active: userStore.currentChannel?.channelId === item.uid, pinned: !!item.top }"
      @click="handleSelectChannel(item)"
      @contextmenu.prevent="openContactMenu($event, item)"
    >
      <div class="avatar-wrap">
        <el-image class="avatar-box" :src="item.avatar">
          <template #error>
            <div>
              <img v-if="item.channelType === 1" class="avatar-box" src="@/assets/images/logo.jpg" alt="avatar" />
              <img v-else class="avatar-box" src="@/assets/images/default_group_avatar.png" alt="avatar" />
            </div>
          </template>
        </el-image>
      </div>
      <div class="msg-content">
        <div class="msg-title">
          <span>{{ item.name }}</span>
        </div>
        <span class="msg-text">{{ showMessage(item.lastMessage || '') }}</span>
      </div>
    </div>

    <div v-if="loadingMore" class="list-loading">加载中...</div>
  </div>

  <!-- 会话右键菜单：置顶 / 取消置顶 -->
  <teleport to="body">
    <div
      v-if="contactMenu.visible"
      class="contact-context-menu"
      :style="{ left: `${contactMenu.x}px`, top: `${contactMenu.y}px` }"
    >
      <div class="contact-context-menu__item" @click="handleToggleTop">
        {{ contactMenu.contact?.top ? '取消置顶' : '置顶该聊天' }}
      </div>
    </div>
  </teleport>
</template>

<style lang="scss" scoped>
.contact-list {
  width: 100%;
  flex: 1;
  box-sizing: border-box;
  padding: size(14);
  overflow: hidden;
  overflow-y: scroll;

  .list-loading {
    padding: size(10) 0;
    font-size: size(12);
    color: #999;
    text-align: center;
  }

  .msg-item {
    background: #fff;
    display: flex;
    align-items: center;
    margin-bottom: size(13);
    border-radius: size(10);
    box-shadow: rgba(50, 50, 93, 0.15) 0 2px 5px -1px,
                rgba(0, 0, 0, 0.2) 0 1px 3px -1px;
    box-sizing: border-box;
    cursor: pointer;
    padding: size(14);

    // 置顶会话：淡灰底区分（微信风格）
    &.pinned {
      background: #e9ebee;
    }

    &:hover,
    &.active {
      background: $theme-color;
      .msg-title {
        color: #fff !important;
      }
      .msg-time {
        color: #fff !important;
      }
      .msg-text {
        color: #fff !important;
      }
    }
  }

  .avatar-wrap {
    width: size(64);
    height: size(64);
    position: relative;
  }

  .avatar-box {
    width: 100%;
    height: 100%;
    border-radius: size(13);
  }

  .unread-badge {
    position: absolute;
    top: size(-6);
    right: size(-6);
    min-width: size(24);
    height: size(24);
    line-height: size(24);
    padding: 0 size(6);
    background: #f56c6c;
    color: #fff;
    border-radius: size(12);
    font-size: size(12);
    text-align: center;
  }

  .msg-content {
    flex: 1;
    height: 100%;
    display: flex;
    flex-direction: column;
    margin-left: size(12);

    .msg-title {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: size(18);

      span {
        font-size: size(17);
        font-weight: 500;
        letter-spacing: 1px;
        @include textEllipsis(1);
      }

      .msg-time {
        font-size: size(12);
        color: #999;
        font-weight: normal;
      }
    }

    .msg-text {
      font-size: size(12);
      color: #bfbfbf;
      letter-spacing: 1px;
      @include textEllipsis(1);
    }
  }

  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}

// 会话右键菜单（teleport 到 body，故写在外层）
.contact-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: size(150);
  padding: size(6) 0;
  background: #fff;
  border-radius: size(8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, .16);
  user-select: none;

  &__item {
    padding: size(10) size(18);
    font-size: size(14);
    color: #333;
    cursor: pointer;

    &:hover {
      background: #f2f3f5;
    }
  }
}
</style>
